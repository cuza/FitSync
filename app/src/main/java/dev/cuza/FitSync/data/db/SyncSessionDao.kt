package dev.cuza.FitSync.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface SyncSessionDao {

    @Query("SELECT * FROM sync_sessions ORDER BY startTime DESC")
    fun observeAll(): Flow<List<SyncSessionEntity>>

    @Query("SELECT * FROM sync_sessions WHERE healthConnectSessionId = :sessionId LIMIT 1")
    suspend fun bySessionId(sessionId: String): SyncSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SyncSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<SyncSessionEntity>)

    @Query(
        "SELECT * FROM sync_sessions WHERE uploadStatus IN ('READY','FAILED') " +
            "ORDER BY startTime ASC",
    )
    suspend fun pendingUploads(): List<SyncSessionEntity>

    @Query("DELETE FROM sync_sessions WHERE endTime < :cutoff")
    suspend fun deleteOlderThan(cutoff: Instant)
}
