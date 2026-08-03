package com.example.hanabihinttracker

import com.example.hanabihinttracker.data.GameStateCodec
import com.example.hanabihinttracker.domain.AppSettings
import com.example.hanabihinttracker.domain.Color
import com.example.hanabihinttracker.domain.GameState
import com.example.hanabihinttracker.domain.HanabiAppState
import com.example.hanabihinttracker.domain.Hint
import com.example.hanabihinttracker.domain.HintApplicationResult
import com.example.hanabihinttracker.domain.HintEngine
import com.example.hanabihinttracker.domain.Preset
import com.example.hanabihinttracker.domain.Ruleset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GameStateCodecTest {
    @Test
    fun versionedStateRoundTrips() {
        val initial = GameState.new(Ruleset.forPreset(Preset.BLACK_POWDER_RAINBOW_MULTI), 4)
        val hinted = (HintEngine.applyHint(initial, Hint.NumberHint(3), setOf(1L)) as HintApplicationResult.Accepted).state
        val state = HanabiAppState(
            game = hinted,
            settings = AppSettings(replacementFromRight = false, darkBackground = true)
        )

        assertEquals(state, GameStateCodec.decode(GameStateCodec.encode(state)))
    }

    @Test
    fun legacyV1StateStillLoads() {
        val legacy = """
            {
              "preset":"STANDARD",
              "handSize":4,
              "fromRight":false,
              "darkBackground":true,
              "cards":[
                {"id":1,"colors":["RED"],"numbers":[1,2,3,4,5],"colorHints":["RED"],"numberHints":[]}
              ],
              "history":[
                {"id":123,"kind":"COLOR","value":"RED","matches":[1]}
              ]
            }
        """.trimIndent()

        val decoded = GameStateCodec.decode(legacy)

        assertNotNull(decoded)
        assertEquals(false, decoded!!.settings.replacementFromRight)
        assertEquals(true, decoded.settings.darkBackground)
        assertEquals(Hint.ColorHint(Color.RED), decoded.game.history.single().hint)
    }
}
