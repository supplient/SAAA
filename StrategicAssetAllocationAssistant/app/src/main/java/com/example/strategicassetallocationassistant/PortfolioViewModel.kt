package com.example.strategicassetallocationassistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
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
            initialValue = Portfolio(emptyList(), 0.0)
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

    /** 手动刷新市场数据 */
    fun refreshMarketData() {
        viewModelScope.launch {
            updateMarketData()
        }
    }
}
