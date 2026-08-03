package com.example.hanabihinttracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.hanabihinttracker.domain.Hint
import com.example.hanabihinttracker.domain.HintRecord
import com.example.hanabihinttracker.domain.TrackedCard

@Composable
internal fun HistoryPanel(history: List<HintRecord>, cards: List<TrackedCard>) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Hint history", style = MaterialTheme.typography.titleSmall)
        val summary = if (history.isEmpty()) {
            "No hints recorded yet"
        } else {
            history.takeLast(2).asReversed().joinToString("\n") { record ->
                val label = when (val hint = record.hint) {
                    is Hint.ColorHint -> "Color ${hint.color.label}"
                    is Hint.NumberHint -> "Number #${hint.number}"
                }
                val matchingCards = record.matchingCardIds.count { id -> cards.any { it.id == id } }
                "$label · $matchingCards cards"
            }
        }
        Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
