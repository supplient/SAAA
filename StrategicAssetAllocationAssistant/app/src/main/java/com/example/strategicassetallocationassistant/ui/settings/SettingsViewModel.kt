package com.example.strategicassetallocationassistant.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.strategicassetallocationassistant.data.preferences.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.strategicassetallocationassistant.data.repository.PortfolioRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesRepository,
    private val portfolioRepository: PortfolioRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _intervalMinutes = MutableStateFlow(PreferencesRepository.DEFAULT_INTERVAL_MINUTES)
    val intervalMinutes: StateFlow<Long> = _intervalMinutes

    private val _halfSatR = MutableStateFlow(PreferencesRepository.DEFAULT_HALF_SATURATION_R)
    val halfSaturationR: StateFlow<Double> = _halfSatR

    private val _halfSatD = MutableStateFlow(PreferencesRepository.DEFAULT_HALF_SATURATION_D)
    val halfSaturationD: StateFlow<Double> = _halfSatD

    private val _alpha = MutableStateFlow(PreferencesRepository.DEFAULT_ALPHA)
    val alpha: StateFlow<Double> = _alpha

    private val _events = MutableSharedFlow<Event>()
    val events: SharedFlow<Event> = _events

    init {
        viewModelScope.launch {
            _intervalMinutes.value = prefs.refreshIntervalMinutes.first()

            _halfSatR.value = prefs.halfSaturationR.first()
            _halfSatD.value = prefs.halfSaturationD.first()
            _alpha.value = prefs.alpha.first()
        }
    }

    fun onIntervalSelected(minutes: Long) {
        viewModelScope.launch {
            prefs.setRefreshIntervalMinutes(minutes)
            _intervalMinutes.value = minutes
        }
    }

    fun onHalfSaturationRChanged(value: Double) {
        viewModelScope.launch {
            prefs.setHalfSaturationR(value)
            _halfSatR.value = value
        }
    }

    fun onHalfSaturationDChanged(value: Double) {
        viewModelScope.launch {
            prefs.setHalfSaturationD(value)
            _halfSatD.value = value
        }
    }

    fun onAlphaChanged(value: Double) {
        viewModelScope.launch {
            prefs.setAlpha(value)
            _alpha.value = value
        }
    }

    fun exportData() {
        viewModelScope.launch {
            val json = portfolioRepository.exportDataToJson()
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("portfolio_data", json)
            clipboard.setPrimaryClip(clip)
            _events.emit(Event.ExportSuccess)
        }
    }

    fun importData(json: String) {
        viewModelScope.launch {
            try {
                portfolioRepository.importDataFromJson(json)
                _events.emit(Event.ImportSuccess)
            } catch (e: Exception) {
                _events.emit(Event.ImportError(e.message ?: "未知错误"))
            }
        }
    }

    sealed class Event {
        object ExportSuccess : Event()
        object ImportSuccess : Event()
        data class ImportError(val message: String) : Event()
    }
}
