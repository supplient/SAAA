package com.example.strategicassetallocationassistant.ui.common.util

import com.example.strategicassetallocationassistant.Asset
import com.example.strategicassetallocationassistant.AssetAnalysis
import com.example.strategicassetallocationassistant.ui.common.model.AssetInfo

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
