package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.DefaultStudioCatalog
import com.example.data.model.ServiceCategory
import com.example.data.model.StudioConfig
import com.example.data.model.StudioService
import com.example.ui.components.PriceCalculatorSummaryCard
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.viewmodel.OrderDraft
import com.example.util.buildWhatsAppOrderMessage
import com.example.util.formatInr
import com.example.util.getDisplayFileName
import com.example.util.sharePhotosAndOrderViaWhatsApp

@Composable
fun OrderCalculatorScreen(
  draft: OrderDraft,
  services: List<StudioService>,
  config: StudioConfig,
  onSelectService: (StudioService) -> Unit,
  onQuantityChange: (Long) -> Unit,
  onSizeChange: (String) -> Unit,
  onFrameTypeChange: (String) -> Unit,
  onFrameDesignChange: (String) -> Unit,
  onAddPhotos: (List<Uri>, List<String>) -> Unit,
  onRemovePhoto: (Int) -> Unit,
  onCustomerNameChange: (String) -> Unit,
  onCustomerPhoneChange: (String) -> Unit,
  onCustomerNotesChange: (String) -> Unit,
  onSubmitOrder: () -> Unit,
  onDismissConfirmation: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var showServicePicker by remember { mutableStateOf(false) }

  val multiPhotoLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 15),
    onResult = { uris ->
      if (uris.isNotEmpty()) {
        val names = uris.map { getDisplayFileName(context, it) }
        onAddPhotos(uris, names)
      }
    },
  )

  // Order Success Confirmation Dialog
  val submitted = draft.submittedOrder
  if (submitted != null) {
    AlertDialog(
      onDismissRequest = onDismissConfirmation,
      icon = {
        Icon(
          imageVector = Icons.Filled.CheckCircle,
          contentDescription = "Order Confirmed",
          tint = StatusSuccess,
          modifier = Modifier.size(44.dp),
        )
      },
      title = {
        Text(
          text = "Order Submitted!",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Order Ref: ${submitted.orderCode}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = "${submitted.serviceName} × ${submitted.quantity} = ${formatInr(submitted.totalAmount)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = "Studio Pixy will review and confirm shortly. You can also send your order & selected photos directly on WhatsApp.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val msg = buildWhatsAppOrderMessage(submitted)
            sharePhotosAndOrderViaWhatsApp(
              context = context,
              rawPhone = config.whatsappNumber,
              message = msg,
              photoUris = draft.selectedPhotoUris,
            )
            onDismissConfirmation()
          },
          colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = Color.White),
          modifier = Modifier.testTag("dialog_send_whatsapp_button"),
        ) {
          Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Send via WhatsApp")
        }
      },
      dismissButton = {
        TextButton(onClick = onDismissConfirmation) {
          Text("Done")
        }
      },
    )
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Column {
        Text(
          text = "Price Calculator & Order",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = "Calculate instant ₹ total, upload photos, and submit or send on WhatsApp.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    // 1. Service Selection & Configuration Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Text(
            text = "1. Choose Service & Options",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )

          Box {
            OutlinedButton(
              onClick = { showServicePicker = true },
              modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("order_service_selector"),
              shape = RoundedCornerShape(14.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = draft.serviceName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = "${draft.category.label} • ${formatInr(draft.unitPrice)} ${draft.unitLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
                Icon(Icons.Filled.ExpandMore, contentDescription = "Select service")
              }
            }

            DropdownMenu(
              expanded = showServicePicker,
              onDismissRequest = { showServicePicker = false },
              modifier = Modifier.fillMaxWidth(0.88f),
            ) {
              services.forEach { svc ->
                DropdownMenuItem(
                  text = {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                      Text(
                        text = "${svc.name} (${svc.size})",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = formatInr(svc.price),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                      )
                    }
                  },
                  onClick = {
                    onSelectService(svc)
                    showServicePicker = false
                  },
                )
              }
            }
          }

          // Size / Frame options
          if (draft.category == ServiceCategory.PRINT) {
            OptionChipSelector(
              title = "Print Size",
              options = (listOf(draft.selectedSize) + DefaultStudioCatalog.customPrintSizes)
                .filter { it.isNotBlank() }
                .distinct(),
              selected = draft.selectedSize,
              onSelect = onSizeChange,
            )
          } else if (draft.category == ServiceCategory.FRAME) {
            OptionChipSelector(
              title = "Frame Size",
              options = (listOf(draft.selectedSize) + DefaultStudioCatalog.customFrameSizes)
                .filter { it.isNotBlank() }
                .distinct(),
              selected = draft.selectedSize,
              onSelect = onSizeChange,
            )
            OptionChipSelector(
              title = "Frame Type",
              options = DefaultStudioCatalog.frameTypes,
              selected = draft.selectedFrameType,
              onSelect = onFrameTypeChange,
            )
            OptionChipSelector(
              title = "Frame Design",
              options = DefaultStudioCatalog.frameDesigns,
              selected = draft.selectedFrameDesign,
              onSelect = onFrameDesignChange,
            )
          }

          // Quantity Stepper
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text = "Quantity",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "${formatInr(draft.unitPrice)} × ${draft.quantity}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              FilledTonalIconButton(
                onClick = { onQuantityChange((draft.quantity - 1L).coerceAtLeast(1L)) },
                enabled = draft.quantity > 1L,
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("quantity_decrease_button"),
              ) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity")
              }
              Text(
                text = draft.quantity.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.testTag("quantity_value_text"),
              )
              FilledTonalIconButton(
                onClick = { onQuantityChange((draft.quantity + 1L).coerceAtMost(10_000L)) },
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("quantity_increase_button"),
              ) {
                Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
              }
            }
          }
        }
      }
    }

    // 2. Live Price Calculator Card
    item {
      PriceCalculatorSummaryCard(
        serviceName = draft.serviceName,
        quantity = draft.quantity,
        unitPrice = draft.unitPrice,
        totalAmount = draft.totalAmount,
      )
    }

    // 3. Upload Photos (Android Photo Picker - Zero Permissions)
    item {
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
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "2. Upload Photos (Optional)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Select photos for printing or framing, then attach them via WhatsApp.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          OutlinedButton(
            onClick = {
              multiPhotoLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            modifier = Modifier
              .fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("upload_photos_button"),
          ) {
            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (draft.selectedPhotoUris.isEmpty()) "Select Photos from Gallery"
              else "Add More Photos (${draft.selectedPhotoUris.size} selected)",
            )
          }

          if (draft.selectedPhotoUris.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              itemsIndexed(draft.selectedPhotoUris) { index, uri ->
                Box(
                  modifier = Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(12.dp)),
                ) {
                  AsyncImage(
                    model = uri,
                    contentDescription = draft.selectedPhotoNames.getOrNull(index) ?: "Selected photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                  )
                  Surface(
                    modifier = Modifier
                      .align(Alignment.TopEnd)
                      .padding(4.dp)
                      .size(24.dp)
                      .clickable { onRemovePhoto(index) },
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove photo",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // 4. Customer Details Form & Submit Actions
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = "3. Your Contact & Order Details",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          OutlinedTextField(
            value = draft.customerName,
            onValueChange = onCustomerNameChange,
            label = { Text("Full Name *") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("order_customer_name_input"),
          )
          OutlinedTextField(
            value = draft.customerPhone,
            onValueChange = onCustomerPhoneChange,
            label = { Text("Mobile / WhatsApp Number *") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("order_customer_phone_input"),
          )
          OutlinedTextField(
            value = draft.customerNotes,
            onValueChange = onCustomerNotesChange,
            label = { Text("Order Details (Finish, Event Date, Address, Instructions)") },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("order_customer_notes_input"),
          )

          if (draft.validationError != null) {
            Text(
              text = draft.validationError,
              style = MaterialTheme.typography.bodyMedium,
              color = StatusError,
              fontWeight = FontWeight.Medium,
            )
          }

          Button(
            onClick = onSubmitOrder,
            enabled = !draft.isSubmitting,
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("submit_order_button"),
          ) {
            if (draft.isSubmitting) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp,
              )
            } else {
              Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Submit Order (${formatInr(draft.totalAmount)})", fontWeight = FontWeight.Bold)
            }
          }

          OutlinedButton(
            onClick = {
              val msg = buildWhatsAppOrderMessage(
                orderCode = draft.submittedOrder?.orderCode,
                serviceName = draft.serviceName,
                categoryLabel = draft.category.label,
                size = draft.selectedSize,
                frameType = draft.selectedFrameType,
                frameDesign = draft.selectedFrameDesign,
                quantity = draft.quantity,
                unitPrice = draft.unitPrice,
                totalAmount = draft.totalAmount,
                customerName = draft.customerName,
                customerPhone = draft.customerPhone,
                notes = draft.customerNotes,
                photoCount = draft.selectedPhotoUris.size,
                photoSummary = draft.selectedPhotoNames.take(5).joinToString(", "),
              )
              sharePhotosAndOrderViaWhatsApp(
                context = context,
                rawPhone = config.whatsappNumber,
                message = msg,
                photoUris = draft.selectedPhotoUris,
              )
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("whatsapp_direct_order_button"),
            border = BorderStroke(1.5.dp, WhatsAppGreen),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppGreen),
          ) {
            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Send Order & Photos via WhatsApp", fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(10.dp)) }
  }
}

@Composable
private fun OptionChipSelector(
  title: String,
  options: List<String>,
  selected: String,
  onSelect: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      itemsIndexed(options) { _, option ->
        val isSelected = option.equals(selected, ignoreCase = true)
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable { onSelect(option) },
        ) {
          Text(
            text = option,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          )
        }
      }
    }
  }
}
