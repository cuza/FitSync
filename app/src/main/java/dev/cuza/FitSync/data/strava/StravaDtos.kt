package dev.cuza.FitSync.data.strava

import com.squareup.moshi.Json

data class StravaTokenResponse(
    @Json(name = "token_type") val tokenType: String,
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "expires_at") val expiresAt: Long,
)

data class StravaUploadResponse(
    val id: Long,
    @Json(name = "id_str") val idStr: String?,
    val status: String?,
    val error: String?,
    @Json(name = "activity_id") val activityId: Long?,
)

data class StravaUploadStatusResponse(
    val id: Long,
    @Json(name = "id_str") val idStr: String?,
    val status: String?,
    val error: String?,
    @Json(name = "activity_id") val activityId: Long?,
)

sealed class UploadOutcome {
    data class Success(
        val uploadId: String,
        val activityId: Long,
    ) : UploadOutcome()

    data class Failure(val message: String) : UploadOutcome()
}
