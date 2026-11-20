package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import com.example.data.model.CustomerOrder
import java.io.File
import java.text.NumberFormat
import java.util.Locale

fun formatInr(amount: Long): String {
  val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
  return "₹${formatter.format(amount)}"
}

fun getDisplayFileName(context: Context, uri: Uri): String {
  return try {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
      if (cursor.moveToFirst()) {
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0) cursor.getString(index) else null
      } else null
    } ?: uri.lastPathSegment ?: "photo.jpg"
  } catch (_: Exception) {
    uri.lastPathSegment ?: "photo.jpg"
  }
}

fun copyImageToInternalStorage(context: Context, uri: Uri, prefix: String = "service"): String? {
  return try {
    val dir = File(context.filesDir, "studio_images").apply { mkdirs() }
    val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
      file.outputStream().use { output ->
        input.copyTo(output)
      }
    }
    Uri.fromFile(file).toString()
  } catch (_: Exception) {
    null
  }
}

fun buildWhatsAppOrderMessage(
  orderCode: String?,
  serviceName: String,
  categoryLabel: String,
  size: String,
  frameType: String,
  frameDesign: String,
  quantity: Long,
  unitPrice: Long,
  totalAmount: Long,
  customerName: String,
  customerPhone: String,
  notes: String,
  photoCount: Int,
  photoSummary: String,
): String {
  return buildString {
    appendLine("*STUDIO PIXY — NEW ORDER*")
    if (!orderCode.isNullOrBlank()) {
      appendLine("Order Ref: $orderCode")
    }
    appendLine("Service: $serviceName ($categoryLabel)")
    if (size.isNotBlank()) appendLine("Size / Tier: $size")
    if (frameType.isNotBlank()) appendLine("Frame Type: $frameType")
    if (frameDesign.isNotBlank()) appendLine("Frame Design: $frameDesign")
    appendLine("Quantity: $quantity")
    appendLine("Unit Price: ${formatInr(unitPrice)}")
    appendLine("*Total Amount: ${formatInr(totalAmount)}*")
    appendLine("--------------------")
    if (customerName.isNotBlank()) appendLine("Customer: $customerName")
    if (customerPhone.isNotBlank()) appendLine("Mobile: $customerPhone")
    if (photoCount > 0) appendLine("Selected Photos: $photoCount ($photoSummary)")
    if (notes.isNotBlank()) appendLine("Notes: $notes")
  }.trim()
}

fun buildWhatsAppOrderMessage(order: CustomerOrder): String =
  buildWhatsAppOrderMessage(
    orderCode = order.orderCode,
    serviceName = order.serviceName,
    categoryLabel = order.categoryEnum().label,
    size = order.selectedSize,
    frameType = order.selectedFrameType,
    frameDesign = order.selectedFrameDesign,
    quantity = order.quantity,
    unitPrice = order.unitPrice,
    totalAmount = order.totalAmount,
    customerName = order.customerName,
    customerPhone = order.customerPhone,
    notes = order.customerNotes,
    photoCount = order.photoCount.toInt(),
    photoSummary = order.photoSummary,
  )

fun openWhatsAppChat(context: Context, rawPhone: String, message: String) {
  val digits = rawPhone.filter { it.isDigit() }.ifBlank { "919876543210" }
  val encoded = Uri.encode(message)
  val uri = Uri.parse("https://wa.me/$digits?text=$encoded")
  try {
    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
  } catch (_: Exception) {
    Toast.makeText(context, "Unable to open WhatsApp or browser.", Toast.LENGTH_SHORT).show()
  }
}

fun sharePhotosAndOrderViaWhatsApp(
  context: Context,
  rawPhone: String,
  message: String,
  photoUris: List<Uri>,
) {
  if (photoUris.isEmpty()) {
    openWhatsAppChat(context, rawPhone, message)
    return
  }
  try {
    val intent = if (photoUris.size == 1) {
      Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, photoUris.first())
        putExtra(Intent.EXTRA_TEXT, message)
      }
    } else {
      Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "image/*"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(photoUris))
        putExtra(Intent.EXTRA_TEXT, message)
      }
    }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    val pm = context.packageManager
    val hasWhatsApp = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pkg ->
      try {
        pm.getPackageInfo(pkg, 0)
        true
      } catch (_: Exception) {
        false
      }
    }
    if (hasWhatsApp != null) {
      intent.setPackage(hasWhatsApp)
      context.startActivity(intent)
    } else {
      openWhatsAppChat(context, rawPhone, message)
    }
  } catch (_: Exception) {
    openWhatsAppChat(context, rawPhone, message)
  }
}

fun openGoogleMaps(context: Context, mapsUrl: String) {
  val target = mapsUrl.trim().ifBlank { "https://maps.google.com/?q=Studio+Pixy" }
  try {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
  } catch (_: Exception) {
    Toast.makeText(context, "No maps or browser app available.", Toast.LENGTH_SHORT).show()
  }
}

fun dialPhoneNumber(context: Context, phoneNumber: String) {
  val cleaned = phoneNumber.filter { it.isDigit() || it == '+' }
  if (cleaned.isBlank()) return
  try {
    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned")))
  } catch (_: Exception) {
    Toast.makeText(context, "Unable to open phone dialer.", Toast.LENGTH_SHORT).show()
  }
}
