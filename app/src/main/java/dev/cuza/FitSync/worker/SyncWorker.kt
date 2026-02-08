package dev.cuza.FitSync.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.cuza.FitSync.app.FitSyncApp
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as FitSyncApp).appContainer.syncRepository
        val sync = repository.syncNow()

        return sync.fold(
            onSuccess = { result ->
                Result.success(
                    Data.Builder()
                        .putInt(KEY_ATTEMPTED, result.attempted)
                        .putInt(KEY_SUCCESS, result.success)
                        .putInt(KEY_FAILED, result.failed)
                        .build(),
                )
            },
            onFailure = { throwable ->
                val message = throwable.message.orEmpty().lowercase()
                if (message.contains("rate limited") || message.contains("timeout") || message.contains("http 5")) {
                    Result.retry()
                } else {
                    Result.failure(
                        Data.Builder().putString(KEY_ERROR, throwable.message.orEmpty()).build(),
                    )
                }
            },
        )
    }

    companion object {
        private const val UNIQUE_PERIODIC_NAME = "periodic_sync"
        private const val UNIQUE_ONE_TIME_NAME = "manual_sync"

        const val KEY_ATTEMPTED = "attempted"
        const val KEY_SUCCESS = "success"
        const val KEY_FAILED = "failed"
        const val KEY_ERROR = "error"

        fun enqueuePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun enqueueNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_TIME_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
