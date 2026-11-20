package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

enum class ServiceCategory(val label: String, val pluralLabel: String) {
  PRINT("Photo Print", "Photo Printing"),
  FRAME("Photo Frame", "Photo Frames"),
  PACKAGE("Package", "Photography Packages");

  companion object {
    fun fromRaw(raw: String?): ServiceCategory =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PRINT
  }
}

enum class OrderStatus(val label: String) {
  PENDING("Pending"),
  ACCEPTED("Accepted"),
  IN_PROGRESS("In Progress"),
  COMPLETED("Completed"),
  REJECTED("Rejected");

  companion object {
    fun fromRaw(raw: String?): OrderStatus =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PENDING
  }
}

@Keep
@IgnoreExtraProperties
data class StudioService(
  @DocumentId val id: String = "",
  val name: String = "",
  val category: String = ServiceCategory.PRINT.name,
  val size: String = "",
  val frameType: String = "",
  val frameDesign: String = "",
  val price: Long = 0L,
  val unitLabel: String = "per print",
  val description: String = "",
  val deliverables: String = "",
  val imageUrl: String = "",
  val published: Boolean = true,
  val popular: Boolean = false,
  val sortOrder: Long = 0L,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
) {
  fun categoryEnum(): ServiceCategory = ServiceCategory.fromRaw(category)

  fun toCreateMap(): Map<String, Any> =
    mapOf(
      "name" to name.trim().take(100),
      "category" to ServiceCategory.fromRaw(category).name,
      "size" to size.trim().take(60),
      "frameType" to frameType.trim().take(60),
      "frameDesign" to frameDesign.trim().take(60),
      "price" to price.coerceIn(0L, 10_000_000L),
      "unitLabel" to unitLabel.trim().ifBlank { "per item" }.take(40),
      "description" to description.trim().take(500),
      "deliverables" to deliverables.trim().take(500),
      "imageUrl" to imageUrl.trim().take(1000),
      "published" to published,
      "popular" to popular,
      "sortOrder" to sortOrder.coerceIn(0L, 10_000L),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

  fun toUpdateMap(): Map<String, Any> =
    mapOf(
      "name" to name.trim().take(100),
      "category" to ServiceCategory.fromRaw(category).name,
      "size" to size.trim().take(60),
      "frameType" to frameType.trim().take(60),
      "frameDesign" to frameDesign.trim().take(60),
      "price" to price.coerceIn(0L, 10_000_000L),
      "unitLabel" to unitLabel.trim().ifBlank { "per item" }.take(40),
      "description" to description.trim().take(500),
      "deliverables" to deliverables.trim().take(500),
      "imageUrl" to imageUrl.trim().take(1000),
      "published" to published,
      "popular" to popular,
      "sortOrder" to sortOrder.coerceIn(0L, 10_000L),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class StudioConfig(
  @DocumentId val id: String = "main",
  val businessName: String = "Studio Pixy",
  val tagline: String = "Photography • Archival Prints • Custom Frames • 4K Cinema",
  val whatsappNumber: String = "919876543210",
  val phoneNumber: String = "+91 98765 43210",
  val email: String = "studiopixy64@gmail.com",
  val address: String = "Station Road, Near City Center Plaza, Kolkata 700001",
  val openingHours: String = "Mon – Sun: 9:30 AM – 9:00 PM",
  val mapsUrl: String = "https://maps.google.com/?q=Studio+Pixy+Photography",
  val welcomeBannerTitle: String = "Memories Framed in Royal Clarity",
  val welcomeBannerSubtitle: String = "Instant passport photos, lab prints, custom frames & wedding cinema.",
  val updatedAt: Timestamp? = null,
) {
  fun toSaveMap(): Map<String, Any> =
    mapOf(
      "businessName" to businessName.trim().ifBlank { "Studio Pixy" }.take(80),
      "tagline" to tagline.trim().take(160),
      "whatsappNumber" to whatsappNumber.filter { it.isDigit() }.ifBlank { "919876543210" }.take(20),
      "phoneNumber" to phoneNumber.trim().ifBlank { "+91 98765 43210" }.take(25),
      "email" to email.trim().ifBlank { "studiopixy64@gmail.com" }.take(120),
      "address" to address.trim().ifBlank { "Studio Pixy Main Studio" }.take(250),
      "openingHours" to openingHours.trim().take(100),
      "mapsUrl" to mapsUrl.trim().take(500),
      "welcomeBannerTitle" to welcomeBannerTitle.trim().ifBlank { "Studio Pixy" }.take(140),
      "welcomeBannerSubtitle" to welcomeBannerSubtitle.trim().take(240),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class CustomerOrder(
  @DocumentId val id: String = "",
  val orderCode: String = "",
  val serviceId: String = "",
  val serviceName: String = "",
  val category: String = ServiceCategory.PRINT.name,
  val selectedSize: String = "",
  val selectedFrameType: String = "",
  val selectedFrameDesign: String = "",
  val quantity: Long = 1L,
  val unitPrice: Long = 0L,
  val totalAmount: Long = 0L,
  val customerName: String = "",
  val customerPhone: String = "",
  val customerNotes: String = "",
  val photoCount: Long = 0L,
  val photoSummary: String = "",
  val status: String = OrderStatus.PENDING.name,
  val customerUid: String = "",
  val adminRemarks: String = "",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null,
) {
  fun statusEnum(): OrderStatus = OrderStatus.fromRaw(status)

  fun categoryEnum(): ServiceCategory = ServiceCategory.fromRaw(category)

  fun toCreateMap(): Map<String, Any> {
    val safeQty = quantity.coerceIn(1L, 10_000L)
    val safeUnit = unitPrice.coerceIn(0L, 10_000_000L)
    val computedTotal = safeUnit * safeQty
    return mapOf(
      "orderCode" to orderCode.trim().take(24),
      "serviceId" to serviceId.trim().ifBlank { "custom_service" }.take(128),
      "serviceName" to serviceName.trim().take(120),
      "category" to ServiceCategory.fromRaw(category).name,
      "selectedSize" to selectedSize.trim().take(80),
      "selectedFrameType" to selectedFrameType.trim().take(80),
      "selectedFrameDesign" to selectedFrameDesign.trim().take(80),
      "quantity" to safeQty,
      "unitPrice" to safeUnit,
      "totalAmount" to computedTotal,
      "customerName" to customerName.trim().take(80),
      "customerPhone" to customerPhone.trim().take(20),
      "customerNotes" to customerNotes.trim().take(1000),
      "photoCount" to photoCount.coerceIn(0L, 100L),
      "photoSummary" to photoSummary.trim().take(500),
      "status" to OrderStatus.PENDING.name,
      "customerUid" to customerUid.trim().take(128),
      "adminRemarks" to "",
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
  }
}

object DefaultStudioCatalog {
  val defaultConfig = StudioConfig()

  val frameTypes = listOf("Teak Wood", "Matte Aluminum", "Crystal Acrylic", "Golden Ornate", "Box Canvas")
  val frameDesigns = listOf("Classic Beveled", "Minimalist Borderless", "Royal Gold Trim", "Gallery Mat Mount", "Collage Grid")
  val customPrintSizes = listOf("Passport (8 Copies)", "4x6 inch", "5x7 inch", "8x10 inch", "A4 (8.3x11.7 in)", "A3 (11.7x16.5 in)", "Custom Size")
  val customFrameSizes = listOf("5x7 inch", "8x10 inch", "8x12 inch", "12x18 inch", "16x24 inch", "20x30 inch", "Custom Size")

  val services: List<StudioService> = listOf(
    // --- PHOTO PRINTING ---
    StudioService(
      id = "print_passport",
      name = "Passport Size Photo",
      category = ServiceCategory.PRINT.name,
      size = "35x45 mm (8 Copies)",
      price = 80L,
      unitLabel = "per sheet (8 pcs)",
      description = "Studio-lit or uploaded portrait formatted for Passport, Visa, PAN & ID cards with crisp matte/gloss finish.",
      deliverables = "8 cut prints + soft copy option",
      imageUrl = "drawable:img_photo_printing",
      published = true,
      popular = true,
      sortOrder = 1L,
    ),
    StudioService(
      id = "print_4x6",
      name = "4x6 Photo Print",
      category = ServiceCategory.PRINT.name,
      size = "4x6 inch",
      price = 20L,
      unitLabel = "per print",
      description = "Vibrant 260gsm archival Fujifilm/Kodak lab print ideal for albums, travel memories, and gifts.",
      deliverables = "Glossy or Pearl Matte finish",
      imageUrl = "drawable:img_photo_printing",
      published = true,
      popular = true,
      sortOrder = 2L,
    ),
    StudioService(
      id = "print_5x7",
      name = "5x7 Photo Print",
      category = ServiceCategory.PRINT.name,
      size = "5x7 inch",
      price = 40L,
      unitLabel = "per print",
      description = "Medium portrait print with balanced skin tones and smudge-proof protective coating.",
      deliverables = "Borderless or white border",
      imageUrl = "drawable:img_photo_printing",
      published = true,
      popular = false,
      sortOrder = 3L,
    ),
    StudioService(
      id = "print_8x10",
      name = "8x10 Photo Print",
      category = ServiceCategory.PRINT.name,
      size = "8x10 inch",
      price = 100L,
      unitLabel = "per print",
      description = "Large studio portrait print designed for tabletop and wall display frames.",
      deliverables = "Rich color calibration",
      imageUrl = "drawable:img_photo_printing",
      published = true,
      popular = false,
      sortOrder = 4L,
    ),
    StudioService(
      id = "print_a4",
      name = "A4 Photo Print",
      category = ServiceCategory.PRINT.name,
      size = "8.3x11.7 inch (A4)",
      price = 130L,
      unitLabel = "per print",
      description = "Full A4 high-resolution fine-art print suitable for portfolios, certificates, and family portraits.",
      deliverables = "Archival luster or glossy paper",
      imageUrl = "drawable:img_photo_printing",
      published = true,
      popular = false,
      sortOrder = 5L,
    ),
    StudioService(
      id = "print_a3",
      name = "A3 Photo Print",
      category = ServiceCategory.PRINT.name,
      size = "11.7x16.5 inch (A3)",
      price = 280L,
      unitLabel = "per print",
      description = "Poster-grade A3 photo enlargement with deep contrast and gallery-grade sharpness.",
      deliverables = "Heavyweight 280gsm photo paper",
      imageUrl = "drawable:img_photo_printing",
      published = true,
      popular = false,
      sortOrder = 6L,
    ),
    StudioService(
      id = "print_custom",
      name = "Custom Photo Printing",
      category = ServiceCategory.PRINT.name,
      size = "Custom Dimensions",
      price = 150L,
      unitLabel = "per custom print",
      description = "Tailored panoramic, square, collage, or Polaroid-style prints trimmed to your exact dimensions.",
      deliverables = "Custom crop + color enhancement",
      imageUrl = "drawable:img_photo_printing",
      published = true,
      popular = false,
      sortOrder = 7L,
    ),

    // --- PHOTO FRAMES ---
    StudioService(
      id = "frame_teak_8x10",
      name = "Classic Teak Wood Frame",
      category = ServiceCategory.FRAME.name,
      size = "8x10 inch",
      frameType = "Teak Wood",
      frameDesign = "Classic Beveled",
      price = 450L,
      unitLabel = "per frame",
      description = "Handcrafted seasoned teak wood frame with crystal glass and included 8x10 archival print.",
      deliverables = "Frame + 8x10 Print + Wall Hook",
      imageUrl = "drawable:img_photo_frames",
      published = true,
      popular = true,
      sortOrder = 10L,
    ),
    StudioService(
      id = "frame_acrylic_12x18",
      name = "Crystal Acrylic Floating Frame",
      category = ServiceCategory.FRAME.name,
      size = "12x18 inch",
      frameType = "Crystal Acrylic",
      frameDesign = "Minimalist Borderless",
      price = 950L,
      unitLabel = "per frame",
      description = "Sleek frameless HD acrylic mount with chrome standoffs for contemporary interiors.",
      deliverables = "Acrylic sandwich + metallic studs",
      imageUrl = "drawable:img_photo_frames",
      published = true,
      popular = true,
      sortOrder = 11L,
    ),
    StudioService(
      id = "frame_aluminum_5x7",
      name = "Matte Black Tabletop Frame",
      category = ServiceCategory.FRAME.name,
      size = "5x7 inch",
      frameType = "Matte Aluminum",
      frameDesign = "Gallery Mat Mount",
      price = 320L,
      unitLabel = "per frame",
      description = "Brushed anodized aluminum desk frame with ivory acid-free mat border.",
      deliverables = "Frame + Stand + Print",
      imageUrl = "drawable:img_photo_frames",
      published = true,
      popular = false,
      sortOrder = 12L,
    ),
    StudioService(
      id = "frame_royal_16x24",
      name = "Royal Wedding Collage Frame",
      category = ServiceCategory.FRAME.name,
      size = "16x24 inch",
      frameType = "Golden Ornate",
      frameDesign = "Royal Gold Trim",
      price = 1650L,
      unitLabel = "per frame",
      description = "Grand celebration wall frame with ornate gold trim and custom multi-photo layout.",
      deliverables = "Design proof + Laminated print + Frame",
      imageUrl = "drawable:img_photo_frames",
      published = true,
      popular = false,
      sortOrder = 13L,
    ),
    StudioService(
      id = "frame_custom",
      name = "Custom Photo Frame Order",
      category = ServiceCategory.FRAME.name,
      size = "Custom Size",
      frameType = "Custom Choice",
      frameDesign = "Custom Finish",
      price = 600L,
      unitLabel = "starting price",
      description = "Choose any size, wood/acrylic/metal molding, mat border, and upload your photo for custom framing.",
      deliverables = "Tailored print + protective framing",
      imageUrl = "drawable:img_photo_frames",
      published = true,
      popular = false,
      sortOrder = 14L,
    ),

    // --- PHOTOGRAPHY PACKAGES ---
    StudioService(
      id = "pkg_wedding",
      name = "Wedding Photography",
      category = ServiceCategory.PACKAGE.name,
      size = "2-Day Coverage",
      price = 35000L,
      unitLabel = "per package",
      description = "Complete candid & traditional wedding storytelling capturing every ritual, emotion, and portrait.",
      deliverables = "2 Photographers • 40-Sheet Luxury Album • 500+ Edited Photos • Online Gallery",
      imageUrl = "drawable:img_wedding_packages",
      published = true,
      popular = true,
      sortOrder = 20L,
    ),
    StudioService(
      id = "pkg_prewedding",
      name = "Pre-Wedding Photography",
      category = ServiceCategory.PACKAGE.name,
      size = "Full Day Outdoor",
      price = 15000L,
      unitLabel = "per package",
      description = "Editorial couple shoot at scenic heritage or nature locations with outfit & pose direction.",
      deliverables = "80+ Retouched Portraits • 2-Min Highlight Reel • 1 Framed 12x18 Portrait",
      imageUrl = "drawable:img_wedding_packages",
      published = true,
      popular = true,
      sortOrder = 21L,
    ),
    StudioService(
      id = "pkg_cinematic_video",
      name = "Cinematic Video Shooting",
      category = ServiceCategory.PACKAGE.name,
      size = "4K Cinema Coverage",
      price = 22000L,
      unitLabel = "per event",
      description = "Gimbal-stabilized 4K cinema film with aerial drone shots, wireless audio, and color grading.",
      deliverables = "4-Min Cinematic Teaser • Full Edited Feature Film • Drone Coverage",
      imageUrl = "drawable:img_hero_studio",
      published = true,
      popular = true,
      sortOrder = 22L,
    ),
    StudioService(
      id = "pkg_rice_feeding",
      name = "Rice Feeding Photography",
      category = ServiceCategory.PACKAGE.name,
      size = "Half Day (5 Hours)",
      price = 8500L,
      unitLabel = "per package",
      description = "Warm, joyful Annaprashan coverage capturing baby portraits, family blessings, and ceremony details.",
      deliverables = "Candid + Traditional Photos • 20-Sheet Mini Album • 150+ Edited Images",
      imageUrl = "drawable:img_wedding_packages",
      published = true,
      popular = false,
      sortOrder = 23L,
    ),
    StudioService(
      id = "pkg_birthday",
      name = "Birthday Photography",
      category = ServiceCategory.PACKAGE.name,
      size = "4-Hour Party",
      price = 6500L,
      unitLabel = "per package",
      description = "Vibrant coverage of cake cutting, decor, games, and candid family & kids portraits.",
      deliverables = "120+ Edited Photos • Highlight Reel • 10 Prints (4x6)",
      imageUrl = "drawable:img_hero_studio",
      published = true,
      popular = false,
      sortOrder = 24L,
    ),
    StudioService(
      id = "pkg_event",
      name = "Event Photography",
      category = ServiceCategory.PACKAGE.name,
      size = "Full Event Coverage",
      price = 9500L,
      unitLabel = "per event",
      description = "Professional photography for corporate conferences, cultural programs, anniversaries, and housewarmings.",
      deliverables = "Same-Day Social Previews • 200+ Edited High-Res Photos",
      imageUrl = "drawable:img_hero_studio",
      published = true,
      popular = false,
      sortOrder = 25L,
    ),
  )
}
