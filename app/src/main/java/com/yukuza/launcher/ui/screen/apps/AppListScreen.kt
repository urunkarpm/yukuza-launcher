package com.yukuza.launcher.ui.screen.apps

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvGridItemSpan
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import com.yukuza.launcher.domain.model.AppInfo
import com.yukuza.launcher.ui.components.AppIcon
import com.yukuza.launcher.ui.components.aurora.AuroraBackground
import com.yukuza.launcher.ui.overlay.AppContextMenuOverlay
import androidx.lifecycle.Lifecycle

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppListScreen(
    viewModel: AppListViewModel,
    onBack: () -> Unit,
) {
    val visibleApps by viewModel.visibleApps.collectAsStateWithLifecycle()
    val hiddenApps by viewModel.hiddenApps.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    val context = LocalContext.current

    val uninstallLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { selectedApp = null }

    // Refresh on resume (e.g. after disabling an app in Settings)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val columns = if (screenWidthDp > 3000) 8 else 6

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()
        Column(
            Modifier
                .fillMaxSize()
                .padding(40.dp),
        ) {
            val searchAppsContentDescription = androidx.compose.ui.res.stringResource(com.yukuza.launcher.R.string.search_apps_content_description)
            OutlinedTextField(
                value = query,
                onValueChange = { q ->
                    query = q
                    viewModel.onSearch(q)
                },
                placeholder = { Text(androidx.compose.ui.res.stringResource(com.yukuza.launcher.R.string.search_apps_placeholder)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = searchAppsContentDescription },
            )
            Spacer(Modifier.height(24.dp))
            TvLazyVerticalGrid(
                columns = TvGridCells.Fixed(columns),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier
                    .semantics { isTraversalGroup = true }
                    .focusProperties { exit = { FocusRequester.Cancel } },
            ) {
                // Visible apps
                items(visibleApps, key = { it.packageName }) { app ->
                    AppIcon(
                        app = app,
                        isFocused = false,
                        onFocus = {},
                        onLaunch = {},
                        onLongPress = { selectedApp = app },
                    )
                }

                // Hidden Apps section — only shown when non-empty
                if (hiddenApps.isNotEmpty()) {
                    item(span = { TvGridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "Hidden Apps",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                        )
                    }
                    items(hiddenApps, key = { "hidden_${it.packageName}" }) { app ->
                        AppIcon(
                            app = app,
                            isFocused = false,
                            onFocus = {},
                            onLaunch = {
                                context.packageManager
                                    .getLaunchIntentForPackage(app.packageName)
                                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    ?.let { context.startActivity(it) }
                            },
                            onLongPress = { selectedApp = app },
                            isHidden = true,
                        )
                    }
                }
            }
        }

        // Context menu overlay
        selectedApp?.let { app ->
            AppContextMenuOverlay(
                app = app,
                onDismiss = { selectedApp = null },
                onHide = { viewModel.hideApp(app.packageName) },
                onUnhide = { viewModel.unhideApp(app.packageName) },
                onEnterEditMode = null,
            )
        }
    }
}
