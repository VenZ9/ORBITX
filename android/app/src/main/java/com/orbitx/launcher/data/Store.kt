package com.orbitx.launcher.data

import android.content.Context
import com.orbitx.launcher.core.OrbitLog
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Local JSON storage.
 *
 * The whole launcher state is one JSON document under filesDir/orbitx-state.json.
 * There is deliberately no database and no network sync: profiles, layouts and
 * settings are small, and a single atomic file rewrite keeps writes cheap and
 * crash-safe.
 *
 * State is exposed as a StateFlow so Compose recomposes on every mutation; the
 * previous-generation launcher relied on a manual refresh counter, which is exactly
 * the pattern that caused screens to show stale data.
 */
object Store {
    private const val FILE = "orbitx-state.json"

    private lateinit var stateFile: File
    private val _state = MutableStateFlow(LauncherState())
    val state: StateFlow<LauncherState> = _state.asStateFlow()

    init { /* initialised in init(context) */ }

    fun init(context: Context) {
        stateFile = File(context.filesDir, FILE)
        val loaded = if (stateFile.isFile) {
            runCatching { parse(JSONObject(stateFile.readText())) }
                .onFailure { OrbitLog.w("state parse failed, starting fresh: ${it.message}") }
                .getOrNull()
        } else null

        val s = loaded ?: LauncherState()
        // First run: seed the built-in layouts and a default profile.
        if (s.layouts.isEmpty()) {
            s.layouts = mutableListOf(Layout.default(), pvpLayout())
            s.selectedLayoutId = s.layouts.first().id
        }
        if (s.profiles.isEmpty()) {
            s.profiles = mutableListOf(Profile(name = "Player"))
            s.selectedProfileId = s.profiles.first().id
        }
        if (s.selectedLayoutId == null) s.selectedLayoutId = s.layouts.first().id
        if (s.selectedProfileId == null) s.selectedProfileId = s.profiles.first().id
        s.settings.ramMb = s.settings.ramMb.coerceIn(512, 8192)
        _state.value = s
        if (loaded == null) save()
    }

    private fun pvpLayout(): Layout = Layout(
        name = "PvP",
        controls = mutableListOf(
            Control(action = "ATTACK", x = 0.84f, y = 0.60f, w = 0.14f, h = 0.14f),
            Control(action = "USE", x = 0.84f, y = 0.44f, w = 0.14f, h = 0.14f),
            Control(action = "JUMP", x = 0.68f, y = 0.78f, w = 0.13f, h = 0.13f),
            Control(action = "SNEAK", x = 0.84f, y = 0.78f, w = 0.13f, h = 0.13f),
            Control(action = "SPRINT", x = 0.02f, y = 0.80f, w = 0.10f, h = 0.12f),
            Control(action = "SWAP_HANDS", x = 0.14f, y = 0.80f, w = 0.10f, h = 0.12f),
            Control(action = "INVENTORY", x = 0.02f, y = 0.64f, w = 0.11f, h = 0.11f),
            Control(action = "DROP", x = 0.15f, y = 0.64f, w = 0.09f, h = 0.11f),
        ),
    )

    private fun parse(o: JSONObject): LauncherState {
        val profiles = mutableListOf<Profile>()
        o.optJSONArray("profiles")?.let { a ->
            for (i in 0 until a.length()) a.optJSONObject(i)?.let { profiles += Profile.fromJson(it) }
        }
        val layouts = mutableListOf<Layout>()
        o.optJSONArray("layouts")?.let { a ->
            for (i in 0 until a.length()) a.optJSONObject(i)?.let { layouts += Layout.fromJson(it) }
        }
        return LauncherState(
            profiles = profiles,
            layouts = layouts,
            selectedLayoutId = o.optString("selectedLayoutId").takeIf { it.isNotBlank() },
            settings = o.optJSONObject("settings")?.let { Settings.fromJson(it) } ?: Settings(),
            selectedProfileId = o.optString("selectedProfileId").takeIf { it.isNotBlank() },
        )
    }

    /** Persist the current state to disk. */
    fun save() {
        runCatching {
            val tmp = File(stateFile.parentFile, "$FILE.tmp")
            tmp.writeText(_state.value.toJson().toString(2))
            if (stateFile.exists()) stateFile.delete()
            tmp.renameTo(stateFile)
        }.onFailure { OrbitLog.e("state save failed", it) }
    }

    // --- Profile operations ---------------------------------------------------

    fun addProfile(p: Profile) {
        mutate { it.profiles += p }
    }

    fun updateProfile(id: String, block: (Profile) -> Unit) {
        mutate { s -> s.profiles.firstOrNull { it.id == id }?.let(block) }
    }

    fun removeProfile(id: String) {
        mutate { s ->
            s.profiles.removeAll { it.id == id }
            if (s.selectedProfileId == id) s.selectedProfileId = s.profiles.firstOrNull()?.id
        }
    }

    fun selectProfile(id: String) {
        mutate { it.selectedProfileId = id }
    }

    fun currentProfile(): Profile? =
        _state.value.profiles.firstOrNull { it.id == _state.value.selectedProfileId }
            ?: _state.value.profiles.firstOrNull()

    // --- Layout operations ---------------------------------------------------

    fun addLayout(l: Layout) {
        mutate { s -> s.layouts += l; s.selectedLayoutId = l.id }
    }

    fun updateLayout(id: String, block: (Layout) -> Unit) {
        mutate { s -> s.layouts.firstOrNull { it.id == id }?.let(block) }
    }

    fun removeLayout(id: String) {
        mutate { s ->
            s.layouts.removeAll { it.id == id }
            if (s.selectedLayoutId == id) s.selectedLayoutId = s.layouts.firstOrNull()?.id
        }
    }

    fun selectLayout(id: String) {
        mutate { it.selectedLayoutId = id }
    }

    fun currentLayout(): Layout? =
        _state.value.layouts.firstOrNull { it.id == _state.value.selectedLayoutId }
            ?: _state.value.layouts.firstOrNull()

    /**
     * Reset the CURRENTLY SELECTED layout back to its built-in geometry.
     * A custom layout has no built-in definition, so it is emptied instead; control
     * identity is preserved for the built-ins so nothing else referencing them breaks.
     */
    fun resetSelectedLayout() {
        mutate { s ->
            val idx = s.layouts.indexOfFirst { it.id == s.selectedLayoutId }
            if (idx < 0) return@mutate
            val current = s.layouts[idx]
            val fresh = when (current.name.lowercase()) {
                "default" -> Layout.default()
                "pvp" -> pvpLayout()
                else -> Layout(name = current.name)
            }
            s.layouts[idx] = current.copy(controls = fresh.controls)
        }
    }

    // --- Settings -------------------------------------------------------------

    fun updateSettings(block: (Settings) -> Unit) {
        mutate { block(it.settings) }
    }

    /** Wipe everything (Settings -> Reset). */
    fun resetAll() {
        _state.value = LauncherState(
            profiles = mutableListOf(Profile(name = "Player").also { }),
            layouts = mutableListOf(Layout.default(), pvpLayout()),
        ).also {
            it.selectedProfileId = it.profiles.first().id
            it.selectedLayoutId = it.layouts.first().id
        }
        save()
    }

    private fun mutate(block: (LauncherState) -> Unit) {
        val next = _state.value
        block(next)
        _state.value = next.copy() // new instance so StateFlow emits
        save()
    }
}
