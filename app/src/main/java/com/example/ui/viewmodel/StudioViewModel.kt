package com.example.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.StudioAdminPolicy
import com.example.data.firebase.StudioRepository
import com.example.data.model.CustomerOrder
import com.example.data.model.DefaultStudioCatalog
import com.example.data.model.OrderStatus
import com.example.data.model.ServiceCategory
import com.example.data.model.StudioConfig
import com.example.data.model.StudioService
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

data class OrderDraft(
  val serviceId: String = DefaultStudioCatalog.services.first().id,
  val serviceName: String = DefaultStudioCatalog.services.first().name,
  val category: ServiceCategory = ServiceCategory.PRINT,
  val selectedSize: String = DefaultStudioCatalog.services.first().size,
  val selectedFrameType: String = "",
  val selectedFrameDesign: String = "",
  val quantity: Long = 1L,
  val unitPrice: Long = DefaultStudioCatalog.services.first().price,
  val unitLabel: String = DefaultStudioCatalog.services.first().unitLabel,
  val selectedPhotoUris: List<Uri> = emptyList(),
  val selectedPhotoNames: List<String> = emptyList(),
  val customerName: String = "",
  val customerPhone: String = "",
  val customerNotes: String = "",
  val isSubmitting: Boolean = false,
  val submittedOrder: CustomerOrder? = null,
  val validationError: String? = null,
) {
  val totalAmount: Long get() = unitPrice * quantity
}

data class AuthSession(
  val user: FirebaseUser? = null,
  val isAdmin: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class StudioViewModel(
  private val repository: StudioRepository,
  private val auth: FirebaseAuth = Firebase.auth,
) : ViewModel() {

  private fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
      trySend(firebaseAuth.currentUser)
    }
    auth.addAuthStateListener(listener)
    awaitClose { auth.removeAuthStateListener(listener) }
  }

  val authSession: StateFlow<AuthSession> =
    authStateFlow()
      .flatMapLatest { user ->
        if (user == null) {
          flowOf(AuthSession(user = null, isAdmin = false))
        } else {
          val tokenAdmin = StudioAdminPolicy.isAuthorizedAdmin(user, hasAdminDocument = false)
          if (tokenAdmin) {
            flowOf(AuthSession(user = user, isAdmin = true))
          } else {
            kotlinx.coroutines.flow.flow {
              val hasDoc = repository.checkAdminDocumentExists(user.uid)
              emit(AuthSession(user = user, isAdmin = StudioAdminPolicy.isAuthorizedAdmin(user, hasDoc)))
            }
          }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = AuthSession(
          user = auth.currentUser,
          isAdmin = StudioAdminPolicy.isAuthorizedAdmin(auth.currentUser),
        ),
      )

  private val _cloudCatalogEmpty = MutableStateFlow(false)
  val cloudCatalogEmpty: StateFlow<Boolean> = _cloudCatalogEmpty.asStateFlow()

  val publicServicesState: StateFlow<UiState<List<StudioService>>> =
    repository
      .observePublishedServices()
      .map<List<StudioService>, UiState<List<StudioService>>> { list ->
        _cloudCatalogEmpty.value = list.isEmpty()
        UiState.Success(if (list.isEmpty()) DefaultStudioCatalog.services else list)
      }
      .catch { error ->
        val firestoreError = unwrapFirestoreError(error)
        if (firestoreError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing published services", firestoreError ?: error)
        emit(UiState.Success(DefaultStudioCatalog.services))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val studioConfig: StateFlow<StudioConfig> =
    repository
      .observeStudioConfig()
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DefaultStudioCatalog.defaultConfig,
      )

  // Admin-only streams: strictly gated behind authSession.isAdmin == true
  val adminOrdersState: StateFlow<UiState<List<CustomerOrder>>> =
    authSession
      .flatMapLatest { session ->
        if (!session.isAdmin || session.user == null) {
          flowOf(UiState.Loading)
        } else {
          repository
            .observeAllOrdersForAdmin()
            .map<List<CustomerOrder>, UiState<List<CustomerOrder>>> { UiState.Success(it) }
            .catch { error ->
              val firestoreError = unwrapFirestoreError(error)
              if (firestoreError == null && error is CancellationException) throw error
              val root = firestoreError ?: error
              emit(UiState.Error(root.message ?: "Unable to load customer orders."))
            }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val adminServicesState: StateFlow<UiState<List<StudioService>>> =
    authSession
      .flatMapLatest { session ->
        if (!session.isAdmin || session.user == null) {
          flowOf(UiState.Loading)
        } else {
          repository
            .observeAllServicesForAdmin()
            .map<List<StudioService>, UiState<List<StudioService>>> { UiState.Success(it) }
            .catch { error ->
              val firestoreError = unwrapFirestoreError(error)
              if (firestoreError == null && error is CancellationException) throw error
              val root = firestoreError ?: error
              emit(UiState.Error(root.message ?: "Unable to load studio services."))
            }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  private val _orderDraft = MutableStateFlow(OrderDraft())
  val orderDraft: StateFlow<OrderDraft> = _orderDraft.asStateFlow()

  private val _feedbackMessage = MutableStateFlow<String?>(null)
  val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

  private val _adminActionBusy = MutableStateFlow(false)
  val adminActionBusy: StateFlow<Boolean> = _adminActionBusy.asStateFlow()

  fun clearFeedbackMessage() {
    _feedbackMessage.value = null
  }

  fun selectServiceForOrder(service: StudioService) {
    _orderDraft.update { current ->
      current.copy(
        serviceId = service.id.ifBlank { "custom_service" },
        serviceName = service.name,
        category = service.categoryEnum(),
        selectedSize = service.size,
        selectedFrameType = if (service.categoryEnum() == ServiceCategory.FRAME) {
          service.frameType.ifBlank { DefaultStudioCatalog.frameTypes.first() }
        } else "",
        selectedFrameDesign = if (service.categoryEnum() == ServiceCategory.FRAME) {
          service.frameDesign.ifBlank { DefaultStudioCatalog.frameDesigns.first() }
        } else "",
        quantity = 1L,
        unitPrice = service.price,
        unitLabel = service.unitLabel,
        submittedOrder = null,
        validationError = null,
      )
    }
  }

  fun startCustomOrder(category: ServiceCategory) {
    val (name, size, price) = when (category) {
      ServiceCategory.PRINT -> Triple("Custom Photo Printing", DefaultStudioCatalog.customPrintSizes.first(), 80L)
      ServiceCategory.FRAME -> Triple("Custom Photo Frame Order", DefaultStudioCatalog.customFrameSizes[1], 550L)
      ServiceCategory.PACKAGE -> Triple("Custom Event Photography", "Custom Coverage", 12000L)
    }
    _orderDraft.update { current ->
      current.copy(
        serviceId = "custom_${category.name.lowercase()}",
        serviceName = name,
        category = category,
        selectedSize = size,
        selectedFrameType = if (category == ServiceCategory.FRAME) DefaultStudioCatalog.frameTypes.first() else "",
        selectedFrameDesign = if (category == ServiceCategory.FRAME) DefaultStudioCatalog.frameDesigns.first() else "",
        quantity = 1L,
        unitPrice = price,
        unitLabel = if (category == ServiceCategory.FRAME) "per frame" else "per unit",
        submittedOrder = null,
        validationError = null,
      )
    }
  }

  fun updateOrderQuantity(newQuantity: Long) {
    val clamped = newQuantity.coerceIn(1L, 10_000L)
    _orderDraft.update { it.copy(quantity = clamped, validationError = null) }
  }

  fun updateOrderSize(size: String) {
    _orderDraft.update { it.copy(selectedSize = size, validationError = null) }
  }

  fun updateOrderFrameType(frameType: String) {
    _orderDraft.update { it.copy(selectedFrameType = frameType, validationError = null) }
  }

  fun updateOrderFrameDesign(frameDesign: String) {
    _orderDraft.update { it.copy(selectedFrameDesign = frameDesign, validationError = null) }
  }

  fun updateCustomerName(name: String) {
    _orderDraft.update { it.copy(customerName = name, validationError = null) }
  }

  fun updateCustomerPhone(phone: String) {
    _orderDraft.update { it.copy(customerPhone = phone, validationError = null) }
  }

  fun updateCustomerNotes(notes: String) {
    _orderDraft.update { it.copy(customerNotes = notes, validationError = null) }
  }

  fun addSelectedPhotos(uris: List<Uri>, names: List<String>) {
    _orderDraft.update { current ->
      val combinedUris = (current.selectedPhotoUris + uris).distinct().take(25)
      val combinedNames = (current.selectedPhotoNames + names).take(25)
      current.copy(
        selectedPhotoUris = combinedUris,
        selectedPhotoNames = combinedNames,
        validationError = null,
      )
    }
  }

  fun removeSelectedPhoto(index: Int) {
    _orderDraft.update { current ->
      if (index !in current.selectedPhotoUris.indices) return@update current
      val uris = current.selectedPhotoUris.toMutableList().also { it.removeAt(index) }
      val names = current.selectedPhotoNames.toMutableList().also {
        if (index in it.indices) it.removeAt(index)
      }
      current.copy(selectedPhotoUris = uris, selectedPhotoNames = names)
    }
  }

  fun clearSubmittedOrder() {
    _orderDraft.update { it.copy(submittedOrder = null) }
  }

  fun submitCurrentOrder() {
    val draft = _orderDraft.value
    val name = draft.customerName.trim()
    val phone = draft.customerPhone.trim()

    if (name.length < 2) {
      _orderDraft.update { it.copy(validationError = "Please enter your full name (at least 2 characters).") }
      return
    }
    if (phone.filter { it.isDigit() }.length < 7) {
      _orderDraft.update { it.copy(validationError = "Please enter a valid 10-digit mobile number.") }
      return
    }
    if (draft.quantity < 1L) {
      _orderDraft.update { it.copy(validationError = "Quantity must be at least 1.") }
      return
    }

    viewModelScope.launch {
      _orderDraft.update { it.copy(isSubmitting = true, validationError = null) }
      val photoSummary = if (draft.selectedPhotoNames.isEmpty()) {
        "No local photos attached"
      } else {
        draft.selectedPhotoNames.take(5).joinToString(", ").take(500)
      }
      val order = CustomerOrder(
        serviceId = draft.serviceId,
        serviceName = draft.serviceName,
        category = draft.category.name,
        selectedSize = draft.selectedSize,
        selectedFrameType = draft.selectedFrameType,
        selectedFrameDesign = draft.selectedFrameDesign,
        quantity = draft.quantity,
        unitPrice = draft.unitPrice,
        totalAmount = draft.totalAmount,
        customerName = name,
        customerPhone = phone,
        customerNotes = draft.customerNotes.trim(),
        photoCount = draft.selectedPhotoUris.size.toLong(),
        photoSummary = photoSummary,
      )
      val result = repository.submitOrder(order)
      result
        .onSuccess { savedOrder ->
          _orderDraft.update {
            it.copy(
              isSubmitting = false,
              submittedOrder = savedOrder,
              validationError = null,
            )
          }
          _feedbackMessage.value = "Order ${savedOrder.orderCode} submitted!"
        }
        .onFailure { error ->
          _orderDraft.update {
            it.copy(
              isSubmitting = false,
              validationError = error.localizedMessage ?: "Failed to submit order. Please try again.",
            )
          }
        }
    }
  }

  // --- ADMIN ACTIONS ---

  fun seedCatalogToCloud() {
    viewModelScope.launch {
      _adminActionBusy.value = true
      repository.seedDefaultCatalogToCloud()
        .onSuccess { count ->
          _feedbackMessage.value = if (count > 0) {
            "Synced $count Studio Pixy services & settings to Firestore."
          } else {
            "Cloud catalog is already initialized."
          }
        }
        .onFailure { error ->
          _feedbackMessage.value = error.localizedMessage ?: "Failed to sync catalog."
        }
      _adminActionBusy.value = false
    }
  }

  fun saveService(service: StudioService) {
    viewModelScope.launch {
      _adminActionBusy.value = true
      repository.upsertService(service)
        .onSuccess {
          _feedbackMessage.value = "Saved \"${service.name}\"."
        }
        .onFailure { error ->
          _feedbackMessage.value = error.localizedMessage ?: "Unable to save service."
        }
      _adminActionBusy.value = false
    }
  }

  fun deleteService(service: StudioService) {
    viewModelScope.launch {
      _adminActionBusy.value = true
      repository.deleteService(service.id)
        .onSuccess {
          _feedbackMessage.value = "Deleted \"${service.name}\"."
        }
        .onFailure { error ->
          _feedbackMessage.value = error.localizedMessage ?: "Unable to delete service."
        }
      _adminActionBusy.value = false
    }
  }

  fun updateOrderStatus(
    order: CustomerOrder,
    newStatus: OrderStatus,
    remarks: String,
    unitPriceOverride: Long? = null,
  ) {
    viewModelScope.launch {
      _adminActionBusy.value = true
      repository.updateOrderStatus(
        orderId = order.id,
        newStatus = newStatus,
        adminRemarks = remarks,
        unitPrice = unitPriceOverride,
        quantity = order.quantity,
      )
        .onSuccess {
          _feedbackMessage.value = "Order ${order.orderCode} marked ${newStatus.label}."
        }
        .onFailure { error ->
          _feedbackMessage.value = error.localizedMessage ?: "Failed to update order status."
        }
      _adminActionBusy.value = false
    }
  }

  fun deleteOrder(order: CustomerOrder) {
    viewModelScope.launch {
      _adminActionBusy.value = true
      repository.deleteOrder(order.id)
        .onSuccess {
          _feedbackMessage.value = "Order ${order.orderCode} removed."
        }
        .onFailure { error ->
          _feedbackMessage.value = error.localizedMessage ?: "Unable to remove order."
        }
      _adminActionBusy.value = false
    }
  }

  fun saveStudioConfig(config: StudioConfig) {
    viewModelScope.launch {
      _adminActionBusy.value = true
      repository.updateStudioConfig(config)
        .onSuccess {
          _feedbackMessage.value = "Studio Pixy business info & WhatsApp updated."
        }
        .onFailure { error ->
          _feedbackMessage.value = error.localizedMessage ?: "Unable to save studio settings."
        }
      _adminActionBusy.value = false
    }
  }

  private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
    generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()

  companion object {
    private const val TAG = "StudioViewModel"
    private const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
