package com.example.hanabihinttracker.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.hanabihinttracker.domain.AppSettings
import com.example.hanabihinttracker.domain.CardKnowledge
import com.example.hanabihinttracker.domain.Color
import com.example.hanabihinttracker.domain.GameState
import com.example.hanabihinttracker.domain.HanabiAppState
import com.example.hanabihinttracker.domain.Hint
import com.example.hanabihinttracker.domain.HintRecord
import com.example.hanabihinttracker.domain.Preset
import com.example.hanabihinttracker.domain.Ruleset
import com.example.hanabihinttracker.domain.TrackedCard
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.gameDataStore by preferencesDataStore("hanabi_game")

interface GameStore {
    suspend fun load(): HanabiAppState?
    suspend fun save(state: HanabiAppState)
}

class GameRepository(private val context: Context) : GameStore {
    private val stateKey = stringPreferencesKey("state")

    override suspend fun load(): HanabiAppState? =
        context.gameDataStore.data.map { preferences -> preferences[stateKey]?.let(GameStateCodec::decode) }.first()

    override suspend fun save(state: HanabiAppState) {
        context.gameDataStore.edit { it[stateKey] = GameStateCodec.encode(state) }
    }
}

object GameStateCodec {
    private const val SCHEMA_VERSION = 2

    fun encode(state: HanabiAppState): String {
        val game = state.game
        val root = JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("preset", game.ruleset.preset.name)
            .put("handSize", game.handSize)
            .put(
                "settings",
                JSONObject()
                    .put("fromRight", state.settings.replacementFromRight)
                    .put("darkBackground", state.settings.darkBackground)
            )
        root.put("cards", JSONArray().apply {
            game.cards.forEach { card ->
                put(
                    JSONObject()
                        .put("id", card.id)
                        .put("colors", JSONArray(card.knowledge.possibleColors.map { it.name }))
                        .put("numbers", JSONArray(card.knowledge.possibleNumbers.toList()))
                        .put("colorHints", JSONArray(card.knowledge.colorHints.map { it.name }))
                        .put("numberHints", JSONArray(card.knowledge.numberHints))
                )
            }
        })
        root.put("history", JSONArray().apply {
            game.history.forEach { record ->
                val kind = if (record.hint is Hint.ColorHint) "COLOR" else "NUMBER"
                val value = when (val hint = record.hint) {
                    is Hint.ColorHint -> hint.color.name
                    is Hint.NumberHint -> hint.number.toString()
                }
                put(
                    JSONObject()
                        .put("kind", kind)
                        .put("value", value)
                        .put("matches", JSONArray(record.matchingCardIds.toList()))
                )
            }
        })
        return root.toString()
    }

    fun decode(raw: String): HanabiAppState? = runCatching {
        val root = JSONObject(raw)
        val rules = Ruleset.forPreset(Preset.valueOf(root.getString("preset")))
        val cards = root.getJSONArray("cards").let { array ->
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                val knowledge = CardKnowledge(
                    possibleColors = item.getJSONArray("colors").toStrings().map(Color::valueOf).toSet(),
                    possibleNumbers = item.getJSONArray("numbers").toInts().toSet(),
                    colorHints = item.getJSONArray("colorHints").toStrings().map(Color::valueOf),
                    numberHints = item.getJSONArray("numberHints").toInts()
                )
                TrackedCard(item.getLong("id"), knowledge)
            }
        }
        val history = root.optJSONArray("history")?.let { array ->
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                val hint = when (item.getString("kind")) {
                    "COLOR" -> Hint.ColorHint(Color.valueOf(item.getString("value")))
                    "NUMBER" -> Hint.NumberHint(item.getString("value").toInt())
                    else -> error("Unknown hint kind")
                }
                HintRecord(hint, item.getJSONArray("matches").toLongs().toSet())
            }
        } ?: emptyList()
        val settingsObject = root.optJSONObject("settings")
        val settings = AppSettings(
            replacementFromRight = settingsObject?.optBoolean("fromRight", true)
                ?: root.optBoolean("fromRight", true),
            darkBackground = settingsObject?.optBoolean("darkBackground", false)
                ?: root.optBoolean("darkBackground", false)
        )
        HanabiAppState(
            game = GameState(rules, root.getInt("handSize"), cards, history),
            settings = settings
        )
    }.getOrNull()
}

private fun JSONArray.toStrings() = (0 until length()).map { getString(it) }
private fun JSONArray.toInts() = (0 until length()).map { getInt(it) }
private fun JSONArray.toLongs() = (0 until length()).map { getLong(it) }
