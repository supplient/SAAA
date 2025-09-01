package com.example.strategicassetallocationassistant.domain

import com.example.strategicassetallocationassistant.Asset
import java.util.UUID
import kotlin.math.max

/**
 * 根据给定公式计算每个资产的卖出阈值。
 *
 * 步骤：
 * 1. 计算各资产超配量 a_i = max(0, currentWeight - targetWeight)
 * 2. 计算总体风险 f = Σ k_i * a_i  (仅对超配资产)
 * 3. 风险因子 F = f / (f + fTilde)
 * 4. 对每个超配资产：
 *      S = baseThreshold
 *      S ← (1 - k_i) * S        // 高波动率 -> 更低阈值
 *      S ← (1 - F) * S          // 总体风险越高 -> 更低阈值
 * 未超配资产阈值返回 0.0
 */
class SellThresholdCalculator(
    private val baseThreshold: Double = 0.30,          // S 基础阈值（比例）
    private val halfSaturationTotalRisk: Double = 0.00035 // f~ 半饱和总体风险
) {
    var lastRiskFactor: Double = 0.0

    /**
     * 计算所有资产的卖出阈值。
     * @param assets 资产列表
     * @param cash   可用现金，用于计算当前占比
     * @return 映射 AssetId → 阈值 (0-1)，未超配返回 0.0
     */
    fun calculate(assets: List<Asset>, cash: Double): List<Double> {
        val totalValue = assets.sumOf { it.currentMarketValue } + cash
        if (totalValue <= 0) return List(assets.size) { 0.0 }

        // 1. a_i & current weight
        data class OverAsset(val asset: Asset, val a: Double)
        val overAssets = assets.map { asset ->
            val currentWeight = asset.currentMarketValue / totalValue
            val a = max(0.0, currentWeight - asset.targetWeight)
            OverAsset(asset, a)
        }.filter { it.a > 0 }

        // 2. f
        val f = overAssets.sumOf { (it.asset.volatility ?: 0.0) * it.a }

        // 3. F
        val F = f / (f + halfSaturationTotalRisk)
        lastRiskFactor = F

        // 4. thresholds
        val resultList = mutableListOf<Double>()
        assets.forEach { asset ->
            val vol = asset.volatility ?: 0.0
			var S = baseThreshold
			S *= (1 - vol.coerceIn(0.0, 1.0))
			S *= (1 - F.coerceIn(0.0, 1.0))
			resultList.add(S)
        }
        return resultList
    }
}
