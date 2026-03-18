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
    BlobDef(Color(0xFF8C32FF).copy(alpha = 0.82f), Offset(0.08f, 0.28f), Offset(0.06f,  0.04f), 0.58f, 2.2f, 20000),
    BlobDef(Color(0xFFE628A0).copy(alpha = 0.72f), Offset(0.78f, 0.18f), Offset(-0.07f, 0.05f), 0.50f, 1.8f, 17000),
    BlobDef(Color(0xFF3C64FF).copy(alpha = 0.68f), Offset(0.48f, 0.68f), Offset(0.05f, -0.06f), 0.52f, 2.4f, 22000),
    BlobDef(Color(0xFF00B4DC).copy(alpha = 0.58f), Offset(0.20f, 0.72f), Offset(0.07f, -0.04f), 0.44f, 1.9f, 16000),
    BlobDef(Color(0xFFB43CFF).copy(alpha = 0.52f), Offset(0.62f, 0.48f), Offset(-0.05f, 0.06f), 0.40f, 2.0f, 19000),
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
        drawRect(color = YukuzaColors.DeepBlack)

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
                            0.40f to blob.color.copy(alpha = blob.color.alpha * 0.55f),
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

        // Vignette — transparent centre, heavy dark edge so UI content reads cleanly
        drawRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.00f to Color.Transparent,
                    0.55f to Color(0x28060210),
                    1.00f to Color(0xD0060210),
                ),
                radius = size.maxDimension * 0.70f,
            ),
        )
    }
}
