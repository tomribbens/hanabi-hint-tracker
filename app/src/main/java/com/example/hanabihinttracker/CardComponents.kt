package com.example.hanabihinttracker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.hanabihinttracker.domain.Color
import com.example.hanabihinttracker.domain.TrackedCard
import androidx.compose.ui.graphics.Color as UiColor

@Composable
internal fun KnowledgeCard(
    card: TrackedCard,
    selected: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit,
    dragging: Boolean,
    onCardWidthChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val knownColor = card.knowledge.possibleColors.singleOrNull()
    val knownNumber = card.knowledge.possibleNumbers.singleOrNull()
    Card(
        modifier.fillMaxWidth().height(224.dp).onSizeChanged { onCardWidthChanged(it.width) }
            .clickable(onClick = onSelect)
            .graphicsLayer {
                translationY = if (dragging) -18.dp.toPx() else 0f
                scaleX = if (dragging) 1.08f else 1f
                scaleY = if (dragging) 1.08f else 1f
                shadowElevation = if (dragging) 24.dp.toPx() else 0f
            }
            .zIndex(if (dragging) 1f else 0f),
        colors = CardDefaults.cardColors(
            containerColor = when {
                selected -> MaterialTheme.colorScheme.primaryContainer
                knownColor != null -> UiColor.Transparent
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            Modifier.fillMaxSize()
                .then(if (knownColor != null && !selected) Modifier.background(knownColor.brush()) else Modifier)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            if (knownColor == null) ColorSquares(card.knowledge.possibleColors)
            if (knownNumber != null) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        knownNumber.toString(),
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        color = knownColor?.textColor() ?: MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                NumberSquares(card.knowledge.possibleNumbers)
                Spacer(Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                card.knowledge.colorHints.forEach { ColorSquare(it, 16.dp) }
                card.knowledge.numberHints.forEach { NumberSquare(it, 20.dp) }
            }
            OutlinedButton(onClick = onPlay, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                Text("Play")
            }
        }
    }
}

@Composable
internal fun DragPlaceholder() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().height(224.dp).clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        )
        Row(
            Modifier.height(45.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(2) {
                Box(Modifier.width(2.dp).height(22.dp).background(MaterialTheme.colorScheme.outline))
            }
        }
    }
}

@Composable
internal fun ReorderMarker(cardId: Long, dragging: Boolean = false) {
    Box(
        Modifier.padding(top = 3.dp).width(30.dp).height(42.dp)
            .testTag("drag-handle-$cardId")
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (dragging) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(2) {
                Box(Modifier.width(2.dp).height(22.dp).background(MaterialTheme.colorScheme.onSurfaceVariant))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColorSquares(colors: Set<Color>) {
    FlowRow(
        maxItemsInEachRow = 5,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        colors.forEach { ColorSquare(it, 20.dp) }
    }
}

@Composable
internal fun ColorSquare(color: Color, size: Dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(4.dp)).background(color.brush())
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NumberSquares(numbers: Set<Int>) {
    FlowRow(
        maxItemsInEachRow = 5,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        numbers.sorted().forEach { NumberSquare(it, 20.dp) }
    }
}

@Composable
internal fun NumberSquare(number: Int, size: Dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(number.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun SelectableColorSquare(color: Color, selected: Boolean, size: Dp = 34.dp, onClick: () -> Unit) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(5.dp)).background(color.brush())
            .border(
                if (selected) 3.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(5.dp)
            )
            .clickable(onClick = onClick)
    )
}

@Composable
internal fun SelectableNumberSquare(number: Int, selected: Boolean, size: Dp = 34.dp, onClick: () -> Unit) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(5.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                if (selected) 3.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(5.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(number.toString(), fontWeight = FontWeight.Bold)
    }
}
