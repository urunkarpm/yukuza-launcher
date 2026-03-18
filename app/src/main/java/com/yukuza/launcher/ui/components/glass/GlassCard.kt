package com.yukuza.launcher.ui.components.glass

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader as AndroidShader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.yukuza.launcher.ui.theme.YukuzaColors

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current.density
    Box(
        modifier = modifier
            .background(
                color = YukuzaColors.GlassSurface,
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = if (density >= 2f) 0.5.dp else 1.dp,
                color = YukuzaColors.GlassBorder,
                shape = RoundedCornerShape(12.dp),
            )
            .graphicsLayer {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    @Suppress("NewApi")
                    renderEffect = AndroidRenderEffect
                        .createBlurEffect(30f, 30f, AndroidShader.TileMode.CLAMP)
                        .asComposeRenderEffect()
                }
            },
        content = content,
    )
}
