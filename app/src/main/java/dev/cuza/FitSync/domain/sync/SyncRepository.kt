package dev.cuza.FitSync.domain.sync

import android.util.Log
import dev.cuza.FitSync.data.db.SyncSessionDao
import dev.cuza.FitSync.data.db.SyncSessionEntity
import dev.cuza.FitSync.data.db.UploadStatus
import dev.cuza.FitSync.data.healthconnect.HealthConnectManager
import dev.cuza.FitSync.data.preferences.SettingsRepository
import dev.cuza.FitSync.data.preferences.UserSettings
import dev.cuza.FitSync.data.strava.StravaRepository
import dev.cuza.FitSync.data.strava.UploadOutcome
import dev.cuza.FitSync.domain.export.TcxExporter
import dev.cuza.FitSync.domain.mapping.ExerciseTypeMapper
import dev.cuza.FitSync.domain.model.StravaActivityType
import dev.cuza.FitSync.domain.model.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit

data class SyncRunResult(
    val attempted: Int,
    val success: Int,
    val failed: Int,
)

class SyncRepository(
    private val healthConnectManager: HealthConnectManager,
    private val settingsRepository: SettingsRepository,
    private val syncSessionDao: SyncSessionDao,
    private val exerciseTypeMapper: ExerciseTypeMapper,
    private val hashCalculator: SessionHashCalculator,
    private val tcxExporter: TcxExporter,
    private val stravaRepository: StravaRepository,
    private val exportsDir: File,
) {

    val sessionsFlow: Flow<List<SyncSessionEntity>> = syncSessionDao.observeAll()
    val settingsFlow: Flow<UserSettings> = settingsRepository.settingsFlow

    suspend fun scanRecentSessions(): Result<Int> {
        if (healthConnectManager.sdkStatus() != androidx.health.connect.client.HealthConnectClient.SDK_AVAILABLE) {
            return Result.failure(IllegalStateException("Health Connect unavailable on this device"))
        }
        if (!healthConnectManager.hasAllPermissions()) {
            return Result.failure(IllegalStateException("Health Connect permissions not granted"))
        }

        val settings = settingsRepository.settingsFlow.first()
        val workouts = readWorkoutsForSettings(settings)
        val now = Instant.now()

        val entities = workouts.map { workout ->
            val hash = hashCalculator.hash(workout)
            val existing = syncSessionDao.bySessionId(workout.healthConnectSessionId)
            val mappedType = exerciseTypeMapper.resolve(workout.exerciseType, settings.overrides)
            buildScannedEntity(
                workout = workout,
                mappedType = mappedType,
                hash = hash,
                existing = existing,
                settings = settings,
            )
        }

        if (entities.isNotEmpty()) {
            syncSessionDao.upsertAll(entities)
        }

        val oldCutoff = now.minus(90, ChronoUnit.DAYS)
        syncSessionDao.deleteOlderThan(oldCutoff)

        return Result.success(entities.size)
    }

    suspend fun syncNow(): Result<SyncRunResult> {
        val scanResult = scanRecentSessions()
        if (scanResult.isFailure) {
            return Result.failure(scanResult.exceptionOrNull() ?: IllegalStateException("Scan failed"))
        }

        if (!stravaRepository.isLoggedIn()) {
            return Result.failure(IllegalStateException("Strava not authenticated"))
        }

        val settings = settingsRepository.settingsFlow.first()
        val workouts = readWorkoutsForSettings(settings).associateBy { it.healthConnectSessionId }
        val pending = syncSessionDao.pendingUploads()
        if (pending.isEmpty()) {
            return Result.success(SyncRunResult(attempted = 0, success = 0, failed = 0))
        }

        var successCount = 0
        var failedCount = 0

        pending.forEach { pendingEntity ->
            val now = Instant.now()
            val inProgress = pendingEntity.copy(
                uploadStatus = UploadStatus.SYNCING,
                lastAttempt = now,
                error = null,
            )
            syncSessionDao.upsert(inProgress)

            val session = workouts[pendingEntity.healthConnectSessionId]
            if (session == null) {
                syncSessionDao.upsert(
                    inProgress.copy(
                        uploadStatus = UploadStatus.FAILED,
                        error = "Session not found in current Health Connect window",
                    ),
                )
                failedCount += 1
                return@forEach
            }

            val hash = hashCalculator.hash(session)
            val mappedType = exerciseTypeMapper.resolve(session.exerciseType, settings.overrides)
            val externalId = externalId(session.healthConnectSessionId, hash)

            val tcxFile = runCatching {
                tcxExporter.export(
                    session = session,
                    activityType = mappedType,
                    outputDir = exportsDir,
                )
            }.getOrElse { throwable ->
                syncSessionDao.upsert(
                    inProgress.copy(
                        uploadStatus = UploadStatus.FAILED,
                        lastSeenHash = hash,
                        mappedActivityType = mappedType.uploadValue,
                        error = "TCX export failed: ${throwable.message}",
                    ),
                )
                failedCount += 1
                return@forEach
            }

            when (val upload = stravaRepository.uploadTcx(tcxFile, mappedType, externalId)) {
                is UploadOutcome.Success -> {
                    tcxFile.delete()
                    syncSessionDao.upsert(
                        inProgress.copy(
                            lastSeenHash = hash,
                            mappedActivityType = mappedType.uploadValue,
                            uploadStatus = UploadStatus.SYNCED,
                            stravaUploadId = upload.uploadId,
                            stravaActivityId = upload.activityId,
                            error = null,
                        ),
                    )
                    successCount += 1
                }

                is UploadOutcome.Failure -> {
                    syncSessionDao.upsert(
                        inProgress.copy(
                            lastSeenHash = hash,
                            mappedActivityType = mappedType.uploadValue,
                            uploadStatus = UploadStatus.FAILED,
                            error = upload.message,
                        ),
                    )
                    failedCount += 1
                }
            }
        }

        val result = SyncRunResult(
            attempted = pending.size,
            success = successCount,
            failed = failedCount,
        )
        Log.d(TAG, "Sync finished: $result")
        return Result.success(result)
    }

    fun healthConnectRequiredPermissions(): Set<String> = healthConnectManager.requiredPermissions

    fun healthConnectSdkStatus(): Int = healthConnectManager.sdkStatus()

    suspend fun hasHealthPermissions(): Boolean = healthConnectManager.hasAllPermissions()

    fun stravaAuthIntent() = stravaRepository.authorizationIntent()

    suspend fun handleStravaRedirect(uri: android.net.Uri): Result<Unit> = stravaRepository.handleAuthorizationRedirect(uri)

    suspend fun logoutStrava() = stravaRepository.logout()

    suspend fun isStravaLoggedIn(): Boolean = stravaRepository.isLoggedIn()

    suspend fun userSettings(): UserSettings = settingsRepository.settingsFlow.first()

    suspend fun updateDaysBack(days: Int) = settingsRepository.updateDaysBack(days)

    suspend fun updateReuploadPolicy(enabled: Boolean) = settingsRepository.updateReuploadOnHashChange(enabled)

    suspend fun updateTypeOverride(exerciseType: Int, activityType: StravaActivityType) {
        settingsRepository.putOverride(exerciseType, activityType)
    }

    suspend fun clearTypeOverride(exerciseType: Int) = settingsRepository.clearOverride(exerciseType)

    fun knownSessionTypes(): List<Int> = exerciseTypeMapper.knownSessionTypesForSettings()

    fun exerciseTypeLabel(exerciseType: Int): String = exerciseTypeMapper.displayName(exerciseType)

    private suspend fun readWorkoutsForSettings(settings: UserSettings): List<WorkoutSession> {
        val end = Instant.now()
        val start = end.minus(settings.daysBack.toLong(), ChronoUnit.DAYS)
        return healthConnectManager.readSessions(start = start, end = end)
    }

    private fun buildScannedEntity(
        workout: WorkoutSession,
        mappedType: StravaActivityType,
        hash: String,
        existing: SyncSessionEntity?,
        settings: UserSettings,
    ): SyncSessionEntity {
        val decision = decideScanMerge(
            existing = existing,
            newHash = hash,
            reuploadOnHashChange = settings.reuploadOnHashChange,
        )

        return SyncSessionEntity(
            healthConnectSessionId = workout.healthConnectSessionId,
            title = workout.title,
            startTime = workout.startTime,
            endTime = workout.endTime,
            exerciseType = workout.exerciseType,
            mappedActivityType = mappedType.uploadValue,
            hasHeartRate = workout.hasHeartRate,
            hasDistance = workout.hasDistance,
            lastSeenHash = hash,
            stravaUploadId = if (decision.keepStravaIds) existing?.stravaUploadId else null,
            stravaActivityId = if (decision.keepStravaIds) existing?.stravaActivityId else null,
            uploadStatus = decision.status,
            lastAttempt = existing?.lastAttempt,
            error = decision.errorToKeep,
        )
    }

    private fun externalId(sessionId: String, hash: String): String {
        return "hc_${sessionId}_${hash.take(10)}"
            .replace("[^A-Za-z0-9_-]".toRegex(), "_")
            .take(128)
    }

    companion object {
        private const val TAG = "SyncRepository"
    }
}
