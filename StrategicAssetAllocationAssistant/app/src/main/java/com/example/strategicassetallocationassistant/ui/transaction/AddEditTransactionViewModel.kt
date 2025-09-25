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
import com.example.strategicassetallocationassistant.ui.common.util.buildAssetInfoWithDecimal
import com.example.strategicassetallocationassistant.ui.common.util.MoneyUtils
import com.example.strategicassetallocationassistant.ui.common.util.toBigDecimalMoney
import com.example.strategicassetallocationassistant.ui.common.util.toBigDecimalShare
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.math.BigDecimal

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

    // 用户输入的单价
    private val _priceInput = MutableStateFlow("")
    val priceInput: StateFlow<String> = _priceInput.asStateFlow()

    // 单价输入错误状态
    private val _priceError = MutableStateFlow(false)
    val priceError: StateFlow<Boolean> = _priceError.asStateFlow()

    // 步骤8: 移除Double版本的currentPrice，已由currentPriceDecimal替代

    // BigDecimal版本的当前资产单价 (步骤5: UI模型双字段过渡)
    val currentPriceDecimal: StateFlow<BigDecimal?> = combine(assets, _selectedAssetId) { list, id ->
        list.firstOrNull { it.id == id }?.getUnitValueValue()
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = null)

    // 用户输入的价格（用于计算，优先使用用户输入，否则使用资产当前价格）
    val userPriceDecimal: StateFlow<BigDecimal?> = combine(_priceInput, currentPriceDecimal) { inputStr, assetPrice ->
        inputStr.toBigDecimalMoney() ?: assetPrice
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = null)

    private val _feeInput = MutableStateFlow("5")
    val feeInput: StateFlow<String> = _feeInput.asStateFlow()

    private val _feeError = MutableStateFlow(false)
    val feeError: StateFlow<Boolean> = _feeError.asStateFlow()

    // 步骤8: 移除Double版本的totalAmount，已由totalAmountDecimal替代

    // BigDecimal版本的总金额计算 (步骤5: UI模型双字段过渡)
    val totalAmountDecimal: StateFlow<String> = combine(_sharesInput, userPriceDecimal, _feeInput) { sharesStr, priceDecimal, feeStr ->
        val sharesDecimal = sharesStr.toBigDecimalShare()
        val feeDecimal = feeStr.toBigDecimalMoney()
        if (sharesDecimal == null || priceDecimal == null || feeDecimal == null) return@combine "-"
        val total = MoneyUtils.safeMultiply(sharesDecimal, priceDecimal).add(feeDecimal)
        MoneyUtils.formatMoney(total)
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
        if (ok) {
            // 等待资产数据更新后获取新价格，避免竞态条件
            val updatedAsset = assets.first().firstOrNull { it.id == assetId }
            updatedAsset?.getUnitValueValue()?.let { newPrice ->
                _priceInput.value = MoneyUtils.formatMoneyPlain(newPrice)
            }
        }
        _isRefreshing.value = false
        return ok
    }

    /* ---------------- Preview Infos ---------------- */
    // 步骤8: 移除Double版本的previewInfos，已由previewInfosDecimal替代

    // BigDecimal版本的预览信息 (步骤5: UI模型双字段过渡)
    private val _previewInfosDecimal = MutableStateFlow<List<AssetInfo>>(emptyList())
    val previewInfosDecimal: StateFlow<List<AssetInfo>> = _previewInfosDecimal.asStateFlow()

    private val editingTxId: UUID? = savedStateHandle.get<String>(ARG_TRANSACTION_ID)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    private val fromOpportunityId: UUID? = savedStateHandle.get<String>(ARG_OPPORTUNITY_ID)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    private val initialAssetId: UUID? = savedStateHandle.get<String>(ARG_ASSET_ID)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    val isEditing: Boolean get() = editingTxId != null

    init {
        // 当选择资产或资产价格变化时，自动填充价格输入框（仅在新增模式且价格输入框为空时）
        viewModelScope.launch {
            currentPriceDecimal.collect { price ->
                if (price != null && !isEditing && _priceInput.value.isBlank()) {
                    _priceInput.value = MoneyUtils.formatMoneyPlain(price)
                }
            }
        }

        // 步骤8: 移除Double版本的预览计算逻辑，保留BigDecimal版本

        // BigDecimal版本的预览信息计算 (步骤5: UI模型双字段过渡)
        viewModelScope.launch {
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

                val totalAssetsValueDecimal = portfolio.totalAssetsValueDecimal
                val analysis = analyses.firstOrNull { it.assetId == asset.id }

                val oldInfo = buildAssetInfoWithDecimal(asset, totalAssetsValueDecimal, analysis, false)

                val deltaSharesDecimal = sharesStr.toBigDecimalShare() ?: return@combine listOf(oldInfo)
                val currentSharesDecimal = asset.getSharesValue() ?: BigDecimal.ZERO
                val newSharesDecimal = when (tradeType) {
                    TradeType.BUY -> currentSharesDecimal.add(deltaSharesDecimal)
                    TradeType.SELL -> {
                        val result = currentSharesDecimal.subtract(deltaSharesDecimal)
                        if (result >= BigDecimal.ZERO) result else BigDecimal.ZERO
                    }
                }

                val assetNew = asset.copy(
                    shares = newSharesDecimal.toDouble(),
                    sharesDecimal = newSharesDecimal
                )
                val newInfo = buildAssetInfoWithDecimal(assetNew, totalAssetsValueDecimal, analysis, false)

                listOf(oldInfo, newInfo)
            }.collect {
                _previewInfosDecimal.value = it
            }
        }

        editingTxId?.let { id ->
            viewModelScope.launch {
                repository.getTransactionById(id)?.let { tx ->
                    _type.value = tx.type
                    _selectedAssetId.value = tx.assetId
                    _sharesInput.value = tx.shares.toString()
                    _priceInput.value = tx.priceDecimal?.let { MoneyUtils.formatMoneyPlain(it) } ?: tx.price.toString()
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
                    _sharesInput.value = op.shares.toString()
                    // 对于机会，使用当前资产价格而不是机会中的价格（机会可能过期）
                    // 价格会在选择资产后自动填充
                    _feeInput.value = op.fee.toString()
                    _reasonInput.value = op.reason
                }
            }
        }

        // If launched directly from an asset
        initialAssetId?.let { aid ->
            _selectedAssetId.value = aid
        }
    }

    /* -----------------------  Intents  ----------------------- */
    fun onTypeChange(value: TradeType) { _type.value = value; validateShares() }

    fun onAssetSelected(asset: Asset?) {
        _selectedAssetId.value = asset?.id
        validateShares()
    }

    fun onSharesChange(value: String) { _sharesInput.value = value; validateShares() }

    fun onPriceChange(value: String) { _priceInput.value = value; validatePrice() }

    /** 增加100股 */
    fun incrementShares() {
        val cur = _sharesInput.value.toDoubleOrNull() ?: 0.0
        _sharesInput.value = String.format("%.0f", cur + 100)
        validateShares()
    }

    /** 减少100股（不低于0） */
    fun decrementShares() {
        val cur = _sharesInput.value.toDoubleOrNull() ?: 0.0
        val newVal = (cur - 100).coerceAtLeast(0.0)
        _sharesInput.value = String.format("%.0f", newVal)
        validateShares()
    }

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

    private fun validatePrice() {
        val price = _priceInput.value.toBigDecimalMoney()
        _priceError.value = price?.let { it <= BigDecimal.ZERO } ?: true
    }

    // 保存按钮可用状态
    private val _basicValid: StateFlow<Triple<Boolean, Boolean, Boolean>> = combine(sharesError, feeError, priceError) { sErr, fErr, pErr ->
        Triple(!sErr, !fErr, !pErr)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(false, false, false))

    val canSave: StateFlow<Boolean> = combine(
        _basicValid,
        _selectedAssetId,
        userPriceDecimal, // 使用用户输入的价格
        _sharesInput,
        _feeInput
    ) { validTriple, aid, price, sharesStr, feeStr ->
        val (shareOk, feeOk, priceOk) = validTriple
        shareOk && feeOk && priceOk && aid != null && price != null &&
                sharesStr.isNotBlank() && feeStr.isNotBlank() && _priceInput.value.isNotBlank()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // 步骤8: 移除Double版本的save()方法，已由saveWithDecimal()替代

    suspend fun delete() {
        editingTxId?.let { id ->
            repository.getTransactionById(id)?.let { repository.deleteTransaction(it) }
        }
    }

    /* ---------------------  Helpers  --------------------- */

    // 步骤8: 移除Double版本的buildTransaction()方法，已由buildTransactionDecimal()替代

    /** BigDecimal版本的Transaction构建 (步骤5: UI模型双字段过渡) */
    private fun buildTransactionDecimal(): Transaction? {
        val sharesDecimal = _sharesInput.value.toBigDecimalShare() ?: return null
        val priceDecimal = userPriceDecimal.value ?: return null  // 使用用户输入的价格
        val feeDecimal = _feeInput.value.toBigDecimalMoney() ?: MoneyUtils.createMoney("5")
        val assetUuid = _selectedAssetId.value ?: return null

        val amountDecimal = MoneyUtils.safeMultiply(sharesDecimal, priceDecimal).add(feeDecimal)
        return Transaction(
            id = editingTxId ?: UUID.randomUUID(),
            assetId = assetUuid,
            type = _type.value,
            // Double字段 (向后兼容)
            shares = sharesDecimal.toDouble(),
            price = priceDecimal.toDouble(),
            fee = feeDecimal.toDouble(),
            amount = amountDecimal.toDouble(),
            // BigDecimal字段 (精确版本)
            sharesDecimal = sharesDecimal,
            priceDecimal = priceDecimal,
            feeDecimal = feeDecimal,
            amountDecimal = amountDecimal,
            time = LocalDateTime.now(),
            reason = _reasonInput.value.ifBlank { null }
        )
    }

    /** 切换到BigDecimal版本的保存方法 */
    suspend fun saveWithDecimal(): Boolean {
        val tx = buildTransactionDecimal() ?: return false
        if (editingTxId == null) {
            repository.addTransaction(tx)
            fromOpportunityId?.let { opId ->
                repository.deleteTradingOpportunity(opId)
            }
        } else {
            repository.updateTransaction(tx)
        }
        return true
    }
}
