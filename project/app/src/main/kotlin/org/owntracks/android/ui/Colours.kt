package org.owntracks.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.owntracks.android.ui.Colours.otAccent
import org.owntracks.android.ui.Colours.otDarkerBlue
import org.owntracks.android.ui.Colours.otPrimaryBlue
import org.owntracks.android.ui.Colours.white

object Colours {
  // OwnTracks color scheme from colors.xml
  val otPrimaryBlue = Color(0xFF3F72B5)
  val otDarkerBlue = Color(0xFF305E9F)
  val otAccent = Color(0xFF31ABA6)
  val white = Color(0xFFFFFFFF)
}

@Composable
fun colorScheme(): androidx.compose.material3.ColorScheme {
  return if (isSystemInDarkTheme()) {
    darkColorScheme(
        primary = white,
        onPrimary = otPrimaryBlue,
        primaryContainer = otDarkerBlue,
        onPrimaryContainer = white,
        secondary = otAccent,
        onSecondary = white,
        background = otPrimaryBlue,
        onBackground = white,
        surface = Color(0xFFFF9900),
        onSurface = white)
  } else {
    lightColorScheme(
        primary = otPrimaryBlue,
        onPrimary = white,
        primaryContainer = otDarkerBlue,
        onPrimaryContainer = white,
        secondary = otAccent,
        onSecondary = white,
        background = otPrimaryBlue,
        onBackground = white,
        surface = otPrimaryBlue,
        onSurface = white)
  }
}
