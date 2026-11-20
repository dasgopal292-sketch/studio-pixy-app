package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraEnhance
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterFrames
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ServiceCategory
import com.example.data.model.StudioConfig
import com.example.data.model.StudioService
import com.example.ui.components.PopularBadge
import com.example.ui.components.ServiceImageThumbnail
import com.example.ui.components.StudioBrandMark
import com.example.ui.theme.PixyDeepNavy
import com.example.ui.theme.PixyRoyalBlue
import com.example.ui.theme.WhatsAppGreen
import com.example.util.dialPhoneNumber
import com.example.util.formatInr
import com.example.util.openGoogleMaps
import com.example.util.openWhatsAppChat

private data class QuickCategoryItem(
  val title: String,
  val subtitle: String,
  val icon: ImageVector,
  val category: ServiceCategory,
  val highlightServiceId: String? = null,
)

@Composable
fun HomeScreen(
  config: StudioConfig,
  services: List<StudioService>,
  onBrowseCategory: (ServiceCategory) -> Unit,
  onSelectServiceToOrder: (StudioService) -> Unit,
  onOpenCalculator: () -> Unit,
  onOpenAdmin: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val quickCategories = listOf(
    QuickCategoryItem("Photography Services", "All Studio Services", Icons.Filled.CameraEnhance, ServiceCategory.PACKAGE),
    QuickCategoryItem("Photo Printing", "4x6 to A3 Lab Prints", Icons.Filled.PhotoLibrary, ServiceCategory.PRINT),
    QuickCategoryItem("Photo Frames", "Wood, Acrylic & More", Icons.Filled.FilterFrames, ServiceCategory.FRAME),
    QuickCategoryItem("Passport Photos", "Instant 8-Copy Sheet", Icons.Filled.Badge, ServiceCategory.PRINT, "print_passport"),
    QuickCategoryItem("Wedding Photography", "Candid & Traditional", Icons.Filled.Favorite, ServiceCategory.PACKAGE, "pkg_wedding"),
    QuickCategoryItem("Event Photography", "Birthdays & Programs", Icons.Filled.Celebration, ServiceCategory.PACKAGE, "pkg_event"),
    QuickCategoryItem("Video Shooting", "4K Cinema & Drone", Icons.Filled.Videocam, ServiceCategory.PACKAGE, "pkg_cinematic_video"),
  )

  val featuredServices = services.filter { it.popular }.ifEmpty { services.take(6) }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    // 1. Studio Pixy Branding Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.weight(1f),
        ) {
          StudioBrandMark(size = 46.dp)
          Column {
            Text(
              text = config.businessName,
              style = MaterialTheme.typography.headlineMedium,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = config.tagline,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        IconButton(
          onClick = onOpenAdmin,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("home_admin_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.AdminPanelSettings,
            contentDescription = "Admin Portal",
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }

    // 2. Hero Welcome Banner
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(224.dp),
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_hero_studio),
            contentDescription = "Studio Pixy photography showcase",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(
                    PixyDeepNavy.copy(alpha = 0.25f),
                    PixyRoyalBlue.copy(alpha = 0.85f),
                    PixyDeepNavy.copy(alpha = 0.95f),
                  )
                )
              )
          )
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = Color.White.copy(alpha = 0.18f),
            ) {
              Text(
                text = "WELCOME TO STUDIO PIXY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              )
            }
            Column {
              Text(
                text = config.welcomeBannerTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = config.welcomeBannerSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
              Spacer(modifier = Modifier.height(12.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                  onClick = onOpenCalculator,
                  modifier = Modifier.testTag("hero_order_now_button"),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = PixyRoyalBlue,
                  ),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Calculate,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Price Calculator", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                  onClick = { onBrowseCategory(ServiceCategory.PRINT) },
                  modifier = Modifier.testTag("hero_explore_prints_button"),
                  border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                ) {
                  Text("View Prices")
                }
              }
            }
          }
        }
      }
    }

    // 3. Services Quick Grid
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Our Services",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onBackground,
        )
        quickCategories.chunked(2).forEach { rowItems ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            rowItems.forEach { item ->
              QuickServiceCard(
                item = item,
                onClick = {
                  val target = item.highlightServiceId?.let { id ->
                    services.firstOrNull { it.id == id }
                  }
                  if (target != null) {
                    onSelectServiceToOrder(target)
                  } else {
                    onBrowseCategory(item.category)
                  }
                },
                modifier = Modifier.weight(1f),
              )
            }
            if (rowItems.size == 1) {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }
    }

    // 4. Featured Popular Services
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Popular Picks",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
          )
          FilledTonalButton(
            onClick = { onBrowseCategory(ServiceCategory.PRINT) },
            modifier = Modifier.testTag("home_see_all_services_button"),
          ) {
            Text("Explore All")
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
          }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          items(featuredServices, key = { it.id.ifBlank { it.name } }) { service ->
            Card(
              modifier = Modifier
                .width(230.dp)
                .clickable { onSelectServiceToOrder(service) }
                .testTag("featured_service_${service.id}"),
              shape = RoundedCornerShape(18.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
              Column {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp),
                ) {
                  ServiceImageThumbnail(
                    imageUrl = service.imageUrl,
                    category = service.categoryEnum(),
                    contentDescription = service.name,
                    modifier = Modifier
                      .fillMaxSize()
                      .padding(8.dp),
                  )
                  Box(modifier = Modifier.padding(14.dp)) {
                    PopularBadge()
                  }
                }
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                  Text(
                    text = service.categoryEnum().label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  if (service.size.isNotBlank()) {
                    Text(
                      text = service.size,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  Spacer(modifier = Modifier.height(8.dp))
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = formatInr(service.price),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.ExtraBold,
                      color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                      text = "Order →",
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.secondary,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // 5. Instant WhatsApp Order Banner
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = WhatsAppGreen,
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = "WhatsApp Order",
                tint = Color.White,
                modifier = Modifier.size(24.dp),
              )
            }
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Instant WhatsApp Order",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
              text = "Send photos & print inquiries directly to Studio Pixy.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
            )
          }
          Button(
            onClick = {
              openWhatsAppChat(
                context = context,
                rawPhone = config.whatsappNumber,
                message = "Hello ${config.businessName}! I would like to inquire about photo printing / framing / photography packages.",
              )
            },
            modifier = Modifier.testTag("home_whatsapp_order_button"),
            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen, contentColor = Color.White),
          ) {
            Text("Chat")
          }
        }
      }
    }

    // 6. Contact Information & Google Maps Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = "Visit & Contact Studio Pixy",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          ContactInfoRow(
            icon = Icons.Filled.LocationOn,
            label = "Studio Address",
            value = config.address,
          )
          ContactInfoRow(
            icon = Icons.Filled.Schedule,
            label = "Opening Hours",
            value = config.openingHours,
          )
          ContactInfoRow(
            icon = Icons.Filled.Call,
            label = "Phone",
            value = config.phoneNumber,
          )
          ContactInfoRow(
            icon = Icons.Filled.Email,
            label = "Email",
            value = config.email,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedButton(
              onClick = { dialPhoneNumber(context, config.phoneNumber) },
              modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("home_call_studio_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Call,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Call Studio")
            }
            Button(
              onClick = { openGoogleMaps(context, config.mapsUrl) },
              modifier = Modifier
                .weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("home_open_maps_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Map,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Google Maps")
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(8.dp)) }
  }
}

@Composable
private fun QuickServiceCard(
  item: QuickCategoryItem,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier
      .clip(RoundedCornerShape(18.dp))
      .clickable(onClick = onClick)
      .testTag("quick_service_${item.title.lowercase().replace(' ', '_')}"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Surface(
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
          )
        }
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = item.title,
          style = MaterialTheme.typography.titleSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = item.subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun ContactInfoRow(
  icon: ImageVector,
  label: String,
  value: String,
) {
  Row(
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier
        .padding(top = 2.dp)
        .size(18.dp),
    )
    Column {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}
