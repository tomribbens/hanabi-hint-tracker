package com.example.hanabihinttracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hanabihinttracker.domain.HanabiAppState
import com.example.hanabihinttracker.domain.Hint
import com.example.hanabihinttracker.domain.HintApplicationResult

@Composable
internal fun HanabiScreen(state: HanabiAppState, vm: GameViewModel) {
    val game = state.game
    var selectedHint by remember { mutableStateOf<Hint?>(null) }
    var hintMatches by remember { mutableStateOf(emptySet<Long>()) }
    var hintError by remember { mutableStateOf<String?>(null) }
    var settingsDialogVisible by remember { mutableStateOf(false) }
    var selectedCardId by remember { mutableStateOf<Long?>(null) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            Modifier.padding(padding).padding(horizontal = 12.dp, vertical = 8.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HintToolbar(
                game = game,
                selectedHint = selectedHint,
                canCommit = selectedHint != null && hintMatches.isNotEmpty(),
                onOpenSettings = { settingsDialogVisible = true },
                onUndo = vm::undo,
                onSelectHint = {
                    selectedHint = it
                    hintError = null
                },
                onCommit = {
                    val hint = selectedHint
                    if (hint != null && hintMatches.isNotEmpty()) {
                        when (vm.applyHint(hint, hintMatches)) {
                            is HintApplicationResult.Accepted -> {
                                selectedHint = null
                                hintMatches = emptySet()
                                hintError = null
                            }

                            HintApplicationResult.Contradiction -> {
                                hintError = "That hint contradicts recorded information."
                            }
                        }
                    }
                },
                onCancel = {
                    selectedHint = null
                    hintMatches = emptySet()
                    hintError = null
                }
            )
            DraggableHand(
                cards = game.cards,
                selectedCardIds = if (selectedHint != null) hintMatches else setOfNotNull(selectedCardId),
                onCardSelect = { cardId ->
                    if (selectedHint != null) {
                        hintMatches = if (cardId in hintMatches) hintMatches - cardId else hintMatches + cardId
                    } else {
                        selectedCardId = cardId
                    }
                },
                onPlay = { cardId ->
                    if (selectedHint == null) {
                        vm.play(cardId)
                        selectedCardId = null
                    }
                },
                onReorder = vm::reorder
            )
            hintError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            selectedCardId?.takeIf { selectedHint == null }?.let { cardId ->
                Button(
                    onClick = {
                        vm.play(cardId)
                        selectedCardId = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Play selected card")
                }
            }
            HistoryPanel(game.history, game.cards)
        }
    }

    if (settingsDialogVisible) {
        SettingsDialog(
            state = state,
            onDismiss = { settingsDialogVisible = false },
            onNewGame = { sixthColor, multiColor, blackPowder, size ->
                vm.newGame(sixthColor, multiColor, blackPowder, size)
                selectedHint = null
                hintMatches = emptySet()
                selectedCardId = null
                settingsDialogVisible = false
            },
            onDirection = vm::setDirection,
            onDarkBackground = vm::setDarkBackground
        )
    }
}
