package com.example.strategicassetallocationassistant.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore(name = "settings")

class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_REFRESH_INTERVAL = longPreferencesKey("refresh_interval_minutes")
        const val DEFAULT_INTERVAL_MINUTES: Long = 15L
    }

    val refreshIntervalMinutes: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_REFRESH_INTERVAL] ?: DEFAULT_INTERVAL_MINUTES
    }

    suspend fun setRefreshIntervalMinutes(minutes: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REFRESH_INTERVAL] = minutes
        }
    }
}
