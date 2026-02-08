package dev.cuza.FitSync.domain.sync

import dev.cuza.FitSync.data.db.SyncSessionEntity
import dev.cuza.FitSync.data.db.UploadStatus

internal data class ScanMergeDecision(
    val status: UploadStatus,
    val keepStravaIds: Boolean,
    val errorToKeep: String?,
)

internal fun decideScanMerge(
    existing: SyncSessionEntity?,
    newHash: String,
    reuploadOnHashChange: Boolean,
): ScanMergeDecision {
    val hashChanged = existing?.lastSeenHash?.let { it != newHash } ?: false

    val status = when {
        existing == null -> UploadStatus.READY
        existing.uploadStatus == UploadStatus.SYNCING -> UploadStatus.READY
        !hashChanged -> existing.uploadStatus
        hashChanged && existing.uploadStatus == UploadStatus.SYNCED && reuploadOnHashChange -> UploadStatus.READY
        hashChanged && existing.uploadStatus == UploadStatus.SYNCED && !reuploadOnHashChange -> UploadStatus.SYNCED
        else -> UploadStatus.READY
    }

    val keepStravaIds =
        existing != null &&
            (existing.lastSeenHash == newHash || (!reuploadOnHashChange && existing.uploadStatus == UploadStatus.SYNCED))

    val errorToKeep = existing?.error?.takeUnless { status == UploadStatus.READY || status == UploadStatus.SYNCED }

    return ScanMergeDecision(
        status = status,
        keepStravaIds = keepStravaIds,
        errorToKeep = errorToKeep,
    )
}
