package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.OrderStatus
import com.example.data.model.ServiceCategory
import com.example.ui.theme.PixyDeepNavy
import com.example.ui.theme.PixyRoyalBlue
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusErrorBg
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusInfoBg
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessBg
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningBg
import com.example.util.formatInr

@Composable
fun StudioBrandMark(
  modifier: Modifier = Modifier,
  size: Dp = 42.dp,
) {
  Surface(
    modifier = modifier.size(size),
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.primary,
    shadowElevation = 2.dp,
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.linearGradient(
            colors = listOf(PixyRoyalBlue, PixyDeepNavy),
          )
        ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Filled.CameraAlt,
        contentDescription = "Studio Pixy logo",
        tint = Color.White,
        modifier = Modifier.size(size * 0.56f),
      )
    }
  }
}

@Composable
fun ServiceImageThumbnail(
  imageUrl: String,
  category: ServiceCategory,
  contentDescription: String,
  modifier: Modifier = Modifier,
) {
  val fallbackRes = when (category) {
    ServiceCategory.PRINT -> R.drawable.img_photo_printing
    ServiceCategory.FRAME -> R.drawable.img_photo_frames
    ServiceCategory.PACKAGE -> R.drawable.img_wedding_packages
  }
  val drawableRes = when (imageUrl) {
    "drawable:img_hero_studio" -> R.drawable.img_hero_studio
    "drawable:img_photo_printing" -> R.drawable.img_photo_printing
    "drawable:img_photo_frames" -> R.drawable.img_photo_frames
    "drawable:img_wedding_packages" -> R.drawable.img_wedding_packages
    else -> null
  }

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant),
    contentAlignment = Alignment.Center,
  ) {
    when {
      drawableRes != null -> {
        Image(
          painter = painterResource(id = drawableRes),
          contentDescription = contentDescription,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }
      imageUrl.startsWith("http") || imageUrl.startsWith("file:") || imageUrl.startsWith("content:") -> {
        AsyncImage(
          model = imageUrl,
          contentDescription = contentDescription,
          placeholder = painterResource(id = fallbackRes),
          error = painterResource(id = fallbackRes),
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }
      else -> {
        Image(
          painter = painterResource(id = fallbackRes),
          contentDescription = contentDescription,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }
    }
  }
}

@Composable
fun OrderStatusBadge(
  status: OrderStatus,
  modifier: Modifier = Modifier,
) {
  val (bgColor, fgColor, icon) = when (status) {
    OrderStatus.PENDING -> Triple(StatusWarningBg, StatusWarning, Icons.Filled.HourglassTop)
    OrderStatus.ACCEPTED -> Triple(StatusInfoBg, StatusInfo, Icons.Filled.ThumbUp)
    OrderStatus.IN_PROGRESS -> Triple(Color(0xFFE0F2FE), Color(0xFF0369A1), Icons.Filled.PhotoLibrary)
    OrderStatus.COMPLETED -> Triple(StatusSuccessBg, StatusSuccess, Icons.Filled.CheckCircle)
    OrderStatus.REJECTED -> Triple(StatusErrorBg, StatusError, Icons.Filled.Cancel)
  }

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(999.dp),
    color = bgColor,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = fgColor,
        modifier = Modifier.size(14.dp),
      )
      Text(
        text = status.label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = fgColor,
      )
    }
  }
}

@Composable
fun PopularBadge(modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(999.dp),
    color = MaterialTheme.colorScheme.primaryContainer,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = Icons.Filled.Star,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(12.dp),
      )
      Text(
        text = "Popular",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

@Composable
fun PriceCalculatorSummaryCard(
  serviceName: String,
  quantity: Long,
  unitPrice: Long,
  totalAmount: Long,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "PRICE CALCULATOR",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
        )
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
        ) {
          Text(
            text = "Instant Estimate",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
      CalculatorRow(label = "Service Name", value = serviceName)
      Spacer(modifier = Modifier.height(6.dp))
      CalculatorRow(label = "Unit Price", value = formatInr(unitPrice))
      Spacer(modifier = Modifier.height(6.dp))
      CalculatorRow(label = "Quantity", value = "× $quantity")
      Spacer(modifier = Modifier.height(10.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp)
          .background(MaterialTheme.colorScheme.outlineVariant)
      )
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Total Price",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = formatInr(totalAmount),
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
private fun CalculatorRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.width(12.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}
