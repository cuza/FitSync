package dev.cuza.FitSync.healthconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Health Connect access",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "FitSync needs read access to workout sessions and heart-rate data " +
                            "to export your Galaxy Watch workouts and upload them to Strava.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = "You can revoke this anytime in Health Connect settings.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = { finish() }) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
