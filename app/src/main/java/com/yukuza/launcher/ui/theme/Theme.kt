package com.yukuza.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    background        = YukuzaColors.DeepBlack,
    surface           = YukuzaColors.GlassSurface,
    onBackground      = Color.White,
    onSurface         = Color.White,
    primary           = YukuzaColors.AuroraPurple,
    secondary         = YukuzaColors.AuroraTeal,
    tertiary          = YukuzaColors.AuroraPink,
    error             = YukuzaColors.NetworkOffline,
)

@Composable
fun YukuzaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = YukuzaTypography,
        shapes      = YukuzaM3Shapes,
        content     = content,
    )
}
