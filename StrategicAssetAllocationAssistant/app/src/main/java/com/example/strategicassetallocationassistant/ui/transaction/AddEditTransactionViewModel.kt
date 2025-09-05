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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import com.example.strategicassetallocationassistant.ui.common.model.AssetInfo
import com.example.strategicassetallocationassistant.ui.common.util.buildAssetInfo
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * ViewModel for Add / Edit Transaction screen.
 */
@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val repository: com.example.strategicassetallocationassistant.data.repository.PortfolioRepository,
    private val updateMarketData: com.example.strategicassetallocationassistant.domain.UpdateMarketDataUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        const val ARG_TRANSACTION_ID = "transactionId"
        const val ARG_OPPORTUNITY_ID = "opId"
        const val ARG_ASSET_ID = "assetId"
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

    private val _sharesInput = MutableStateFlow("100")
    val sharesInput: StateFlow<String> = _sharesInput.asStateFlow()

    // shares error flag
    private val _sharesError = MutableStateFlow(false)
    val sharesError: StateFlow<Boolean> = _sharesError.asStateFlow()

    // 当前资产单价 Flow（只读）
    val currentPrice: StateFlow<Double?> = combine(assets, _selectedAssetId) { list, id ->
        list.firstOrNull { it.id == id }?.unitValue
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = null)

    private val _feeInput = MutableStateFlow("5")
    val feeInput: StateFlow<String> = _feeInput.asStateFlow()

    private val _feeError = MutableStateFlow(false)
    val feeError: StateFlow<Boolean> = _feeError.asStateFlow()

    val totalAmount: StateFlow<String> = combine(_sharesInput, currentPrice, _feeInput) { sharesStr, price, feeStr ->
        val shares = sharesStr.toDoubleOrNull()
        val fee = feeStr.toDoubleOrNull()
        if (shares == null || price == null || fee == null) return@combine "-"
        String.format("%.2f", shares * price + fee)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "-")

    private val _reasonInput = MutableStateFlow("")
    val reasonInput: StateFlow<String> = _reasonInput.asStateFlow()

    // price refreshing indicator
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    suspend fun refreshPriceAndAnalysis(): Boolean {
        val assetId = _selectedAssetId.value ?: return false
        val asset = assets.value.firstOrNull { it.id == assetId } ?: return false
        _isRefreshing.value = true
        val ok = updateMarketData.refreshAsset(asset)
        _isRefreshing.value = false
        return ok
    }

    /* ---------------- Preview Infos ---------------- */
    private val _previewInfos = MutableStateFlow<List<AssetInfo>>(emptyList())
    val previewInfos: StateFlow<List<AssetInfo>> = _previewInfos.asStateFlow()

    private val editingTxId: UUID? = savedStateHandle.get<String>(ARG_TRANSACTION_ID)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    private val fromOpportunityId: UUID? = savedStateHandle.get<String>(ARG_OPPORTUNITY_ID)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    private val initialAssetId: UUID? = savedStateHandle.get<String>(ARG_ASSET_ID)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    val isEditing: Boolean get() = editingTxId != null

    init {
        // monitor changes to recompute preview
        viewModelScope.launch {
            // combine first big streams into Triple
            combine(
                repository.portfolioFlow,
                repository.assetsFlow,
                repository.assetAnalysisFlow
            ) { portfolio, assets, analyses ->
                Triple(portfolio, assets, analyses)
            }.combine(
                combine(_selectedAssetId, _sharesInput, _type) { id, sharesStr, tradeType ->
                    Triple(id, sharesStr, tradeType)
                }
            ) { (portfolio, assets, analyses), (selectedId, sharesStr, tradeType) ->
                val asset = assets.firstOrNull { it.id == selectedId } ?: return@combine emptyList<AssetInfo>()

                val totalAssetsValue = portfolio.cash + assets.sumOf { it.currentMarketValue }

                val analysis = analyses.firstOrNull { it.assetId == asset.id }

                val oldInfo = buildAssetInfo(asset, totalAssetsValue, analysis, false)

                val deltaShares = sharesStr.toDoubleOrNull() ?: return@combine listOf(oldInfo)
                val newShares = when (tradeType) {
                    TradeType.BUY -> (asset.shares ?: 0.0) + deltaShares
                    TradeType.SELL -> ((asset.shares ?: 0.0) - deltaShares).coerceAtLeast(0.0)
                }

                val assetNew = asset.copy(shares = newShares)
                val newInfo = buildAssetInfo(assetNew, totalAssetsValue, analysis, false)

                listOf(oldInfo, newInfo)
            }.collect {
                _previewInfos.value = it
            }
        }

        editingTxId?.let { id ->
            viewModelScope.launch {
                repository.getTransactionById(id)?.let { tx ->
                    _type.value = tx.type
                    _assetIdInput.value = tx.assetId?.toString().orEmpty()
                    _sharesInput.value = tx.shares.toString()
                    _feeInput.value = tx.fee.toString()
                    _reasonInput.value = tx.reason.orEmpty()
                }
            }
        }

        fromOpportunityId?.let { opId ->
            viewModelScope.launch {
                // 简化：直接从仓库的流里取当前列表并匹配
                repository.tradingOpportunitiesFlow.firstOrNull()?.firstOrNull { it.id == opId }?.let { op ->
                    _type.value = op.type
                    _selectedAssetId.value = op.assetId
                    _assetIdInput.value = op.assetId?.toString().orEmpty()
                    _sharesInput.value = op.shares.toString()
                    _feeInput.value = op.fee.toString()
                    _reasonInput.value = op.reason
                }
            }
        }

        // If launched directly from an asset
        initialAssetId?.let { aid ->
            _selectedAssetId.value = aid
            _assetIdInput.value = aid.toString()
        }
    }

    /* -----------------------  Intents  ----------------------- */
    fun onTypeChange(value: TradeType) { _type.value = value; validateShares() }
    fun onAssetIdChange(value: String) { _assetIdInput.value = value }

    fun onAssetSelected(asset: Asset?) {
        _selectedAssetId.value = asset?.id
        _assetIdInput.value = asset?.id?.toString().orEmpty()
        validateShares()
    }

    fun onSharesChange(value: String) { _sharesInput.value = value; validateShares() }
    fun onFeeChange(value: String) { _feeInput.value = value; validateFee() }
    fun onReasonChange(value: String) { _reasonInput.value = value }

    private fun validateShares() {
        val shares = _sharesInput.value.toDoubleOrNull() ?: run { _sharesError.value = true; return }
        if (shares < 0) { _sharesError.value = true; return }
        if (_type.value == TradeType.SELL) {
            val assetId = _selectedAssetId.value
            val currentShares = assets.value.firstOrNull { it.id == assetId }?.shares ?: 0.0
            _sharesError.value = shares > currentShares
        } else {
            _sharesError.value = false
        }
    }

    private fun validateFee() {
        _feeError.value = _feeInput.value.toDoubleOrNull()?.let { it < 0 } ?: true
    }

    suspend fun save(): Boolean {
        val tx = buildTransaction() ?: return false
        if (editingTxId == null) {
            repository.addTransaction(tx)
            // 如果是从交易机会转换来的，删除对应的交易机会
            fromOpportunityId?.let { opId ->
                repository.deleteTradingOpportunity(opId)
            }
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
        val price = currentPrice.value ?: return null
        val fee = _feeInput.value.toDoubleOrNull() ?: 5.0
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
            time = LocalDateTime.now(),
            reason = _reasonInput.value.ifBlank { null }
        )
    }
}
