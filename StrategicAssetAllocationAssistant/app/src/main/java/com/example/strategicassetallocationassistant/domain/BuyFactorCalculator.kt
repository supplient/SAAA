package com.example.strategicassetallocationassistant.domain

import com.example.strategicassetallocationassistant.Asset
import kotlin.math.max

/**
 * 根据设定公式计算买入因子。
 *
 * 公式来源：
 *  E = r / (r + r~)  其中 r = (target - current)/target
 *  D = d / (d + d~)  其中 d = max(0, -delta) ; delta 为七日涨跌幅
 *  B = (1 - w * k) ( alpha * E + (1 - alpha) * D )
 *
 * 默认参数：
 *  r~  = 0.10   (半饱和相对偏移)
 *  d~  = 0.05   (半饱和跌幅)
 *  alpha = 0.8  (偏移权重)
 *  w   = 0.5    (波动率权重)
 *
 * 其中 k 为资产年化波动率 [0,1]（>=1 时会将 B 裁剪为 0）。
 */
class BuyFactorCalculator(
    private val halfSaturationRelativeOffset: Double = 0.10, // r~
    private val halfSaturationDrawdown: Double = 0.05,      // d~
    private val alpha: Double = 0.8,                        // α
    private val volatilityWeight: Double = 0.5              // w - 波动率权重
) {

    data class Result(
        val relativeOffset: Double,     // r - 相对偏移
        val offsetFactor: Double,       // E - 偏移因子
        val drawdownFactor: Double,     // D - 跌幅因子
        val preVolatilityBuyFactor: Double, // 去波动率的买入因子
        val buyFactor: Double,          // B - 买入因子
        val calculationLog: String      // 计算过程日志
    )

    /**
     * 计算给定资产的 [Result]。
     * @param asset 资产对象，包含 targetWeight 等基本信息
     * @param totalAssetsValue 所有资产总市值 + 现金，用于计算当前占比
     * @param volatility 资产波动率
     * @param sevenDayReturn 七日涨跌幅
     */
    fun calculate(asset: Asset, totalAssetsValue: Double, volatility: Double?, sevenDayReturn: Double?): Result {
        if (asset.targetWeight <= 0 || totalAssetsValue <= 0) {
            return Result(0.0, 0.0, 0.0, 0.0, 0.0, "无效输入: targetWeight=${asset.targetWeight}, totalAssetsValue=$totalAssetsValue")
        }

        // 当前占比
        val currentWeight = asset.currentMarketValue / totalAssetsValue

        // r = (target - current)/target
        val relativeOffset = (asset.targetWeight - currentWeight) / asset.targetWeight

        val offsetFactor = if (relativeOffset <= 0) 0.0 else relativeOffset / (relativeOffset + halfSaturationRelativeOffset)

        // 七日跌幅绝对值 d
        val delta = sevenDayReturn ?: 0.0
        val d = max(0.0, -delta)
        val drawdownFactor = if (d <= 0) 0.0 else d / (d + halfSaturationDrawdown)

        // 去波动率的买入因子
        val preVolatilityBuyFactor = alpha * offsetFactor + (1 - alpha) * drawdownFactor

        // 资产波动率
        val k = volatility ?: 0.0
        val kClamped = k.coerceIn(0.0, 1.0)

        // 最终买入因子
        val buyFactor = (1 - volatilityWeight * kClamped) * preVolatilityBuyFactor

        // 生成计算过程日志
        val log = buildString {
            append("(<目标占比>-<当前占比>)/<目标占比>=<相对偏移>; ")
            append(String.format("(%.3f-%.3f)/%.3f=%.3f", asset.targetWeight, currentWeight, asset.targetWeight, (asset.targetWeight - currentWeight)/asset.targetWeight))
            append("; ")
            append("<相对偏移>/(<相对偏移>+<半饱和相对偏移>=<偏移因子>; ")
            append(String.format("%.3f/(%.3f+%.3f)=%.3f", relativeOffset, relativeOffset, halfSaturationRelativeOffset, offsetFactor))
            append("; ")
            append("max(0, -<七日涨跌幅>)=<七日绝对跌幅>; ")
            append(String.format("max(0, -%.3f)=%.3f", delta, d))
            append("; ")
            append("<七日绝对跌幅>/(<七日绝对跌幅>+<半饱和跌幅>)=<跌幅因子>; ")
            append(String.format("%.3f/(%.3f+%.3f)=%.3f", d, d, halfSaturationDrawdown, drawdownFactor))
            append("; ")
            append("<偏移权重>*<偏移因子>+<1-偏移权重>*<跌幅因子>=<去波动率的买入因子>; ")
            append(String.format("%.3f*%.3f+%.3f*%.3f=%.3f", alpha, offsetFactor, 1-alpha, drawdownFactor, preVolatilityBuyFactor))
            append("; ")
            append("<1-波动率权重*资产波动率>*<去波动率的买入因子>=<买入因子>; ")
            append(String.format("(1-%.3f*%.3f)*%.3f=%.3f", volatilityWeight, kClamped, preVolatilityBuyFactor, buyFactor))
        }

        return Result(relativeOffset, offsetFactor, drawdownFactor, preVolatilityBuyFactor, buyFactor, log)
    }

    /**
     * 计算给定资产的 [Result]（向后兼容版本，从Asset对象获取分析数据）
     * @deprecated 使用新版本的calculate方法，传入volatility和sevenDayReturn参数
     */
    @Deprecated("Asset类不再包含分析数据，使用新版本的calculate方法")
    fun calculate(asset: Asset, totalAssetsValue: Double): Result {
        // 这个方法现在不可用了，因为Asset不再包含volatility和sevenDayReturn
        return calculate(asset, totalAssetsValue, null, null)
    }
}
