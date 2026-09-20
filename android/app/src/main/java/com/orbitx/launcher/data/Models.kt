package com.orbitx.launcher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Offline player profile: identity is generated locally, no account service involved.
 * This is the "offline mode" every Java launcher offers, and it is why the launcher
 * needs no Microsoft authentication to play.
 */
data class Profile(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    /** Offline UUID derived from the name, so it is stable across installs. */
    var uuid: String = offlineUuid(name),
    var versionId: String = "",
    var ramMb: Int = 2048,
    var rendererId: String = "gl4es_es3",
    var width: Int = 1280,
    var height: Int = 720,
    var javaMajor: Int = 0,
    var lastPlayed: Long = 0L,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name); put("uuid", uuid)
        put("versionId", versionId); put("ramMb", ramMb)
        put("rendererId", rendererId)
        put("width", width); put("height", height)
        put("javaMajor", javaMajor); put("lastPlayed", lastPlayed)
    }

    companion object {
        fun fromJson(o: JSONObject) = Profile(
            id = o.optString("id", UUID.randomUUID().toString()),
            name = o.optString("name", "Player"),
            uuid = o.optString("uuid").ifBlank { offlineUuid(o.optString("name", "Player")) },
            versionId = o.optString("versionId"),
            ramMb = o.optInt("ramMb", 2048),
            rendererId = o.optString("rendererId", "gl4es_es3"),
            width = o.optInt("width", 1280),
            height = o.optInt("height", 720),
            javaMajor = o.optInt("javaMajor", 0),
            lastPlayed = o.optLong("lastPlayed", 0L),
        )

        /**
         * The standard offline UUID: MD5 of "OfflinePlayer:<name>" with the Java
         * UUID version/variant bits forced, matching what the game computes for an
         * unauthenticated player. Using the same value keeps a world's player data
         * consistent with a desktop client.
         */
        fun offlineUuid(name: String): String {
            val md = java.security.MessageDigest.getInstance("MD5")
            val hash = md.digest("OfflinePlayer:$name".toByteArray(Charsets.UTF_8))
            hash[6] = ((hash[6].toInt() and 0x0f) or 0x30).toByte()
            hash[8] = ((hash[8].toInt() and 0x3f) or 0x80).toByte()
            val sb = StringBuilder()
            for (b in hash) sb.append("%02x".format(b))
            val h = sb.toString()
            return "${h.substring(0, 8)}-${h.substring(8, 12)}-${h.substring(12, 16)}-" +
                "${h.substring(16, 20)}-${h.substring(20, 32)}"
        }
    }
}

/**
 * The bindable actions.
 *
 * Stored as plain strings so a layout written by an older build still loads, and so a
 * layout can reference an action this build does not recognise without failing to parse.
 */
val ControlActions = listOf(
    "JUMP", "SNEAK", "SPRINT", "ATTACK", "USE", "INVENTORY", "DROP",
    "SWAP_HANDS", "CHAT", "PAUSE", "PERSPECTIVE",
    "MOVE_UP", "MOVE_LEFT", "MOVE_DOWN", "MOVE_RIGHT",
    "SLOT_1", "SLOT_2", "SLOT_3", "SLOT_4", "SLOT_5",
    "SLOT_6", "SLOT_7", "SLOT_8", "SLOT_9",
)

/** Human-readable label for an action id. */
fun controlLabel(action: String): String = when (action) {
    "MOVE_UP" -> "▲"
    "MOVE_LEFT" -> "◀"
    "MOVE_DOWN" -> "▼"
    "MOVE_RIGHT" -> "▶"
    "ATTACK" -> "LMB"
    "USE" -> "RMB"
    "SWAP_HANDS" -> "SWAP"
    "INVENTORY" -> "INV"
    "PERSPECTIVE" -> "VIEW"
    else -> action.replace('_', ' ')
}

/** A control binding in the Custom Controls editor (normalized 0..1 coordinates). */
data class Control(
    val id: String = UUID.randomUUID().toString(),
    var action: String,
    var x: Float,
    var y: Float,
    var w: Float,
    var h: Float,
    var opacity: Float = 1f,
    var enabled: Boolean = true,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("action", action)
        put("x", x.toDouble()); put("y", y.toDouble())
        put("w", w.toDouble()); put("h", h.toDouble())
        put("opacity", opacity.toDouble()); put("enabled", enabled)
    }

    companion object {
        fun fromJson(o: JSONObject) = Control(
            id = o.optString("id", UUID.randomUUID().toString()),
            action = o.optString("action", "JUMP"),
            x = o.optDouble("x", 0.1).toFloat(),
            y = o.optDouble("y", 0.1).toFloat(),
            w = o.optDouble("w", 0.12).toFloat(),
            h = o.optDouble("h", 0.12).toFloat(),
            opacity = o.optDouble("opacity", 1.0).toFloat(),
            enabled = o.optBoolean("enabled", true),
        )

        /** Shorthand for the built-in layouts. */
        fun at(action: String, x: Float, y: Float, w: Float, h: Float) =
            Control(action = action, x = x, y = y, w = w, h = h)
    }
}

/** A named control layout. */
data class Layout(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var controls: MutableList<Control> = mutableListOf(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name)
        put("controls", JSONArray().apply { controls.forEach { put(it.toJson()) } })
    }

    companion object {
        fun fromJson(o: JSONObject): Layout {
            val arr = o.optJSONArray("controls") ?: JSONArray()
            val list = ArrayList<Control>(arr.length())
            for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { list += Control.fromJson(it) }
            return Layout(
                id = o.optString("id", UUID.randomUUID().toString()),
                name = o.optString("name", "Layout"),
                // A layout saved by an older build can hold a control that sits past the
                // canvas edge; clamping on load is what stops it rendering outside.
                controls = list.map { it.clampIntoCanvas() }.toMutableList(),
            )
        }

        /** Nine hotbar slots laid out as a centred row. */
        private fun hotbar(startX: Float, y: Float, w: Float = 0.062f, h: Float = 0.085f) =
            (1..9).map { i -> Control.at("SLOT_$i", startX + (i - 1) * w, y, w, h) }

        /** A four-way movement pad: up, left, down, right around a cell grid. */
        private fun dPad(cx: Float, cy: Float) = listOf(
            Control.at("MOVE_UP", cx - 0.030f, cy - 0.135f, 0.075f, 0.130f),
            Control.at("MOVE_LEFT", cx - 0.105f, cy, 0.075f, 0.130f),
            Control.at("MOVE_DOWN", cx - 0.030f, cy + 0.135f, 0.075f, 0.130f),
            Control.at("MOVE_RIGHT", cx + 0.045f, cy, 0.075f, 0.130f),
        )

        /**
         * The stock layout: the arrangement PojavLauncher and Zalith Launcher ship with.
         *
         * Movement and the toggles sit under the left thumb, look/attack/use and jump
         * under the right, the status buttons run along the top-right, and the hotbar is a
         * centred row along the bottom. Every rectangle satisfies x + w <= 1 and
         * y + h <= 1 by construction, so nothing can render outside the canvas.
         */
        fun default(): Layout = Layout(
            name = "Default",
            controls = (
                dPad(cx = 0.140f, cy = 0.625f) + listOf(
                    Control.at("SNEAK", 0.030f, 0.250f, 0.095f, 0.150f),
                    Control.at("SPRINT", 0.030f, 0.845f, 0.095f, 0.140f),
                    Control.at("ATTACK", 0.760f, 0.430f, 0.150f, 0.150f),
                    Control.at("USE", 0.760f, 0.600f, 0.150f, 0.150f),
                    Control.at("JUMP", 0.870f, 0.760f, 0.115f, 0.170f),
                    Control.at("INVENTORY", 0.620f, 0.175f, 0.090f, 0.130f),
                    Control.at("CHAT", 0.720f, 0.175f, 0.075f, 0.130f),
                    Control.at("DROP", 0.810f, 0.175f, 0.075f, 0.130f),
                    Control.at("PAUSE", 0.900f, 0.175f, 0.075f, 0.130f),
                ) + hotbar(startX = 0.215f, y = 0.895f)
                ).toMutableList(),
        )

        /**
         * The PvP layout: attack/use enlarged under the right thumb, movement and the
         * hotbar pushed left, and the utility buttons along the top.
         */
        fun pvp(): Layout = Layout(
            name = "PvP",
            controls = (
                dPad(cx = 0.105f, cy = 0.670f) + listOf(
                    Control.at("SPRINT", 0.700f, 0.315f, 0.095f, 0.115f),
                    Control.at("SWAP_HANDS", 0.810f, 0.315f, 0.095f, 0.115f),
                    Control.at("INVENTORY", 0.700f, 0.150f, 0.110f, 0.145f),
                    Control.at("DROP", 0.820f, 0.150f, 0.095f, 0.145f),
                    Control.at("CHAT", 0.925f, 0.150f, 0.070f, 0.145f),
                    Control.at("ATTACK", 0.820f, 0.560f, 0.165f, 0.175f),
                    Control.at("USE", 0.820f, 0.365f, 0.165f, 0.175f),
                    Control.at("SNEAK", 0.880f, 0.845f, 0.110f, 0.145f),
                    Control.at("JUMP", 0.470f, 0.700f, 0.120f, 0.145f),
                ) + hotbar(startX = 0.300f, y = 0.885f, h = 0.100f)
                ).toMutableList(),
        )
    }
}

/** Global launcher settings (the Settings screen). */
data class Settings(
    var ramMb: Int = 2048,
    var rendererId: String = "gl4es_es3",
    var width: Int = 1280,
    var height: Int = 720,
    var gameDirOverride: String = "",
    var skipAssets: Boolean = false,
    /** Disables the on-screen Java/GL diagnostics banner on the Home screen. */
    var showDiagnostics: Boolean = true,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("ramMb", ramMb); put("rendererId", rendererId)
        put("width", width); put("height", height)
        put("gameDirOverride", gameDirOverride)
        put("skipAssets", skipAssets); put("showDiagnostics", showDiagnostics)
    }

    companion object {
        fun fromJson(o: JSONObject) = Settings(
            ramMb = o.optInt("ramMb", 2048),
            rendererId = o.optString("rendererId", "gl4es_es3"),
            width = o.optInt("width", 1280),
            height = o.optInt("height", 720),
            gameDirOverride = o.optString("gameDirOverride", ""),
            skipAssets = o.optBoolean("skipAssets", false),
            showDiagnostics = o.optBoolean("showDiagnostics", true),
        )
    }
}

/** Installed-version metadata surfaced by the Versions screen. */
data class InstalledVersion(
    val id: String,
    val loader: String,
    val hasJar: Boolean,
    val folder: java.io.File,
)

/** The whole persisted state. */
data class LauncherState(
    var profiles: MutableList<Profile> = mutableListOf(),
    var layouts: MutableList<Layout> = mutableListOf(),
    var selectedLayoutId: String? = null,
    var settings: Settings = Settings(),
    var selectedProfileId: String? = null,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("profiles", JSONArray().apply { profiles.forEach { put(it.toJson()) } })
        put("layouts", JSONArray().apply { layouts.forEach { put(it.toJson()) } })
        put("selectedLayoutId", selectedLayoutId ?: JSONObject.NULL)
        put("settings", settings.toJson())
        put("selectedProfileId", selectedProfileId ?: JSONObject.NULL)
    }
}
