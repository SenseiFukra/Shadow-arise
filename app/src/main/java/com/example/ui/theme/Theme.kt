package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = GlowPurple,
    secondary = BrightPurpleHighlight,
    tertiary = TextSecondary,
    background = DeepPurpleBg,
    surface = CardPurpleBg,
    onPrimary = DeepPurpleBg,
    onSecondary = DeepPurpleBg,
    onTertiary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary
  )

private val LightColorScheme = DarkColorScheme // Always maintain dark fantasy theme!

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme
  dynamicColor: Boolean = false, // Force custom theme colors
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme // Standard Solo Leveling theme override

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
