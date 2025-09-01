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

        // Buy factor parameters
        private val KEY_HALF_SATURATION_R = androidx.datastore.preferences.core.doublePreferencesKey("half_saturation_relative_offset")
        private val KEY_HALF_SATURATION_D = androidx.datastore.preferences.core.doublePreferencesKey("half_saturation_drawdown")
        private val KEY_ALPHA = androidx.datastore.preferences.core.doublePreferencesKey("offset_weight_alpha")

        // Sell threshold params
        private val KEY_BASE_SELL_THRESHOLD = androidx.datastore.preferences.core.doublePreferencesKey("base_sell_threshold")
        private val KEY_HALF_TOTAL_RISK = androidx.datastore.preferences.core.doublePreferencesKey("half_total_risk")

        const val DEFAULT_HALF_SATURATION_R = 0.10
        const val DEFAULT_HALF_SATURATION_D = 0.05
        const val DEFAULT_ALPHA = 0.8

        const val DEFAULT_BASE_SELL_THRESHOLD = 0.30
        const val DEFAULT_HALF_TOTAL_RISK = 0.00035
    }

    val refreshIntervalMinutes: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_REFRESH_INTERVAL] ?: DEFAULT_INTERVAL_MINUTES
    }

    val halfSaturationR: Flow<Double> = context.dataStore.data.map { it[KEY_HALF_SATURATION_R] ?: DEFAULT_HALF_SATURATION_R }
    val halfSaturationD: Flow<Double> = context.dataStore.data.map { it[KEY_HALF_SATURATION_D] ?: DEFAULT_HALF_SATURATION_D }
    val alpha: Flow<Double> = context.dataStore.data.map { it[KEY_ALPHA] ?: DEFAULT_ALPHA }

    val baseSellThreshold: Flow<Double> = context.dataStore.data.map { it[KEY_BASE_SELL_THRESHOLD] ?: DEFAULT_BASE_SELL_THRESHOLD }
    val halfTotalRisk: Flow<Double> = context.dataStore.data.map { it[KEY_HALF_TOTAL_RISK] ?: DEFAULT_HALF_TOTAL_RISK }

    suspend fun setRefreshIntervalMinutes(minutes: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REFRESH_INTERVAL] = minutes
        }
    }

    suspend fun setHalfSaturationR(value: Double) {
        context.dataStore.edit { it[KEY_HALF_SATURATION_R] = value }
    }

    suspend fun setHalfSaturationD(value: Double) {
        context.dataStore.edit { it[KEY_HALF_SATURATION_D] = value }
    }

    suspend fun setAlpha(value: Double) {
        context.dataStore.edit { it[KEY_ALPHA] = value }
    }

    suspend fun setBaseSellThreshold(value: Double) {
        context.dataStore.edit { it[KEY_BASE_SELL_THRESHOLD] = value }
    }

    suspend fun setHalfTotalRisk(value: Double) {
        context.dataStore.edit { it[KEY_HALF_TOTAL_RISK] = value }
    }
}
