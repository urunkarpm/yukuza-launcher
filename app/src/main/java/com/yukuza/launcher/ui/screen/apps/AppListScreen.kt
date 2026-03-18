package com.yukuza.launcher.ui.screen.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import com.yukuza.launcher.ui.components.AppIcon
import com.yukuza.launcher.ui.components.aurora.AuroraBackground

@Composable
fun AppListScreen(
    viewModel: AppListViewModel,
    onBack: () -> Unit,
) {
    val apps by viewModel.filteredApps.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    // Use 8 columns on 4K screens (width > 3000dp), 6 columns otherwise
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val columns = if (screenWidthDp > 3000) 8 else 6

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()
        Column(
            Modifier
                .fillMaxSize()
                .padding(40.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { q ->
                    query = q
                    viewModel.onSearch(q)
                },
                placeholder = { Text("Search apps…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Search apps" },
            )
            Spacer(Modifier.height(24.dp))
            TvLazyVerticalGrid(
                columns = TvGridCells.Fixed(columns),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.semantics { isTraversalGroup = true },
            ) {
                items(apps, key = { it.packageName }) { app ->
                    AppIcon(
                        app = app,
                        isFocused = false,
                        onFocus = {},
                        onLaunch = {},
                        onLongPress = {},
                    )
                }
            }
        }
    }
}
