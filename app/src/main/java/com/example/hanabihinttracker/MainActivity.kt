package com.example.hanabihinttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color as UiColor
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
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
    var displayOrder by remember { mutableStateOf<List<TrackedCard>>(emptyList()) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragStartIndex by remember { mutableIntStateOf(-1) }
    var dragCurrentIndex by remember { mutableIntStateOf(-1) }
    var cardWidthPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.cards) {
        if (draggingId == null) displayOrder = state.cards
    }
    val cardsToShow = if (displayOrder.isEmpty()) state.cards else displayOrder
    val density = LocalDensity.current
    val reorderStepPx = cardWidthPx + with(density) { 8.dp.toPx() }

    fun beginCardDrag(card: TrackedCard) {
        draggingId = card.id
        dragStartIndex = cardsToShow.indexOfFirst { it.id == card.id }
        dragCurrentIndex = dragStartIndex
        dragOffsetX = 0f
    }

    fun moveDraggedCard(deltaX: Float) {
        if (draggingId == null || reorderStepPx <= 0f) return
        dragOffsetX += deltaX
        while (dragOffsetX > reorderStepPx / 2 && dragCurrentIndex < cardsToShow.lastIndex) {
            val next = displayOrder.toMutableList()
            val index = next.indexOfFirst { it.id == draggingId }
            val moved = next.removeAt(index)
            next.add(index + 1, moved)
            displayOrder = next
            dragCurrentIndex++
            dragOffsetX -= reorderStepPx
        }
        while (dragOffsetX < -reorderStepPx / 2 && dragCurrentIndex > 0) {
            val next = displayOrder.toMutableList()
            val index = next.indexOfFirst { it.id == draggingId }
            val moved = next.removeAt(index)
            next.add(index - 1, moved)
            displayOrder = next
            dragCurrentIndex--
            dragOffsetX += reorderStepPx
        }
    }

    fun endCardDrag(play: Boolean = false) {
        val start = dragStartIndex
        val end = dragCurrentIndex
        val id = draggingId
        if (play && id != null) vm.play(id) else if (start >= 0 && end >= 0 && start != end) vm.reorder(start, end)
        draggingId = null
        dragOffsetX = 0f
        dragStartIndex = -1
        dragCurrentIndex = -1
        displayOrder = if (play) state.cards else displayOrder
    }

    HanabiTheme(darkBackground = state.darkBackground) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            Column(
                Modifier.padding(padding).padding(horizontal = 12.dp, vertical = 8.dp).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = vm::undo, enabled = state.history.isNotEmpty()) { Icon(Icons.AutoMirrored.Filled.Undo, "Undo") }
                    IconButton(onClick = { settingsDialog = true }) { Icon(Icons.Default.Settings, "Settings") }
                    Spacer(Modifier.weight(1f))
                    AssistChip(onClick = { hintDialog = true }, label = { Text("Give hint") }, leadingIcon = { Icon(Icons.Default.Add, null) })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cardsToShow.forEachIndexed { index, card ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            KnowledgeCard(
                                card = card,
                                state = state,
                                selected = selectedCard == card.id,
                                onSelect = { selectedCard = card.id },
                                onPlay = { if (draggingId == null) vm.play(card.id) else endCardDrag(play = true); selectedCard = null },
                                dragging = draggingId == card.id,
                                dragOffsetX = if (draggingId == card.id) dragOffsetX else 0f,
                                onCardWidthChanged = { cardWidthPx = it },
                                onDragStart = { beginCardDrag(card) },
                                onDragDelta = ::moveDraggedCard,
                                onDragEnd = { endCardDrag() },
                                onDragCancel = { displayOrder = state.cards; draggingId = null; dragOffsetX = 0f }
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
        if (hintDialog) HintDialog(state, onDismiss = { hintDialog = false }, onConfirm = { kind, value, matches -> vm.applyHint(kind, value, matches); hintDialog = false })
        if (settingsDialog) SettingsDialog(
            state = state,
            onDismiss = { settingsDialog = false },
            onNewGame = { sixthColor, multiColor, blackPowder, size -> vm.newGame(sixthColor, multiColor, blackPowder, size); settingsDialog = false },
            onDirection = vm::setDirection,
            onDarkBackground = vm::setDarkBackground
        )
    }
}

@Composable
private fun KnowledgeCard(
    card: TrackedCard,
    state: GameState,
    selected: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit,
    dragging: Boolean,
    dragOffsetX: Float,
    onCardWidthChanged: (Int) -> Unit,
    onDragStart: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    var dragY by remember { mutableFloatStateOf(0f) }
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragDelta by rememberUpdatedState(onDragDelta)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDragCancel by rememberUpdatedState(onDragCancel)
    val currentOnPlay by rememberUpdatedState(onPlay)
    Card(
        Modifier.fillMaxWidth().height(224.dp).onSizeChanged { onCardWidthChanged(it.width) }
            .graphicsLayer { translationX = if (dragging) dragOffsetX else 0f; translationY = if (dragging) -12.dp.toPx() else 0f }
            .zIndex(if (dragging) 1f else 0f)
            .pointerInput(card.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { currentOnDragStart() },
                    onDrag = { change, amount -> change.consume(); dragY += amount.y; currentOnDragDelta(amount.x) },
                    onDragEnd = {
                        if (dragY < -70) currentOnPlay() else currentOnDragEnd()
                        dragY = 0f
                    },
                    onDragCancel = { dragY = 0f; currentOnDragCancel() }
                )
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
        Modifier.padding(top = 3.dp).width(30.dp).height(42.dp).clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant).pointerInput(Unit) {
                detectDragGestures(onDrag = { change, amount -> change.consume(); dragX += amount.x }, onDragEnd = {
                    if (kotlin.math.abs(dragX) > 35) if (dragX < 0) moveLeft() else moveRight()
                    dragX = 0f
                }, onDragCancel = { dragX = 0f })
            },
        contentAlignment = Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(2) { Box(Modifier.width(2.dp).height(22.dp).background(MaterialTheme.colorScheme.onSurfaceVariant)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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

@OptIn(ExperimentalLayoutApi::class)
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
            FlowRow(maxItemsInEachRow = 6, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.ruleset.hintableColors.forEach { color ->
                    SelectableColorSquare(color, kind == HintKind.COLOR && value == color.name) { kind = HintKind.COLOR; value = color.name }
                }
            }
            FlowRow(maxItemsInEachRow = 6, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.ruleset.numbers.forEach { number ->
                    SelectableNumberSquare(number, kind == HintKind.NUMBER && value == number.toString()) { kind = HintKind.NUMBER; value = number.toString() }
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
private fun SettingsDialog(state: GameState, onDismiss: () -> Unit, onNewGame: (Boolean, Boolean, Boolean, Int) -> Unit, onDirection: (Boolean) -> Unit, onDarkBackground: (Boolean) -> Unit) {
    var sixthColor by remember { mutableStateOf(state.ruleset.preset in setOf(Preset.RAINBOW_SIXTH, Preset.BLACK_POWDER_RAINBOW_SIXTH)) }
    var multiColor by remember { mutableStateOf(state.ruleset.preset in setOf(Preset.RAINBOW_MULTI, Preset.BLACK_POWDER_RAINBOW_MULTI)) }
    var blackPowder by remember { mutableStateOf(state.ruleset.preset in setOf(Preset.BLACK_POWDER, Preset.BLACK_POWDER_RAINBOW_SIXTH, Preset.BLACK_POWDER_RAINBOW_MULTI)) }
    var size by remember { mutableIntStateOf(state.handSize) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Settings") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ToggleSetting("6th color", sixthColor) {
                sixthColor = it
                if (it) multiColor = false
            }
            ToggleSetting("Multi-color", multiColor) {
                multiColor = it
                if (it) sixthColor = false
            }
            ToggleSetting("Black Powder", blackPowder) { blackPowder = it }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Hand size", Modifier.weight(1f)); FilterChip(size == 4, { size = 4 }, label = { Text("4") }); Spacer(Modifier.width(6.dp)); FilterChip(size == 5, { size = 5 }, label = { Text("5") }) }
            Text("Replacement enters from", style = MaterialTheme.typography.labelLarge)
            Row { FilterChip(!state.replacementFromRight, { onDirection(false) }, label = { Text("Left") }); Spacer(Modifier.width(6.dp)); FilterChip(state.replacementFromRight, { onDirection(true) }, label = { Text("Right") }) }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Black background", Modifier.weight(1f)); Switch(checked = state.darkBackground, onCheckedChange = onDarkBackground) }
        }
    }, confirmButton = { Button(onClick = { onNewGame(sixthColor, multiColor, blackPowder, size) }) { Text("New game") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

@Composable
private fun ToggleSetting(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
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
private fun Color.brush(): Brush = if (this == Color.RAINBOW) Brush.linearGradient(listOf(UiColor.Red, UiColor.Yellow, UiColor.Green, UiColor.Cyan, UiColor.Blue, UiColor.Magenta)) else Brush.linearGradient(listOf(uiColor(), uiColor()))
