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

    // 当资产列表更新时，自动计算每个资产的市值，并创建一个 ID → 市值 的映射（遵循 A2B 命名）
    val assetId2Value: StateFlow<Map<UUID, Double>> = assets.map { assetList ->
        assetList.associate { asset ->
            asset.id to asset.currentMarketValue
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    // 刷新失败的资产ID列表
    private val _failedRefreshAssetIds = MutableStateFlow<Set<UUID>>(emptySet())
    val failedRefreshAssetIds: StateFlow<Set<UUID>> = _failedRefreshAssetIds.asStateFlow()

    /**
     * Asset level analytics calculated based on current market value and target weight.
     */
    data class AssetAnalysis(
        val asset: Asset,
        val marketValue: Double,
        val currentWeight: Double,          // 当前占比 (0-1)
        val deviationPct: Double,           // 与目标占比的偏离 (正:+ 负:-)
        val targetMarketValue: Double,      // 目标市值
        val deviationValue: Double,         // 与目标市值的偏离 (正:超出 负:不足)
        val isRefreshFailed: Boolean,       // 是否刷新失败
        // 来自AssetAnalysis表的数据
        val volatility: Double? = null,     // 波动率
        val sevenDayReturn: Double? = null, // 七日涨跌幅
        val buyFactor: Double? = null,      // 买入因子
        val sellThreshold: Double? = null   // 卖出阈值
    )

    /** AssetAnalysis 列表 Flow */
    val assetAnalyses: StateFlow<List<AssetAnalysis>> = combine(
        assets,
        portfolioState,
        failedRefreshAssetIds,
        repository.assetAnalysisFlow
    ) { assetList, portfolio, failedIds, analysisDataList ->
        val totalMarketValue = assetList.sumOf { it.currentMarketValue }
        val totalAssetsValue = totalMarketValue + portfolio.cash // 总资产 = 资产市值 + 可用现金
        
        // 创建分析数据的映射
        val analysisMap = analysisDataList.associateBy { it.assetId }
        
        assetList.map { asset ->
            val value = asset.currentMarketValue
            // 修正：资产当前占比 = 资产当前市值 / (所有资产总市值 + 可用现金)
            val weight = if (totalAssetsValue > 0) value / totalAssetsValue else 0.0
            val deviationPct = weight - asset.targetWeight
            // 修正：目标市值也应该基于总资产计算
            val targetValue = totalAssetsValue * asset.targetWeight
            val deviationValue = value - targetValue
            
            // 获取对应的分析数据
            val analysisData = analysisMap[asset.id]
            
            AssetAnalysis(
                asset = asset,
                marketValue = value,
                currentWeight = weight,
                deviationPct = deviationPct,
                targetMarketValue = targetValue,
                deviationValue = deviationValue,
                isRefreshFailed = failedIds.contains(asset.id),
                volatility = analysisData?.volatility,
                sevenDayReturn = analysisData?.sevenDayReturn,
                buyFactor = analysisData?.buyFactor,
                sellThreshold = analysisData?.sellThreshold
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

    /** 更新可用现金 */
    fun updateCash(newCash: Double) {
        viewModelScope.launch {
            repository.updateCash(newCash)
        }
    }

    /** 更新投资组合备注 */
    fun updateNote(note: String?) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    // 隐藏资产数目的状态
    private val _isAssetAmountHidden = MutableStateFlow(false)
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
        BUY_FACTOR("买入因子"),
        SELL_THRESHOLD("卖出阈值")
    }

    // 排序状态
    private val _sortOption = MutableStateFlow(SortOption.ORIGINAL)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _isAscending = MutableStateFlow(false)
    val isAscending: StateFlow<Boolean> = _isAscending.asStateFlow()

    /** 设置排序方案 */
    fun setSortOption(option: SortOption) {
        if (_sortOption.value == option) {
            // 如果选择的是当前排序方案，则切换升降序
            _isAscending.value = !_isAscending.value
        } else {
            // 如果选择的是新排序方案，则设置为降序（默认）
            _sortOption.value = option
            _isAscending.value = false
        }
    }

    /** 排序后的资产分析列表 */
    val sortedAssetAnalyses: StateFlow<List<AssetAnalysis>> = combine(
        assetAnalyses,
        sortOption,
        isAscending
    ) { analyses, sort, ascending ->
        when (sort) {
            SortOption.ORIGINAL -> analyses
            SortOption.CURRENT_WEIGHT -> if (ascending) analyses.sortedBy { it.currentWeight } else analyses.sortedByDescending { it.currentWeight }
            SortOption.TARGET_WEIGHT -> if (ascending) analyses.sortedBy { it.asset.targetWeight } else analyses.sortedByDescending { it.asset.targetWeight }
            SortOption.WEIGHT_DEVIATION -> if (ascending) analyses.sortedBy { it.deviationPct } else analyses.sortedByDescending { it.deviationPct }
            SortOption.WEIGHT_DEVIATION_ABS -> if (ascending) analyses.sortedBy { kotlin.math.abs(it.deviationPct) } else analyses.sortedByDescending { kotlin.math.abs(it.deviationPct) }
            SortOption.CURRENT_MARKET_VALUE -> if (ascending) analyses.sortedBy { it.marketValue } else analyses.sortedByDescending { it.marketValue }
            SortOption.TARGET_MARKET_VALUE -> if (ascending) analyses.sortedBy { it.targetMarketValue } else analyses.sortedByDescending { it.targetMarketValue }
            SortOption.MARKET_VALUE_DEVIATION -> if (ascending) analyses.sortedBy { it.deviationValue } else analyses.sortedByDescending { it.deviationValue }
            SortOption.MARKET_VALUE_DEVIATION_ABS -> if (ascending) analyses.sortedBy { kotlin.math.abs(it.deviationValue) } else analyses.sortedByDescending { kotlin.math.abs(it.deviationValue) }
            SortOption.UNIT_PRICE -> if (ascending) analyses.sortedBy { it.asset.unitValue ?: 0.0 } else analyses.sortedByDescending { it.asset.unitValue ?: 0.0 }
            SortOption.SHARES -> if (ascending) analyses.sortedBy { it.asset.shares ?: 0.0 } else analyses.sortedByDescending { it.asset.shares ?: 0.0 }
            SortOption.BUY_FACTOR -> if (ascending) analyses.sortedBy { it.buyFactor ?: 0.0 } else analyses.sortedByDescending { it.buyFactor ?: 0.0 }
            SortOption.SELL_THRESHOLD -> if (ascending) analyses.sortedBy { it.sellThreshold ?: 0.0 } else analyses.sortedByDescending { it.sellThreshold ?: 0.0 }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}
