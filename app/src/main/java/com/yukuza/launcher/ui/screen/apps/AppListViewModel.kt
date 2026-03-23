package com.yukuza.launcher.ui.screen.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yukuza.launcher.data.repository.AppRepository
import com.yukuza.launcher.domain.model.AppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppListViewModel @Inject constructor(
    private val repository: AppRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    /** Visible (non-hidden) apps filtered by search query. */
    val visibleApps: StateFlow<ImmutableList<AppInfo>> =
        combine(repository.getVisibleApps(), query) { apps, q ->
            if (q.isBlank()) apps
            else apps.filter { it.label.contains(q, ignoreCase = true) }.toImmutableList()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, persistentListOf())

    /** Hidden apps filtered by search query. */
    val hiddenApps: StateFlow<ImmutableList<AppInfo>> =
        combine(repository.getHiddenApps(), query) { apps, q ->
            if (q.isBlank()) apps
            else apps.filter { it.label.contains(q, ignoreCase = true) }.toImmutableList()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, persistentListOf())

    fun onSearch(q: String) {
        query.value = q
    }

    fun hideApp(packageName: String) {
        viewModelScope.launch { repository.hideApp(packageName) }
    }

    fun unhideApp(packageName: String) {
        viewModelScope.launch { repository.unhideApp(packageName) }
    }

    /** Call on ON_RESUME to refresh the list after a disable action. */
    fun refresh() {
        repository.refresh()
    }
}
