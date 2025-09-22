package com.example.strategicassetallocationassistant.ui.common.model

import com.example.strategicassetallocationassistant.Asset
import java.math.BigDecimal

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
    
    // 新增BigDecimal字段 (步骤4: 计算类适配层)
    val marketValueDecimal: BigDecimal? = null,        // 市值 (BigDecimal版本)
    val currentWeightDecimal: BigDecimal? = null,      // 当前占比 (BigDecimal版本)
    val deviationPctDecimal: BigDecimal? = null,       // 偏离百分比 (BigDecimal版本)
    val targetMarketValueDecimal: BigDecimal? = null,  // 目标市值 (BigDecimal版本)
    val deviationValueDecimal: BigDecimal? = null,     // 偏离值 (BigDecimal版本)
    
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
) {
    // ========== 双字段过渡期间的同步逻辑 ==========
    
    /**
     * 获取市值，优先使用BigDecimal字段
     */
    fun getMarketValueValue(): BigDecimal {
        return marketValueDecimal ?: BigDecimal.valueOf(marketValue)
    }
    
    /**
     * 获取当前权重，优先使用BigDecimal字段
     */
    fun getCurrentWeightValue(): BigDecimal {
        return currentWeightDecimal ?: BigDecimal.valueOf(currentWeight)
    }
    
    /**
     * 获取偏离百分比，优先使用BigDecimal字段
     */
    fun getDeviationPctValue(): BigDecimal {
        return deviationPctDecimal ?: BigDecimal.valueOf(deviationPct)
    }
    
    /**
     * 获取目标市值，优先使用BigDecimal字段
     */
    fun getTargetMarketValueValue(): BigDecimal {
        return targetMarketValueDecimal ?: BigDecimal.valueOf(targetMarketValue)
    }
    
    /**
     * 获取偏离值，优先使用BigDecimal字段
     */
    fun getDeviationValueValue(): BigDecimal {
        return deviationValueDecimal ?: BigDecimal.valueOf(deviationValue)
    }
}
