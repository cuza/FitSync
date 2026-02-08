package dev.cuza.FitSync.data.strava

import android.content.Intent
import android.net.Uri
import android.util.Log
import dev.cuza.FitSync.BuildConfig
import dev.cuza.FitSync.domain.model.StravaActivityType
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.time.Instant

class StravaRepository(
    private val tokenStore: TokenStore,
) {

    private val api: StravaApiService by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()

        Retrofit.Builder()
            .baseUrl("https://www.strava.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(StravaApiService::class.java)
    }

    fun redirectUri(): String {
        return "${BuildConfig.STRAVA_REDIRECT_SCHEME}://${BuildConfig.STRAVA_REDIRECT_HOST}/callback"
    }

    fun authorizationIntent(): Intent {
        val uri = Uri.parse("https://www.strava.com/oauth/mobile/authorize")
            .buildUpon()
            .appendQueryParameter("client_id", BuildConfig.STRAVA_CLIENT_ID)
            .appendQueryParameter("redirect_uri", redirectUri())
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("approval_prompt", "auto")
            .appendQueryParameter("scope", "activity:write,activity:read_all")
            .build()

        return Intent(Intent.ACTION_VIEW, uri)
    }

    suspend fun handleAuthorizationRedirect(uri: Uri): Result<Unit> {
        val error = uri.getQueryParameter("error")
        if (!error.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Strava auth failed: $error"))
        }

        val code = uri.getQueryParameter("code")
            ?: return Result.failure(IllegalStateException("Missing authorization code"))

        if (BuildConfig.STRAVA_CLIENT_ID.isBlank() || BuildConfig.STRAVA_CLIENT_SECRET.isBlank()) {
            return Result.failure(
                IllegalStateException(
                    "Missing STRAVA_CLIENT_ID/STRAVA_CLIENT_SECRET in Gradle properties",
                ),
            )
        }

        return runCatching {
            val tokenResponse = api.exchangeCode(
                clientId = BuildConfig.STRAVA_CLIENT_ID,
                clientSecret = BuildConfig.STRAVA_CLIENT_SECRET,
                code = code,
            )
            tokenStore.saveTokens(
                StoredTokens(
                    accessToken = tokenResponse.accessToken,
                    refreshToken = tokenResponse.refreshToken,
                    expiresAtEpochSeconds = tokenResponse.expiresAt,
                ),
            )
            Log.d(TAG, "Strava token exchange successful")
        }
    }

    suspend fun logout() {
        tokenStore.clear()
    }

    suspend fun isLoggedIn(): Boolean {
        return tokenStore.readTokens() != null
    }

    suspend fun uploadTcx(
        tcxFile: File,
        activityType: StravaActivityType,
        externalId: String,
    ): UploadOutcome {
        val accessToken = ensureValidAccessToken()
            ?: return UploadOutcome.Failure("Strava auth expired. Please sign in again.")

        val uploadResponse = runCatching {
            api.uploadActivity(
                authHeader = bearer(accessToken),
                file = MultipartBody.Part.createFormData(
                    name = "file",
                    filename = tcxFile.name,
                    body = tcxFile.asRequestBody("application/xml".toMediaType()),
                ),
                dataType = "tcx".toRequestBody("text/plain".toMediaType()),
                activityType = activityType.uploadValue.toRequestBody("text/plain".toMediaType()),
                externalId = externalId.toRequestBody("text/plain".toMediaType()),
            )
        }.getOrElse { throwable ->
            return UploadOutcome.Failure(httpErrorMessage("Upload failed", throwable))
        }

        uploadResponse.error?.takeIf { it.isNotBlank() }?.let { error ->
            return UploadOutcome.Failure(error)
        }

        val uploadId = uploadResponse.id
        return pollUploadResult(uploadId)
    }

    private suspend fun pollUploadResult(uploadId: Long): UploadOutcome {
        repeat(MAX_POLL_ATTEMPTS) { attempt ->
            val accessToken = ensureValidAccessToken()
                ?: return UploadOutcome.Failure("Strava auth expired during upload polling")

            val status = runCatching {
                api.getUploadStatus(
                    authHeader = bearer(accessToken),
                    uploadId = uploadId,
                )
            }.getOrElse { throwable ->
                return UploadOutcome.Failure(httpErrorMessage("Unable to poll upload status", throwable))
            }

            status.error?.takeIf { it.isNotBlank() }?.let { error ->
                return UploadOutcome.Failure(error)
            }

            status.activityId?.let { activityId ->
                return UploadOutcome.Success(
                    uploadId = status.idStr ?: uploadId.toString(),
                    activityId = activityId,
                )
            }

            Log.d(TAG, "Upload $uploadId status='${status.status}' attempt=${attempt + 1}")
            delay(POLL_INTERVAL_MS)
        }

        return UploadOutcome.Failure("Strava upload timed out")
    }

    private suspend fun ensureValidAccessToken(): String? {
        val tokens = tokenStore.readTokens() ?: return null
        val now = Instant.now().epochSecond

        if (tokens.expiresAtEpochSeconds > now + TOKEN_EXPIRY_SKEW_SECONDS) {
            return tokens.accessToken
        }

        if (BuildConfig.STRAVA_CLIENT_ID.isBlank() || BuildConfig.STRAVA_CLIENT_SECRET.isBlank()) {
            return null
        }

        val refreshed = runCatching {
            api.refreshToken(
                clientId = BuildConfig.STRAVA_CLIENT_ID,
                clientSecret = BuildConfig.STRAVA_CLIENT_SECRET,
                refreshToken = tokens.refreshToken,
            )
        }.getOrElse { throwable ->
            Log.e(TAG, "Failed to refresh token", throwable)
            return null
        }

        val newTokens = StoredTokens(
            accessToken = refreshed.accessToken,
            refreshToken = refreshed.refreshToken,
            expiresAtEpochSeconds = refreshed.expiresAt,
        )
        tokenStore.saveTokens(newTokens)
        return newTokens.accessToken
    }

    private fun bearer(token: String): String = "Bearer $token"

    private fun httpErrorMessage(prefix: String, throwable: Throwable): String {
        return if (throwable is HttpException) {
            val code = throwable.code()
            if (code == 429) {
                "$prefix: rate limited by Strava (429)"
            } else {
                "$prefix: HTTP $code"
            }
        } else {
            "$prefix: ${throwable.message ?: "unknown error"}"
        }
    }

    companion object {
        private const val TAG = "StravaRepository"
        private const val POLL_INTERVAL_MS = 2_500L
        private const val MAX_POLL_ATTEMPTS = 16
        private const val TOKEN_EXPIRY_SKEW_SECONDS = 60L
    }
}
