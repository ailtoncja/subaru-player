package com.subaruplayer.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val AppJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    coerceInputValues = true
}

enum class HwMode { AUTO, HARDWARE, SOFTWARE }
enum class SortBy { NAME, DATE, DURATION, SIZE }

@Serializable
data class AppSettings(
    // Reprodução
    val resume: Boolean = true,
    val autoNext: Boolean = true,
    /** Segundos pulados a cada toque duplo (5, 10, 15 ou 30). */
    val skipSeconds: Int = 10,
    /** Velocidade enquanto o dedo segura a tela. */
    val holdSpeed: Float = 2f,
    val hwMode: HwMode = HwMode.AUTO,
    val pip: Boolean = true,
    // Legendas (valores do libVLC: --freetype-rel-fontsize, onde número menor = letra maior; cor RGB)
    val subSize: Int = 16,
    val subColor: Int = 0xFFFFFF,
    // Abertura
    val boxerOnOpen: Boolean = true,
    // Biblioteca
    val gridView: Boolean = false,
    val sortBy: SortBy = SortBy.NAME,
    val sortDesc: Boolean = false,
)

/** Guarda as configurações como JSON em SharedPreferences e as expõe como fluxo. */
object SettingsStore {
    private const val PREFS = "settings"
    private const val KEY = "json"
    private lateinit var appContext: Context
    private val _state = MutableStateFlow(AppSettings())
    val state: StateFlow<AppSettings> = _state.asStateFlow()
    val value get() = _state.value

    fun init(context: Context) {
        appContext = context.applicationContext
        val raw = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        if (raw != null) _state.value = runCatching { AppJson.decodeFromString<AppSettings>(raw) }.getOrDefault(AppSettings())
    }

    fun update(block: (AppSettings) -> AppSettings) {
        val novo = block(_state.value)
        _state.value = novo
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, AppJson.encodeToString(AppSettings.serializer(), novo)).apply()
    }
}
