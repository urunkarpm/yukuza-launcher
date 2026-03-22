package com.yukuza.launcher.ui.components

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
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
import com.yukuza.launcher.ui.theme.YukuzaColors
import com.yukuza.launcher.ui.theme.YukuzaMotion
import com.yukuza.launcher.ui.theme.YukuzaShapes
import com.yukuza.launcher.ui.theme.YukuzaSpacing

private val ScaleEasing = YukuzaMotion.focusEasing
private const val SCALE_DURATION = YukuzaMotion.FOCUS_SCALE_MS

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

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.18f else 1f,
        animationSpec = tween(durationMillis = SCALE_DURATION, easing = ScaleEasing),
        label = "scale",
    )
    val saturation by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = snap(),
        label = "saturation",
    )
    val ringAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = snap(),
        label = "ringAlpha",
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.5f,
        animationSpec = snap(),
        label = "labelAlpha",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(YukuzaSpacing.appIconWidth)
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
                .size(YukuzaSpacing.appIconSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(YukuzaShapes.appIcon)
                .border(
                    width = 2.dp,
                    color = YukuzaColors.ContentPrimary.copy(alpha = ringAlpha),
                    shape = YukuzaShapes.appIcon,
                )
                .background(
                    color = YukuzaColors.ContentPrimary.copy(alpha = ringAlpha * 0.15f),
                    shape = YukuzaShapes.appIcon,
                )
                .padding(YukuzaSpacing.xs),
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
                    .size(YukuzaSpacing.appIconDrawable)
                    .clip(YukuzaShapes.appIconInner),
            )
        }
        Spacer(Modifier.height(YukuzaSpacing.md))
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            color = YukuzaColors.ContentPrimary.copy(alpha = labelAlpha),
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
