package com.example.strategicassetallocationassistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.strategicassetallocationassistant.data.repository.PortfolioRepository
import com.example.strategicassetallocationassistant.domain.BuyOpportunityCalculator
import com.example.strategicassetallocationassistant.domain.SellOpportunityCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TradingOpportunityViewModel @Inject constructor(
    private val repository: PortfolioRepository,
    private val sellCalculator: SellOpportunityCalculator,
    private val buyCalculator: BuyOpportunityCalculator
) : ViewModel() {

    // 用于在 UI 中展示算法思考过程
    private val _reasoningLog = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val reasoningLog: kotlinx.coroutines.flow.StateFlow<String?> = _reasoningLog

    val opportunities: StateFlow<List<TradingOpportunity>> =
        repository.tradingOpportunitiesFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 获取资产信息，用于显示资产名称
    val portfolio: StateFlow<Portfolio> =
        repository.portfolioFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Portfolio(emptyList(), 0.0)
        )

    // 组合交易机会和资产信息，用于UI显示
    data class TradingOpportunityWithAsset(
        val opportunity: TradingOpportunity,
        val assetName: String?
    )

    val opportunitiesWithAssets: StateFlow<List<TradingOpportunityWithAsset>> = combine(
        opportunities,
        portfolio
    ) { opps, portfolio ->
        opps.map { opp ->
            val asset = opp.assetId?.let { assetId ->
                portfolio.assets.find { it.id == assetId }
            }
            TradingOpportunityWithAsset(
                opportunity = opp,
                assetName = asset?.name
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /** 步骤6: 切换到BigDecimal版本的卖出机会检查 */
    fun checkSell() {
        viewModelScope.launch {
            val portfolio = repository.getPortfolioOnce()
            val items = sellCalculator.calculateWithDecimal(portfolio)
            if (items.isNotEmpty()) repository.insertTradingOpportunities(items)
            _reasoningLog.value = sellCalculator.lastLog
        }
    }

    /** 步骤6: 切换到BigDecimal版本的买入机会检查 */
    fun checkBuy() {
        viewModelScope.launch {
            val portfolio = repository.getPortfolioOnce()
            val items = buyCalculator.calculateWithDecimal(portfolio)
            if (items.isNotEmpty()) repository.insertTradingOpportunities(items)
            _reasoningLog.value = buyCalculator.lastLog
        }
    }

    // 步骤8: 移除Double版本的方法，已完全切换到BigDecimal版本

    fun clearAll() {
        viewModelScope.launch { repository.clearTradingOpportunities() }
    }

    fun deleteOne(id: UUID) {
        viewModelScope.launch { repository.deleteTradingOpportunity(id) }
    }

    // 在展示完毕后调用以清除日志
    fun clearReasoningLog() { _reasoningLog.value = null }
}


