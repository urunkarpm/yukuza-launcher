package com.yukuza.launcher.ui.components.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yukuza.launcher.R
import com.yukuza.launcher.domain.model.MediaData
import com.yukuza.launcher.ui.components.glass.GlassCard
import com.yukuza.launcher.ui.theme.YukuzaColors
import com.yukuza.launcher.ui.theme.YukuzaMotion
import com.yukuza.launcher.ui.theme.YukuzaShapes
import com.yukuza.launcher.ui.theme.YukuzaSpacing

@Composable
fun pulseAlpha(): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    return infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(YukuzaMotion.PULSE_MS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    ).value
}

@Composable
fun NowPlayingWidget(
    data: MediaData?,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = data != null,
        enter = fadeIn() + scaleIn(initialScale = 0.95f),
        exit = fadeOut() + scaleOut(targetScale = 0.95f),
        modifier = modifier.testTag("now_playing"),
    ) {
        data ?: return@AnimatedVisibility

        // Album art rotation animation
        val infiniteTransition = rememberInfiniteTransition(label = "albumSpin")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(YukuzaMotion.ALBUM_SPIN_MS, easing = LinearEasing),
            ),
            label = "albumRotation",
        )

        GlassCard(modifier = Modifier.width(YukuzaSpacing.nowPlayingWidth)) {
            Box {
                // Dominant color bleed background
                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            Brush.radialGradient(
                                listOf(data.dominantColor.copy(alpha = 0.12f), Color.Transparent)
                            )
                        )
                )
                Column(Modifier.padding(horizontal = YukuzaSpacing.nowPlayingHorizontal, vertical = YukuzaSpacing.nowPlayingVertical)) {
                    // Source badge with pulsing dot
                    val pulse = pulseAlpha()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(YukuzaSpacing.pulseDotSize)
                                .background(YukuzaColors.NowPlayingGreen, CircleShape)
                                .graphicsLayer { alpha = pulse }
                        )
                        Spacer(Modifier.width(YukuzaSpacing.sm))
                        Text(
                            text = data.sourceAppLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = YukuzaColors.ContentQuaternary,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Spinning album art
                        AsyncImage(
                            model = data.albumArtUri,
                            contentDescription = androidx.compose.ui.res.stringResource(com.yukuza.launcher.R.string.album_art_content_description),
                            error = painterResource(R.drawable.ic_music_note),
                            modifier = Modifier
                                .size(YukuzaSpacing.albumArtSize)
                                .clip(YukuzaShapes.albumArt)
                                .graphicsLayer {
                                    if (data.isPlaying) rotationZ = rotation
                                },
                        )
                        Spacer(Modifier.width(YukuzaSpacing.xl))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = data.trackTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = YukuzaColors.ContentPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = data.artist,
                                style = MaterialTheme.typography.labelSmall,
                                color = YukuzaColors.ContentDimmed,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(YukuzaSpacing.md))
                            // Progress bar
                            val progress = if (data.durationMs > 0) {
                                (data.elapsedMs.toFloat() / data.durationMs.toFloat()).coerceIn(0f, 1f)
                            } else 0f
                            LinearProgressIndicator(
                                progress = { progress },
                                color = data.dominantColor,
                                trackColor = YukuzaColors.ContentGhost,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(YukuzaSpacing.progressBarHeight),
                            )
                        }
                    }
                }
            }
        }
    }
}
