package dev.cuza.FitSync.domain.sync

import dev.cuza.FitSync.data.db.SyncSessionEntity
import dev.cuza.FitSync.data.db.UploadStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ScanMergePolicyTest {

    @Test
    fun `new session starts in ready state`() {
        val decision = decideScanMerge(
            existing = null,
            newHash = "hash-1",
            reuploadOnHashChange = false,
        )

        assertEquals(UploadStatus.READY, decision.status)
        assertFalse(decision.keepStravaIds)
        assertNull(decision.errorToKeep)
    }

    @Test
    fun `unchanged synced session stays synced and keeps ids`() {
        val existing = existingEntity(status = UploadStatus.SYNCED, hash = "hash-1")

        val decision = decideScanMerge(
            existing = existing,
            newHash = "hash-1",
            reuploadOnHashChange = true,
        )

        assertEquals(UploadStatus.SYNCED, decision.status)
        assertTrue(decision.keepStravaIds)
        assertNull(decision.errorToKeep)
    }

    @Test
    fun `changed synced session becomes ready when reupload is enabled`() {
        val existing = existingEntity(status = UploadStatus.SYNCED, hash = "hash-1")

        val decision = decideScanMerge(
            existing = existing,
            newHash = "hash-2",
            reuploadOnHashChange = true,
        )

        assertEquals(UploadStatus.READY, decision.status)
        assertFalse(decision.keepStravaIds)
        assertNull(decision.errorToKeep)
    }

    @Test
    fun `changed synced session stays synced when reupload is disabled`() {
        val existing = existingEntity(status = UploadStatus.SYNCED, hash = "hash-1")

        val decision = decideScanMerge(
            existing = existing,
            newHash = "hash-2",
            reuploadOnHashChange = false,
        )

        assertEquals(UploadStatus.SYNCED, decision.status)
        assertTrue(decision.keepStravaIds)
        assertNull(decision.errorToKeep)
    }

    @Test
    fun `failed unchanged session keeps failed state and error`() {
        val existing = existingEntity(status = UploadStatus.FAILED, hash = "hash-1", error = "upload failed")

        val decision = decideScanMerge(
            existing = existing,
            newHash = "hash-1",
            reuploadOnHashChange = false,
        )

        assertEquals(UploadStatus.FAILED, decision.status)
        assertEquals("upload failed", decision.errorToKeep)
        assertTrue(decision.keepStravaIds)
    }

    private fun existingEntity(
        status: UploadStatus,
        hash: String,
        error: String? = null,
    ): SyncSessionEntity {
        return SyncSessionEntity(
            healthConnectSessionId = "session-1",
            title = "Workout",
            startTime = Instant.parse("2026-01-01T10:00:00Z"),
            endTime = Instant.parse("2026-01-01T10:30:00Z"),
            exerciseType = 1,
            mappedActivityType = "workout",
            hasHeartRate = true,
            hasDistance = true,
            lastSeenHash = hash,
            stravaUploadId = "123",
            stravaActivityId = 456L,
            uploadStatus = status,
            lastAttempt = Instant.parse("2026-01-01T10:31:00Z"),
            error = error,
        )
    }
}
