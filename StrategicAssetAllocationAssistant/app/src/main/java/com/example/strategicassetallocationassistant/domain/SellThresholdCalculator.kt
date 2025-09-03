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
    var lastOverallRiskFactorLog: String = ""

    data class SellThresholdResult(
        val thresholds: List<Double>,                    // 每个资产的卖出阈值
        val thresholdLogs: List<String>,                // 每个资产的卖出阈值计算过程日志
        val assetRisks: List<Double>,                    // 每个资产的风险 k_i * a_i
        val overallRiskFactor: Double,                  // 总体风险因子
        val overallRiskFactorLog: String                // 总体风险因子计算过程日志
    )

    /**
     * 计算所有资产的卖出阈值（新版本，包含计算过程日志）
     * @param assets 资产列表
     * @param cash   可用现金，用于计算当前占比
     * @param volatilityMap 资产ID到波动率的映射
     * @return SellThresholdResult 包含阈值、日志和总体风险因子信息
     */
    fun calculateWithLogs(assets: List<Asset>, cash: Double, volatilityMap: Map<UUID, Double?>): SellThresholdResult {
        val totalValue = assets.sumOf { it.currentMarketValue } + cash
        if (totalValue <= 0) {
            return SellThresholdResult(
                thresholds = List(assets.size) { 0.0 },
                thresholdLogs = List(assets.size) { "无效输入: totalValue=$totalValue" },
                assetRisks = List(assets.size) { 0.0 },
                overallRiskFactor = 0.0,
                overallRiskFactorLog = "无效输入: totalValue=$totalValue"
            )
        }

        // 1. 计算各资产的超配量和风险
        data class AssetRiskInfo(val asset: Asset, val a: Double, val volatility: Double, val risk: Double)
        val assetRiskInfos = assets.map { asset ->
            val currentWeight = asset.currentMarketValue / totalValue
            val a = max(0.0, currentWeight - asset.targetWeight)
            val volatility = volatilityMap[asset.id] ?: 0.0
            val risk = volatility * a
            AssetRiskInfo(asset, a, volatility, risk)
        }

        // 2. 计算总体风险 f
        val f = assetRiskInfos.sumOf { it.risk }

        // 3. 收集每个资产的风险
        val assetRisks = assetRiskInfos.map { it.risk }

        // 4. 计算风险因子 F
        val F = f / (f + halfSaturationTotalRisk)
        lastRiskFactor = F

        // 5. 生成总体风险因子计算过程日志
        val overallRiskFactorLog = buildString {
            append("<第一个资产波动值*绝对偏移量>+<第二个资产波动值*绝对偏移量>+...<最后一个资产波动值*绝对偏移量>=<资产总体风险>; ")
            // 列举每个资产的风险贡献
            assetRiskInfos.forEachIndexed { index, info ->
                if (index > 0) append("+")
                append(String.format("%.6f*%.6f", info.volatility, info.a))
            }
            append(String.format("=%.6f", f))
            append("; ")
            append("<资产总体风险>/(<资产总体风险>+<半饱和总体风险>)=<总体风险因子>; ")
            append(String.format("%.6f/(%.6f+%.6f)=%.3f", f, f, halfSaturationTotalRisk, F))
        }
        lastOverallRiskFactorLog = overallRiskFactorLog

        // 5. 计算每个资产的卖出阈值及其日志
        val thresholds = mutableListOf<Double>()
        val thresholdLogs = mutableListOf<String>()
        
        assets.forEach { asset ->
            val vol = volatilityMap[asset.id] ?: 0.0
            val volClamped = vol.coerceIn(0.0, 1.0)
            val FClamped = F.coerceIn(0.0, 1.0)
            
            var S = baseThreshold
            S *= (1 - volClamped)
            S *= (1 - FClamped)
            
            // 生成此资产的卖出阈值计算过程日志
            var log = "<基础卖出阈值>*<1-资产波动率>*<1-总体风险因子>=<卖出阈值>; "
            log += String.format("%.3f*(1-%.3f)*(1-%.3f)=%.3f", 
                baseThreshold, volClamped, FClamped, S)
            
            thresholds.add(S)
            thresholdLogs.add(log)
        }

        return SellThresholdResult(
            thresholds = thresholds,
            thresholdLogs = thresholdLogs,
            assetRisks = assetRisks,
            overallRiskFactor = F,
            overallRiskFactorLog = overallRiskFactorLog
        )
    }

    /**
     * 计算所有资产的卖出阈值。
     * @param assets 资产列表
     * @param cash   可用现金，用于计算当前占比
     * @param volatilityMap 资产ID到波动率的映射
     * @return 映射 AssetId → 阈值 (0-1)，未超配返回 0.0
     */
    fun calculate(assets: List<Asset>, cash: Double, volatilityMap: Map<UUID, Double?>): List<Double> {
        return calculateWithLogs(assets, cash, volatilityMap).thresholds
    }

    /**
     * 计算所有资产的卖出阈值（向后兼容版本）。
     * @deprecated Asset类不再包含volatility字段，使用新版本的calculate方法
     */
    @Deprecated("Asset类不再包含volatility字段，使用新版本的calculate方法")
    fun calculate(assets: List<Asset>, cash: Double): List<Double> {
        val volatilityMap = assets.associate { it.id to 0.0 } // 默认波动率为0
        return calculate(assets, cash, volatilityMap)
    }
}
