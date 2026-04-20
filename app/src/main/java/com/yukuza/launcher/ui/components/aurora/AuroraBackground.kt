package com.yukuza.launcher.ui.components.aurora

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import com.yukuza.launcher.ui.theme.YukuzaColors

private data class BlobDef(
    val color: Color,
    val baseOffset: Offset, // normalised 0..1
    val drift: Offset,      // normalised drift magnitude
    val radius: Float,      // fraction of screen height
    val xStretch: Float,    // horizontal stretch > 1 = wide nebula cloud
    val durationMs: Int,
)

private val blobs = listOf(
    BlobDef(YukuzaColors.AuroraPurple.copy(alpha = 0.35f), Offset(0.10f, 0.25f), Offset(0.05f,  0.03f), 0.55f, 2.0f, 22000),
    BlobDef(YukuzaColors.AuroraPink.copy(alpha = 0.28f), Offset(0.75f, 0.20f), Offset(-0.06f, 0.04f), 0.48f, 1.7f, 19000),
    BlobDef(YukuzaColors.AuroraBlue.copy(alpha = 0.30f), Offset(0.50f, 0.65f), Offset(0.04f, -0.05f), 0.50f, 2.2f, 24000),
    BlobDef(YukuzaColors.AuroraTeal.copy(alpha = 0.25f), Offset(0.22f, 0.70f), Offset(0.06f, -0.03f), 0.42f, 1.8f, 18000),
)

@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")

    val offsets = blobs.mapIndexed { i, blob ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = blob.durationMs, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "blob_$i",
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        // Deep modern dark background
        drawRect(color = YukuzaColors.BackgroundDarker)

        blobs.forEachIndexed { i, blob ->
            val t = offsets[i].value
            val cx = (blob.baseOffset.x + blob.drift.x * t) * size.width
            val cy = (blob.baseOffset.y + blob.drift.y * t) * size.height
            val radius = blob.radius * size.height

            withTransform({
                scale(scaleX = blob.xStretch, scaleY = 1f, pivot = Offset(cx, cy))
            }) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to blob.color,
                            0.35f to blob.color.copy(alpha = blob.color.alpha * 0.5f),
                            1.00f to Color.Transparent,
                        ),
                        center = Offset(cx, cy),
                        radius = radius,
                    ),
                    radius = radius,
                    center = Offset(cx, cy),
                    blendMode = BlendMode.Screen,
                )
            }
        }

        // Subtle vignette for better content readability
        drawRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to Color.Transparent,
                    0.50f to YukuzaColors.BackgroundDark.copy(alpha = 0.3f),
                    1.00f to YukuzaColors.BackgroundDark.copy(alpha = 0.7f),
                ),
                radius = size.maxDimension * 0.75f,
            ),
        )
    }
}
