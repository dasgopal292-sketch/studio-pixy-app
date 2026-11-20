package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.CustomerOrder
import com.example.data.model.DefaultStudioCatalog
import com.example.data.model.OrderStatus
import com.example.data.model.StudioConfig
import com.example.data.model.StudioService
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StudioRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireSignedInUid(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("Admin must sign in with Google before modifying studio data.")
  }

  fun observePublishedServices(): Flow<List<StudioService>> =
    db.collection(COLLECTION_SERVICES)
      .whereEqualTo("published", true)
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(StudioService::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedWith(compareBy<StudioService> { it.sortOrder }.thenBy { it.name })
      }
      .catch { error ->
        val firestoreError = unwrapFirestoreError(error)
        if (firestoreError == null && error is CancellationException) throw error
        handleFirestoreError(firestoreError ?: error, OperationType.LIST, COLLECTION_SERVICES)
        throw (firestoreError ?: error)
      }

  fun observeAllServicesForAdmin(): Flow<List<StudioService>> = flow {
    val uid = requireSignedInUid()
    emitAll(
      db.collection(COLLECTION_SERVICES)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(StudioService::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedWith(compareBy<StudioService> { it.category }.thenBy { it.sortOrder }.thenBy { it.name })
        }
        .catch { error ->
          val firestoreError = unwrapFirestoreError(error)
          if (firestoreError == null && error is CancellationException) throw error
          if (auth.currentUser?.uid == uid) {
            handleFirestoreError(firestoreError ?: error, OperationType.LIST, COLLECTION_SERVICES)
          }
          throw (firestoreError ?: error)
        }
    )
  }

  fun observeStudioConfig(): Flow<StudioConfig> =
    db.collection(COLLECTION_CONFIG)
      .document(DOC_CONFIG_MAIN)
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(StudioConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?: DefaultStudioCatalog.defaultConfig
        } else {
          DefaultStudioCatalog.defaultConfig
        }
      }
      .catch { error ->
        val firestoreError = unwrapFirestoreError(error)
        if (firestoreError == null && error is CancellationException) throw error
        handleFirestoreError(firestoreError ?: error, OperationType.GET, "$COLLECTION_CONFIG/$DOC_CONFIG_MAIN")
        emit(DefaultStudioCatalog.defaultConfig)
      }

  fun observeAllOrdersForAdmin(): Flow<List<CustomerOrder>> = flow {
    val uid = requireSignedInUid()
    emitAll(
      db.collection(COLLECTION_ORDERS)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(CustomerOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          val firestoreError = unwrapFirestoreError(error)
          if (firestoreError == null && error is CancellationException) throw error
          if (auth.currentUser?.uid == uid) {
            handleFirestoreError(firestoreError ?: error, OperationType.LIST, COLLECTION_ORDERS)
          }
          throw (firestoreError ?: error)
        }
    )
  }

  fun observeMyOrders(customerUid: String): Flow<List<CustomerOrder>> = flow {
    require(customerUid.isNotBlank()) { "Customer UID must not be blank" }
    val activeUid = requireSignedInUid()
    check(activeUid == customerUid) { "Active user changed; refusing cross-user query." }
    emitAll(
      db.collection(COLLECTION_ORDERS)
        .whereEqualTo("customerUid", customerUid)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(CustomerOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          val firestoreError = unwrapFirestoreError(error)
          if (firestoreError == null && error is CancellationException) throw error
          if (auth.currentUser?.uid == customerUid) {
            handleFirestoreError(firestoreError ?: error, OperationType.LIST, COLLECTION_ORDERS)
          }
          throw (firestoreError ?: error)
        }
    )
  }

  suspend fun checkAdminDocumentExists(uid: String): Boolean {
    if (uid.isBlank()) return false
    return try {
      val doc = db.collection(COLLECTION_ADMINS).document(uid).get().await()
      doc.exists()
    } catch (e: CancellationException) {
      throw e
    } catch (_: Exception) {
      false
    }
  }

  suspend fun submitOrder(order: CustomerOrder): Result<CustomerOrder> {
    var docPath = COLLECTION_ORDERS
    return try {
      require(order.serviceName.trim().length in 2..120) { "Service name must be 2–120 characters." }
      require(order.customerName.trim().length in 2..80) { "Please enter your full name (2–80 characters)." }
      require(order.customerPhone.trim().length in 7..20) { "Please enter a valid mobile number (7–20 digits)." }
      require(order.quantity in 1L..10_000L) { "Quantity must be between 1 and 10,000." }
      require(order.unitPrice in 0L..10_000_000L) { "Invalid unit price." }

      val docRef = db.collection(COLLECTION_ORDERS).document()
      docPath = docRef.path
      val generatedCode = order.orderCode.ifBlank {
        "PX-${(100000..999999).random()}"
      }
      val attachedUid = auth.currentUser?.uid ?: ""
      val normalized = order.copy(
        id = docRef.id,
        orderCode = generatedCode,
        totalAmount = order.unitPrice * order.quantity,
        status = OrderStatus.PENDING.name,
        customerUid = attachedUid,
        adminRemarks = "",
      )
      docRef.set(normalized.toCreateMap()).await()
      Result.success(normalized)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun upsertService(service: StudioService): Result<Unit> {
    var docPath = COLLECTION_SERVICES
    return try {
      requireSignedInUid()
      require(service.name.trim().length in 2..100) { "Service name must be 2–100 characters." }
      require(service.price in 0L..10_000_000L) { "Price must be between ₹0 and ₹1,00,00,000." }
      require(service.unitLabel.trim().isNotBlank()) { "Unit label cannot be blank." }

      val isUpdate = service.id.isNotBlank() && service.createdAt != null
      if (isUpdate) {
        val docRef = db.collection(COLLECTION_SERVICES).document(service.id)
        docPath = docRef.path
        docRef.update(service.toUpdateMap()).await()
      } else {
        val docRef = if (service.id.isNotBlank() && VALID_ID_REGEX.matches(service.id)) {
          db.collection(COLLECTION_SERVICES).document(service.id)
        } else {
          db.collection(COLLECTION_SERVICES).document()
        }
        docPath = docRef.path
        docRef.set(service.toCreateMap()).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
  }

  suspend fun deleteService(serviceId: String): Result<Unit> {
    val docPath = "$COLLECTION_SERVICES/$serviceId"
    return try {
      requireSignedInUid()
      require(serviceId.isNotBlank() && VALID_ID_REGEX.matches(serviceId)) { "Invalid service ID." }
      db.collection(COLLECTION_SERVICES).document(serviceId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  suspend fun updateOrderStatus(
    orderId: String,
    newStatus: OrderStatus,
    adminRemarks: String,
    unitPrice: Long? = null,
    quantity: Long? = null,
  ): Result<Unit> {
    val docPath = "$COLLECTION_ORDERS/$orderId"
    return try {
      requireSignedInUid()
      require(orderId.isNotBlank() && VALID_ID_REGEX.matches(orderId)) { "Invalid order ID." }
      val updates = mutableMapOf<String, Any>(
        "status" to newStatus.name,
        "adminRemarks" to adminRemarks.trim().take(500),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      if (unitPrice != null && quantity != null && quantity >= 1L && unitPrice >= 0L) {
        updates["unitPrice"] = unitPrice
        updates["totalAmount"] = unitPrice * quantity
      }
      db.collection(COLLECTION_ORDERS).document(orderId).update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun deleteOrder(orderId: String): Result<Unit> {
    val docPath = "$COLLECTION_ORDERS/$orderId"
    return try {
      requireSignedInUid()
      require(orderId.isNotBlank() && VALID_ID_REGEX.matches(orderId)) { "Invalid order ID." }
      db.collection(COLLECTION_ORDERS).document(orderId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  suspend fun updateStudioConfig(config: StudioConfig): Result<Unit> {
    val docPath = "$COLLECTION_CONFIG/$DOC_CONFIG_MAIN"
    return try {
      requireSignedInUid()
      require(config.businessName.trim().length >= 2) { "Business name is required." }
      require(config.whatsappNumber.filter { it.isDigit() }.length in 8..20) {
        "WhatsApp number must be 8–20 digits including country code (e.g. 919876543210)."
      }
      db.collection(COLLECTION_CONFIG).document(DOC_CONFIG_MAIN).set(config.toSaveMap()).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
  }

  suspend fun seedDefaultCatalogToCloud(): Result<Int> {
    return try {
      requireSignedInUid()
      val existingServices = db.collection(COLLECTION_SERVICES).limit(1).get().await()
      var seededCount = 0
      if (existingServices.isEmpty) {
        val batch = db.batch()
        DefaultStudioCatalog.services.forEach { svc ->
          val ref = db.collection(COLLECTION_SERVICES).document(svc.id)
          batch.set(ref, svc.toCreateMap())
          seededCount++
        }
        val configRef = db.collection(COLLECTION_CONFIG).document(DOC_CONFIG_MAIN)
        val existingConfig = configRef.get().await()
        if (!existingConfig.exists()) {
          batch.set(configRef, DefaultStudioCatalog.defaultConfig.toSaveMap())
        }
        batch.commit().await()
      }
      Result.success(seededCount)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, COLLECTION_SERVICES)
      Result.failure(e)
    }
  }

  private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
    generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()

  companion object {
    const val COLLECTION_SERVICES = "services"
    const val COLLECTION_CONFIG = "studio_config"
    const val DOC_CONFIG_MAIN = "main"
    const val COLLECTION_ORDERS = "orders"
    const val COLLECTION_ADMINS = "admins"
    private val VALID_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]+$")
  }
}
