package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = VaultCyanLight,
  onPrimary = Color(0xFF00354E),
  primaryContainer = Color(0xFF004D70),
  onPrimaryContainer = Color(0xFFC3E8FF),
  secondary = VaultTeal,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFF00504B),
  onSecondaryContainer = Color(0xFF70F7EC),
  tertiary = VaultIndigo,
  background = DarkBackground,
  onBackground = DarkOnSurface,
  surface = DarkSurface,
  onSurface = DarkOnSurface,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkOnSurfaceVariant,
  outline = DarkOutline,
  error = VaultRose
)

private val LightColorScheme = lightColorScheme(
  primary = VaultCyan,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFC3E8FF),
  onPrimaryContainer = Color(0xFF001E2E),
  secondary = VaultTeal,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFB1F3EC),
  onSecondaryContainer = Color(0xFF00201D),
  tertiary = VaultIndigo,
  background = LightBackground,
  onBackground = LightOnSurface,
  surface = LightSurface,
  onSurface = LightOnSurface,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = LightOnSurfaceVariant,
  outline = LightOutline,
  error = VaultRose
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
