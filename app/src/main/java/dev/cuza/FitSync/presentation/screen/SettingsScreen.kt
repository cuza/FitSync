package dev.cuza.FitSync.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.cuza.FitSync.domain.model.StravaActivityType
import dev.cuza.FitSync.presentation.viewmodel.MainUiState

@Composable
fun SettingsScreen(
    uiState: MainUiState,
    exerciseTypeLabel: (Int) -> String,
    onDaysBackChange: (Int) -> Unit,
    onReuploadChanged: (Boolean) -> Unit,
    onOverrideChange: (Int, StravaActivityType?) -> Unit,
) {
    var sliderDaysBack by remember { mutableStateOf(uiState.settings.daysBack.toFloat()) }
    LaunchedEffect(uiState.settings.daysBack) {
        sliderDaysBack = uiState.settings.daysBack.toFloat()
    }

    val knownTypes = remember(uiState.sessions, uiState.knownSessionTypes) {
        (uiState.sessions.map { it.exerciseType } + uiState.knownSessionTypes)
            .distinct()
            .sorted()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "Sync Window",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text("Days back: ${sliderDaysBack.toInt()}")
            Slider(
                value = sliderDaysBack,
                onValueChange = { sliderDaysBack = it },
                onValueChangeFinished = { onDaysBackChange(sliderDaysBack.toInt()) },
                valueRange = 1f..30f,
                steps = 28,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Re-upload when data changes",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "If disabled, changed sessions stay synced and are not re-uploaded.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = uiState.settings.reuploadOnHashChange,
                    onCheckedChange = onReuploadChanged,
                )
            }
        }

        item {
            Text(
                text = "Activity Type Overrides",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Default mapping uses Health Connect type names + session title hints (for example spin, trail run, virtual ride/row). Override any type below.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        items(knownTypes, key = { it }) { type ->
            val override = uiState.settings.overrides[type]
            OverrideRow(
                label = exerciseTypeLabel(type),
                value = override,
                onSelect = { selected -> onOverrideChange(type, selected) },
            )
        }
    }
}

@Composable
private fun OverrideRow(
    label: String,
    value: StravaActivityType?,
    onSelect: (StravaActivityType?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = { expanded = true }) {
            Text(value?.displayName ?: "Default")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Default") },
                onClick = {
                    expanded = false
                    onSelect(null)
                },
            )
            StravaActivityType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName) },
                    onClick = {
                        expanded = false
                        onSelect(type)
                    },
                )
            }
        }
    }
}
