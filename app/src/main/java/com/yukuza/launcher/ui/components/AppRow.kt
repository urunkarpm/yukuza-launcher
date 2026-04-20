package com.yukuza.launcher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yukuza.launcher.domain.model.AppInfo
import com.yukuza.launcher.domain.model.AppInfo.Companion.PACKAGE_TV_SETTINGS
import com.yukuza.launcher.ui.theme.YukuzaColors
import kotlinx.collections.immutable.ImmutableList

@Composable
fun AppRow(
    apps: ImmutableList<AppInfo>,
    focusedIndex: Int,
    isEditMode: Boolean,
    onFocus: (Int) -> Unit,
    onLaunch: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    onLongPress: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editList by remember(apps) { mutableStateOf(apps.toMutableList()) }

    val settingsIndex = apps.indexOfFirst { it.packageName == PACKAGE_TV_SETTINGS }
    val settingsFocusRequester = remember { FocusRequester() }
    var rowHasFocus by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .semantics { isTraversalGroup = true }
            .onFocusChanged { state ->
                if (state.hasFocus && !rowHasFocus && settingsIndex >= 0) {
                    settingsFocusRequester.requestFocus()
                }
                rowHasFocus = state.hasFocus
            },
    ) {
        Text(
            text = androidx.compose.ui.res.stringResource(com.yukuza.launcher.R.string.apps_title),
            style = MaterialTheme.typography.titleMedium,
            color = YukuzaColors.TextSecondary,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            modifier = Modifier.padding(bottom = 16.dp, start = 8.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            itemsIndexed(
                items = if (isEditMode) editList else apps.toList(),
                key = { _, app -> app.packageName },
            ) { index, app ->
                AppIcon(
                    app = app,
                    isFocused = focusedIndex == index && rowHasFocus,
                    onFocus = { onFocus(index) },
                    onLaunch = { onLaunch(app.packageName) },
                    modifier = if (index == settingsIndex) Modifier.focusRequester(settingsFocusRequester) else Modifier,
                    onLongPress = {
                        if (isEditMode) {
                            // Move focused item one position right in edit mode
                            if (index < editList.size - 1) {
                                val tmp = editList[index]
                                editList = editList.toMutableList().apply {
                                    set(index, editList[index + 1])
                                    set(index + 1, tmp)
                                }
                                onReorder(editList.map { it.packageName })
                            }
                        } else {
                            onLongPress(app)
                        }
                    },
                )
            }
        }
    }
}
