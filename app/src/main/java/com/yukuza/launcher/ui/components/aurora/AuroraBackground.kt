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
import androidx.compose.ui.graphics.graphicsLayer
import com.yukuza.launcher.ui.theme.YukuzaColors

data class BlobDef(
    val color: Color,
    val baseOffset: Offset,
    val drift: Offset,
    val widthFraction: Float,
    val heightFraction: Float,
)

@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")

    val offsets = (0..4).map { i ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 10_000 + i * 3_000,
                    easing = EaseInOut,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "blob_$i",
        )
    }

    val blobDefs = listOf(
        BlobDef(YukuzaColors.AuroraPurple.copy(alpha = 0.55f), Offset(-0.08f, -0.15f), Offset(0.04f,  0.07f), 0.65f, 0.60f),
        BlobDef(YukuzaColors.AuroraTeal.copy(alpha = 0.45f),   Offset(0.53f,  -0.10f), Offset(-0.05f, 0.05f), 0.55f, 0.55f),
        BlobDef(YukuzaColors.AuroraPink.copy(alpha = 0.30f),   Offset(0.28f,   0.08f), Offset(0.05f,  0.03f), 0.50f, 0.45f),
        BlobDef(YukuzaColors.AuroraBlue.copy(alpha = 0.28f),   Offset(0.42f,   0.00f), Offset(-0.04f, 0.05f), 0.40f, 0.40f),
        BlobDef(Color(0xFF9B82F5).copy(alpha = 0.38f),          Offset(0.62f,  -0.05f), Offset(0.04f, -0.04f), 0.38f, 0.35f),
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        drawRect(color = YukuzaColors.DeepBlack)

        blobDefs.forEachIndexed { i, blob ->
            val t = offsets[i].value
            val cx = (blob.baseOffset.x + blob.drift.x * t) * size.width
            val cy = (blob.baseOffset.y + blob.drift.y * t) * size.height
            val radius = blob.widthFraction * size.width * 0.5f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(blob.color, Color.Transparent),
                    center = Offset(cx, cy),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(cx, cy),
                blendMode = BlendMode.Screen,
            )
        }

        // Vignette
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0xA0020108)),
                radius = size.minDimension * 0.8f,
            ),
        )
    }
}
