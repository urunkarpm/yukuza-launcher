package com.yukuza.launcher.ui.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yukuza.launcher.domain.model.AppInfo
import com.yukuza.launcher.domain.model.AppInfo.Companion.PACKAGE_TV_SETTINGS

private val FocusEasing = FastOutSlowInEasing
private const val FOCUS_DURATION = 180
private const val UNFOCUS_DURATION = 80

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIcon(
    app: AppInfo,
    isFocused: Boolean,
    onFocus: () -> Unit,
    onLaunch: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val focusSpec = tween<Float>(durationMillis = FOCUS_DURATION, easing = FocusEasing)
    val unfocusSpec = tween<Float>(durationMillis = UNFOCUS_DURATION, easing = FocusEasing)

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.18f else 1f,
        animationSpec = if (isFocused) focusSpec else unfocusSpec,
        label = "scale",
    )
    val saturation by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = if (isFocused) focusSpec else unfocusSpec,
        label = "saturation",
    )
    val ringAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = if (isFocused) focusSpec else unfocusSpec,
        label = "ringAlpha",
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.5f,
        animationSpec = if (isFocused) focusSpec else unfocusSpec,
        label = "labelAlpha",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(88.dp)
            .onFocusChanged { if (it.isFocused) onFocus() }
            .focusable()
            .combinedClickable(
                onClick = { onLaunch(); launchApp(context, app.packageName) },
                onLongClick = onLongPress,
            )
            .semantics { contentDescription = "${app.label}, app icon" },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(20.dp))
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = ringAlpha),
                    shape = RoundedCornerShape(20.dp),
                )
                .background(
                    color = Color.White.copy(alpha = ringAlpha * 0.15f),
                    shape = RoundedCornerShape(20.dp),
                )
                .padding(4.dp),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(appIcon(context, app.packageName))
                    .build(),
                contentDescription = null,
                colorFilter = ColorFilter.colorMatrix(
                    ColorMatrix().apply { setToSaturation(saturation) }
                ),
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = labelAlpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun appIcon(context: Context, packageName: String) =
    try { context.packageManager.getApplicationIcon(packageName) }
    catch (e: Exception) {
        context.packageManager.getApplicationIcon("android")
    }

private fun launchApp(context: Context, packageName: String) {
    if (packageName == PACKAGE_TV_SETTINGS) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        return
    }
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    intent?.let { context.startActivity(it) }
}
