package com.example.hanabihinttracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hanabihinttracker.domain.HanabiAppState
import com.example.hanabihinttracker.domain.Preset

@Composable
internal fun SettingsDialog(
    state: HanabiAppState,
    onDismiss: () -> Unit,
    onNewGame: (Boolean, Boolean, Boolean, Int) -> Unit,
    onDirection: (Boolean) -> Unit,
    onDarkBackground: (Boolean) -> Unit
) {
    val game = state.game
    var sixthColor by remember {
        mutableStateOf(game.ruleset.preset in setOf(Preset.RAINBOW_SIXTH, Preset.BLACK_POWDER_RAINBOW_SIXTH))
    }
    var multiColor by remember {
        mutableStateOf(game.ruleset.preset in setOf(Preset.RAINBOW_MULTI, Preset.BLACK_POWDER_RAINBOW_MULTI))
    }
    var blackPowder by remember {
        mutableStateOf(
            game.ruleset.preset in setOf(
                Preset.BLACK_POWDER,
                Preset.BLACK_POWDER_RAINBOW_SIXTH,
                Preset.BLACK_POWDER_RAINBOW_MULTI
            )
        )
    }
    var size by remember { mutableIntStateOf(game.handSize) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ToggleSetting("6th color", sixthColor) {
                    sixthColor = it
                    if (it) multiColor = false
                }
                ToggleSetting("Multi-color", multiColor) {
                    multiColor = it
                    if (it) sixthColor = false
                }
                ToggleSetting("Black Powder", blackPowder) { blackPowder = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Hand size", Modifier.weight(1f))
                    FilterChip(size == 4, { size = 4 }, label = { Text("4") })
                    Spacer(Modifier.width(6.dp))
                    FilterChip(size == 5, { size = 5 }, label = { Text("5") })
                    Spacer(Modifier.width(6.dp))
                    FilterChip(size == 6, { size = 6 }, label = { Text("6") })
                }
                Text("Replacement enters from")
                Row {
                    FilterChip(
                        !state.settings.replacementFromRight,
                        { onDirection(false) },
                        label = { Text("Left") }
                    )
                    Spacer(Modifier.width(6.dp))
                    FilterChip(
                        state.settings.replacementFromRight,
                        { onDirection(true) },
                        label = { Text("Right") }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Black background", Modifier.weight(1f))
                    Switch(
                        checked = state.settings.darkBackground,
                        onCheckedChange = onDarkBackground
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onNewGame(sixthColor, multiColor, blackPowder, size) }) {
                Text("New game")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun ToggleSetting(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
