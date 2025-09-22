package com.example.strategicassetallocationassistant.ui.common.util

import com.example.strategicassetallocationassistant.Asset
import com.example.strategicassetallocationassistant.AssetAnalysis
import com.example.strategicassetallocationassistant.ui.common.model.AssetInfo
import java.math.BigDecimal

/**
 * 根据 Asset + 总资产值 + 可选分析数据 计算 AssetInfo。
 */
fun buildAssetInfo(
    asset: Asset,
    totalAssetsValue: Double,
    analysis: AssetAnalysis? = null,
    isRefreshFailed: Boolean = false
): AssetInfo {
    val value = asset.currentMarketValue
    val weight = if (totalAssetsValue > 0) value / totalAssetsValue else 0.0
    val deviationPct = weight - asset.targetWeight
    val targetValue = totalAssetsValue * asset.targetWeight
    val deviationValue = value - targetValue

    return AssetInfo(
        asset = asset,
        marketValue = value,
        currentWeight = weight,
        deviationPct = deviationPct,
        targetMarketValue = targetValue,
        deviationValue = deviationValue,
        isRefreshFailed = isRefreshFailed,
        volatility = analysis?.volatility,
        sevenDayReturn = analysis?.sevenDayReturn,
        buyFactor = analysis?.buyFactor,
        sellThreshold = analysis?.sellThreshold,
        buyFactorLog = analysis?.buyFactorLog,
        sellThresholdLog = analysis?.sellThresholdLog,
        relativeOffset = analysis?.relativeOffset,
        offsetFactor = analysis?.offsetFactor,
        drawdownFactor = analysis?.drawdownFactor,
        preVolatilityBuyFactor = analysis?.preVolatilityBuyFactor,
        assetRisk = analysis?.assetRisk
    )
}

/**
 * BigDecimal版本的AssetInfo构建函数 (步骤4: 计算类适配层)
 * 使用BigDecimal进行精确计算，避免浮点数精度问题
 */
fun buildAssetInfoWithDecimal(
    asset: Asset,
    totalAssetsValueDecimal: BigDecimal,
    analysis: AssetAnalysis? = null,
    isRefreshFailed: Boolean = false
): AssetInfo {
    val valueDecimal = asset.currentMarketValueDecimal
    val weightDecimal = if (totalAssetsValueDecimal > BigDecimal.ZERO) {
        MoneyUtils.safeDivide(valueDecimal, totalAssetsValueDecimal, 8)
    } else {
        BigDecimal.ZERO
    }
    val targetWeightDecimal = BigDecimal.valueOf(asset.targetWeight)
    val deviationPctDecimal = weightDecimal.subtract(targetWeightDecimal)
    val targetValueDecimal = MoneyUtils.safeMultiply(totalAssetsValueDecimal, targetWeightDecimal)
    val deviationValueDecimal = valueDecimal.subtract(targetValueDecimal)

    return AssetInfo(
        asset = asset,
        // Double字段 (向后兼容)
        marketValue = valueDecimal.toDouble(),
        currentWeight = weightDecimal.toDouble(),
        deviationPct = deviationPctDecimal.toDouble(),
        targetMarketValue = targetValueDecimal.toDouble(),
        deviationValue = deviationValueDecimal.toDouble(),
        // BigDecimal字段 (精确版本)
        marketValueDecimal = valueDecimal,
        currentWeightDecimal = weightDecimal,
        deviationPctDecimal = deviationPctDecimal,
        targetMarketValueDecimal = targetValueDecimal,
        deviationValueDecimal = deviationValueDecimal,
        
        isRefreshFailed = isRefreshFailed,
        // 分析数据保持不变
        volatility = analysis?.volatility,
        sevenDayReturn = analysis?.sevenDayReturn,
        buyFactor = analysis?.buyFactor,
        sellThreshold = analysis?.sellThreshold,
        buyFactorLog = analysis?.buyFactorLog,
        sellThresholdLog = analysis?.sellThresholdLog,
        relativeOffset = analysis?.relativeOffset,
        offsetFactor = analysis?.offsetFactor,
        drawdownFactor = analysis?.drawdownFactor,
        preVolatilityBuyFactor = analysis?.preVolatilityBuyFactor,
        assetRisk = analysis?.assetRisk
    )
}

/**
 * 过渡期比较方法 - 用于验证新旧方法的计算结果一致性
 */
fun validateAssetInfoConsistency(
    asset: Asset,
    totalAssetsValue: Double,
    analysis: AssetAnalysis? = null
): Boolean {
    return try {
        val oldResult = buildAssetInfo(asset, totalAssetsValue, analysis)
        val newResult = buildAssetInfoWithDecimal(asset, BigDecimal.valueOf(totalAssetsValue), analysis)
        
        // 比较关键数值字段的一致性
        MoneyUtils.isEqual(newResult.getMarketValueValue(), oldResult.getMarketValueValue(), MoneyUtils.MONEY_SCALE) &&
        MoneyUtils.isEqual(newResult.getCurrentWeightValue(), oldResult.getCurrentWeightValue(), 6) &&
        MoneyUtils.isEqual(newResult.getTargetMarketValueValue(), oldResult.getTargetMarketValueValue(), MoneyUtils.MONEY_SCALE) &&
        MoneyUtils.isEqual(newResult.getDeviationValueValue(), oldResult.getDeviationValueValue(), MoneyUtils.MONEY_SCALE)
    } catch (e: Exception) {
        false
    }
}
