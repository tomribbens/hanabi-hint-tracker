package com.example.hanabihinttracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as UiColor
import androidx.compose.ui.unit.dp
import com.example.hanabihinttracker.domain.GameState
import com.example.hanabihinttracker.domain.Hint

@Composable
internal fun HintToolbar(
    game: GameState,
    selectedHint: Hint?,
    canCommit: Boolean,
    onOpenSettings: () -> Unit,
    onUndo: () -> Unit,
    onSelectHint: (Hint) -> Unit,
    onCommit: () -> Unit,
    onCancel: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onUndo, enabled = game.history.isNotEmpty()) {
            Icon(Icons.AutoMirrored.Filled.Undo, "Undo")
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Default.Settings, "Settings")
        }
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            game.ruleset.hintableColors.forEach { color ->
                val hint = Hint.ColorHint(color)
                SelectableColorSquare(color, selectedHint == hint, 28.dp) { onSelectHint(hint) }
                Spacer(Modifier.width(3.dp))
            }
            game.ruleset.numbers.sorted().forEach { number ->
                val hint = Hint.NumberHint(number)
                SelectableNumberSquare(number, selectedHint == hint, 28.dp) { onSelectHint(hint) }
                Spacer(Modifier.width(3.dp))
            }
            IconButton(onClick = onCommit, enabled = canCommit) {
                Icon(Icons.Default.Check, "Commit hint", tint = UiColor(0xFF2E7D32))
            }
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, "Cancel hint", tint = UiColor(0xFFC62828))
            }
        }
    }
}
