package com.example.strategicassetallocationassistant.domain

import com.example.strategicassetallocationassistant.AssetType
import com.example.strategicassetallocationassistant.TradeType
import com.example.strategicassetallocationassistant.TradingOpportunity
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import kotlin.math.floor
import kotlin.math.max

/**
 * 在已确认买入机会窗口的前提下，根据资产配置计算买入机会。
 */
class BuyOpportunityCalculator @Inject constructor() {
    private val handSize = 100.0
    private val minSingleAmountRatio = 0.01 // 1%
    private val minAbsoluteAmount = 1000.0

    fun calculate(portfolio: com.example.strategicassetallocationassistant.Portfolio): List<TradingOpportunity> {
        val total = portfolio.assets.sumOf { it.currentMarketValue } + portfolio.cash
        if (total <= 0) return emptyList()
        val cashTargetWeight = 1.0 - portfolio.assets.sumOf { it.targetWeight }
        val pendingAmount = portfolio.cash - (total * cashTargetWeight)
        if (pendingAmount < minAbsoluteAmount) return emptyList()

        val minSingle = max(total * minSingleAmountRatio, minAbsoluteAmount)
        val amountPlan = when {
            pendingAmount > minSingle * 25 -> pendingAmount / 25
            pendingAmount >= minSingle -> minSingle
            else -> pendingAmount
        }

        // 找到最缺的资产
        val target = portfolio.assets.minByOrNull { asset ->
            val currentWeight = asset.currentMarketValue / total
            currentWeight - asset.targetWeight
        } ?: return emptyList()

        val (shares, amountFinal) = sharesAndAmount(target, amountPlan)
        if (amountFinal < minAbsoluteAmount || amountFinal > pendingAmount) return emptyList()

        val opportunity = TradingOpportunity(
            id = UUID.randomUUID(),
            assetId = target.id,
            type = TradeType.BUY,
            shares = shares,
            price = target.unitValue ?: 1.0,
            fee = 0.0,
            amount = amountFinal,
            time = LocalDateTime.now(),
            reason = "补齐资产 ${target.name} 占比不足"
        )
        return listOf(opportunity)
    }

    private fun sharesAndAmount(asset: com.example.strategicassetallocationassistant.Asset, amountPlan: Double): Pair<Double, Double> {
        val price = asset.unitValue ?: 1.0
        var shares = amountPlan / price
        if (asset.type == AssetType.STOCK) {
            val lots = shares / handSize
            shares = floor(lots) * handSize // 买入向下取整
            if (shares <= 0) shares = handSize // 如果不足一手则向上
        }
        val amountFinal = shares * price
        return shares to amountFinal
    }
}
