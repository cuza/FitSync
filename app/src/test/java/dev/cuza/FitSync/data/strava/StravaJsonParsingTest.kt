package dev.cuza.FitSync.data.strava

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class StravaJsonParsingTest {

    @Test
    fun `token response json parses into kotlin data class`() {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val adapter = moshi.adapter(StravaTokenResponse::class.java)
        val json = """
            {
              "token_type": "Bearer",
              "access_token": "access123",
              "refresh_token": "refresh123",
              "expires_at": 1735689600
            }
        """.trimIndent()

        val parsed = requireNotNull(adapter.fromJson(json))
        assertEquals("Bearer", parsed.tokenType)
        assertEquals("access123", parsed.accessToken)
        assertEquals("refresh123", parsed.refreshToken)
        assertEquals(1735689600L, parsed.expiresAt)
    }
}
