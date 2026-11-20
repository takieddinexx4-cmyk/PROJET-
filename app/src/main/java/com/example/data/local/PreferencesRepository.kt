package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.MirrorConfig
import com.example.data.model.MirrorQualityPreset
import com.example.ui.theme.GlassThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dzCastDataStore: DataStore<Preferences> by preferencesDataStore(name = "dz_cast_prefs")

data class UserSettings(
    val themeMode: GlassThemeMode = GlassThemeMode.DUAL_PRISM,
    val frostIntensity: Float = 0.74f,
    val adBlockEnabled: Boolean = true,
    val desktopUserAgent: Boolean = false,
    val serverPort: Int = 8899,
    val mirrorConfig: MirrorConfig = MirrorConfig()
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FROST_INTENSITY = floatPreferencesKey("frost_intensity")
        val AD_BLOCK_ENABLED = booleanPreferencesKey("ad_block_enabled")
        val DESKTOP_USER_AGENT = booleanPreferencesKey("desktop_user_agent")
        val SERVER_PORT = intPreferencesKey("server_port")
        val MIRROR_PRESET = stringPreferencesKey("mirror_preset")
        val MIRROR_FPS = intPreferencesKey("mirror_fps")
        val MIRROR_QUALITY = intPreferencesKey("mirror_quality")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    }

    val settingsFlow: Flow<UserSettings> = context.dzCastDataStore.data.map { prefs ->
        val themeName = prefs[Keys.THEME_MODE]
        val themeMode = GlassThemeMode.entries.firstOrNull { it.name == themeName }
            ?: GlassThemeMode.DUAL_PRISM
        val preset = MirrorQualityPreset.fromId(prefs[Keys.MIRROR_PRESET])
        UserSettings(
            themeMode = themeMode,
            frostIntensity = prefs[Keys.FROST_INTENSITY] ?: 0.74f,
            adBlockEnabled = prefs[Keys.AD_BLOCK_ENABLED] ?: true,
            desktopUserAgent = prefs[Keys.DESKTOP_USER_AGENT] ?: false,
            serverPort = (prefs[Keys.SERVER_PORT] ?: 8899).coerceIn(8080, 65000),
            mirrorConfig = MirrorConfig(
                preset = preset,
                targetFps = prefs[Keys.MIRROR_FPS] ?: 30,
                jpegQuality = (prefs[Keys.MIRROR_QUALITY] ?: preset.defaultQuality).coerceIn(45, 92),
                keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true
            )
        )
    }

    suspend fun setThemeMode(mode: GlassThemeMode) {
        context.dzCastDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setFrostIntensity(value: Float) {
        context.dzCastDataStore.edit { it[Keys.FROST_INTENSITY] = value.coerceIn(0.35f, 1.0f) }
    }

    suspend fun setAdBlockEnabled(enabled: Boolean) {
        context.dzCastDataStore.edit { it[Keys.AD_BLOCK_ENABLED] = enabled }
    }

    suspend fun setDesktopUserAgent(enabled: Boolean) {
        context.dzCastDataStore.edit { it[Keys.DESKTOP_USER_AGENT] = enabled }
    }

    suspend fun setMirrorPreset(preset: MirrorQualityPreset) {
        context.dzCastDataStore.edit {
            it[Keys.MIRROR_PRESET] = preset.id
            it[Keys.MIRROR_QUALITY] = preset.defaultQuality
        }
    }

    suspend fun setMirrorFps(fps: Int) {
        context.dzCastDataStore.edit { it[Keys.MIRROR_FPS] = fps.coerceIn(15, 60) }
    }

    suspend fun setMirrorJpegQuality(quality: Int) {
        context.dzCastDataStore.edit { it[Keys.MIRROR_QUALITY] = quality.coerceIn(45, 92) }
    }

    suspend fun setKeepScreenOn(keepOn: Boolean) {
        context.dzCastDataStore.edit { it[Keys.KEEP_SCREEN_ON] = keepOn }
    }
}
