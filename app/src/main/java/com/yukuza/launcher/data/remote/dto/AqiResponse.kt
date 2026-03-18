package com.yukuza.launcher.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AqiResponse(
    @Json(name = "current") val current: CurrentAqi,
) {
    @JsonClass(generateAdapter = true)
    data class CurrentAqi(
        @Json(name = "european_aqi") val europeanAqi: Int,
    )
}
