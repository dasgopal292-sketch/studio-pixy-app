package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CustomerOrder
import com.example.data.model.OrderStatus
import com.example.data.model.ServiceCategory
import com.example.data.model.StudioService
import com.example.ui.components.ServiceImageThumbnail
import com.example.util.copyImageToInternalStorage
import com.example.util.formatInr

@Composable
fun ServiceEditorDialog(
  initialService: StudioService?,
  onDismiss: () -> Unit,
  onSave: (StudioService) -> Unit,
) {
  val context = LocalContext.current
  val isEditing = initialService != null

  var name by remember { mutableStateOf(initialService?.name ?: "") }
  var category by remember { mutableStateOf(initialService?.categoryEnum() ?: ServiceCategory.PRINT) }
  var size by remember { mutableStateOf(initialService?.size ?: "") }
  var frameType by remember { mutableStateOf(initialService?.frameType ?: "") }
  var frameDesign by remember { mutableStateOf(initialService?.frameDesign ?: "") }
  var priceText by remember { mutableStateOf(initialService?.price?.toString() ?: "") }
  var unitLabel by remember { mutableStateOf(initialService?.unitLabel ?: "per print") }
  var description by remember { mutableStateOf(initialService?.description ?: "") }
  var deliverables by remember { mutableStateOf(initialService?.deliverables ?: "") }
  var imageUrl by remember {
    mutableStateOf(
      initialService?.imageUrl ?: "drawable:img_photo_printing"
    )
  }
  var published by remember { mutableStateOf(initialService?.published ?: true) }
  var popular by remember { mutableStateOf(initialService?.popular ?: false) }
  var errorText by remember { mutableStateOf<String?>(null) }

  val imagePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
    onResult = { uri ->
      if (uri != null) {
        val localUri = copyImageToInternalStorage(context, uri, prefix = "svc")
        if (localUri != null) {
          imageUrl = localUri
        }
      }
    },
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (isEditing) "Edit Service & Price" else "Add New Service",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          ServiceCategory.entries.forEach { cat ->
            FilterChip(
              selected = category == cat,
              onClick = {
                category = cat
                if (!isEditing) {
                  imageUrl = when (cat) {
                    ServiceCategory.PRINT -> "drawable:img_photo_printing"
                    ServiceCategory.FRAME -> "drawable:img_photo_frames"
                    ServiceCategory.PACKAGE -> "drawable:img_wedding_packages"
                  }
                  unitLabel = when (cat) {
                    ServiceCategory.PRINT -> "per print"
                    ServiceCategory.FRAME -> "per frame"
                    ServiceCategory.PACKAGE -> "per package"
                  }
                }
              },
              label = { Text(cat.label) },
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          ServiceImageThumbnail(
            imageUrl = imageUrl,
            category = category,
            contentDescription = "Service preview",
            modifier = Modifier.size(68.dp),
          )
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(
              onClick = {
                imagePickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Pick Service Image")
            }
            Text(
              text = "Or enter HTTPS URL below",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Service Name *") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_service_name_input"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = size,
            onValueChange = { size = it },
            label = { Text("Size / Duration") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Price (₹) *") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
              .weight(1f)
              .testTag("admin_service_price_input"),
          )
        }
        OutlinedTextField(
          value = unitLabel,
          onValueChange = { unitLabel = it },
          label = { Text("Unit Label (e.g. per print)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        if (category == ServiceCategory.FRAME) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = frameType,
              onValueChange = { frameType = it },
              label = { Text("Frame Type") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = frameDesign,
              onValueChange = { frameDesign = it },
              label = { Text("Frame Design") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }
        }
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = deliverables,
          onValueChange = { deliverables = it },
          label = { Text("Included Highlights / Deliverables") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = imageUrl,
          onValueChange = { imageUrl = it },
          label = { Text("Image Reference / URL") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = published, onCheckedChange = { published = it })
            Text("Published", style = MaterialTheme.typography.bodyMedium)
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = popular, onCheckedChange = { popular = it })
            Text("Popular", style = MaterialTheme.typography.bodyMedium)
          }
        }

        if (errorText != null) {
          Text(
            text = errorText!!,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val parsedPrice = priceText.toLongOrNull()
          if (name.trim().length < 2) {
            errorText = "Service name must be at least 2 characters."
            return@Button
          }
          if (parsedPrice == null || parsedPrice < 0L) {
            errorText = "Enter a valid price in ₹."
            return@Button
          }
          onSave(
            StudioService(
              id = initialService?.id ?: "",
              name = name.trim(),
              category = category.name,
              size = size.trim(),
              frameType = frameType.trim(),
              frameDesign = frameDesign.trim(),
              price = parsedPrice,
              unitLabel = unitLabel.trim().ifBlank { "per item" },
              description = description.trim(),
              deliverables = deliverables.trim(),
              imageUrl = imageUrl.trim(),
              published = published,
              popular = popular,
              sortOrder = initialService?.sortOrder ?: 50L,
              createdAt = initialService?.createdAt,
              updatedAt = initialService?.updatedAt,
            )
          )
        },
        modifier = Modifier.testTag("admin_save_service_button"),
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
    shape = RoundedCornerShape(22.dp),
  )
}

@Composable
fun OrderStatusUpdateDialog(
  order: CustomerOrder,
  onDismiss: () -> Unit,
  onConfirm: (OrderStatus, String, Long?) -> Unit,
) {
  var selectedStatus by remember { mutableStateOf(order.statusEnum()) }
  var remarks by remember { mutableStateOf(order.adminRemarks) }
  var unitPriceText by remember { mutableStateOf(order.unitPrice.toString()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Manage Order ${order.orderCode}",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "${order.customerName} (${order.customerPhone})",
          style = MaterialTheme.typography.titleSmall,
        )
        Text(
          text = "${order.serviceName} • Qty ${order.quantity} • Total ${formatInr(order.totalAmount)}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "Update Status:",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          OrderStatus.entries.chunked(3).forEach { rowStatuses ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              rowStatuses.forEach { st ->
                FilterChip(
                  selected = selectedStatus == st,
                  onClick = { selectedStatus = st },
                  label = { Text(st.label) },
                )
              }
            }
          }
        }
        OutlinedTextField(
          value = unitPriceText,
          onValueChange = { unitPriceText = it.filter { ch -> ch.isDigit() } },
          label = { Text("Unit Price (₹) — adjust if custom quote") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = remarks,
          onValueChange = { remarks = it },
          label = { Text("Admin Remarks (e.g. Ready for pickup at 6 PM)") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val unitOverride = unitPriceText.toLongOrNull()
          onConfirm(selectedStatus, remarks.trim(), unitOverride)
        },
        modifier = Modifier.testTag("admin_confirm_order_status_button"),
      ) {
        Text("Update Order")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
    shape = RoundedCornerShape(22.dp),
  )
}
