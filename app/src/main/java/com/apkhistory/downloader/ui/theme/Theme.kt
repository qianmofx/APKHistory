package com.apkhistory.downloader.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = AppColors.Primary,
    onPrimary = Color.White,
    primaryContainer = AppColors.PrimaryLight,
    onPrimaryContainer = AppColors.Primary,
    secondary = Color(0xFF5856D6),
    onSecondary = Color.White,
    background = AppColors.Background,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.Surface,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.CardBackground,
    onSurfaceVariant = AppColors.TextSecondary,
    outline = AppColors.Border,
    outlineVariant = AppColors.Divider,
    error = AppColors.Error,
    onError = Color.White
)

@Composable
fun APKHistoryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
