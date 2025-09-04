package com.example.strategicassetallocationassistant.ui.common.model

import com.example.strategicassetallocationassistant.Asset

/**
 * 公共模型：资产信息快照，供 UI 组件复用。
 * 与原先 PortfolioViewModel.AssetInfo 内容保持一致。
 */
data class AssetInfo(
    val asset: Asset,
    val marketValue: Double,
    val currentWeight: Double,          // 当前占比 (0-1)
    val deviationPct: Double,           // 与目标占比的偏离 (正:+ 负:-)
    val targetMarketValue: Double,      // 目标市值
    val deviationValue: Double,         // 与目标市值的偏离 (正:超出 负:不足)
    val isRefreshFailed: Boolean,       // 是否刷新失败
    // 来自AssetAnalysis表的数据
    val volatility: Double? = null,     // 波动率
    val sevenDayReturn: Double? = null, // 七日涨跌幅
    val buyFactor: Double? = null,      // 买入因子
    val sellThreshold: Double? = null,  // 卖出阈值
    val buyFactorLog: String? = null,   // 买入因子计算过程日志
    val sellThresholdLog: String? = null, // 卖出阈值计算过程日志
    // 中间计算结果
    val relativeOffset: Double? = null,     // 相对偏移 r
    val offsetFactor: Double? = null,       // 偏移因子 E
    val drawdownFactor: Double? = null,     // 跌幅因子 D
    val preVolatilityBuyFactor: Double? = null, // 去波动率的买入因子
    val assetRisk: Double? = null           // 资产风险 k_i * a_i
)
