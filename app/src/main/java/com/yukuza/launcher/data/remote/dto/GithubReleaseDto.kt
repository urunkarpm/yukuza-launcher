package com.yukuza.launcher.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GithubReleaseDto(
    @Json(name = "tag_name") val tagName: String,
    @Json(name = "body") val body: String,
    @Json(name = "assets") val assets: List<GithubAssetDto>,
)

@JsonClass(generateAdapter = true)
data class GithubAssetDto(
    @Json(name = "name") val name: String,
    @Json(name = "browser_download_url") val browserDownloadUrl: String,
)
