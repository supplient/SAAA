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
    suspend operator fun invoke(): MarketDataUpdateStats {
        return withContext(Dispatchers.IO) {
            // 获取当前资产列表及偏好参数
            val portfolio = repository.getPortfolioOnce()

            val rTilde = prefs.halfSaturationR.first()
            val dTilde = prefs.halfSaturationD.first()
            val alpha = prefs.alpha.first()

            val calculator = BuyFactorCalculator(rTilde, dTilde, alpha)

            val baseSell = prefs.baseSellThreshold.first()
            val halfRisk = prefs.halfTotalRisk.first()
            val sellCalc = SellThresholdCalculator(baseSell, halfRisk)

            var success = 0
            var fail = 0
            val failedAssetIds = mutableListOf<UUID>()

            portfolio.assets.forEach { asset ->
                
                // 如果没有代码，计入失败
                val code = asset.code ?: run { 
                    fail++
                    failedAssetIds.add(asset.id)
                    return@forEach 
                }

                val stats = runCatching { AShare.getMarketStats(code) }.getOrNull()

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

                    // 3. 计算并更新买入因子和计算过程日志
                    val totalAssetsValue = portfolio.assets.sumOf { it.currentMarketValue } + portfolio.cash
                    val factors = calculator.calculate(updatedAsset, totalAssetsValue, stats.annualVolatility, stats.sevenDayReturn)
                    
                    repository.updateAssetBuyFactorWithLog(asset.id, factors.buyFactor, factors.calculationLog)
                    success++
                } else {
                    fail++
                    failedAssetIds.add(asset.id)
                }
            }

            // 计算并写入卖出阈值一次性（包含计算过程日志）
            // 首先获取所有资产的波动率数据
            val allAnalyses = repository.assetAnalysisFlow.first()
            val volatilityMap = allAnalyses.associate { it.assetId to it.volatility }
            val sellThresholdResult = sellCalc.calculateWithLogs(portfolio.assets, portfolio.cash, volatilityMap)
            
            // 更新每个资产的卖出阈值和计算过程日志
            sellThresholdResult.thresholds.forEachIndexed { idx, threshold ->
                val thresholdLog = sellThresholdResult.thresholdLogs[idx]
                repository.updateAssetSellThresholdWithLog(portfolio.assets[idx].id, threshold, thresholdLog)
            }

            // 更新总体风险因子和计算过程日志
            repository.updateOverallRiskFactorWithLog(sellThresholdResult.overallRiskFactor, sellThresholdResult.overallRiskFactorLog)

            MarketDataUpdateStats(
                success = success, 
                fail = fail,
                failedAssetIds = failedAssetIds
            )
        }
    }
}
