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

/**
 * 投资组合ViewModel
 * 负责管理投资组合的数据状态和业务逻辑
 */
class PortfolioViewModel : ViewModel() {

    private val _portfolio = MutableStateFlow(createSamplePortfolio())
    val portfolioState: StateFlow<Portfolio> = _portfolio.asStateFlow()

    // 从顶层Portfolio StateFlow中派生出资产列表的Flow
    private val assets: StateFlow<List<Asset>> = portfolioState.map { it.assets }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 当资产列表更新时，自动计算每个资产的市值，并创建一个ID到市值的映射
    val assetId2Value: StateFlow<Map<UUID, Double>> = assets.map { assetList ->
        assetList.associate { asset ->
            asset.id to asset.currentMarketValue
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    // 创建样本投资组合数据
    private fun createSamplePortfolio(): Portfolio {
        val now = LocalDateTime.now()

        val sampleAssets = listOf(
            // 股票组合（空持仓）
            Asset(
                id = UUID.randomUUID(),
                name = "股票组合",
                type = AssetType.STOCK,
                targetWeight = 0.30
            ),
            // 股票1
            Asset(
                id = UUID.randomUUID(),
                name = "腾讯控股",
                type = AssetType.STOCK,
                targetWeight = 0.15,
                code = "00700",
                shares = 200.0,
                unitValue = 380.0,
                lastUpdateTime = now.minusHours(1)
            ),
            // 股票2
            Asset(
                id = UUID.randomUUID(),
                name = "阿里巴巴",
                type = AssetType.STOCK,
                targetWeight = 0.15,
                code = "09988",
                shares = 300.0,
                unitValue = 85.0,
                lastUpdateTime = now.minusHours(1)
            ),
            // 场外基金
            Asset(
                id = UUID.randomUUID(),
                name = "易方达蓝筹精选混合",
                type = AssetType.OFFSHORE_FUND,
                targetWeight = 0.30,
                code = "005827",
                shares = 1000.0,
                unitValue = 2.15,
                lastUpdateTime = now.minusDays(1)
            ),
            // 货币基金
            Asset(
                id = UUID.randomUUID(),
                name = "余额宝货币基金",
                type = AssetType.MONEY_FUND,
                targetWeight = 0.10,
                code = "000198",
                shares = 50000.0,
                unitValue = 1.0,
                lastUpdateTime = now.minusDays(1)
            )
        )

        return Portfolio(
            assets = sampleAssets,
            cash = 10000.0 // 顶层现金
        )
    }
}
