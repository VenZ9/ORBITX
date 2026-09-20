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
                controls = list,
            )
        }

        /**
         * The built-in Default layout: the survival hotbar plus movement.
         * Geometry is chosen so every control already satisfies
         * x >= 0, y >= 0, x + w <= 1, y + h <= 1.
         */
        fun default(): Layout = Layout(
            name = "Default",
            controls = mutableListOf(
                Control(action = "JUMP", x = 0.80f, y = 0.62f, w = 0.13f, h = 0.13f),
                Control(action = "SNEAK", x = 0.80f, y = 0.78f, w = 0.13f, h = 0.13f),
                Control(action = "SPRINT", x = 0.93f, y = 0.78f, w = 0.07f, h = 0.13f),
                Control(action = "ATTACK", x = 0.03f, y = 0.72f, w = 0.14f, h = 0.14f),
                Control(action = "USE", x = 0.03f, y = 0.55f, w = 0.14f, h = 0.14f),
                Control(action = "INVENTORY", x = 0.03f, y = 0.38f, w = 0.14f, h = 0.12f),
                Control(action = "SLOT_1", x = 0.22f, y = 0.90f, w = 0.055f, h = 0.08f),
                Control(action = "SLOT_2", x = 0.285f, y = 0.90f, w = 0.055f, h = 0.08f),
                Control(action = "SLOT_3", x = 0.35f, y = 0.90f, w = 0.055f, h = 0.08f),
                Control(action = "SLOT_4", x = 0.415f, y = 0.90f, w = 0.055f, h = 0.08f),
                Control(action = "SLOT_5", x = 0.48f, y = 0.90f, w = 0.055f, h = 0.08f),
            ),
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
