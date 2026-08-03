package com.example.hanabihinttracker.domain

enum class Color(val label: String, val hex: Long) {
    RED("Red", 0xFFE45756), YELLOW("Yellow", 0xFFF2C14E), GREEN("Green", 0xFF4CAF75),
    BLUE("Blue", 0xFF4D9DE0), WHITE("White", 0xFFECECEC), RAINBOW("Rainbow", 0xFF9B5DE5),
    BLACK("Black", 0xFF343A40)
}

enum class Preset(val title: String) {
    STANDARD("Standard Hanabi"), RAINBOW_SIXTH("Rainbow · sixth color"),
    RAINBOW_MULTI("Rainbow · multicolor"), BLACK_POWDER("Black Powder"),
    BLACK_POWDER_RAINBOW_SIXTH("Black Powder + Rainbow · sixth color"),
    BLACK_POWDER_RAINBOW_MULTI("Black Powder + Rainbow · multicolor")
}

data class Ruleset(
    val preset: Preset,
    val colors: Set<Color>,
    val numbers: Set<Int> = (1..5).toSet(),
    val multicolor: Boolean = false
) {
    val hintableColors: Set<Color> get() = colors - Color.BLACK - if (multicolor) setOf(Color.RAINBOW) else emptySet()

    fun matchesColor(possible: Color, hint: Color): Boolean =
        if (multicolor && possible == Color.RAINBOW) hint != Color.RAINBOW else possible == hint

    companion object {
        fun forOptions(sixthColor: Boolean, multiColor: Boolean, blackPowder: Boolean): Ruleset {
            val preset = when {
                blackPowder && multiColor -> Preset.BLACK_POWDER_RAINBOW_MULTI
                blackPowder && sixthColor -> Preset.BLACK_POWDER_RAINBOW_SIXTH
                blackPowder -> Preset.BLACK_POWDER
                multiColor -> Preset.RAINBOW_MULTI
                sixthColor -> Preset.RAINBOW_SIXTH
                else -> Preset.STANDARD
            }
            return forPreset(preset)
        }

        fun forPreset(preset: Preset): Ruleset = when (preset) {
            Preset.STANDARD -> Ruleset(preset, setOf(Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE, Color.WHITE))
            Preset.RAINBOW_SIXTH -> Ruleset(preset, setOf(Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE, Color.WHITE, Color.RAINBOW))
            Preset.RAINBOW_MULTI -> Ruleset(preset, setOf(Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE, Color.WHITE, Color.RAINBOW), multicolor = true)
            Preset.BLACK_POWDER -> Ruleset(preset, setOf(Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE, Color.WHITE, Color.BLACK))
            Preset.BLACK_POWDER_RAINBOW_SIXTH -> Ruleset(preset, setOf(Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE, Color.WHITE, Color.BLACK, Color.RAINBOW))
            Preset.BLACK_POWDER_RAINBOW_MULTI -> Ruleset(preset, setOf(Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE, Color.WHITE, Color.BLACK, Color.RAINBOW), multicolor = true)
        }
    }
}

sealed interface Hint {
    data class ColorHint(val color: Color) : Hint
    data class NumberHint(val number: Int) : Hint
}

data class CardKnowledge(
    val possibleColors: Set<Color>,
    val possibleNumbers: Set<Int>,
    val colorHints: List<Color> = emptyList(),
    val numberHints: List<Int> = emptyList()
) {
    companion object {
        fun unknown(rules: Ruleset) = CardKnowledge(rules.colors, rules.numbers)
    }
}

data class TrackedCard(val id: Long, val knowledge: CardKnowledge)
data class HintRecord(val hint: Hint, val matchingCardIds: Set<Long>)

data class GameState(
    val ruleset: Ruleset,
    val handSize: Int = 5,
    val cards: List<TrackedCard> = emptyList(),
    val history: List<HintRecord> = emptyList()
) {
    companion object {
        fun new(ruleset: Ruleset, handSize: Int = 5): GameState =
            GameState(ruleset, handSize, List(handSize) { TrackedCard(it + 1L, CardKnowledge.unknown(ruleset)) })
    }
}

data class AppSettings(
    val replacementFromRight: Boolean = true,
    val darkBackground: Boolean = false
)

data class HanabiAppState(
    val game: GameState,
    val settings: AppSettings = AppSettings()
) {
    companion object {
        fun default() = HanabiAppState(GameState.new(Ruleset.forPreset(Preset.STANDARD)))
    }
}

sealed interface HintApplicationResult {
    data class Accepted(val state: GameState) : HintApplicationResult
    data object Contradiction : HintApplicationResult
}

object HintEngine {
    fun applyHint(state: GameState, hint: Hint, matches: Set<Long>): HintApplicationResult {
        val updated = updateCards(state, hint, matches)
        if (updated.any { it.knowledge.possibleColors.isEmpty() || it.knowledge.possibleNumbers.isEmpty() }) {
            return HintApplicationResult.Contradiction
        }
        return HintApplicationResult.Accepted(
            state.copy(cards = updated, history = state.history + HintRecord(hint, matches))
        )
    }

    fun undo(state: GameState): GameState {
        if (state.history.isEmpty()) return state
        val remainingHistory = state.history.dropLast(1)
        val unknownCards = state.cards.map { it.copy(knowledge = CardKnowledge.unknown(state.ruleset)) }
        return remainingHistory.fold(state.copy(cards = unknownCards, history = emptyList())) { current, record ->
            when (val result = applyHint(current, record.hint, record.matchingCardIds)) {
                is HintApplicationResult.Accepted -> result.state
                HintApplicationResult.Contradiction -> current
            }
        }
    }

    fun play(state: GameState, cardId: Long, replacementFromRight: Boolean): GameState {
        val index = state.cards.indexOfFirst { it.id == cardId }
        if (index < 0) return state
        val nextId = (state.cards.maxOfOrNull { it.id } ?: 0L) + 1
        val fresh = TrackedCard(nextId, CardKnowledge.unknown(state.ruleset))
        val remaining = state.cards.toMutableList().apply { removeAt(index) }
        val replacement = if (replacementFromRight) remaining + fresh else listOf(fresh) + remaining
        return state.copy(cards = replacement)
    }

    fun reorder(state: GameState, cardId: Long, targetIndex: Int): GameState {
        val sourceIndex = state.cards.indexOfFirst { it.id == cardId }
        if (sourceIndex < 0 || state.cards.isEmpty()) return state
        val destination = targetIndex.coerceIn(state.cards.indices)
        if (sourceIndex == destination) return state
        return state.copy(cards = state.cards.toMutableList().apply {
            add(destination, removeAt(sourceIndex))
        })
    }

    private fun updateCards(state: GameState, hint: Hint, matches: Set<Long>): List<TrackedCard> =
        state.cards.map { card ->
            val isMatch = card.id in matches
            val knowledge = when (hint) {
                is Hint.ColorHint -> {
                    val colors = if (isMatch) {
                        card.knowledge.possibleColors.filter { state.ruleset.matchesColor(it, hint.color) }.toSet()
                    } else {
                        card.knowledge.possibleColors - hint.color -
                            if (state.ruleset.multicolor) setOf(Color.RAINBOW) else emptySet()
                    }
                    card.knowledge.copy(
                        possibleColors = colors,
                        colorHints = if (isMatch) card.knowledge.colorHints + hint.color else card.knowledge.colorHints
                    )
                }

                is Hint.NumberHint -> {
                    val numbers = if (isMatch) {
                        card.knowledge.possibleNumbers intersect setOf(hint.number)
                    } else {
                        card.knowledge.possibleNumbers - hint.number
                    }
                    card.knowledge.copy(
                        possibleNumbers = numbers,
                        numberHints = if (isMatch) card.knowledge.numberHints + hint.number else card.knowledge.numberHints
                    )
                }
            }
            card.copy(knowledge = knowledge)
        }
}
