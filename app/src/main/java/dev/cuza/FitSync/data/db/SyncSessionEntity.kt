package dev.cuza.FitSync.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "sync_sessions",
    indices = [
        Index(value = ["startTime", "endTime", "exerciseType"]),
    ],
)
data class SyncSessionEntity(
    @PrimaryKey
    val healthConnectSessionId: String,
    val title: String,
    val startTime: Instant,
    val endTime: Instant,
    val exerciseType: Int,
    val mappedActivityType: String,
    val hasHeartRate: Boolean,
    val hasDistance: Boolean,
    val lastSeenHash: String,
    val stravaUploadId: String?,
    val stravaActivityId: Long?,
    val uploadStatus: UploadStatus,
    val lastAttempt: Instant?,
    val error: String?,
)
