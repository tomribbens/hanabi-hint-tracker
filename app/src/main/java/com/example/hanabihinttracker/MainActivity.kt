package com.example.hanabihinttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as UiColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hanabihinttracker.domain.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HanabiTheme { HanabiApp() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HanabiApp(vm: GameViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var hintDialog by remember { mutableStateOf(false) }
    var settingsDialog by remember { mutableStateOf(false) }
    var selectedCard by remember { mutableStateOf<Long?>(null) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Hanabi Hint Tracker", fontWeight = FontWeight.Bold) }, actions = {
            IconButton(onClick = vm::undo, enabled = state.history.isNotEmpty()) { Icon(Icons.Default.Undo, "Undo") }
            IconButton(onClick = { settingsDialog = true }) { Icon(Icons.Default.Settings, "Settings") }
        })
    }) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(state.ruleset.preset.title, style = MaterialTheme.typography.titleMedium)
                    Text("${state.cards.size} cards · Drag to reorder", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AssistChip(onClick = { hintDialog = true }, label = { Text("Give hint") }, leadingIcon = { Icon(Icons.Default.Add, null) })
            }
            Text("Play area", style = MaterialTheme.typography.labelLarge)
            Box(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Text("Drag a card here to play it", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(state.cards, key = { _, card -> card.id }) { index, card ->
                    KnowledgeCard(card, state, selected = selectedCard == card.id, onSelect = { selectedCard = card.id }, onPlay = { vm.play(card.id); selectedCard = null }, onMove = { target -> vm.reorder(index, target) })
                }
            }
            if (selectedCard != null) {
                Button(onClick = { vm.play(selectedCard!!); selectedCard = null }, modifier = Modifier.fillMaxWidth()) { Text("Play selected card") }
            }
            HistoryPanel(state.history, state)
        }
    }
    if (hintDialog) HintDialog(state, onDismiss = { hintDialog = false }, onConfirm = { kind, value, matches -> vm.applyHint(kind, value, matches); hintDialog = false })
    if (settingsDialog) SettingsDialog(state, onDismiss = { settingsDialog = false }, onNewGame = { preset, size -> vm.newGame(preset, size); settingsDialog = false }, onDirection = vm::setDirection)
}

@Composable
private fun KnowledgeCard(card: TrackedCard, state: GameState, selected: Boolean, onSelect: () -> Unit, onPlay: () -> Unit, onMove: (Int) -> Unit) {
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    Card(Modifier.width(142.dp).height(250.dp).pointerInput(state.cards) {
        detectDragGesturesAfterLongPress(onDrag = { change, amount -> change.consume(); dragX += amount.x; dragY += amount.y }, onDragEnd = {
            if (dragY < -100) onPlay()
            else if (kotlin.math.abs(dragX) > 50) onMove(if (dragX > 0) (state.cards.indexOf(card) + 1).coerceAtMost(state.cards.lastIndex) else (state.cards.indexOf(card) - 1).coerceAtLeast(0))
            dragX = 0f; dragY = 0f
        }, onDragCancel = { dragX = 0f; dragY = 0f })
    }, colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Card ${state.cards.indexOf(card) + 1}", style = MaterialTheme.typography.labelLarge)
                IconButton(onClick = onSelect, modifier = Modifier.size(26.dp)) { Icon(if (selected) Icons.Default.Check else Icons.Default.MoreVert, "Select") }
            }
            Text("Known hints", style = MaterialTheme.typography.labelSmall)
            Text((card.knowledge.colorHints.map { it.label } + card.knowledge.numberHints.map { "#$it" }).ifEmpty { listOf("None yet") }.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
            Divider()
            Text("Possible colors", style = MaterialTheme.typography.labelSmall)
            Text(card.knowledge.possibleColors.joinToString(", ") { it.label }, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            Text("Possible numbers", style = MaterialTheme.typography.labelSmall)
            Text(card.knowledge.possibleNumbers.sorted().joinToString(", ").ifEmpty { "None" }, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onPlay, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) { Text("Play") }
        }
    }
}

@Composable
private fun HintDialog(state: GameState, onDismiss: () -> Unit, onConfirm: (HintKind, String, Set<Long>) -> Unit) {
    var kind by remember { mutableStateOf(HintKind.COLOR) }
    var value by remember { mutableStateOf(state.ruleset.hintableColors.first().name) }
    var matches by remember { mutableStateOf(emptySet<Long>()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Record a hint") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(kind == HintKind.COLOR, { kind = HintKind.COLOR }, label = { Text("Color") }); FilterChip(kind == HintKind.NUMBER, { kind = HintKind.NUMBER }, label = { Text("Number") }) }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val options: List<Any> = if (kind == HintKind.COLOR) state.ruleset.hintableColors.toList() else state.ruleset.numbers.toList()
                items(items = options) { item ->
                    val itemValue = if (item is Color) item.name else item.toString()
                    FilterChip(value == itemValue, { value = itemValue }, label = { Text(if (item is Color) item.label else "#$itemValue") })
                }
            }
            Text("Tap the cards that match", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { itemsIndexed(state.cards) { i, card -> FilterChip(card.id in matches, { matches = if (card.id in matches) matches - card.id else matches + card.id }, label = { Text("${i + 1}") }) } }
        }
    }, confirmButton = { Button(onClick = { onConfirm(kind, value, matches) }, enabled = matches.isNotEmpty()) { Text("Save hint") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun HistoryPanel(history: List<HintRecord>, state: GameState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Hint history", style = MaterialTheme.typography.titleSmall)
        Text(if (history.isEmpty()) "No hints recorded yet" else history.takeLast(3).asReversed().joinToString("\n") { hint -> "${hint.kind.name.lowercase().replaceFirstChar { it.uppercase() }} ${if (hint.kind == HintKind.COLOR) Color.valueOf(hint.value).label else "#${hint.value}"} · ${hint.matchingCardIds.count { id -> state.cards.any { it.id == id } }} cards" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsDialog(state: GameState, onDismiss: () -> Unit, onNewGame: (Preset, Int) -> Unit, onDirection: (Boolean) -> Unit) {
    var preset by remember { mutableStateOf(state.ruleset.preset) }
    var size by remember { mutableIntStateOf(state.handSize) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Game settings") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Preset", style = MaterialTheme.typography.labelLarge)
            Preset.entries.forEach { FilterChip(preset == it, { preset = it }, label = { Text(it.title) }) }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Hand size", Modifier.weight(1f)); FilterChip(size == 4, { size = 4 }, label = { Text("4") }); Spacer(Modifier.width(6.dp)); FilterChip(size == 5, { size = 5 }, label = { Text("5") }) }
            Text("Replacement enters from", style = MaterialTheme.typography.labelLarge)
            Row { FilterChip(!state.replacementFromRight, { onDirection(false) }, label = { Text("Left") }); Spacer(Modifier.width(6.dp)); FilterChip(state.replacementFromRight, { onDirection(true) }, label = { Text("Right") }) }
        }
    }, confirmButton = { Button(onClick = { onNewGame(preset, size) }) { Text("New game") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

@Composable
private fun HanabiTheme(content: @Composable () -> Unit) { MaterialTheme(colorScheme = lightColorScheme(primary = UiColor(0xFF5A3E85), secondary = UiColor(0xFF176B87)), content = content) }
