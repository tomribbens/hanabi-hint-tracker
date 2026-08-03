package com.example.hanabihinttracker

import com.example.hanabihinttracker.domain.Color
import com.example.hanabihinttracker.domain.GameState
import com.example.hanabihinttracker.domain.Hint
import com.example.hanabihinttracker.domain.HintApplicationResult
import com.example.hanabihinttracker.domain.HintEngine
import com.example.hanabihinttracker.domain.Preset
import com.example.hanabihinttracker.domain.Ruleset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HintEngineTest {
    @Test
    fun colorHintKeepsValueOnMatchAndRemovesItElsewhere() {
        val state = standardGame()
        val result = HintEngine.applyHint(state, Hint.ColorHint(Color.RED), setOf(1L)).acceptedState()

        assertEquals(setOf(Color.RED), result.cards[0].knowledge.possibleColors)
        assertTrue(Color.RED !in result.cards[1].knowledge.possibleColors)
        assertEquals(1, result.history.size)
    }

    @Test
    fun contradictoryNumberHintReturnsExplicitResult() {
        val state = standardGame()
        val first = HintEngine.applyHint(state, Hint.NumberHint(3), setOf(1L)).acceptedState()
        val second = HintEngine.applyHint(first, Hint.NumberHint(4), setOf(1L))

        assertSame(HintApplicationResult.Contradiction, second)
    }

    @Test
    fun playAddsUnknownCardAtConfiguredEdge() {
        val state = standardGame()
        val right = HintEngine.play(state, state.cards[1].id, replacementFromRight = true)
        val left = HintEngine.play(state, state.cards[1].id, replacementFromRight = false)

        assertEquals(4, right.cards.size)
        assertEquals(state.cards[0].id, right.cards[0].id)
        assertEquals(state.ruleset.colors, right.cards.last().knowledge.possibleColors)
        assertTrue(left.cards.first().id > state.cards.maxOf { it.id })
    }

    @Test
    fun reorderUsesStableCardIdentity() {
        val state = standardGame()
        val movedId = state.cards.first().id
        val result = HintEngine.reorder(state, movedId, state.cards.lastIndex)

        assertEquals(movedId, result.cards.last().id)
        assertEquals(state.cards.drop(1).map { it.id }, result.cards.dropLast(1).map { it.id })
    }

    @Test
    fun gameCanStartWithSixCards() {
        val state = GameState.new(Ruleset.forPreset(Preset.STANDARD), 6)

        assertEquals(6, state.cards.size)
        assertTrue(state.cards.all { it.knowledge.possibleColors == state.ruleset.colors })
    }

    @Test
    fun undoRebuildsKnowledge() {
        val state = standardGame()
        val changed = HintEngine.applyHint(state, Hint.ColorHint(Color.BLUE), setOf(1L)).acceptedState()
        val undone = HintEngine.undo(changed)

        assertEquals(state.cards.map { it.knowledge }, undone.cards.map { it.knowledge })
        assertTrue(undone.history.isEmpty())
    }

    @Test
    fun unmatchedRainbowMulticolorCardLosesRainbowPossibility() {
        val state = GameState.new(Ruleset.forPreset(Preset.RAINBOW_MULTI), 4)
        val result = HintEngine.applyHint(state, Hint.ColorHint(Color.RED), setOf(1L)).acceptedState()

        assertTrue(Color.RAINBOW in result.cards[0].knowledge.possibleColors)
        assertTrue(Color.RAINBOW !in result.cards[1].knowledge.possibleColors)
    }

    private fun standardGame() = GameState.new(Ruleset.forPreset(Preset.STANDARD), 4)

    private fun HintApplicationResult.acceptedState(): GameState =
        (this as HintApplicationResult.Accepted).state
}
