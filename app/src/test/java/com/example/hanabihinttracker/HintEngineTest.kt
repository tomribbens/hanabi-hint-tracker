package com.example.hanabihinttracker

import com.example.hanabihinttracker.domain.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HintEngineTest {
    @Test fun colorHintKeepsValueOnMatchAndRemovesItElsewhere() {
        val state = GameState.new(Ruleset.forPreset(Preset.STANDARD), 4)
        val result = HintEngine.applyHint(state, HintKind.COLOR, Color.RED.name, setOf(1L))
        assertEquals(setOf(Color.RED), result.cards[0].knowledge.possibleColors)
        assertTrue(Color.RED !in result.cards[1].knowledge.possibleColors)
        assertEquals(1, result.history.size)
    }

    @Test fun contradictoryNumberHintIsNotRecorded() {
        val state = GameState.new(Ruleset.forPreset(Preset.STANDARD), 4)
        val result = HintEngine.applyHint(state, HintKind.NUMBER, "3", setOf(1L))
        val second = HintEngine.applyHint(result, HintKind.NUMBER, "4", setOf(1L))
        assertEquals(result, second)
    }

    @Test fun playAddsUnknownCardAtConfiguredEdge() {
        val state = GameState.new(Ruleset.forPreset(Preset.STANDARD), 4, fromRight = true)
        val result = HintEngine.play(state, state.cards[1].id)
        assertEquals(4, result.cards.size)
        assertEquals(state.cards[0].id, result.cards[0].id)
        assertTrue(result.cards.last().knowledge.possibleColors.size == 5)
        val left = HintEngine.play(state.copy(replacementFromRight = false), state.cards[1].id)
        assertTrue(left.cards.first().id > state.cards.maxOf { it.id })
    }

    @Test fun undoRebuildsKnowledge() {
        val state = GameState.new(Ruleset.forPreset(Preset.STANDARD), 4)
        val changed = HintEngine.applyHint(state, HintKind.COLOR, Color.BLUE.name, setOf(1L))
        val undone = HintEngine.undo(changed)
        assertEquals(state.cards.map { it.knowledge }, undone.cards.map { it.knowledge })
        assertTrue(undone.history.isEmpty())
    }

    @Test fun unmatchedRainbowMulticolorCardLosesRainbowPossibility() {
        val state = GameState.new(Ruleset.forPreset(Preset.RAINBOW_MULTI), 4)
        val result = HintEngine.applyHint(state, HintKind.COLOR, Color.RED.name, setOf(1L))
        assertTrue(Color.RAINBOW in result.cards[0].knowledge.possibleColors)
        assertTrue(Color.RAINBOW !in result.cards[1].knowledge.possibleColors)
    }

    @Test fun contradictoryHintIsNotRecorded() {
        val state = GameState.new(Ruleset.forPreset(Preset.STANDARD), 4)
        val knownRed = HintEngine.applyHint(state, HintKind.COLOR, Color.RED.name, setOf(1L))
        val contradictory = HintEngine.applyHint(knownRed, HintKind.COLOR, Color.BLUE.name, setOf(1L))
        assertEquals(knownRed, contradictory)
    }
}
