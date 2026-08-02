package com.example.hanabihinttracker.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.hanabihinttracker.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.gameDataStore by preferencesDataStore("hanabi_game")

class GameRepository(private val context: Context) {
    private val stateKey = stringPreferencesKey("state")

    suspend fun load(): GameState? = context.gameDataStore.data.map { prefs -> prefs[stateKey]?.let(::decode) }.first()

    suspend fun save(state: GameState) {
        context.gameDataStore.edit { it[stateKey] = encode(state) }
    }

    private fun encode(state: GameState): String {
        val root = JSONObject().put("preset", state.ruleset.preset.name).put("handSize", state.handSize).put("fromRight", state.replacementFromRight)
        root.put("cards", JSONArray().apply { state.cards.forEach { card ->
            put(JSONObject().put("id", card.id).put("colors", JSONArray(card.knowledge.possibleColors.map { it.name }))
                .put("numbers", JSONArray(card.knowledge.possibleNumbers.toList())).put("colorHints", JSONArray(card.knowledge.colorHints.map { it.name }))
                .put("numberHints", JSONArray(card.knowledge.numberHints)))
        } })
        root.put("history", JSONArray().apply { state.history.forEach { hint ->
            put(JSONObject().put("id", hint.id).put("kind", hint.kind.name).put("value", hint.value).put("matches", JSONArray(hint.matchingCardIds.toList())))
        } })
        return root.toString()
    }

    private fun decode(raw: String): GameState? = runCatching {
        val root = JSONObject(raw)
        val rules = Ruleset.forPreset(Preset.valueOf(root.getString("preset")))
        val cards = root.getJSONArray("cards").let { array -> (0 until array.length()).map { i ->
            val item = array.getJSONObject(i)
            val knowledge = CardKnowledge(item.getJSONArray("colors").toStrings().map(Color::valueOf).toSet(), item.getJSONArray("numbers").toInts().toSet(), item.getJSONArray("colorHints").toStrings().map(Color::valueOf), item.getJSONArray("numberHints").toInts())
            TrackedCard(item.getLong("id"), knowledge)
        } }
        val history = root.optJSONArray("history")?.let { array -> (0 until array.length()).map { i ->
            val item = array.getJSONObject(i)
            HintRecord(item.getLong("id"), HintKind.valueOf(item.getString("kind")), item.getString("value"), item.getJSONArray("matches").toLongs().toSet())
        } } ?: emptyList()
        GameState(rules, root.getInt("handSize"), cards, history, root.optBoolean("fromRight", true))
    }.getOrNull()
}

private fun JSONArray.toStrings() = (0 until length()).map { getString(it) }
private fun JSONArray.toInts() = (0 until length()).map { getInt(it) }
private fun JSONArray.toLongs() = (0 until length()).map { getLong(it) }
