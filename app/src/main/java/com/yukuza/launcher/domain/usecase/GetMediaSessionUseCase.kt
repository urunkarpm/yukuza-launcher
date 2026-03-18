package com.yukuza.launcher.domain.usecase

import com.yukuza.launcher.data.repository.MediaRepository
import com.yukuza.launcher.domain.model.MediaData
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMediaSessionUseCase @Inject constructor(private val repo: MediaRepository) {
    operator fun invoke(): Flow<MediaData?> = repo.getActiveMedia()
}
