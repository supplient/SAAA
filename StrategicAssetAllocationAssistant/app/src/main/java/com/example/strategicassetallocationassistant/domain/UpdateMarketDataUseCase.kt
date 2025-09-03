package com.example.strategicassetallocationassistant.domain

import com.example.strategicassetallocationassistant.data.network.AShare
import com.example.strategicassetallocationassistant.data.repository.PortfolioRepository
import com.example.strategicassetallocationassistant.data.preferences.PreferencesRepository
import com.example.strategicassetallocationassistant.domain.BuyFactorCalculator
import com.example.strategicassetallocationassistant.domain.SellThresholdCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

data class MarketDataUpdateStats(
    val success: Int, 
    val fail: Int,
    val failedAssetIds: List<UUID> = emptyList()
)

/**
 * UseCase：遍历数据库资产并更新市场数据。
 * 直接使用 AShare 获取实时市场数据。
 * 不刷新货币基金类型的资产。
 */
class UpdateMarketDataUseCase @Inject constructor(
    private val repository: PortfolioRepository,
    private val prefs: PreferencesRepository
) {
    suspend operator fun invoke(): MarketDataUpdateStats = withContext(Dispatchers.IO) {
        // 更新资产信息：市价、波动率、七日收益率
        val updateStats = updateAssets()

        // 更新资产分析数据：买入因子、卖出阈值以及总体风险因子
        updateAssetAnalyses()

        updateStats
    }

    /**
     * 遍历资产并更新 Asset 信息与 AssetAnalysis 的波动率/七日收益率/买入因子。
     * 返回更新统计结果。
     */
    suspend fun updateAssets(): MarketDataUpdateStats {
        var success = 0
        var fail = 0
        val failedAssetIds = mutableListOf<UUID>()

        // 获取当前资产列表
        val portfolio = repository.getPortfolioOnce()

        // 遍历资产并更新资产信息：市价、波动率、七日收益率
        portfolio.assets.forEach { asset ->
            // 如果没有代码，计入失败
            val code = asset.code ?: run {
                fail++
                failedAssetIds.add(asset.id)
                return@forEach
            }

            // 获取资产市场数据
            val stats = runCatching { AShare.getMarketStats(code) }.getOrNull()

            // 如果市场数据获取成功，更新资产信息
            if (stats != null && stats.latestClose > 0.0) {
                // 1. 更新资产基本信息（单价和更新时间）
                val updatedAsset = asset.copy(
                    unitValue = stats.latestClose,
                    lastUpdateTime = LocalDateTime.now()
                )
                repository.updateAsset(updatedAsset)

                // 2. 更新资产分析数据（波动率和七日收益率）
                repository.updateAssetMarketData(
                    assetId = asset.id,
                    volatility = stats.annualVolatility,
                    sevenDayReturn = stats.sevenDayReturn
                )

                success++
            } else {
                fail++
                failedAssetIds.add(asset.id)
            }
        }

        return MarketDataUpdateStats(success, fail, failedAssetIds)
    }

    /**
     * 根据最新的资产与分析数据，统一计算并更新卖出阈值以及总体风险因子。
     */
    suspend fun updateAssetAnalyses() {
        // 获取参数
        val rTilde = prefs.halfSaturationR.first()
        val dTilde = prefs.halfSaturationD.first()
        val alpha = prefs.alpha.first()

        val baseSell = prefs.baseSellThreshold.first()
        val halfRisk = prefs.halfTotalRisk.first()

        val buyCalc = BuyFactorCalculator(rTilde, dTilde, alpha)
        val sellCalc = SellThresholdCalculator(baseSell, halfRisk)

        // 重新获取最新 Portfolio（已包含更新后的 unitValue）
        val portfolio = repository.getPortfolioOnce()

        // 获取所有资产分析数据
        val allAnalyses = repository.assetAnalysisFlow.first()
        val volatilityMap = allAnalyses.associate { it.assetId to it.volatility }
        val sevenDayReturnMap = allAnalyses.associate { it.assetId to it.sevenDayReturn }

        // 1. 计算并更新买入因子及日志
        val totalAssetsValue = portfolio.assets.sumOf { it.currentMarketValue } + portfolio.cash
        portfolio.assets.forEach { asset ->
            val vol = volatilityMap[asset.id]
            val sevenDayRet = sevenDayReturnMap[asset.id]
            val factors = buyCalc.calculate(asset, totalAssetsValue, vol, sevenDayRet)
            repository.updateAssetBuyFactorWithLog(asset.id, factors.buyFactor, factors.calculationLog)
        }

        // 2. 计算卖出阈值及风险因子
        val sellThresholdResult = sellCalc.calculateWithLogs(
            portfolio.assets,
            portfolio.cash,
            volatilityMap
        )

        // 更新资产卖出阈值及其日志
        sellThresholdResult.thresholds.forEachIndexed { idx, threshold ->
            val thresholdLog = sellThresholdResult.thresholdLogs[idx]
            repository.updateAssetSellThresholdWithLog(
                portfolio.assets[idx].id,
                threshold,
                thresholdLog
            )
        }

        // 更新总体风险因子及日志
        repository.updateOverallRiskFactorWithLog(
            sellThresholdResult.overallRiskFactor,
            sellThresholdResult.overallRiskFactorLog
        )
    }
}
