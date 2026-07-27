package net.afanasev.radioplayer.core.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.afanasev.radioplayer.core.analytics.NoOpPlayerAnalytics
import net.afanasev.radioplayer.core.analytics.PlayerAnalytics

// Must stay a top-level singleton delegate: DataStore throws if more than one instance is
// created against the same underlying file, which would happen if this lived per-ThemeStore-instance.
private val Context.themeDataStore by preferencesDataStore(name = "radio_player_settings")

private val KEY_THEME = stringPreferencesKey("theme")

class ThemeStore(
    private val context: Context,
    private val analytics: PlayerAnalytics = NoOpPlayerAnalytics,
) {

    val theme: Flow<PlayerTheme> = context.themeDataStore.data.map { prefs ->
        prefs[KEY_THEME]?.let { name -> runCatching { PlayerTheme.valueOf(name) }.getOrNull() }
            ?: PlayerTheme.ARTWORK
    }

    suspend fun saveTheme(theme: PlayerTheme) {
        context.themeDataStore.edit { prefs -> prefs[KEY_THEME] = theme.name }
        analytics.onThemeSelect(theme)
    }
}
