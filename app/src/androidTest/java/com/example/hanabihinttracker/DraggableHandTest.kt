package com.example.hanabihinttracker

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class DraggableHandTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun draggingAcrossHandReordersWithoutPlaying() {
        var reorderedCardId: Long? = null
        var targetIndex: Int? = null
        var playedCardId: Long? = null
        val cards = com.example.hanabihinttracker.domain.GameState.new(
            com.example.hanabihinttracker.domain.Ruleset.forPreset(
                com.example.hanabihinttracker.domain.Preset.STANDARD
            ),
            handSize = 5
        ).cards

        composeRule.setContent {
            HanabiTheme(darkBackground = false) {
                DraggableHand(
                    cards = cards,
                    selectedCardIds = emptySet(),
                    onCardSelect = {},
                    onPlay = { playedCardId = it },
                    onReorder = { cardId, index ->
                        reorderedCardId = cardId
                        targetIndex = index
                    }
                )
            }
        }

        composeRule.onNodeWithTag("drag-handle-${cards.first().id}").performTouchInput {
            down(center)
            moveBy(Offset(2_000f, 0f), delayMillis = 300)
            up()
        }

        composeRule.runOnIdle {
            assertEquals(cards.first().id, reorderedCardId)
            assertEquals(cards.lastIndex, targetIndex)
            assertNull(playedCardId)
        }
    }
}
