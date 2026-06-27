package com.example.customkeyboard.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "keyboard_settings")

class DataStoreHelper(private val context: Context) {
    companion object {
        private val RECENT_SYMBOLS_KEY = stringPreferencesKey("recent_symbols")
        private val DEFAULT_RECENTS = listOf("★", "♥", "😂", "👍", "🙏", "😊", "✨", "🔥", "✔", "❌")
    }

    val recentSymbols: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val raw = preferences[RECENT_SYMBOLS_KEY]
        if (raw.isNullOrEmpty()) {
            DEFAULT_RECENTS
        } else {
            raw.split(",")
        }
    }

    suspend fun addRecentSymbol(symbol: String) {
        if (symbol.isEmpty()) return
        context.dataStore.edit { preferences ->
            val raw = preferences[RECENT_SYMBOLS_KEY]
            val list = if (raw.isNullOrEmpty()) {
                DEFAULT_RECENTS.toMutableList()
            } else {
                raw.split(",").toMutableList()
            }

            // Move to front if exists
            list.remove(symbol)
            list.add(0, symbol)

            // Limit to 28 symbols (7x4 grid size)
            if (list.size > 28) {
                while (list.size > 28) {
                    list.removeAt(list.lastIndex)
                }
            }

            preferences[RECENT_SYMBOLS_KEY] = list.joinToString(",")
        }
    }
}
