package com.yukuza.launcher.ui.components

import android.content.Context
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yukuza.launcher.domain.model.AppInfo
import com.yukuza.launcher.ui.theme.YukuzaColors

@Composable
fun AppIcon(
    app: AppInfo,
    isFocused: Boolean,
    onFocus: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density

    val saturation by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "saturation",
    )
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale",
    )
    val offsetY by animateFloatAsState(
        targetValue = if (isFocused) -8f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "offsetY",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .focusable()
            .onFocusChanged { if (it.isFocused) onFocus() }
            .combinedClickable(
                onClick = { launchApp(context, app.packageName) },
                onLongClick = onLongPress,
            )
            .semantics { contentDescription = "${app.label}, app icon" },
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(context.packageManager.getApplicationIcon(app.packageName))
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationY = offsetY
                    colorFilter = ColorFilter.colorMatrix(
                        ColorMatrix().apply { setToSaturation(saturation) }
                    )
                    if (isFocused) {
                        shadowElevation = 16f
                        ambientShadowColor = app.dominantColor.toArgb()
                        spotShadowColor = app.dominantColor.toArgb()
                    }
                }
                .clip(RoundedCornerShape(18.dp))
                .border(
                    width = if (density >= 2f) 0.5.dp else 1.dp,
                    color = if (isFocused) app.dominantColor.copy(alpha = 0.35f)
                            else YukuzaColors.GlassBorder,
                    shape = RoundedCornerShape(18.dp),
                ),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isFocused) androidx.compose.ui.graphics.Color.White
                    else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun launchApp(context: Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    intent?.let { context.startActivity(it) }
}
