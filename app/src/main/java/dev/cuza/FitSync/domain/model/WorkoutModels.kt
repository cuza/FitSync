package dev.cuza.FitSync.domain.model

import java.time.Instant

data class WorkoutSample(
    val timestamp: Instant,
    val heartRateBpm: Int? = null,
    val distanceMeters: Double? = null,
    val speedMps: Double? = null,
)

data class WorkoutSession(
    val healthConnectSessionId: String,
    val title: String,
    val startTime: Instant,
    val endTime: Instant,
    val exerciseType: Int,
    val totalDistanceMeters: Double?,
    val totalCaloriesKcal: Double?,
    val totalSteps: Long?,
    val samples: List<WorkoutSample>,
) {
    val hasHeartRate: Boolean = samples.any { it.heartRateBpm != null }
    val hasDistance: Boolean = (totalDistanceMeters ?: 0.0) > 0.0 || samples.any { it.distanceMeters != null }
}
