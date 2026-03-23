package com.yukuza.launcher.ui.overlay

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yukuza.launcher.domain.model.AppInfo

@Composable
fun AppContextMenuOverlay(
    app: AppInfo,
    onDismiss: () -> Unit,
    onHide: () -> Unit,
    onUnhide: () -> Unit,
    onEnterEditMode: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val isSystemApp = remember(app.packageName) {
        try {
            val flags = context.packageManager.getApplicationInfo(app.packageName, 0).flags
            (flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
        } catch (e: Exception) {
            false
        }
    }

    val uninstallLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { onDismiss() }

    BackHandler { onDismiss() }

    val firstButtonFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        firstButtonFocusRequester.requestFocus()
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Dim scrim — tap to dismiss
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable { onDismiss() },
        )

        // Icon + side panel
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.align(Alignment.Center),
        ) {
            // Highlighted app icon with dominantColor ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        width = 3.dp,
                        color = app.dominantColor,
                        shape = RoundedCornerShape(20.dp),
                    ),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(
                            try { context.packageManager.getApplicationIcon(app.packageName) }
                            catch (e: Exception) { null }
                        )
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(14.dp)),
                )
            }

            Spacer(Modifier.width(24.dp))

            // Side panel
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E1E2E))
                    .padding(8.dp),
            ) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )

                // Hide / Unhide
                ContextMenuButton(
                    icon = if (app.isHidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    label = if (app.isHidden) "Unhide" else "Hide",
                    focusRequester = firstButtonFocusRequester,
                    onClick = {
                        if (app.isHidden) onUnhide() else onHide()
                        onDismiss()
                    },
                )

                // Uninstall or Disable
                ContextMenuButton(
                    icon = Icons.Rounded.Delete,
                    label = if (isSystemApp) "Disable" else "Uninstall",
                    tint = Color(0xFFFF6B6B),
                    onClick = {
                        if (isSystemApp) {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:${app.packageName}")
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                            onDismiss()
                        } else {
                            uninstallLauncher.launch(
                                Intent(
                                    Intent.ACTION_DELETE,
                                    Uri.parse("package:${app.packageName}")
                                )
                            )
                        }
                    },
                )

                // Edit Order — only on home row
                if (onEnterEditMode != null) {
                    ContextMenuButton(
                        icon = Icons.Rounded.Edit,
                        label = "Edit Order",
                        onClick = {
                            onEnterEditMode()
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ContextMenuButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    tint: Color = Color.White,
) {
    var isFocused by remember { mutableStateOf(false) }

    val bgAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.15f else 0.06f,
        animationSpec = tween(100),
        label = "bg",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = bgAlpha))
            .then(
                if (isFocused) Modifier.border(1.dp, tint.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                else Modifier
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
        )
    }
}
