package com.example.strategicassetallocationassistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import com.example.strategicassetallocationassistant.ui.common.util.MoneyUtils
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch

/**
 * 投资组合ViewModel
 * 负责管理投资组合的数据状态和业务逻辑
 */
@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val repository: com.example.strategicassetallocationassistant.data.repository.PortfolioRepository
    , private val updateMarketData: com.example.strategicassetallocationassistant.domain.UpdateMarketDataUseCase
) : ViewModel() {

    /**
     * Observe portfolio from [PortfolioRepository] and convert it to a hot [StateFlow]
     * so that Compose UI can collect it safely without re-subscribing for every recomposition.
     */
    val portfolioState: StateFlow<Portfolio> =
        repository.portfolioFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Portfolio(emptyList(), 0.0, null)
        )

    // 从顶层 Portfolio Flow 中派生资产列表 Flow
    private val assets: StateFlow<List<Asset>> = portfolioState.map { it.assets }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 步骤8: 移除Double版本的assetId2Value，已由assetId2ValueDecimal替代

    // BigDecimal版本的资产ID到市值映射 (步骤5: UI模型双字段过渡)
    val assetId2ValueDecimal: StateFlow<Map<UUID, BigDecimal>> = assets.map { assetList ->
        assetList.associate { asset ->
            asset.id to asset.currentMarketValueDecimal
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    // 刷新失败的资产ID列表
    private val _failedRefreshAssetIds = MutableStateFlow<Set<UUID>>(emptySet())
    val failedRefreshAssetIds: StateFlow<Set<UUID>> = _failedRefreshAssetIds.asStateFlow()

    // AssetInfo 已迁移到 ui.common.model 包

    // 步骤8: 移除Double版本的assetAnalyses，已由assetAnalysesDecimal替代

    /** BigDecimal版本的AssetInfo列表Flow (步骤5: UI模型双字段过渡) */
    val assetAnalysesDecimal: StateFlow<List<com.example.strategicassetallocationassistant.ui.common.model.AssetInfo>> = combine(
        assets,
        portfolioState,
        failedRefreshAssetIds,
        repository.assetAnalysisFlow
    ) { assetList, portfolio, failedIds, analysisDataList ->
        val totalAssetsValueDecimal = portfolio.totalAssetsValueDecimal
        val analysisMap = analysisDataList.associateBy { it.assetId }
        assetList.map { asset ->
            com.example.strategicassetallocationassistant.ui.common.util.buildAssetInfoWithDecimal(
                asset = asset,
                totalAssetsValueDecimal = totalAssetsValueDecimal,
                analysis = analysisMap[asset.id],
                isRefreshFailed = failedIds.contains(asset.id)
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /** 手动刷新市场数据 */
    fun refreshMarketData() {
        viewModelScope.launch {
            val result = updateMarketData()
            _failedRefreshAssetIds.value = result.failedAssetIds.toSet()
        }
    }

    /** 仅刷新分析数据（买入因子、卖出阈值等），不更新资产市价数据 */
    fun refreshAnalysisData() {
        viewModelScope.launch {
            updateMarketData.updateAssetAnalyses()
        }
    }

    /** 更新可用现金 - 步骤6: 内部切换到BigDecimal版本 */
    fun updateCash(newCash: Double) {
        viewModelScope.launch {
            // 步骤6: 向后兼容的Double版本，内部转换为BigDecimal
            repository.updateCashDecimal(BigDecimal.valueOf(newCash))
        }
    }

    /** BigDecimal版本的更新可用现金 (步骤5: UI模型双字段过渡) */
    fun updateCashDecimal(newCashDecimal: BigDecimal) {
        viewModelScope.launch {
            // 步骤6: 切换到Repository的BigDecimal版本
            repository.updateCashDecimal(newCashDecimal)
        }
    }

    /** 更新投资组合备注 */
    fun updateNote(note: String?) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    // 隐藏资产数目的状态
    private val _isAssetAmountHidden = MutableStateFlow(true)
    val isAssetAmountHidden: StateFlow<Boolean> = _isAssetAmountHidden.asStateFlow()

    /** 切换资产数目隐藏状态 */
    fun toggleAssetAmountHidden() {
        _isAssetAmountHidden.value = !_isAssetAmountHidden.value
    }

    /**
     * 排序方案枚举
     */
    enum class SortOption(val displayName: String) {
        ORIGINAL("原排序"),
        CURRENT_WEIGHT("当前占比"),
        TARGET_WEIGHT("目标占比"),
        WEIGHT_DEVIATION("占比偏差"),
        WEIGHT_DEVIATION_ABS("占比偏差绝对值"),
        CURRENT_MARKET_VALUE("当前市值"),
        TARGET_MARKET_VALUE("目标市值"),
        MARKET_VALUE_DEVIATION("市值偏差"),
        MARKET_VALUE_DEVIATION_ABS("市值偏差绝对值"),
        UNIT_PRICE("单价"),
        SHARES("份额"),
        SEVEN_DAY_RETURN("七日涨跌幅"),
        VOLATILITY("波动率"),
        BUY_FACTOR("买入因子"),
        SELL_THRESHOLD("卖出阈值"),
        RELATIVE_OFFSET("相对偏移"),
        OFFSET_FACTOR("偏移因子"),
        DRAWDOWN_FACTOR("跌幅因子"),
        PRE_VOLATILITY_BUY_FACTOR("去波动买入因子"),
        ASSET_RISK("资产风险")
    }

    // 排序状态
    private val _sortOption = MutableStateFlow(SortOption.ORIGINAL)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _isAscending = MutableStateFlow(false)
    val isAscending: StateFlow<Boolean> = _isAscending.asStateFlow()

    private val _currentSortColumnTitle = MutableStateFlow<String?>(null)
    val currentSortColumnTitle: StateFlow<String?> = _currentSortColumnTitle.asStateFlow()

    /** 设置排序方案 */
    fun setSortOption(option: SortOption) {
        if (_sortOption.value == option && option != SortOption.ORIGINAL) {
            // 如果选择的是当前排序方案，则切换升降序
            _isAscending.value = !_isAscending.value
        } else {
            // 如果选择的是新排序方案，则设置为降序（默认）
            _sortOption.value = option
            _isAscending.value = false
        }
        if (option == SortOption.ORIGINAL) {
            _currentSortColumnTitle.value = null
        }
    }

    fun setCurrentSortColumnTitle(title: String) {
        _currentSortColumnTitle.value = title
    }

    // 步骤8: 移除Double版本的sortedAssetAnalyses，已由sortedAssetAnalysesDecimal替代

    /** BigDecimal版本的排序后资产分析列表 (步骤5: UI模型双字段过渡) */
    val sortedAssetAnalysesDecimal: StateFlow<List<com.example.strategicassetallocationassistant.ui.common.model.AssetInfo>> = combine(
        assetAnalysesDecimal,
        sortOption,
        isAscending
    ) { analyses, sort, ascending ->
        when (sort) {
            SortOption.ORIGINAL -> analyses
            SortOption.CURRENT_WEIGHT -> if (ascending) analyses.sortedBy { it.getCurrentWeightValue() } else analyses.sortedByDescending { it.getCurrentWeightValue() }
            SortOption.TARGET_WEIGHT -> if (ascending) analyses.sortedBy { it.asset.targetWeight } else analyses.sortedByDescending { it.asset.targetWeight }
            SortOption.WEIGHT_DEVIATION -> if (ascending) analyses.sortedBy { it.getDeviationPctValue() } else analyses.sortedByDescending { it.getDeviationPctValue() }
            SortOption.WEIGHT_DEVIATION_ABS -> if (ascending) analyses.sortedBy { it.getDeviationPctValue().abs() } else analyses.sortedByDescending { it.getDeviationPctValue().abs() }
            SortOption.CURRENT_MARKET_VALUE -> if (ascending) analyses.sortedBy { it.getMarketValueValue() } else analyses.sortedByDescending { it.getMarketValueValue() }
            SortOption.TARGET_MARKET_VALUE -> if (ascending) analyses.sortedBy { it.getTargetMarketValueValue() } else analyses.sortedByDescending { it.getTargetMarketValueValue() }
            SortOption.MARKET_VALUE_DEVIATION -> if (ascending) analyses.sortedBy { it.getDeviationValueValue() } else analyses.sortedByDescending { it.getDeviationValueValue() }
            SortOption.MARKET_VALUE_DEVIATION_ABS -> if (ascending) analyses.sortedBy { it.getDeviationValueValue().abs() } else analyses.sortedByDescending { it.getDeviationValueValue().abs() }
            SortOption.UNIT_PRICE -> if (ascending) analyses.sortedBy { it.asset.getUnitValueValue() ?: BigDecimal.ZERO } else analyses.sortedByDescending { it.asset.getUnitValueValue() ?: BigDecimal.ZERO }
            SortOption.SHARES -> if (ascending) analyses.sortedBy { it.asset.getSharesValue() ?: BigDecimal.ZERO } else analyses.sortedByDescending { it.asset.getSharesValue() ?: BigDecimal.ZERO }
            SortOption.SEVEN_DAY_RETURN -> if (ascending) analyses.sortedBy { it.sevenDayReturn ?: Double.NEGATIVE_INFINITY } else analyses.sortedByDescending { it.sevenDayReturn ?: Double.NEGATIVE_INFINITY }
            SortOption.VOLATILITY -> if (ascending) analyses.sortedBy { it.volatility ?: 0.0 } else analyses.sortedByDescending { it.volatility ?: 0.0 }
            SortOption.BUY_FACTOR -> if (ascending) analyses.sortedBy { it.buyFactor ?: 0.0 } else analyses.sortedByDescending { it.buyFactor ?: 0.0 }
            SortOption.SELL_THRESHOLD -> if (ascending) analyses.sortedBy { it.sellThreshold ?: 0.0 } else analyses.sortedByDescending { it.sellThreshold ?: 0.0 }
            SortOption.RELATIVE_OFFSET -> if (ascending) analyses.sortedBy { it.relativeOffset ?: Double.NEGATIVE_INFINITY } else analyses.sortedByDescending { it.relativeOffset ?: Double.NEGATIVE_INFINITY }
            SortOption.OFFSET_FACTOR -> if (ascending) analyses.sortedBy { it.offsetFactor ?: 0.0 } else analyses.sortedByDescending { it.offsetFactor ?: 0.0 }
            SortOption.DRAWDOWN_FACTOR -> if (ascending) analyses.sortedBy { it.drawdownFactor ?: 0.0 } else analyses.sortedByDescending { it.drawdownFactor ?: 0.0 }
            SortOption.PRE_VOLATILITY_BUY_FACTOR -> if (ascending) analyses.sortedBy { it.preVolatilityBuyFactor ?: 0.0 } else analyses.sortedByDescending { it.preVolatilityBuyFactor ?: 0.0 }
            SortOption.ASSET_RISK -> if (ascending) analyses.sortedBy { it.assetRisk ?: 0.0 } else analyses.sortedByDescending { it.assetRisk ?: 0.0 }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}
