package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.firebase.StudioRepository
import com.example.data.model.DefaultStudioCatalog
import com.example.data.model.ServiceCategory
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OrderCalculatorScreen
import com.example.ui.theme.StudioPixyTheme
import com.example.ui.viewmodel.StudioViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.firestore.FirebaseFirestore

object StudioRoutes {
  const val HOME = "home"
  const val CATALOG = "catalog"
  const val CALCULATOR = "calculator"
  const val ADMIN = "admin"
}

private data class NavTabItem(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
)

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      StudioPixyTheme {
        StudioPixyApp()
      }
    }
  }
}

@Composable
fun StudioPixyApp() {
  val viewModel: StudioViewModel = viewModel(
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        StudioViewModel(StudioRepository(db))
      }
    }
  )

  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: StudioRoutes.HOME

  val servicesState by viewModel.publicServicesState.collectAsStateWithLifecycle()
  val studioConfig by viewModel.studioConfig.collectAsStateWithLifecycle()
  val orderDraft by viewModel.orderDraft.collectAsStateWithLifecycle()
  val authSession by viewModel.authSession.collectAsStateWithLifecycle()
  val adminOrdersState by viewModel.adminOrdersState.collectAsStateWithLifecycle()
  val adminServicesState by viewModel.adminServicesState.collectAsStateWithLifecycle()
  val cloudCatalogEmpty by viewModel.cloudCatalogEmpty.collectAsStateWithLifecycle()
  val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()
  val adminActionBusy by viewModel.adminActionBusy.collectAsStateWithLifecycle()

  var selectedCatalogCategory by rememberSaveable { mutableStateOf(ServiceCategory.PRINT) }
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(feedbackMessage) {
    val msg = feedbackMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearFeedbackMessage()
    }
  }

  val services = (servicesState as? UiState.Success)?.data ?: DefaultStudioCatalog.services

  val navItems = listOf(
    NavTabItem(StudioRoutes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    NavTabItem(StudioRoutes.CATALOG, "Services", Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary),
    NavTabItem(StudioRoutes.CALCULATOR, "Order", Icons.Filled.Calculate, Icons.Outlined.Calculate),
    NavTabItem(StudioRoutes.ADMIN, "Admin", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings),
  )

  fun navigateTo(route: String) {
    navController.navigate(route) {
      popUpTo(StudioRoutes.HOME) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 600.dp

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = MaterialTheme.colorScheme.background,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (!useRail) {
          NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            navItems.forEach { item ->
              val selected = currentRoute == item.route
              NavigationBarItem(
                selected = selected,
                onClick = { navigateTo(item.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                  )
                },
                label = { Text(item.label) },
                modifier = Modifier.testTag("nav_${item.route}"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        if (useRail) {
          NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
            navItems.forEach { item ->
              val selected = currentRoute == item.route
              NavigationRailItem(
                selected = selected,
                onClick = { navigateTo(item.route) },
                icon = {
                  Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                  )
                },
                label = { Text(item.label) },
                modifier = Modifier.testTag("rail_${item.route}"),
              )
            }
          }
        }

        NavHost(
          navController = navController,
          startDestination = StudioRoutes.HOME,
          modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .padding(top = 8.dp),
        ) {
          composable(StudioRoutes.HOME) {
            HomeScreen(
              config = studioConfig,
              services = services,
              onBrowseCategory = { category ->
                selectedCatalogCategory = category
                navigateTo(StudioRoutes.CATALOG)
              },
              onSelectServiceToOrder = { service ->
                viewModel.selectServiceForOrder(service)
                navigateTo(StudioRoutes.CALCULATOR)
              },
              onOpenCalculator = {
                navigateTo(StudioRoutes.CALCULATOR)
              },
              onOpenAdmin = {
                navigateTo(StudioRoutes.ADMIN)
              },
            )
          }

          composable(StudioRoutes.CATALOG) {
            CatalogScreen(
              selectedCategory = selectedCatalogCategory,
              onSelectCategory = { selectedCatalogCategory = it },
              services = services,
              onSelectServiceToOrder = { service ->
                viewModel.selectServiceForOrder(service)
                navigateTo(StudioRoutes.CALCULATOR)
              },
              onStartCustomOrder = { category ->
                viewModel.startCustomOrder(category)
                navigateTo(StudioRoutes.CALCULATOR)
              },
            )
          }

          composable(StudioRoutes.CALCULATOR) {
            OrderCalculatorScreen(
              draft = orderDraft,
              services = services,
              config = studioConfig,
              onSelectService = viewModel::selectServiceForOrder,
              onQuantityChange = viewModel::updateOrderQuantity,
              onSizeChange = viewModel::updateOrderSize,
              onFrameTypeChange = viewModel::updateOrderFrameType,
              onFrameDesignChange = viewModel::updateOrderFrameDesign,
              onAddPhotos = viewModel::addSelectedPhotos,
              onRemovePhoto = viewModel::removeSelectedPhoto,
              onCustomerNameChange = viewModel::updateCustomerName,
              onCustomerPhoneChange = viewModel::updateCustomerPhone,
              onCustomerNotesChange = viewModel::updateCustomerNotes,
              onSubmitOrder = viewModel::submitCurrentOrder,
              onDismissConfirmation = viewModel::clearSubmittedOrder,
            )
          }

          composable(StudioRoutes.ADMIN) {
            androidx.compose.runtime.key(authSession.user?.uid ?: "guest") {
              AdminScreen(
                authSession = authSession,
                ordersState = adminOrdersState,
                servicesState = adminServicesState,
                studioConfig = studioConfig,
                cloudCatalogEmpty = cloudCatalogEmpty,
                isBusy = adminActionBusy,
                onSeedCatalog = viewModel::seedCatalogToCloud,
                onSaveService = viewModel::saveService,
                onDeleteService = viewModel::deleteService,
                onUpdateOrderStatus = viewModel::updateOrderStatus,
                onDeleteOrder = viewModel::deleteOrder,
                onSaveConfig = viewModel::saveStudioConfig,
              )
            }
          }
        }
      }
    }
  }
}
