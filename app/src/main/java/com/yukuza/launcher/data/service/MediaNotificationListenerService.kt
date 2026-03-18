package com.yukuza.launcher.data.service

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.yukuza.launcher.domain.model.MediaData
import kotlinx.coroutines.flow.MutableStateFlow

class MediaNotificationListenerService : NotificationListenerService() {

    companion object {
        val activeSessionFlow = MutableStateFlow<MediaData?>(null)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        updateMediaState()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        updateMediaState()
    }

    private fun updateMediaState() {
        val manager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val sessions = try {
            manager.getActiveSessions(
                ComponentName(this, MediaNotificationListenerService::class.java)
            )
        } catch (e: SecurityException) {
            activeSessionFlow.value = null
            return
        }

        val active = sessions.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        }

        if (active == null) {
            activeSessionFlow.value = null
            return
        }

        val meta = active.metadata ?: return
        val sourceLabel = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(active.packageName, 0)
            ).toString()
        } catch (e: Exception) {
            active.packageName
        }

        activeSessionFlow.value = MediaData(
            trackTitle = meta.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "",
            artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "",
            albumArtUri = meta.getString(MediaMetadata.METADATA_KEY_ART_URI),
            elapsedMs = active.playbackState?.position ?: 0L,
            durationMs = meta.getLong(MediaMetadata.METADATA_KEY_DURATION),
            isPlaying = true,
            sourceAppLabel = sourceLabel,
        )
    }
}
