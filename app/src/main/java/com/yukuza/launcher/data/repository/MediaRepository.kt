package com.yukuza.launcher.data.repository

import com.yukuza.launcher.data.service.MediaNotificationListenerService
import com.yukuza.launcher.domain.model.MediaData
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor() {
    fun getActiveMedia(): Flow<MediaData?> = MediaNotificationListenerService.activeSessionFlow
}
