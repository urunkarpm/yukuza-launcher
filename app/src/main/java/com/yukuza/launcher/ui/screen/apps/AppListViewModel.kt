package com.yukuza.launcher.ui.screen.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yukuza.launcher.domain.model.AppInfo
import com.yukuza.launcher.domain.usecase.GetAppsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppListViewModel @Inject constructor(
    getApps: GetAppsUseCase,
) : ViewModel() {

    private val allApps = getApps()
        .stateIn(viewModelScope, SharingStarted.Eagerly, persistentListOf())

    private val query = MutableStateFlow("")

    val filteredApps: StateFlow<ImmutableList<AppInfo>> =
        combine(allApps, query) { apps, q ->
            if (q.isBlank()) apps
            else apps.filter { it.label.contains(q, ignoreCase = true) }.toImmutableList()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, persistentListOf())

    fun onSearch(q: String) {
        query.value = q
    }
}
