package dev.cuza.FitSync.data.strava

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface StravaApiService {

    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun exchangeCode(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") code: String,
        @Field("grant_type") grantType: String = "authorization_code",
    ): StravaTokenResponse

    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun refreshToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String = "refresh_token",
    ): StravaTokenResponse

    @Multipart
    @POST("api/v3/uploads")
    suspend fun uploadActivity(
        @Header("Authorization") authHeader: String,
        @Part file: MultipartBody.Part,
        @Part("data_type") dataType: RequestBody,
        @Part("activity_type") activityType: RequestBody,
        @Part("external_id") externalId: RequestBody,
    ): StravaUploadResponse

    @GET("api/v3/uploads/{uploadId}")
    suspend fun getUploadStatus(
        @Header("Authorization") authHeader: String,
        @Path("uploadId") uploadId: Long,
    ): StravaUploadStatusResponse
}
