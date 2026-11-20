package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val StudioLightColorScheme = lightColorScheme(
  primary = PixyRoyalBlue,
  onPrimary = PixyPureWhite,
  primaryContainer = PixyIceBlue,
  onPrimaryContainer = PixyDeepNavy,
  secondary = PixyCobalt,
  onSecondary = PixyPureWhite,
  secondaryContainer = PixyFrostBlue,
  onSecondaryContainer = PixyDeepNavy,
  tertiary = PixySkyBlue,
  onTertiary = PixyPureWhite,
  tertiaryContainer = Color(0xFFE0F2FE),
  onTertiaryContainer = Color(0xFF0C4A6E),
  background = PixyPorcelainWhite,
  onBackground = PixyTextPrimary,
  surface = PixyPureWhite,
  onSurface = PixyTextPrimary,
  surfaceVariant = PixySurfaceTint,
  onSurfaceVariant = PixyTextSecondary,
  outline = PixyOutline,
  outlineVariant = Color(0xFFE2E8F0),
  error = StatusError,
  onError = PixyPureWhite,
  errorContainer = StatusErrorBg,
  onErrorContainer = Color(0xFF7F1D1D),
)

private val StudioDarkColorScheme = darkColorScheme(
  primary = Color(0xFF60A5FA),
  onPrimary = Color(0xFF052444),
  primaryContainer = Color(0xFF0F3D6B),
  onPrimaryContainer = Color(0xFFDBEAFE),
  secondary = Color(0xFF93C5FD),
  onSecondary = Color(0xFF082F49),
  secondaryContainer = Color(0xFF1E3A5F),
  onSecondaryContainer = Color(0xFFE0F2FE),
  tertiary = Color(0xFF38BDF8),
  onTertiary = Color(0xFF082F49),
  background = Color(0xFF0B1424),
  onBackground = Color(0xFFF1F5F9),
  surface = Color(0xFF111C2E),
  onSurface = Color(0xFFF1F5F9),
  surfaceVariant = Color(0xFF1A2942),
  onSurfaceVariant = Color(0xFFCBD5E1),
  outline = Color(0xFF334155),
  outlineVariant = Color(0xFF1E293B),
)

private val StudioShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun StudioPixyTheme(
  darkTheme: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) StudioDarkColorScheme else StudioLightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = StudioPixyTypography,
    shapes = StudioShapes,
    content = content,
  )
}
