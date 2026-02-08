package dev.cuza.FitSync.data.healthconnect

import android.content.Context
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dev.cuza.FitSync.domain.model.WorkoutSample
import dev.cuza.FitSync.domain.model.WorkoutSession
import java.time.Instant
import java.util.SortedSet
import java.util.TreeSet
import kotlin.reflect.KClass

class HealthConnectManager(private val context: Context) {

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(SpeedRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
    )

    fun sdkStatus(): Int {
        return HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PROVIDER_PACKAGE)
    }

    fun requestPermissionsContract(): ActivityResultContract<Set<String>, Set<String>> {
        return PermissionController.createRequestPermissionResultContract()
    }

    suspend fun hasAllPermissions(): Boolean {
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(requiredPermissions)
    }

    suspend fun readSessions(start: Instant, end: Instant): List<WorkoutSession> {
        val sessions = readAll(
            recordType = ExerciseSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end),
        )

        return sessions.map { session -> toWorkoutSession(session) }
    }

    private suspend fun toWorkoutSession(session: ExerciseSessionRecord): WorkoutSession {
        val start = session.startTime
        val end = session.endTime

        val heartRateRecords = readAll(
            recordType = HeartRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end),
        )
        val distanceRecords = readAll(
            recordType = DistanceRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end),
        )
        val speedRecords = readAll(
            recordType = SpeedRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end),
        )
        val totalCaloriesRecords = readAll(
            recordType = TotalCaloriesBurnedRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end),
        )
        val activeCaloriesRecords = readAll(
            recordType = ActiveCaloriesBurnedRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end),
        )
        val stepsRecords = readAll(
            recordType = StepsRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end),
        )

        val heartRateSamples = heartRateRecords
            .flatMap { record ->
                record.samples.map { sample ->
                    sample.time to sample.beatsPerMinute.toInt()
                }
            }
            .sortedBy { (time, _) -> time }

        val speedSamples = speedRecords
            .flatMap { record ->
                record.samples.map { sample ->
                    sample.time to sample.speed.inMetersPerSecond
                }
            }
            .sortedBy { (time, _) -> time }

        val cumulativeDistanceTimeline = buildDistanceTimeline(distanceRecords)
        val allTimestamps = collectTimelineTimestamps(
            start = start,
            end = end,
            heartRate = heartRateSamples.map { it.first },
            speed = speedSamples.map { it.first },
            distance = cumulativeDistanceTimeline.map { it.first },
        )

        val heartRateMap = heartRateSamples.toMap()
        val speedMap = speedSamples.toMap()
        val mergedSamples = allTimestamps.map { timestamp ->
            WorkoutSample(
                timestamp = timestamp,
                heartRateBpm = heartRateMap[timestamp],
                distanceMeters = distanceAt(timestamp, cumulativeDistanceTimeline),
                speedMps = speedMap[timestamp],
            )
        }

        val totalDistance = distanceRecords.sumOf { record -> record.distance.inMeters }
        val calories = (totalCaloriesRecords.sumOf { record -> record.energy.inKilocalories } +
            activeCaloriesRecords.sumOf { record -> record.energy.inKilocalories })
            .takeIf { it > 0.0 }
        val steps = stepsRecords.sumOf { it.count }.takeIf { it > 0L }

        return WorkoutSession(
            healthConnectSessionId = session.metadata.id.takeUnless { it.isNullOrBlank() }
                ?: fallbackSessionId(session),
            title = session.title?.takeUnless { it.isBlank() } ?: "Workout",
            startTime = start,
            endTime = end,
            exerciseType = session.exerciseType,
            totalDistanceMeters = totalDistance.takeIf { it > 0.0 },
            totalCaloriesKcal = calories,
            totalSteps = steps,
            samples = mergedSamples,
        )
    }

    private fun fallbackSessionId(record: ExerciseSessionRecord): String {
        return "${record.startTime.toEpochMilli()}_${record.endTime.toEpochMilli()}_${record.exerciseType}"
    }

    private fun buildDistanceTimeline(records: List<DistanceRecord>): List<Pair<Instant, Double>> {
        if (records.isEmpty()) return emptyList()

        var cumulative = 0.0
        return records
            .sortedBy { it.endTime }
            .map { record ->
                cumulative += record.distance.inMeters
                record.endTime to cumulative
            }
    }

    private fun collectTimelineTimestamps(
        start: Instant,
        end: Instant,
        heartRate: List<Instant>,
        speed: List<Instant>,
        distance: List<Instant>,
    ): List<Instant> {
        val timestamps: SortedSet<Instant> = TreeSet()
        timestamps += start
        timestamps += end
        timestamps += heartRate
        timestamps += speed
        timestamps += distance
        return timestamps.toList()
    }

    private fun distanceAt(timestamp: Instant, timeline: List<Pair<Instant, Double>>): Double? {
        if (timeline.isEmpty()) return null
        var last: Double? = null
        for ((time, value) in timeline) {
            if (time <= timestamp) {
                last = value
            } else {
                break
            }
        }
        return last
    }

    private suspend fun <T : Record> readAll(
        recordType: KClass<T>,
        timeRangeFilter: TimeRangeFilter,
    ): List<T> {
        val all = mutableListOf<T>()
        var pageToken: String? = null

        do {
            val request = ReadRecordsRequest(
                recordType = recordType,
                timeRangeFilter = timeRangeFilter,
                pageToken = pageToken,
            )
            val response = client.readRecords(request)
            all += response.records
            pageToken = response.pageToken
        } while (!pageToken.isNullOrBlank())

        Log.d(TAG, "Fetched ${all.size} ${recordType.simpleName} records")
        return all
    }

    companion object {
        private const val TAG = "HealthConnectManager"
        private const val HEALTH_CONNECT_PROVIDER_PACKAGE = "com.google.android.apps.healthdata"
    }
}
