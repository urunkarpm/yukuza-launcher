package com.yukuza.launcher.data.remote

import com.yukuza.launcher.data.remote.dto.GithubReleaseDto
import retrofit2.http.GET

// Base URL: https://api.github.com/
interface GithubReleasesApi {
    @GET("repos/prasenjeet-urunkar/yukuza-launcher/releases/latest")
    suspend fun getLatestRelease(): GithubReleaseDto
}
