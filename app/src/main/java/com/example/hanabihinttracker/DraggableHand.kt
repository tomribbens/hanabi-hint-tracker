package com.example.hanabihinttracker

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.hanabihinttracker.domain.TrackedCard
import kotlin.math.abs
import kotlin.math.roundToInt

internal data class ActiveCardDrag(
    val cardId: Long,
    val startIndex: Int,
    val targetIndex: Int,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

internal fun dragTargetIndex(startIndex: Int, offsetX: Float, slotStep: Float, cardCount: Int): Int {
    if (slotStep <= 0f || cardCount <= 0) return startIndex
    return (startIndex + (offsetX / slotStep).roundToInt()).coerceIn(0, cardCount - 1)
}

internal fun <T> moveItem(items: List<T>, sourceIndex: Int, targetIndex: Int): List<T> {
    if (sourceIndex !in items.indices || targetIndex !in items.indices || sourceIndex == targetIndex) return items
    return items.toMutableList().apply { add(targetIndex, removeAt(sourceIndex)) }
}

@Composable
internal fun DraggableHand(
    cards: List<TrackedCard>,
    selectedCardIds: Set<Long>,
    onCardSelect: (Long) -> Unit,
    onPlay: (Long) -> Unit,
    onReorder: (Long, Int) -> Unit
) {
    var displayOrder by remember { mutableStateOf(cards) }
    var activeDrag by remember { mutableStateOf<ActiveCardDrag?>(null) }
    var cardWidthPx by remember { mutableIntStateOf(0) }
    var handWidthPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(cards) {
        if (activeDrag == null) displayOrder = cards
    }

    val density = LocalDensity.current
    val spacingPx = with(density) { 8.dp.toPx() }
    val dragHandleTopPx = with(density) { 224.dp.toPx() }
    val dragHandleHalfWidthPx = with(density) { 24.dp.toPx() }
    val slotStepPx = if (displayOrder.isNotEmpty() && handWidthPx > 0) {
        (handWidthPx - spacingPx * (displayOrder.size - 1)) / displayOrder.size + spacingPx
    } else {
        cardWidthPx + spacingPx
    }

    val draggedCard = activeDrag?.let { drag -> displayOrder.firstOrNull { it.id == drag.cardId } }
    val rowSlots: List<TrackedCard?> = if (draggedCard == null) {
        displayOrder
    } else {
        displayOrder.filterNot { it.id == draggedCard.id }
            .map<TrackedCard, TrackedCard?> { it }
            .toMutableList()
            .apply { add(activeDrag!!.targetIndex.coerceIn(0, size), null) }
    }

    fun finishDrag() {
        val drag = activeDrag ?: return
        val sourceIndex = displayOrder.indexOfFirst { it.id == drag.cardId }
        if (sourceIndex >= 0 && sourceIndex != drag.targetIndex) {
            displayOrder = moveItem(displayOrder, sourceIndex, drag.targetIndex)
            onReorder(drag.cardId, drag.targetIndex)
        }
        activeDrag = null
    }

    fun cancelDrag() {
        activeDrag = null
        displayOrder = cards
    }

    Box(
        Modifier.fillMaxWidth().height(270.dp).onSizeChanged { handWidthPx = it.width }
            .pointerInput(slotStepPx, displayOrder, dragHandleTopPx, dragHandleHalfWidthPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val slotIndex = if (slotStepPx > 0f) (down.position.x / slotStepPx).toInt() else -1
                    val slotWidthPx = slotStepPx - spacingPx
                    val positionInSlotPx = down.position.x - slotIndex * slotStepPx
                    val onDragHandle = down.position.y >= dragHandleTopPx &&
                        abs(positionInSlotPx - slotWidthPx / 2f) <= dragHandleHalfWidthPx
                    if (onDragHandle && slotIndex in displayOrder.indices) {
                        val card = displayOrder[slotIndex]
                        activeDrag = ActiveCardDrag(card.id, slotIndex, slotIndex)
                        down.consume()
                        val completed = drag(down.id) { change ->
                            val amount = change.positionChange()
                            if (amount.x != 0f || amount.y != 0f) {
                                change.consume()
                                val current = activeDrag ?: return@drag
                                val nextOffsetX = current.offsetX + amount.x
                                activeDrag = current.copy(
                                    targetIndex = dragTargetIndex(
                                        current.startIndex,
                                        nextOffsetX,
                                        slotStepPx,
                                        displayOrder.size
                                    ),
                                    offsetX = nextOffsetX,
                                    offsetY = current.offsetY + amount.y
                                )
                            }
                        }
                        if (completed) finishDrag() else cancelDrag()
                    }
                }
            }
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            rowSlots.forEach { card ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (card == null) {
                        DragPlaceholder()
                    } else {
                        KnowledgeCard(
                            card = card,
                            selected = card.id in selectedCardIds,
                            onSelect = { onCardSelect(card.id) },
                            onPlay = { onPlay(card.id) },
                            dragging = false,
                            onCardWidthChanged = { cardWidthPx = it }
                        )
                        ReorderMarker(card.id)
                    }
                }
            }
        }
        val drag = activeDrag
        if (draggedCard != null && drag != null) {
            Column(
                Modifier
                    .width(with(density) { cardWidthPx.coerceAtLeast(1).toDp() })
                    .offset {
                        IntOffset(
                            (drag.startIndex * slotStepPx + drag.offsetX).roundToInt(),
                            drag.offsetY.roundToInt()
                        )
                    }
                    .zIndex(2f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                KnowledgeCard(
                    card = draggedCard,
                    selected = draggedCard.id in selectedCardIds,
                    onSelect = { onCardSelect(draggedCard.id) },
                    onPlay = { onPlay(draggedCard.id) },
                    dragging = true,
                    onCardWidthChanged = { cardWidthPx = it }
                )
                ReorderMarker(draggedCard.id, dragging = true)
            }
        }
    }
}
