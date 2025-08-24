package com.example.strategicassetallocationassistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.example.strategicassetallocationassistant.data.repository.PortfolioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID

/**
 * ViewModel for Add / Edit Asset screen.
 *
 * It stores temporary form data, handles data validation, distinguishes between
 * “add” and “edit” modes, and communicates with [PortfolioRepository] to
 * persist changes.
 */
class AddEditAssetViewModel(
    private val repository: PortfolioRepository,
    assetIdArg: String?
) : ViewModel() {

    /** navArgument key used in NavGraph */
    companion object {
        const val ARG_ASSET_ID = "assetId"
    }

    /** ---------------------------  UI State  --------------------------- */

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _type = MutableStateFlow(AssetType.MONEY_FUND)
    val type: StateFlow<AssetType> = _type.asStateFlow()

    private val _targetWeightInput = MutableStateFlow("")
    val targetWeightInput: StateFlow<String> = _targetWeightInput.asStateFlow()

    private val _code = MutableStateFlow("")
    val code: StateFlow<String> = _code.asStateFlow()

    private val _sharesInput = MutableStateFlow("")
    val sharesInput: StateFlow<String> = _sharesInput.asStateFlow()

    private val _unitValueInput = MutableStateFlow("")
    val unitValueInput: StateFlow<String> = _unitValueInput.asStateFlow()

    /** id == null means we are adding a new asset */
    private var editingAssetId: UUID? = assetIdArg?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    init {
        // If editing existing asset, load it from repository
        editingAssetId?.let { id ->
            viewModelScope.launch {
                repository.getAssetById(id)?.let { asset ->
                    _name.value = asset.name
                    _type.value = asset.type
                    _targetWeightInput.value = (asset.targetWeight * 100).toString()
                    _code.value = asset.code ?: ""
                    _sharesInput.value = asset.shares?.toString() ?: ""
                    _unitValueInput.value = asset.unitValue?.toString() ?: ""
                }
            }
        }
    }

    /** ---------------------------  User Intents --------------------------- */

    fun onNameChange(value: String) { _name.value = value }
    fun onTypeChange(value: AssetType) { _type.value = value }
    fun onTargetWeightChange(value: String) { _targetWeightInput.value = value }
    fun onCodeChange(value: String) { _code.value = value }
    fun onSharesChange(value: String) { _sharesInput.value = value }
    fun onUnitValueChange(value: String) { _unitValueInput.value = value }

    /**
     * Persist the form. Returns true if success, false if validation failed.
     */
    suspend fun save(): Boolean {
        val parsed = buildAsset() ?: return false
        if (editingAssetId == null) {
            repository.insertAsset(parsed)
        } else {
            repository.updateAsset(parsed)
        }
        return true
    }

    suspend fun delete() {
        editingAssetId?.let { id ->
            repository.getAssetById(id)?.let { repository.deleteAsset(it) }
        }
    }

    /** ---------------------------  Helpers --------------------------- */

    private fun buildAsset(): Asset? {
        // Basic validation
        if (_name.value.isBlank()) return null
        val targetWeight = _targetWeightInput.value.toDoubleOrNull() ?: return null
        val shares = _sharesInput.value.toDoubleOrNull()
        val unitValue = _unitValueInput.value.toDoubleOrNull()

        return Asset(
            id = editingAssetId ?: UUID.randomUUID(),
            name = _name.value.trim(),
            type = _type.value,
            targetWeight = targetWeight / 100.0, // UI uses percentage input
            code = _code.value.ifBlank { null },
            shares = shares,
            unitValue = unitValue,
            lastUpdateTime = LocalDateTime.now()
        )
    }
}
