package com.example.thecommunity.data.repositories

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking


private val Context.dataStore by preferencesDataStore(name = "settings")

class DataStoreRepository(private val context: Context) {

    private val dataStore = context.dataStore

    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

    val darkModeFlow: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[DARK_MODE_KEY] ?: false }

    suspend fun saveDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }
}

class ThemeManager(private val context: Context) {

    private val datastoreRepository = DataStoreRepository(context)

    // Function to get theme preference synchronously (for initialization)
    fun getInitialThemeMode(): Boolean {
        return runBlocking {
            datastoreRepository.darkModeFlow.first()
        }
    }
}

