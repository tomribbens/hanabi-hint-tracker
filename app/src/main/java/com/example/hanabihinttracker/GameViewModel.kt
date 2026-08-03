package com.example.hanabihinttracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hanabihinttracker.data.GameRepository
import com.example.hanabihinttracker.data.GameStore
import com.example.hanabihinttracker.domain.GameState
import com.example.hanabihinttracker.domain.HanabiAppState
import com.example.hanabihinttracker.domain.Hint
import com.example.hanabihinttracker.domain.HintApplicationResult
import com.example.hanabihinttracker.domain.HintEngine
import com.example.hanabihinttracker.domain.Ruleset
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: GameStore = GameRepository(application)
) : AndroidViewModel(application) {
    private val _state = MutableStateFlow<HanabiAppState?>(null)
    val state: StateFlow<HanabiAppState?> = _state.asStateFlow()
    private val saveRequests = Channel<HanabiAppState>(Channel.CONFLATED)

    init {
        viewModelScope.launch {
            _state.value = repository.load() ?: HanabiAppState.default()
        }
        viewModelScope.launch {
            for (state in saveRequests) repository.save(state)
        }
    }

    private fun update(transform: (HanabiAppState) -> HanabiAppState) {
        val current = _state.value ?: return
        val next = transform(current)
        _state.value = next
        saveRequests.trySend(next)
    }

    fun newGame(sixthColor: Boolean, multiColor: Boolean, blackPowder: Boolean, handSize: Int) = update { state ->
        state.copy(game = GameState.new(Ruleset.forOptions(sixthColor, multiColor, blackPowder), handSize))
    }

    fun setDirection(fromRight: Boolean) = update { state ->
        state.copy(settings = state.settings.copy(replacementFromRight = fromRight))
    }

    fun setDarkBackground(enabled: Boolean) = update { state ->
        state.copy(settings = state.settings.copy(darkBackground = enabled))
    }

    fun applyHint(hint: Hint, matches: Set<Long>): HintApplicationResult {
        val current = _state.value ?: return HintApplicationResult.Contradiction
        return when (val result = HintEngine.applyHint(current.game, hint, matches)) {
            is HintApplicationResult.Accepted -> {
                val next = current.copy(game = result.state)
                _state.value = next
                saveRequests.trySend(next)
                result
            }

            HintApplicationResult.Contradiction -> result
        }
    }

    fun undo() = update { state -> state.copy(game = HintEngine.undo(state.game)) }

    fun play(cardId: Long) = update { state ->
        state.copy(
            game = HintEngine.play(state.game, cardId, state.settings.replacementFromRight)
        )
    }

    fun reorder(cardId: Long, targetIndex: Int) = update { state ->
        state.copy(game = HintEngine.reorder(state.game, cardId, targetIndex))
    }
}
