package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val BStUsphereLightColors = lightColorScheme(
  primary = BstuCobalt,
  onPrimary = Color.White,
  primaryContainer = BstuCobaltContainer,
  onPrimaryContainer = BstuOnCobaltContainer,
  secondary = BstuSlate,
  onSecondary = Color.White,
  secondaryContainer = BstuSlateContainer,
  onSecondaryContainer = BstuOnSlateContainer,
  tertiary = BstuAmber,
  onTertiary = Color.White,
  tertiaryContainer = BstuAmberContainer,
  onTertiaryContainer = BstuOnAmberContainer,
  background = BstuLimestone,
  onBackground = BstuInk,
  surface = BstuLimestone,
  onSurface = BstuInk,
  surfaceVariant = BstuSurfaceWarm,
  onSurfaceVariant = BstuInkMuted,
  surfaceContainerLowest = BstuSurfaceCard,
  surfaceContainerLow = Color(0xFFF8F5EF),
  surfaceContainer = BstuSurfaceWarm,
  surfaceContainerHigh = Color(0xFFECE8DF),
  outline = BstuOutline,
  outlineVariant = Color(0xFFE5E1D8),
  error = BstuError,
  errorContainer = BstuErrorContainer,
)

private val BStUsphereDarkColors = darkColorScheme(
  primary = BstuCobaltDark,
  onPrimary = BstuOnCobaltDark,
  primaryContainer = BstuCobaltContainerDark,
  onPrimaryContainer = BstuCobaltContainer,
  secondary = BstuSlateDark,
  onSecondary = BstuOnSlateContainer,
  secondaryContainer = BstuSlateContainerDark,
  onSecondaryContainer = BstuSlateContainer,
  tertiary = BstuAmberDark,
  onTertiary = Color(0xFF3F2300),
  tertiaryContainer = BstuAmberContainerDark,
  onTertiaryContainer = BstuAmberContainer,
  background = BstuNightBg,
  onBackground = BstuNightInk,
  surface = BstuNightBg,
  onSurface = BstuNightInk,
  surfaceVariant = BstuNightSurface,
  onSurfaceVariant = BstuNightMuted,
  surfaceContainerLowest = BstuNightBg,
  surfaceContainerLow = BstuNightSurface,
  surfaceContainer = BstuNightSurface,
  surfaceContainerHigh = BstuNightSurfaceElevated,
  outline = BstuNightOutline,
  outlineVariant = Color(0xFF233040),
)

val BStUsphereShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun BStUsphereTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> BStUsphereDarkColors
    else -> BStUsphereLightColors
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = BStUsphereTypography,
    shapes = BStUsphereShapes,
    content = content,
  )
}
