package dev.cuza.FitSync.app

import android.content.Context
import dev.cuza.FitSync.data.db.AppDatabase
import dev.cuza.FitSync.data.healthconnect.HealthConnectManager
import dev.cuza.FitSync.data.preferences.SettingsRepository
import dev.cuza.FitSync.data.strava.StravaRepository
import dev.cuza.FitSync.data.strava.TokenStore
import dev.cuza.FitSync.domain.export.TcxExporter
import dev.cuza.FitSync.domain.mapping.ExerciseTypeMapper
import dev.cuza.FitSync.domain.sync.SessionHashCalculator
import dev.cuza.FitSync.domain.sync.SyncRepository
import java.io.File

class AppContainer(context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val healthConnectManager = HealthConnectManager(context)
    private val settingsRepository = SettingsRepository(context)
    private val tokenStore = TokenStore(context)
    private val stravaRepository = StravaRepository(tokenStore)
    private val typeMapper = ExerciseTypeMapper()
    private val hashCalculator = SessionHashCalculator()
    private val exporter = TcxExporter()

    val syncRepository: SyncRepository = SyncRepository(
        healthConnectManager = healthConnectManager,
        settingsRepository = settingsRepository,
        syncSessionDao = database.syncSessionDao(),
        exerciseTypeMapper = typeMapper,
        hashCalculator = hashCalculator,
        tcxExporter = exporter,
        stravaRepository = stravaRepository,
        exportsDir = File(context.cacheDir, "exports"),
    )
}
