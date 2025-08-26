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

    fun checkSell() {
        viewModelScope.launch {
            val portfolio = repository.getPortfolioOnce()
            val items = sellCalculator.calculate(portfolio)
            if (items.isNotEmpty()) repository.insertTradingOpportunities(items)
            _reasoningLog.value = sellCalculator.lastLog
        }
    }

    fun checkBuy() {
        viewModelScope.launch {
            val portfolio = repository.getPortfolioOnce()
            val items = buyCalculator.calculate(portfolio)
            if (items.isNotEmpty()) repository.insertTradingOpportunities(items)
            _reasoningLog.value = buyCalculator.lastLog
        }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clearTradingOpportunities() }
    }

    fun deleteOne(id: UUID) {
        viewModelScope.launch { repository.deleteTradingOpportunity(id) }
    }

    // 在展示完毕后调用以清除日志
    fun clearReasoningLog() { _reasoningLog.value = null }
}


