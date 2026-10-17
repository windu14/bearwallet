package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = MintSecondary,
  onPrimary = ForestGreenDark,
  primaryContainer = ForestGreenLight,
  onPrimaryContainer = MintLight,
  secondary = MintLight,
  onSecondary = ForestGreenDark,
  secondaryContainer = ForestGreenPrimary,
  onSecondaryContainer = MintContainer,
  background = Color(0xFF0F1713),
  surface = Color(0xFF16211C),
  onBackground = Color(0xFFEAF1ED),
  onSurface = Color(0xFFEAF1ED),
  surfaceVariant = Color(0xFF223029),
  onSurfaceVariant = Color(0xFFC0CEC6),
  outline = Color(0xFF384B42)
)

private val LightColorScheme = lightColorScheme(
  primary = ForestGreenPrimary,
  onPrimary = PureWhite,
  primaryContainer = SageSurface,
  onPrimaryContainer = ForestGreenPrimary,
  secondary = MintSecondary,
  onSecondary = PureWhite,
  secondaryContainer = MintContainer,
  onSecondaryContainer = ForestGreenDark,
  tertiary = SageMuted,
  onTertiary = PureWhite,
  background = WarmWhite,
  surface = PureWhite,
  onBackground = TextPrimary,
  onSurface = TextPrimary,
  surfaceVariant = SurfaceVariantColor,
  onSurfaceVariant = TextSecondary,
  outline = BorderLight
)

@Composable
fun BearWalletTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = BearWalletTypography,
    content = content
  )
}
