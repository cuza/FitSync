package dev.cuza.FitSync.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dev.cuza.FitSync.domain.model.StravaActivityType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.io.IOException

data class UserSettings(
    val daysBack: Int = 7,
    val reuploadOnHashChange: Boolean = false,
    val overrides: Map<Int, StravaActivityType> = emptyMap(),
)

class SettingsRepository(context: Context) {

    private val dataStore = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile("settings.preferences_pb")
    }

    val settingsFlow: Flow<UserSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            UserSettings(
                daysBack = prefs[DAYS_BACK]?.coerceIn(1, 30) ?: 7,
                reuploadOnHashChange = prefs[REUPLOAD_ON_HASH_CHANGE] ?: false,
                overrides = decodeOverrides(prefs[OVERRIDES_JSON].orEmpty()),
            )
        }

    suspend fun updateDaysBack(days: Int) {
        dataStore.edit { prefs ->
            prefs[DAYS_BACK] = days.coerceIn(1, 30)
        }
    }

    suspend fun updateReuploadOnHashChange(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[REUPLOAD_ON_HASH_CHANGE] = enabled
        }
    }

    suspend fun putOverride(exerciseType: Int, activityType: StravaActivityType) {
        dataStore.edit { prefs ->
            val current = decodeOverrides(prefs[OVERRIDES_JSON].orEmpty()).toMutableMap()
            current[exerciseType] = activityType
            prefs[OVERRIDES_JSON] = encodeOverrides(current)
        }
    }

    suspend fun clearOverride(exerciseType: Int) {
        dataStore.edit { prefs ->
            val current = decodeOverrides(prefs[OVERRIDES_JSON].orEmpty()).toMutableMap()
            current.remove(exerciseType)
            prefs[OVERRIDES_JSON] = encodeOverrides(current)
        }
    }

    private fun encodeOverrides(overrides: Map<Int, StravaActivityType>): String {
        val json = JSONObject()
        overrides.forEach { (key, value) ->
            json.put(key.toString(), value.uploadValue)
        }
        return json.toString()
    }

    private fun decodeOverrides(payload: String): Map<Int, StravaActivityType> {
        if (payload.isBlank()) return emptyMap()

        return runCatching {
            val json = JSONObject(payload)
            json.keys().asSequence().mapNotNull { key ->
                val intKey = key.toIntOrNull() ?: return@mapNotNull null
                val activity = StravaActivityType.fromUploadValue(json.optString(key, ""))
                intKey to activity
            }.toMap()
        }.getOrElse { emptyMap() }
    }

    companion object {
        private val DAYS_BACK = intPreferencesKey("days_back")
        private val REUPLOAD_ON_HASH_CHANGE = booleanPreferencesKey("reupload_on_hash_change")
        private val OVERRIDES_JSON = stringPreferencesKey("type_overrides_json")
    }
}
