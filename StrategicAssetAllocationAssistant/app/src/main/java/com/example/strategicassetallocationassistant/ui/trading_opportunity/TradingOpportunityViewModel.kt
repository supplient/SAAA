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
        }
    }

    fun checkBuy() {
        viewModelScope.launch {
            val portfolio = repository.getPortfolioOnce()
            val items = buyCalculator.calculate(portfolio)
            if (items.isNotEmpty()) repository.insertTradingOpportunities(items)
        }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clearTradingOpportunities() }
    }

    fun deleteOne(id: UUID) {
        viewModelScope.launch { repository.deleteTradingOpportunity(id) }
    }
}


