package dev.cuza.FitSync.presentation.viewmodel

import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.cuza.FitSync.data.db.SyncSessionEntity
import dev.cuza.FitSync.data.db.UploadStatus
import dev.cuza.FitSync.data.preferences.UserSettings
import dev.cuza.FitSync.domain.model.StravaActivityType
import dev.cuza.FitSync.domain.sync.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val sessions: List<SyncSessionEntity> = emptyList(),
    val settings: UserSettings = UserSettings(),
    val knownSessionTypes: List<Int> = emptyList(),
    val requiredPermissions: Set<String> = emptySet(),
    val healthSdkStatus: Int = HealthConnectClient.SDK_UNAVAILABLE,
    val hasHealthPermissions: Boolean = false,
    val stravaLoggedIn: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSyncing: Boolean = false,
    val message: String? = null,
)

class MainViewModel(
    private val syncRepository: SyncRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                requiredPermissions = syncRepository.healthConnectRequiredPermissions(),
                knownSessionTypes = syncRepository.knownSessionTypes(),
                healthSdkStatus = syncRepository.healthConnectSdkStatus(),
            )
        }

        viewModelScope.launch {
            syncRepository.sessionsFlow.collect { sessions ->
                _uiState.update { state -> state.copy(sessions = sessions) }
            }
        }

        viewModelScope.launch {
            syncRepository.settingsFlow.collect { settings ->
                _uiState.update { state -> state.copy(settings = settings) }
            }
        }

        refreshState()
    }

    fun refreshState() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            val sdkStatus = syncRepository.healthConnectSdkStatus()
            val hasPermissions = if (sdkStatus == HealthConnectClient.SDK_AVAILABLE) {
                runCatching { syncRepository.hasHealthPermissions() }.getOrDefault(false)
            } else {
                false
            }
            val stravaLoggedIn = runCatching { syncRepository.isStravaLoggedIn() }.getOrDefault(false)

            _uiState.update {
                it.copy(
                    healthSdkStatus = sdkStatus,
                    hasHealthPermissions = hasPermissions,
                    stravaLoggedIn = stravaLoggedIn,
                    isRefreshing = false,
                )
            }

            if (hasPermissions && sdkStatus == HealthConnectClient.SDK_AVAILABLE) {
                scanSessions()
            }
        }
    }

    fun scanSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, message = null) }
            val result = syncRepository.scanRecentSessions()
            _uiState.update { state ->
                state.copy(
                    isRefreshing = false,
                    message = result.fold(
                        onSuccess = { count -> "Detected $count session(s)" },
                        onFailure = { error -> error.message ?: "Failed to read Health Connect" },
                    ),
                )
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, message = null) }
            val result = syncRepository.syncNow()
            _uiState.update { state ->
                state.copy(
                    isSyncing = false,
                    message = result.fold(
                        onSuccess = { summary ->
                            "Sync complete: ${summary.success}/${summary.attempted} uploaded, ${summary.failed} failed"
                        },
                        onFailure = { error -> error.message ?: "Sync failed" },
                    ),
                )
            }
            refreshState()
        }
    }

    fun onPermissionResult(granted: Set<String>) {
        val hasAll = granted.containsAll(_uiState.value.requiredPermissions)
        _uiState.update { it.copy(hasHealthPermissions = hasAll) }
        if (hasAll) {
            scanSessions()
        }
    }

    fun stravaLoginIntent(): Intent = syncRepository.stravaAuthIntent()

    fun handleStravaRedirect(uri: Uri) {
        viewModelScope.launch {
            val result = syncRepository.handleStravaRedirect(uri)
            _uiState.update { state ->
                state.copy(
                    message = result.fold(
                        onSuccess = { "Strava login succeeded" },
                        onFailure = { error -> error.message ?: "Strava login failed" },
                    ),
                )
            }
            refreshState()
        }
    }

    fun logoutStrava() {
        viewModelScope.launch {
            syncRepository.logoutStrava()
            _uiState.update {
                it.copy(
                    stravaLoggedIn = false,
                    message = "Strava logged out",
                )
            }
        }
    }

    fun updateDaysBack(days: Int) {
        viewModelScope.launch {
            syncRepository.updateDaysBack(days)
            scanSessions()
        }
    }

    fun updateReuploadPolicy(enabled: Boolean) {
        viewModelScope.launch {
            syncRepository.updateReuploadPolicy(enabled)
        }
    }

    fun setOverride(exerciseType: Int, activityType: StravaActivityType?) {
        viewModelScope.launch {
            if (activityType == null) {
                syncRepository.clearTypeOverride(exerciseType)
            } else {
                syncRepository.updateTypeOverride(exerciseType, activityType)
            }
            scanSessions()
        }
    }

    fun exerciseTypeLabel(exerciseType: Int): String = syncRepository.exerciseTypeLabel(exerciseType)

    fun statusLabel(status: UploadStatus): String = when (status) {
        UploadStatus.READY -> "Ready"
        UploadStatus.SYNCING -> "Syncing"
        UploadStatus.SYNCED -> "Synced"
        UploadStatus.FAILED -> "Failed"
        UploadStatus.SKIPPED -> "Skipped"
    }

    companion object {
        fun factory(syncRepository: SyncRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(syncRepository) as T
                }
            }
        }
    }
}
