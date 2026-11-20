package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.auth.GoogleSignInButton
import com.example.auth.StudioAdminPolicy
import com.example.auth.signOutFromApp
import com.example.data.model.CustomerOrder
import com.example.data.model.OrderStatus
import com.example.data.model.ServiceCategory
import com.example.data.model.StudioConfig
import com.example.data.model.StudioService
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.ServiceImageThumbnail
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.viewmodel.AuthSession
import com.example.ui.viewmodel.UiState
import com.example.util.dialPhoneNumber
import com.example.util.formatInr
import com.example.util.openWhatsAppChat

@Composable
fun AdminScreen(
  authSession: AuthSession,
  ordersState: UiState<List<CustomerOrder>>,
  servicesState: UiState<List<StudioService>>,
  studioConfig: StudioConfig,
  cloudCatalogEmpty: Boolean,
  isBusy: Boolean,
  onSeedCatalog: () -> Unit,
  onSaveService: (StudioService) -> Unit,
  onDeleteService: (StudioService) -> Unit,
  onUpdateOrderStatus: (CustomerOrder, OrderStatus, String, Long?) -> Unit,
  onDeleteOrder: (CustomerOrder) -> Unit,
  onSaveConfig: (StudioConfig) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var authError by remember { mutableStateOf<String?>(null) }

  if (authSession.user == null) {
    AdminSignInGate(
      authError = authError,
      onAuthSuccess = { authError = null },
      onAuthError = { authError = it },
      modifier = modifier,
    )
    return
  }

  if (!authSession.isAdmin) {
    UnauthorizedAdminCard(
      signedInEmail = authSession.user.email ?: "Unknown account",
      onSignOut = {
        signOutFromApp(credentialManager, scope)
      },
      modifier = modifier,
    )
    return
  }

  val ordersList = (ordersState as? UiState.Success)?.data ?: emptyList()
  val servicesList = (servicesState as? UiState.Success)?.data ?: emptyList()

  var selectedTab by remember { mutableIntStateOf(0) }
  var orderFilter by remember { mutableStateOf<OrderStatus?>(null) }
  var serviceCategoryFilter by remember { mutableStateOf<ServiceCategory?>(null) }
  var editingService by remember { mutableStateOf<StudioService?>(null) }
  var showCreateServiceDialog by remember { mutableStateOf(false) }
  var updatingOrder by remember { mutableStateOf<CustomerOrder?>(null) }

  if (showCreateServiceDialog || editingService != null) {
    ServiceEditorDialog(
      initialService = editingService,
      onDismiss = {
        showCreateServiceDialog = false
        editingService = null
      },
      onSave = { svc ->
        onSaveService(svc)
        showCreateServiceDialog = false
        editingService = null
      },
    )
  }

  if (updatingOrder != null) {
    OrderStatusUpdateDialog(
      order = updatingOrder!!,
      onDismiss = { updatingOrder = null },
      onConfirm = { newStatus, remarks, unitOverride ->
        onUpdateOrderStatus(updatingOrder!!, newStatus, remarks, unitOverride)
        updatingOrder = null
      },
    )
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Admin Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Admin Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "Signed in: ${authSession.user.email ?: "Admin"}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        OutlinedButton(
          onClick = { signOutFromApp(credentialManager, scope) },
          modifier = Modifier.testTag("admin_sign_out_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Sign Out")
        }
      }
    }

    // Seed Starter Catalog prompt if Firestore services collection is empty
    if (cloudCatalogEmpty) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Initialize Cloud Catalog",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Text(
                text = "Publish the 18 default Studio Pixy prints, frames & packages to Firestore for live editing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = onSeedCatalog,
              enabled = !isBusy,
              modifier = Modifier.testTag("admin_seed_catalog_button"),
            ) {
              Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Sync")
            }
          }
        }
      }
    }

    // KPI Cards
    item {
      val totalOrders = ordersList.size
      val pendingOrders = ordersList.count { it.statusEnum() == OrderStatus.PENDING }
      val completedOrders = ordersList.count { it.statusEnum() == OrderStatus.COMPLETED }
      val totalServices = servicesList.size

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          KpiCard("Total Orders", totalOrders.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
          KpiCard("Pending Orders", pendingOrders.toString(), StatusWarning, Modifier.weight(1f))
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          KpiCard("Completed Orders", completedOrders.toString(), StatusSuccess, Modifier.weight(1f))
          KpiCard("Total Services", totalServices.toString(), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
        }
      }
    }

    // Admin Sub-Tabs
    item {
      TabRow(selectedTabIndex = selectedTab) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text("Orders (${ordersList.size})") },
          modifier = Modifier.testTag("admin_tab_orders"),
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("Services") },
          modifier = Modifier.testTag("admin_tab_services"),
        )
        Tab(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          text = { Text("Studio Info") },
          modifier = Modifier.testTag("admin_tab_studio"),
        )
      }
    }

    when (selectedTab) {
      0 -> {
        item {
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
              FilterChip(
                selected = orderFilter == null,
                onClick = { orderFilter = null },
                label = { Text("All") },
              )
            }
            items(OrderStatus.entries) { status ->
              FilterChip(
                selected = orderFilter == status,
                onClick = { orderFilter = status },
                label = { Text(status.label) },
              )
            }
          }
        }
        when (ordersState) {
          is UiState.Loading -> item {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
              CircularProgressIndicator()
            }
          }
          is UiState.Error -> item {
            Text(ordersState.message, color = StatusError)
          }
          is UiState.Success -> {
            val filteredOrders = if (orderFilter == null) ordersList else ordersList.filter { it.statusEnum() == orderFilter }
            if (filteredOrders.isEmpty()) {
              item {
                EmptyAdminState("No customer orders in this filter yet.")
              }
            } else {
              items(filteredOrders, key = { it.id }) { order ->
                AdminOrderCard(
                  order = order,
                  onAccept = { onUpdateOrderStatus(order, OrderStatus.ACCEPTED, "Order accepted by Studio Pixy", null) },
                  onComplete = { onUpdateOrderStatus(order, OrderStatus.COMPLETED, "Ready for pickup / delivered", null) },
                  onReject = { onUpdateOrderStatus(order, OrderStatus.REJECTED, "Unable to fulfill at this time", null) },
                  onEditStatus = { updatingOrder = order },
                  onDelete = { onDeleteOrder(order) },
                  onCallCustomer = { dialPhoneNumber(context, order.customerPhone) },
                  onWhatsAppCustomer = {
                    openWhatsAppChat(
                      context = context,
                      rawPhone = order.customerPhone,
                      message = "Hello ${order.customerName}, update on your Studio Pixy order ${order.orderCode} (${order.serviceName}): Current status is ${order.statusEnum().label}.",
                    )
                  },
                )
              }
            }
          }
        }
      }

      1 -> {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            LazyRow(
              modifier = Modifier.weight(1f),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              item {
                FilterChip(
                  selected = serviceCategoryFilter == null,
                  onClick = { serviceCategoryFilter = null },
                  label = { Text("All") },
                )
              }
              items(ServiceCategory.entries) { cat ->
                FilterChip(
                  selected = serviceCategoryFilter == cat,
                  onClick = { serviceCategoryFilter = cat },
                  label = { Text(cat.label) },
                )
              }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = { showCreateServiceDialog = true },
              modifier = Modifier.testTag("admin_add_service_button"),
            ) {
              Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add")
            }
          }
        }

        when (servicesState) {
          is UiState.Loading -> item {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
              CircularProgressIndicator()
            }
          }
          is UiState.Error -> item {
            Text(servicesState.message, color = StatusError)
          }
          is UiState.Success -> {
            val filteredSvcs = if (serviceCategoryFilter == null) servicesList
            else servicesList.filter { it.categoryEnum() == serviceCategoryFilter }
            if (filteredSvcs.isEmpty()) {
              item {
                EmptyAdminState("No cloud services yet. Tap Sync above or + Add to create one.")
              }
            } else {
              items(filteredSvcs, key = { it.id }) { svc ->
                AdminServiceRow(
                  service = svc,
                  onEdit = { editingService = svc },
                  onDelete = { onDeleteService(svc) },
                )
              }
            }
          }
        }
      }

      2 -> {
        item {
          StudioConfigEditorCard(
            config = studioConfig,
            isBusy = isBusy,
            onSave = onSaveConfig,
          )
        }
      }
    }

    item { Spacer(modifier = Modifier.height(12.dp)) }
  }
}

@Composable
private fun AdminSignInGate(
  authError: String?,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .padding(20.dp),
    contentAlignment = Alignment.Center,
  ) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          modifier = Modifier.size(64.dp),
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Filled.AdminPanelSettings,
              contentDescription = "Admin Portal",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(34.dp),
            )
          }
        }
        Text(
          text = "Studio Pixy Admin Portal",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
        )
        Text(
          text = "Sign in with Google using an authorized administrator account (${StudioAdminPolicy.PRIMARY_ADMIN_EMAIL}) to manage prices, photo frames, packages, and customer orders.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        GoogleSignInButton(
          label = "Admin Sign in with Google",
          testTag = "admin_google_sign_in_button",
          onAuthSuccess = onAuthSuccess,
          onAuthError = onAuthError,
        )

        if (authError != null) {
          Text(
            text = authError,
            style = MaterialTheme.typography.bodyMedium,
            color = StatusError,
            textAlign = TextAlign.Center,
          )
        }
      }
    }
  }
}

@Composable
private fun UnauthorizedAdminCard(
  signedInEmail: String,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .padding(20.dp),
    contentAlignment = Alignment.Center,
  ) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Lock,
          contentDescription = "Restricted Access",
          tint = StatusError,
          modifier = Modifier.size(44.dp),
        )
        Text(
          text = "Admin Access Restricted",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Signed in as $signedInEmail. Only authorized Studio Pixy admin accounts (${StudioAdminPolicy.PRIMARY_ADMIN_EMAIL}) can access the dashboard.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
        Button(
          onClick = onSignOut,
          modifier = Modifier.testTag("unauthorized_sign_out_button"),
        ) {
          Text("Sign Out & Switch Account")
        }
      }
    }
  }
}

@Composable
private fun KpiCard(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.ExtraBold,
        color = accent,
      )
    }
  }
}

@Composable
private fun AdminOrderCard(
  order: CustomerOrder,
  onAccept: () -> Unit,
  onComplete: () -> Unit,
  onReject: () -> Unit,
  onEditStatus: () -> Unit,
  onDelete: () -> Unit,
  onCallCustomer: () -> Unit,
  onWhatsAppCustomer: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("admin_order_card_${order.id}"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = order.orderCode,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
        )
        OrderStatusBadge(status = order.statusEnum())
      }

      Text(
        text = "${order.serviceName} (${order.selectedSize.ifBlank { order.categoryEnum().label }})",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      if (order.selectedFrameType.isNotBlank() || order.selectedFrameDesign.isNotBlank()) {
        Text(
          text = "Frame: ${listOf(order.selectedFrameType, order.selectedFrameDesign).filter { it.isNotBlank() }.joinToString(" • ")}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.secondary,
        )
      }
      Text(
        text = "Qty ${order.quantity} × ${formatInr(order.unitPrice)} = ${formatInr(order.totalAmount)}",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary,
      )

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(
            text = "Customer: ${order.customerName} • ${order.customerPhone}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
          )
          if (order.photoCount > 0) {
            Text(
              text = "Photos (${order.photoCount}): ${order.photoSummary}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          if (order.customerNotes.isNotBlank()) {
            Text(
              text = "Notes: ${order.customerNotes}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          if (order.adminRemarks.isNotBlank()) {
            Text(
              text = "Admin Note: ${order.adminRemarks}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      // Quick Actions Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (order.statusEnum() == OrderStatus.PENDING) {
          Button(
            onClick = onAccept,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
          ) {
            Text("Accept")
          }
          OutlinedButton(
            onClick = onReject,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
          ) {
            Text("Reject")
          }
        } else if (order.statusEnum() != OrderStatus.COMPLETED) {
          Button(
            onClick = onComplete,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess, contentColor = Color.White),
          ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Mark Completed")
          }
        }
        IconButton(
          onClick = onCallCustomer,
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Icon(Icons.Filled.Call, contentDescription = "Call customer", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(
          onClick = onWhatsAppCustomer,
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "WhatsApp customer", tint = WhatsAppGreen)
        }
        IconButton(
          onClick = onEditStatus,
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Icon(Icons.Filled.Edit, contentDescription = "Edit order status", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(
          onClick = onDelete,
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Icon(Icons.Filled.Delete, contentDescription = "Delete order", tint = StatusError)
        }
      }
    }
  }
}

@Composable
private fun AdminServiceRow(
  service: StudioService,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      ServiceImageThumbnail(
        imageUrl = service.imageUrl,
        category = service.categoryEnum(),
        contentDescription = service.name,
        modifier = Modifier.size(60.dp),
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = service.name,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${service.categoryEnum().label} • ${service.size} • ${formatInr(service.price)}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = if (service.published) "Published" else "Hidden Draft",
          style = MaterialTheme.typography.labelSmall,
          color = if (service.published) StatusSuccess else StatusWarning,
        )
      }
      IconButton(
        onClick = onEdit,
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Icon(Icons.Filled.Edit, contentDescription = "Edit service", tint = MaterialTheme.colorScheme.primary)
      }
      IconButton(
        onClick = onDelete,
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Icon(Icons.Filled.Delete, contentDescription = "Delete service", tint = StatusError)
      }
    }
  }
}

@Composable
private fun StudioConfigEditorCard(
  config: StudioConfig,
  isBusy: Boolean,
  onSave: (StudioConfig) -> Unit,
) {
  var businessName by remember(config) { mutableStateOf(config.businessName) }
  var tagline by remember(config) { mutableStateOf(config.tagline) }
  var bannerTitle by remember(config) { mutableStateOf(config.welcomeBannerTitle) }
  var bannerSubtitle by remember(config) { mutableStateOf(config.welcomeBannerSubtitle) }
  var whatsapp by remember(config) { mutableStateOf(config.whatsappNumber) }
  var phone by remember(config) { mutableStateOf(config.phoneNumber) }
  var email by remember(config) { mutableStateOf(config.email) }
  var address by remember(config) { mutableStateOf(config.address) }
  var hours by remember(config) { mutableStateOf(config.openingHours) }
  var mapsUrl by remember(config) { mutableStateOf(config.mapsUrl) }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = "Business Info & WhatsApp Settings",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      OutlinedTextField(
        value = businessName,
        onValueChange = { businessName = it },
        label = { Text("Business Name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      OutlinedTextField(
        value = tagline,
        onValueChange = { tagline = it },
        label = { Text("Tagline") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      OutlinedTextField(
        value = whatsapp,
        onValueChange = { whatsapp = it.filter { ch -> ch.isDigit() } },
        label = { Text("WhatsApp Number (with country code, e.g. 919876543210)") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("admin_whatsapp_input"),
      )
      OutlinedTextField(
        value = phone,
        onValueChange = { phone = it },
        label = { Text("Display Phone Number") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Studio Email") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      OutlinedTextField(
        value = address,
        onValueChange = { address = it },
        label = { Text("Studio Address") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
      )
      OutlinedTextField(
        value = hours,
        onValueChange = { hours = it },
        label = { Text("Opening Hours") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      OutlinedTextField(
        value = mapsUrl,
        onValueChange = { mapsUrl = it },
        label = { Text("Google Maps Link") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      OutlinedTextField(
        value = bannerTitle,
        onValueChange = { bannerTitle = it },
        label = { Text("Welcome Banner Title") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      OutlinedTextField(
        value = bannerSubtitle,
        onValueChange = { bannerSubtitle = it },
        label = { Text("Welcome Banner Subtitle") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
      )

      Button(
        onClick = {
          onSave(
            config.copy(
              businessName = businessName,
              tagline = tagline,
              whatsappNumber = whatsapp,
              phoneNumber = phone,
              email = email,
              address = address,
              openingHours = hours,
              mapsUrl = mapsUrl,
              welcomeBannerTitle = bannerTitle,
              welcomeBannerSubtitle = bannerSubtitle,
            )
          )
        },
        enabled = !isBusy,
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("admin_save_config_button"),
      ) {
        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Save Studio Information", fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun EmptyAdminState(message: String) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
    )
  }
}
