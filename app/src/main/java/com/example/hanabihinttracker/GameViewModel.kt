package com.example.hanabihinttracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hanabihinttracker.data.GameRepository
import com.example.hanabihinttracker.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GameRepository(application)
    private val _state = MutableStateFlow(GameState.new(Ruleset.forPreset(Preset.STANDARD)))
    val state: StateFlow<GameState> = _state.asStateFlow()

    init { viewModelScope.launch { repository.load()?.let { _state.value = it } } }

    fun update(transform: (GameState) -> GameState) {
        val next = transform(_state.value)
        _state.value = next
        viewModelScope.launch { repository.save(next) }
    }

    fun newGame(preset: Preset, handSize: Int) = update { GameState.new(Ruleset.forPreset(preset), handSize, it.replacementFromRight).copy(darkBackground = it.darkBackground) }
    fun setDirection(fromRight: Boolean) = update { it.copy(replacementFromRight = fromRight) }
    fun setDarkBackground(enabled: Boolean) = update { it.copy(darkBackground = enabled) }
    fun applyHint(kind: HintKind, value: String, matches: Set<Long>) = update { HintEngine.applyHint(it, kind, value, matches) }
    fun undo() = update(HintEngine::undo)
    fun play(cardId: Long) = update { HintEngine.play(it, cardId) }
    fun reorder(from: Int, to: Int) = update { state -> state.copy(cards = state.cards.toMutableList().apply { add(to.coerceIn(0, size - 1), removeAt(from)) }) }
}
