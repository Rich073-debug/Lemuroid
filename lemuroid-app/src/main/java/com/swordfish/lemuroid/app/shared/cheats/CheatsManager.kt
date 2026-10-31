package com.swordfish.lemuroid.app.shared.cheats

import android.content.Context
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.LibretroDroid
import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable

data class Cheat(
    val name: String,
    val code: String,
    val enabled: Boolean,
) : Serializable

/**
 * Stores the cheat codes of each game. Codes are kept per game (system + file name), so they
 * survive library rescans, and they are sent to the core every time the game screen is resumed.
 */
class CheatsManager(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getCheats(game: Game): List<Cheat> {
        val raw = preferences.getString(computeKey(game), null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                Cheat(
                    name = item.optString(KEY_NAME, ""),
                    code = item.optString(KEY_CODE, ""),
                    enabled = item.optBoolean(KEY_ENABLED, false),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveCheats(
        game: Game,
        cheats: List<Cheat>,
    ) {
        val array = JSONArray()
        cheats.forEach {
            array.put(
                JSONObject()
                    .put(KEY_NAME, it.name)
                    .put(KEY_CODE, it.code)
                    .put(KEY_ENABLED, it.enabled),
            )
        }
        preferences.edit().putString(computeKey(game), array.toString()).apply()
    }

    private fun computeKey(game: Game): String {
        return "cheats_${game.systemId}_${game.fileName}"
    }

    companion object {
        private const val PREFERENCES_NAME = "com.swordfish.lemuroid.cheats"
        private const val KEY_NAME = "name"
        private const val KEY_CODE = "code"
        private const val KEY_ENABLED = "enabled"

        // Same syntax accepted by the melonDS DS core: groups of 8 hex digits.
        private val NDS_CODE_REGEX = Regex("^[0-9A-Fa-f]{8}([+\\s-]*[0-9A-Fa-f]{8})*$")
        private val HEX_TOKEN_REGEX = Regex("[0-9A-Fa-f]{8}")

        private val SUPPORTED_SYSTEMS = setOf(SystemID.NDS.dbname)

        fun isSupported(systemId: String): Boolean {
            return systemId in SUPPORTED_SYSTEMS
        }

        fun isValidCode(code: String): Boolean {
            val trimmed = code.trim()
            if (trimmed.isEmpty()) return false
            val tokens = HEX_TOKEN_REGEX.findAll(trimmed).count()
            // Action Replay codes are made of pairs of 32 bit words.
            return NDS_CODE_REGEX.matches(trimmed) && tokens % 2 == 0
        }

        /** Converts any spacing/new lines into the "XXXXXXXX YYYYYYYY+..." form the cores expect. */
        fun normalizeCode(code: String): String {
            return HEX_TOKEN_REGEX.findAll(code)
                .map { it.value.uppercase() }
                .chunked(2)
                .joinToString("+") { it.joinToString(" ") }
        }
    }
}

fun GLRetroView.resetCheats() {
    LibretroDroid.resetCheat()
}
