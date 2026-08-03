package com.example.hanabihinttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as UiColor
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hanabihinttracker.domain.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HanabiApp() }
    }
}

@Composable
private fun HanabiApp(vm: GameViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var hintDialog by remember { mutableStateOf(false) }
    var settingsDialog by remember { mutableStateOf(false) }
    var selectedCard by remember { mutableStateOf<Long?>(null) }

    HanabiTheme(darkBackground = state.darkBackground) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            Column(
                Modifier.padding(padding).padding(horizontal = 12.dp, vertical = 8.dp).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = vm::undo, enabled = state.history.isNotEmpty()) { Icon(Icons.Default.Undo, "Undo") }
                    IconButton(onClick = { settingsDialog = true }) { Icon(Icons.Default.Settings, "Settings") }
                    Spacer(Modifier.weight(1f))
                    AssistChip(onClick = { hintDialog = true }, label = { Text("Give hint") }, leadingIcon = { Icon(Icons.Default.Add, null) })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.cards.forEachIndexed { index, card ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            KnowledgeCard(
                                card = card,
                                state = state,
                                selected = selectedCard == card.id,
                                onSelect = { selectedCard = card.id },
                                onPlay = { vm.play(card.id); selectedCard = null }
                            )
                            ReorderMarker(
                                moveLeft = { vm.reorder(index, (index - 1).coerceAtLeast(0)) },
                                moveRight = { vm.reorder(index, (index + 1).coerceAtMost(state.cards.lastIndex)) }
                            )
                        }
                    }
                }
                if (selectedCard != null) {
                    Button(onClick = { vm.play(selectedCard!!); selectedCard = null }, modifier = Modifier.fillMaxWidth()) { Text("Play selected card") }
                }
                HistoryPanel(state.history, state)
            }
        }
    }
    if (hintDialog) HintDialog(state, onDismiss = { hintDialog = false }, onConfirm = { kind, value, matches -> vm.applyHint(kind, value, matches); hintDialog = false })
    if (settingsDialog) SettingsDialog(
        state = state,
        onDismiss = { settingsDialog = false },
        onNewGame = { preset, size -> vm.newGame(preset, size); settingsDialog = false },
        onDirection = vm::setDirection,
        onDarkBackground = vm::setDarkBackground
    )
}

@Composable
private fun KnowledgeCard(card: TrackedCard, state: GameState, selected: Boolean, onSelect: () -> Unit, onPlay: () -> Unit) {
    var dragY by remember { mutableFloatStateOf(0f) }
    Card(
        Modifier.fillMaxWidth().height(224.dp).pointerInput(state.cards) {
            detectDragGesturesAfterLongPress(onDrag = { change, amount -> change.consume(); dragY += amount.y }, onDragEnd = {
                if (dragY < -70) onPlay()
                dragY = 0f
            }, onDragCancel = { dragY = 0f })
        },
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onSelect, modifier = Modifier.size(25.dp)) { Icon(if (selected) Icons.Default.Check else Icons.Default.MoreVert, "Select") }
            }
            if (card.knowledge.possibleColors.size == 1 && card.knowledge.possibleNumbers.size == 1) {
                val color = card.knowledge.possibleColors.first()
                Box(
                    Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(12.dp)).background(color.brush()),
                    contentAlignment = Alignment.Center
                ) { Text(card.knowledge.possibleNumbers.first().toString(), fontSize = 72.sp, fontWeight = FontWeight.Bold, color = color.textColor()) }
            } else {
                ColorSquares(card.knowledge.possibleColors)
                NumberSquares(card.knowledge.possibleNumbers)
                Spacer(Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                card.knowledge.colorHints.forEach { ColorSquare(it, 16.dp) }
                card.knowledge.numberHints.forEach { NumberSquare(it, 20.dp) }
            }
            OutlinedButton(onClick = onPlay, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) { Text("Play") }
        }
    }
}

@Composable
private fun ReorderMarker(moveLeft: () -> Unit, moveRight: () -> Unit) {
    var dragX by remember { mutableFloatStateOf(0f) }
    Box(
        Modifier.padding(top = 3.dp).fillMaxWidth().height(28.dp).clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant).pointerInput(Unit) {
                detectDragGesturesAfterLongPress(onDrag = { change, amount -> change.consume(); dragX += amount.x }, onDragEnd = {
                    if (kotlin.math.abs(dragX) > 35) if (dragX < 0) moveLeft() else moveRight()
                    dragX = 0f
                }, onDragCancel = { dragX = 0f })
            },
        contentAlignment = Alignment.Center
    ) { Icon(Icons.Default.DragHandle, "Drag to reorder", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
private fun ColorSquares(colors: Set<Color>) {
    FlowRow(maxItemsInEachRow = 5, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        colors.forEach { ColorSquare(it, 20.dp) }
    }
}

@Composable
private fun ColorSquare(color: Color, size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(4.dp)).background(color.brush()).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)))
}

@Composable
private fun NumberSquares(numbers: Set<Int>) {
    FlowRow(maxItemsInEachRow = 5, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        numbers.sorted().forEach { NumberSquare(it, 20.dp) }
    }
}

@Composable
private fun NumberSquare(number: Int, size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Text(number.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HintDialog(state: GameState, onDismiss: () -> Unit, onConfirm: (HintKind, String, Set<Long>) -> Unit) {
    var kind by remember { mutableStateOf(HintKind.COLOR) }
    var value by remember { mutableStateOf(state.ruleset.hintableColors.first().name) }
    var matches by remember { mutableStateOf(emptySet<Long>()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Record a hint") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(kind == HintKind.COLOR, { kind = HintKind.COLOR }, label = { Text("Color") }); FilterChip(kind == HintKind.NUMBER, { kind = HintKind.NUMBER }, label = { Text("Number") }) }
            FlowRow(maxItemsInEachRow = 6, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val options: List<Any> = if (kind == HintKind.COLOR) state.ruleset.hintableColors.toList() else state.ruleset.numbers.toList()
                options.forEach { item ->
                    val itemValue = if (item is Color) item.name else item.toString()
                    if (item is Color) SelectableColorSquare(item, value == itemValue) { value = itemValue }
                    else SelectableNumberSquare(itemValue.toInt(), value == itemValue) { value = itemValue }
                }
            }
            Text("Tap the cards that match", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { state.cards.forEachIndexed { i, card -> FilterChip(card.id in matches, { matches = if (card.id in matches) matches - card.id else matches + card.id }, label = { Text("${i + 1}") }) } }
        }
    }, confirmButton = { Button(onClick = { onConfirm(kind, value, matches) }, enabled = matches.isNotEmpty()) { Text("Save hint") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun HistoryPanel(history: List<HintRecord>, state: GameState) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Hint history", style = MaterialTheme.typography.titleSmall)
        Text(if (history.isEmpty()) "No hints recorded yet" else history.takeLast(2).asReversed().joinToString("\n") { hint -> "${hint.kind.name.lowercase().replaceFirstChar { it.uppercase() }} ${if (hint.kind == HintKind.COLOR) Color.valueOf(hint.value).label else "#${hint.value}"} · ${hint.matchingCardIds.count { id -> state.cards.any { it.id == id } }} cards" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsDialog(state: GameState, onDismiss: () -> Unit, onNewGame: (Preset, Int) -> Unit, onDirection: (Boolean) -> Unit, onDarkBackground: (Boolean) -> Unit) {
    var preset by remember { mutableStateOf(state.ruleset.preset) }
    var size by remember { mutableIntStateOf(state.handSize) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Settings") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Preset", style = MaterialTheme.typography.labelLarge)
            Preset.entries.forEach { FilterChip(preset == it, { preset = it }, label = { Text(it.title) }) }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Hand size", Modifier.weight(1f)); FilterChip(size == 4, { size = 4 }, label = { Text("4") }); Spacer(Modifier.width(6.dp)); FilterChip(size == 5, { size = 5 }, label = { Text("5") }) }
            Text("Replacement enters from", style = MaterialTheme.typography.labelLarge)
            Row { FilterChip(!state.replacementFromRight, { onDirection(false) }, label = { Text("Left") }); Spacer(Modifier.width(6.dp)); FilterChip(state.replacementFromRight, { onDirection(true) }, label = { Text("Right") }) }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Black background", Modifier.weight(1f)); Switch(checked = state.darkBackground, onCheckedChange = onDarkBackground) }
        }
    }, confirmButton = { Button(onClick = { onNewGame(preset, size) }) { Text("New game") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

@Composable
private fun SelectableColorSquare(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(34.dp).clip(RoundedCornerShape(5.dp)).background(color.brush()).border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(5.dp)).clickable(onClick = onClick))
}

@Composable
private fun SelectableNumberSquare(number: Int, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(34.dp).clip(RoundedCornerShape(5.dp)).background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant).border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(5.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) { Text(number.toString(), fontWeight = FontWeight.Bold) }
}

@Composable
private fun HanabiTheme(darkBackground: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkBackground) darkColorScheme(background = UiColor.Black, surface = UiColor(0xFF1B1B1B), surfaceVariant = UiColor(0xFF303030)) else lightColorScheme(primary = UiColor(0xFF5A3E85), secondary = UiColor(0xFF176B87))
    MaterialTheme(colorScheme = colors, content = content)
}

private fun Color.uiColor() = UiColor(hex)
private fun Color.textColor() = if (this == Color.WHITE || this == Color.YELLOW) UiColor.Black else UiColor.White
private fun Color.brush(): Brush = if (this == Color.RAINBOW) Brush.linearGradient(listOf(UiColor.Red, UiColor.Yellow, UiColor.Green, UiColor.Cyan, UiColor.Blue, UiColor.Magenta)) else Brush.solidColor(uiColor())
