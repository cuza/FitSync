package dev.cuza.FitSync.presentation.screen

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import dev.cuza.FitSync.data.db.SyncSessionEntity
import dev.cuza.FitSync.data.db.UploadStatus
import dev.cuza.FitSync.presentation.component.StatusBadge
import dev.cuza.FitSync.presentation.viewmodel.MainUiState
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    uiState: MainUiState,
    statusLabel: (UploadStatus) -> String,
    exerciseTypeLabel: (Int) -> String,
    onRequestStravaLogin: () -> Intent,
    onLogoutStrava: () -> Unit,
    onPermissionResult: (Set<String>) -> Unit,
    onScan: () -> Unit,
    onSync: () -> Unit,
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        onPermissionResult(granted)
    }

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm").withZone(ZoneId.systemDefault())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Strava Login",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (uiState.stravaLoggedIn) "Connected" else "Not connected",
                        color = if (uiState.stravaLoggedIn) Color(0xFF2E7D32) else Color(0xFF8E0000),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (uiState.stravaLoggedIn) {
                            OutlinedButton(onClick = onLogoutStrava) {
                                Text("Logout")
                            }
                        } else {
                            Button(onClick = { context.startActivity(onRequestStravaLogin()) }) {
                                Text("Login to Strava")
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Health Connect Permissions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

                    val availabilityText = when (uiState.healthSdkStatus) {
                        HealthConnectClient.SDK_AVAILABLE -> "Health Connect available"
                        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> "Health Connect needs install/update"
                        else -> "Health Connect unavailable"
                    }

                    Text(text = availabilityText)
                    Text(
                        text = if (uiState.hasHealthPermissions) "Read permissions granted" else "Read permissions missing",
                        color = if (uiState.hasHealthPermissions) Color(0xFF2E7D32) else Color(0xFF8E0000),
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (uiState.healthSdkStatus == HealthConnectClient.SDK_AVAILABLE) {
                            OutlinedButton(onClick = {
                                if (uiState.permissionRequestSet.isEmpty()) {
                                    openHealthConnectPermissions(context)
                                } else {
                                    runCatching {
                                        permissionLauncher.launch(uiState.permissionRequestSet)
                                    }.onFailure {
                                        openHealthConnectPermissions(context)
                                    }
                                }
                            }) {
                                Text("Grant Permissions")
                            }
                            OutlinedButton(onClick = {
                                openHealthConnectPermissions(context)
                            }) {
                                Text("Open HC Settings")
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("market://details?id=com.google.android.apps.healthdata"),
                                        ),
                                    )
                                },
                            ) {
                                Text("Install / Update")
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Sync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onScan,
                            enabled = !uiState.isRefreshing,
                        ) {
                            Text("Scan Sessions")
                        }
                        Button(
                            onClick = onSync,
                            enabled = !uiState.isSyncing,
                        ) {
                            Text("Sync Now")
                        }
                    }
                    if (uiState.isRefreshing || uiState.isSyncing) {
                        CircularProgressIndicator()
                    }
                    uiState.message?.let { msg ->
                        Text(text = msg, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            Text(
                text = "Detected Sessions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (uiState.sessions.isEmpty()) {
            item {
                Text("No sessions found in current window.")
            }
        } else {
            items(uiState.sessions, key = { it.healthConnectSessionId }) { session ->
                SessionRow(
                    session = session,
                    statusLabel = statusLabel(session.uploadStatus),
                    dateFormatter = dateFormatter,
                    exerciseTypeLabel = exerciseTypeLabel,
                )
                HorizontalDivider()
            }
        }
    }
}

private fun openHealthConnectPermissions(context: android.content.Context) {
    val managePermissionsIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        Intent(android.health.connect.HealthConnectManager.ACTION_MANAGE_HEALTH_PERMISSIONS).apply {
            putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName)
        }
    } else {
        Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
    }

    runCatching {
        context.startActivity(managePermissionsIntent)
    }.onFailure {
        context.startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS))
    }
}

@Composable
private fun SessionRow(
    session: SyncSessionEntity,
    statusLabel: String,
    dateFormatter: DateTimeFormatter,
    exerciseTypeLabel: (Int) -> String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = session.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "${dateFormatter.format(session.startTime)} - ${dateFormatter.format(session.endTime)}",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "Mapped type: ${session.mappedActivityType}",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "Health Connect type: ${exerciseTypeLabel(session.exerciseType)} (${session.exerciseType})",
            style = MaterialTheme.typography.bodySmall,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val statusColor = when (session.uploadStatus) {
                UploadStatus.SYNCED -> Color(0xFF2E7D32)
                UploadStatus.FAILED -> Color(0xFFC62828)
                UploadStatus.SYNCING -> Color(0xFF1565C0)
                else -> Color(0xFF455A64)
            }
            StatusBadge(label = statusLabel, color = statusColor)
            if (!session.hasHeartRate) {
                StatusBadge(label = "Missing HR", color = Color(0xFFEF6C00))
            }
            if (!session.hasDistance) {
                StatusBadge(label = "Missing distance", color = Color(0xFF6A1B9A))
            }
        }

        session.error?.takeIf { it.isNotBlank() }?.let { error ->
            Text(
                text = error,
                color = Color(0xFFC62828),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
