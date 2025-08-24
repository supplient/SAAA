package com.example.strategicassetallocationassistant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for Add / Edit Transaction screen.
 */
@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val repository: com.example.strategicassetallocationassistant.data.repository.PortfolioRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        const val ARG_TRANSACTION_ID = "transactionId"
    }

    /* --------------------------  State  -------------------------- */
    private val _type = MutableStateFlow(TradeType.BUY)
    val type: StateFlow<TradeType> = _type.asStateFlow()

    private val _assetIdInput = MutableStateFlow("")
    val assetIdInput: StateFlow<String> = _assetIdInput.asStateFlow()

    // All assets for dropdown
    val assets: StateFlow<List<Asset>> = repository.assetsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedAssetId = MutableStateFlow<UUID?>(null)
    val selectedAssetId: StateFlow<UUID?> = _selectedAssetId.asStateFlow()

    private val _sharesInput = MutableStateFlow("")
    val sharesInput: StateFlow<String> = _sharesInput.asStateFlow()

    private val _priceInput = MutableStateFlow("")
    val priceInput: StateFlow<String> = _priceInput.asStateFlow()

    private val _feeInput = MutableStateFlow("")
    val feeInput: StateFlow<String> = _feeInput.asStateFlow()

    private val editingTxId: UUID? = savedStateHandle.get<String>(ARG_TRANSACTION_ID)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    val isEditing: Boolean get() = editingTxId != null

    init {
        editingTxId?.let { id ->
            viewModelScope.launch {
                repository.getTransactionById(id)?.let { tx ->
                    _type.value = tx.type
                    _assetIdInput.value = tx.assetId?.toString().orEmpty()
                    _sharesInput.value = tx.shares.toString()
                    _priceInput.value = tx.price.toString()
                    _feeInput.value = tx.fee.toString()
                }
            }
        }
    }

    /* -----------------------  Intents  ----------------------- */
    fun onTypeChange(value: TradeType) { _type.value = value }
    fun onAssetIdChange(value: String) { _assetIdInput.value = value }

    fun onAssetSelected(asset: Asset?) {
        _selectedAssetId.value = asset?.id
        _assetIdInput.value = asset?.id?.toString().orEmpty()
    }

    fun onSharesChange(value: String) { _sharesInput.value = value }
    fun onPriceChange(value: String) { _priceInput.value = value }
    fun onFeeChange(value: String) { _feeInput.value = value }

    suspend fun save(): Boolean {
        val tx = buildTransaction() ?: return false
        if (editingTxId == null) {
            repository.addTransaction(tx)
        } else {
            repository.updateTransaction(tx)
        }
        return true
    }

    suspend fun delete() {
        editingTxId?.let { id ->
            repository.getTransactionById(id)?.let { repository.deleteTransaction(it) }
        }
    }

    /* ---------------------  Helpers  --------------------- */

    private fun buildTransaction(): Transaction? {
        val shares = _sharesInput.value.toDoubleOrNull() ?: return null
        val price = _priceInput.value.toDoubleOrNull() ?: return null
        val fee = _feeInput.value.toDoubleOrNull() ?: 0.0
        val assetUuid = _selectedAssetId.value ?: return null // Asset is mandatory now

        val amount = shares * price + fee
        return Transaction(
            id = editingTxId ?: UUID.randomUUID(),
            assetId = assetUuid,
            type = _type.value,
            shares = shares,
            price = price,
            fee = fee,
            amount = amount,
            time = LocalDateTime.now()
        )
    }
}
