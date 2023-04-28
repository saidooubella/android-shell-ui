package com.saidooubella.shellui.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.saidooubella.shellui.utils.toStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.runBlocking

private val Context.preferences by preferencesDataStore(name = "settings")
private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")

internal class ShellPreferences(private val context: Context) {

    internal suspend fun toggleDarkTheme() {
        context.preferences.edit { settings ->
            val darkTheme = settings[DARK_THEME_KEY] ?: false
            settings[DARK_THEME_KEY] = !darkTheme
        }
    }

    internal fun getDarkTheme(scope: CoroutineScope): StateFlow<Boolean> {
        return context.preferences.toStateFlow(scope, DARK_THEME_KEY, false)
    }
}
