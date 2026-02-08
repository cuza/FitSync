package dev.cuza.FitSync.app

import android.app.Application
import dev.cuza.FitSync.worker.SyncWorker

class FitSyncApp : Application() {

    val appContainer: AppContainer by lazy {
        AppContainer(this)
    }

    override fun onCreate() {
        super.onCreate()
        SyncWorker.enqueuePeriodic(this)
    }
}
