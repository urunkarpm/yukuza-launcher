package com.yukuza.launcher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yukuza.launcher.domain.model.AppInfo
import kotlinx.collections.immutable.ImmutableList

@Composable
fun AppRow(
    apps: ImmutableList<AppInfo>,
    focusedIndex: Int,
    isEditMode: Boolean,
    onFocus: (Int) -> Unit,
    onReorder: (List<String>) -> Unit,
    onLongPress: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editList by remember(apps) { mutableStateOf(apps.toMutableList()) }

    Column(
        modifier = modifier.semantics { isTraversalGroup = true },
    ) {
        Text(
            text = "APPS",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.45f),
            modifier = Modifier.padding(bottom = 12.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            itemsIndexed(
                items = if (isEditMode) editList else apps.toList(),
                key = { _, app -> app.packageName },
            ) { index, app ->
                AppIcon(
                    app = app,
                    isFocused = focusedIndex == index,
                    onFocus = { onFocus(index) },
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
