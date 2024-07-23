package org.owntracks.android.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

private val lightScheme = lightColors(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    error = errorLight,
    onError = onErrorLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
)

private val darkScheme = darkColors(
    primary = primaryDark,
    onPrimary = onPrimaryDark,

    secondary = secondaryDark,
    onSecondary = onSecondaryDark,

    error = errorDark,
    onError = onErrorDark,

    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
)

@Composable
fun AppTheme(
  darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable() () -> Unit
) {
  val colorScheme = when {
    darkTheme -> darkScheme
    else -> lightScheme
  }

  MaterialTheme(
          colors = colorScheme,
          typography = Typography(),
          content = content,
  )
}
