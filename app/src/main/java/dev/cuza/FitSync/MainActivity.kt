package dev.cuza.FitSync

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.cuza.FitSync.app.FitSyncApp
import dev.cuza.FitSync.presentation.navigation.AppNav
import dev.cuza.FitSync.presentation.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.factory((application as FitSyncApp).appContainer.syncRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleRedirectIntent(intent)

        setContent {
            val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
            Surface(color = MaterialTheme.colorScheme.background) {
                AppNav(
                    uiState = uiState,
                    viewModel = viewModel,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleRedirectIntent(intent)
    }

    private fun handleRedirectIntent(intent: Intent?) {
        val data = intent?.data ?: return
        val expectedScheme = BuildConfig.STRAVA_REDIRECT_SCHEME
        val expectedHost = BuildConfig.STRAVA_REDIRECT_HOST

        if (data.scheme == expectedScheme && data.host == expectedHost) {
            viewModel.handleStravaRedirect(data)
        }
    }
}
