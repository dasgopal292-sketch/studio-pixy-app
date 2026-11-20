package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterFrames
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ServiceCategory
import com.example.data.model.StudioService
import com.example.ui.components.PopularBadge
import com.example.ui.components.ServiceImageThumbnail
import com.example.ui.theme.PixyDeepNavy
import com.example.ui.theme.PixyRoyalBlue
import com.example.util.formatInr

@Composable
fun CatalogScreen(
  selectedCategory: ServiceCategory,
  onSelectCategory: (ServiceCategory) -> Unit,
  services: List<StudioService>,
  onSelectServiceToOrder: (StudioService) -> Unit,
  onStartCustomOrder: (ServiceCategory) -> Unit,
  modifier: Modifier = Modifier,
) {
  val filtered = services.filter { it.categoryEnum() == selectedCategory }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Header & Category Chips
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Services & Price List",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ServiceCategory.entries.forEach { category ->
            val selected = category == selectedCategory
            FilterChip(
              selected = selected,
              onClick = { onSelectCategory(category) },
              modifier = Modifier.testTag("catalog_tab_${category.name.lowercase()}"),
              label = {
                Text(
                  text = when (category) {
                    ServiceCategory.PRINT -> "Prints"
                    ServiceCategory.FRAME -> "Frames"
                    ServiceCategory.PACKAGE -> "Packages"
                  },
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
              },
              leadingIcon = {
                Icon(
                  imageVector = when (category) {
                    ServiceCategory.PRINT -> Icons.Filled.PhotoLibrary
                    ServiceCategory.FRAME -> Icons.Filled.FilterFrames
                    ServiceCategory.PACKAGE -> Icons.Filled.CameraAlt
                  },
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = Color.White,
                selectedLeadingIconColor = Color.White,
              ),
            )
          }
        }
      }
    }

    // Category Showcase Banner
    item {
      val (bannerRes, title, subtitle) = when (selectedCategory) {
        ServiceCategory.PRINT -> Triple(
          R.drawable.img_photo_printing,
          "Lab-Grade Photo Printing",
          "Passport sheets, 4x6, 5x7, 8x10, A4, A3 & custom sizes on archival 260–280gsm paper.",
        )
        ServiceCategory.FRAME -> Triple(
          R.drawable.img_photo_frames,
          "Custom Photo Framing",
          "Handcrafted Teak Wood, Crystal Acrylic, Matte Aluminum & Royal Wedding collage frames.",
        )
        ServiceCategory.PACKAGE -> Triple(
          R.drawable.img_wedding_packages,
          "Photography & 4K Cinema Packages",
          "Wedding, Pre-Wedding, Rice Feeding, Birthday, Event & Cinematic Drone Video.",
        )
      }

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(148.dp),
        ) {
          Image(
            painter = painterResource(id = bannerRes),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.horizontalGradient(
                  colors = listOf(
                    PixyDeepNavy.copy(alpha = 0.9f),
                    PixyRoyalBlue.copy(alpha = 0.55f),
                    Color.Transparent,
                  )
                )
              )
          )
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(16.dp),
            verticalArrangement = Arrangement.Bottom,
          ) {
            Text(
              text = title,
              style = MaterialTheme.typography.titleLarge,
              color = Color.White,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = subtitle,
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White.copy(alpha = 0.9f),
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }

    // Custom Order Shortcut
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
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
              text = when (selectedCategory) {
                ServiceCategory.PRINT -> "Need Custom Print Dimensions?"
                ServiceCategory.FRAME -> "Build a Custom Photo Frame"
                ServiceCategory.PACKAGE -> "Custom Event Coverage Quote"
              },
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
              text = "Upload your photos and configure exact size & style.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          FilledTonalButton(
            onClick = { onStartCustomOrder(selectedCategory) },
            modifier = Modifier.testTag("catalog_custom_order_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.AddPhotoAlternate,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Custom")
          }
        }
      }
    }

    if (filtered.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.PhotoLibrary,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(36.dp),
            )
            Text(
              text = "No ${selectedCategory.pluralLabel} listed yet",
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text = "Use Custom Order above or check back shortly.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      items(filtered, key = { it.id.ifBlank { it.name } }) { service ->
        ServiceCatalogCard(
          service = service,
          onOrderClick = { onSelectServiceToOrder(service) },
        )
      }
    }

    item { Spacer(modifier = Modifier.height(8.dp)) }
  }
}

@Composable
private fun ServiceCatalogCard(
  service: StudioService,
  onOrderClick: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("service_card_${service.id}"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
      ) {
        ServiceImageThumbnail(
          imageUrl = service.imageUrl,
          category = service.categoryEnum(),
          contentDescription = service.name,
          modifier = Modifier.size(92.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (service.size.isNotBlank()) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
              ) {
                Text(
                  text = service.size,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
              }
            }
            if (service.popular) {
              PopularBadge()
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = service.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          if (service.frameType.isNotBlank() || service.frameDesign.isNotBlank()) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
              text = listOf(service.frameType, service.frameDesign)
                .filter { it.isNotBlank() }
                .joinToString(" • "),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.secondary,
              fontWeight = FontWeight.SemiBold,
            )
          }
          if (service.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = service.description,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 3,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      if (service.deliverables.isNotBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Check,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(15.dp),
            )
            Text(
              text = service.deliverables,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = formatInr(service.price),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = service.unitLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Button(
          onClick = onOrderClick,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("order_service_button_${service.id}"),
        ) {
          Icon(
            imageVector = Icons.Filled.Calculate,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (service.categoryEnum() == ServiceCategory.PACKAGE) "Book Package" else "Calculate & Order",
          )
        }
      }
    }
  }
}
