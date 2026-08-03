package com.example.hanabihinttracker.domain

enum class Color(val label: String, val hex: Long) {
    RED("Red", 0xFFE45756), YELLOW("Yellow", 0xFFF2C14E), GREEN("Green", 0xFF4CAF75),
    BLUE("Blue", 0xFF4D9DE0), WHITE("White", 0xFFECECEC), RAINBOW("Rainbow", 0xFF9B5DE5),
    BLACK("Black", 0xFF343A40)
}

enum class HintKind { COLOR, NUMBER }
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
            // Black Powder's documented base deck uses the standard five suits plus a black suit.
            Preset.BLACK_POWDER -> Ruleset(preset, setOf(Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE, Color.WHITE, Color.BLACK))
            Preset.BLACK_POWDER_RAINBOW_SIXTH -> Ruleset(preset, setOf(Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE, Color.WHITE, Color.BLACK, Color.RAINBOW))
            Preset.BLACK_POWDER_RAINBOW_MULTI -> Ruleset(preset, setOf(Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE, Color.WHITE, Color.BLACK, Color.RAINBOW), multicolor = true)
        }
    }
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
data class HintRecord(
    val id: Long,
    val kind: HintKind,
    val value: String,
    val matchingCardIds: Set<Long>
)

data class GameState(
    val ruleset: Ruleset,
    val handSize: Int = 5,
    val cards: List<TrackedCard> = emptyList(),
    val history: List<HintRecord> = emptyList(),
    val replacementFromRight: Boolean = true,
    val darkBackground: Boolean = false
) {
    companion object {
        fun new(ruleset: Ruleset, handSize: Int = 5, fromRight: Boolean = true): GameState =
            GameState(ruleset, handSize, List(handSize) { TrackedCard(it + 1L, CardKnowledge.unknown(ruleset)) }, replacementFromRight = fromRight)
    }
}

object HintEngine {
    fun applyHint(state: GameState, kind: HintKind, value: String, matches: Set<Long>): GameState {
        val updated = state.cards.map { card ->
            val isMatch = card.id in matches
            val knowledge = when (kind) {
                HintKind.COLOR -> {
                    val color = Color.valueOf(value)
                    val colors = if (isMatch) card.knowledge.possibleColors.filter { state.ruleset.matchesColor(it, color) }.toSet()
                    else card.knowledge.possibleColors - color - if (state.ruleset.multicolor) setOf(Color.RAINBOW) else emptySet()
                    card.knowledge.copy(possibleColors = colors, colorHints = if (isMatch) card.knowledge.colorHints + color else card.knowledge.colorHints)
                }
                HintKind.NUMBER -> {
                    val number = value.toInt()
                    val numbers = if (isMatch) card.knowledge.possibleNumbers intersect setOf(number)
                    else card.knowledge.possibleNumbers - number
                    card.knowledge.copy(possibleNumbers = numbers, numberHints = if (isMatch) card.knowledge.numberHints + number else card.knowledge.numberHints)
                }
            }
            card.copy(knowledge = knowledge)
        }
        // Never record a hint that leaves a card with no possible identity.
        if (updated.any { it.knowledge.possibleColors.isEmpty() || it.knowledge.possibleNumbers.isEmpty() }) {
            return state
        }
        return state.copy(cards = updated, history = state.history + HintRecord(System.nanoTime(), kind, value, matches))
    }

    fun undo(state: GameState): GameState {
        val last = state.history.lastOrNull() ?: return state
        val base = GameState.new(state.ruleset, state.handSize, state.replacementFromRight).copy(
            cards = state.cards.map { it.copy(knowledge = CardKnowledge.unknown(state.ruleset)) },
            darkBackground = state.darkBackground
        )
        return state.history.dropLast(1).fold(base) { current, hint -> applyHintWithoutHistory(current, hint) }.copy(history = state.history.dropLast(1))
    }

    private fun applyHintWithoutHistory(state: GameState, hint: HintRecord): GameState =
        applyHint(state, hint.kind, hint.value, hint.matchingCardIds).copy(history = state.history)

    fun play(state: GameState, cardId: Long): GameState {
        val index = state.cards.indexOfFirst { it.id == cardId }
        if (index < 0) return state
        val nextId = (state.cards.maxOfOrNull { it.id } ?: 0L) + 1
        val fresh = TrackedCard(nextId, CardKnowledge.unknown(state.ruleset))
        val remaining = state.cards.toMutableList().apply { removeAt(index) }
        val replacement = if (state.replacementFromRight) remaining + fresh else listOf(fresh) + remaining
        return state.copy(cards = replacement)
    }
}
