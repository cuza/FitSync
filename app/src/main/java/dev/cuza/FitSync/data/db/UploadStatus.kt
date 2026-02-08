package dev.cuza.FitSync.data.db

enum class UploadStatus {
    READY,
    SYNCING,
    SYNCED,
    FAILED,
    SKIPPED,
}
