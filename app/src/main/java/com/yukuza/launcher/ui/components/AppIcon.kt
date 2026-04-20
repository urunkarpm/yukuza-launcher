package com.yukuza.launcher.ui.components

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
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

private val ScaleEasing = FastOutSlowInEasing
private const val SCALE_DURATION = 100

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIcon(
    app: AppInfo,
    isFocused: Boolean,
    onFocus: () -> Unit,
    onLaunch: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    isHidden: Boolean = false,
) {
    val context = LocalContext.current

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1f,
        animationSpec = tween(durationMillis = SCALE_DURATION, easing = ScaleEasing),
        label = "scale",
    )
    val saturation by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.3f,
        animationSpec = tween(durationMillis = 150),
        label = "saturation",
    )
    val ringAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "ringAlpha",
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.6f,
        animationSpec = tween(durationMillis = 150),
        label = "labelAlpha",
    )
    val shadowElevation by animateFloatAsState(
        targetValue = if (isFocused) 12f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "elevation",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(92.dp)
            .then(if (isHidden) Modifier.alpha(0.5f) else Modifier)
            .onFocusChanged { if (it.isFocused) onFocus() }
            .focusable()
            .combinedClickable(
                onClick = { onLaunch(); launchApp(context, app.packageName) },
                onLongClick = onLongPress,
            )
            .semantics { contentDescription = context.getString(com.yukuza.launcher.R.string.app_icon_content_description, app.label) },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(84.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.shadowElevation = shadowElevation
                    ambientShadowColor = YukuzaColors.PrimaryBlue.copy(alpha = 0.3f * ringAlpha)
                    spotShadowColor = YukuzaColors.PrimaryBlue.copy(alpha = 0.5f * ringAlpha)
                }
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 2.5.dp,
                    color = YukuzaColors.FocusRing.copy(alpha = ringAlpha),
                    shape = RoundedCornerShape(24.dp),
                )
                .background(
                    color = YukuzaColors.SurfaceCard.copy(alpha = 0.3f * ringAlpha + 0.1f),
                    shape = RoundedCornerShape(24.dp),
                )
                .padding(5.dp),
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
                    .size(68.dp)
                    .clip(RoundedCornerShape(18.dp)),
            )
            if (isHidden) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = YukuzaColors.TextSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopEnd)
                        .padding(3.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyMedium,
            color = YukuzaColors.TextPrimary.copy(alpha = labelAlpha),
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
    val pm = context.packageManager

    fun buildExplicitIntent(category: String): Intent? {
        val query = Intent(Intent.ACTION_MAIN).addCategory(category).setPackage(packageName)
        val ri = pm.resolveActivity(query, 0) ?: return null
        return Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName(ri.activityInfo.packageName, ri.activityInfo.name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
    }

    // Prefer the TV (Leanback) entry point; fall back to the standard launcher activity
    val intent = buildExplicitIntent(Intent.CATEGORY_LEANBACK_LAUNCHER)
        ?: buildExplicitIntent(Intent.CATEGORY_LAUNCHER)
    intent?.let { context.startActivity(it) }
}
