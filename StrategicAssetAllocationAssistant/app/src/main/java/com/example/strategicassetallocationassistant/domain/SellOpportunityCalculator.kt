package com.example.strategicassetallocationassistant.domain

import com.example.strategicassetallocationassistant.Asset
import com.example.strategicassetallocationassistant.AssetType
import com.example.strategicassetallocationassistant.TradeType
import com.example.strategicassetallocationassistant.TradingOpportunity
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import kotlin.math.ceil

/**
 * 根据资产配置计算卖出机会。
 */
class SellOpportunityCalculator @Inject constructor() {
    private val handSize = 100.0

    fun calculate(portfolio: com.example.strategicassetallocationassistant.Portfolio): List<TradingOpportunity> {
        val total = portfolio.assets.sumOf { it.currentMarketValue } + portfolio.cash
        if (total <= 0) return emptyList()
        val list = mutableListOf<TradingOpportunity>()
        for (asset in portfolio.assets) {
            val currentWeight = asset.currentMarketValue / total
            val deviation = currentWeight - asset.targetWeight
            if (deviation > 0.004) {
                val amountPlan = deviation / 2 * total
                val (shares, amountFinal) = sharesAndAmount(asset, amountPlan)
                if (amountFinal >= 1000) { // 最小单笔金额
                    list += TradingOpportunity(
                        id = UUID.randomUUID(),
                        assetId = asset.id,
                        type = TradeType.SELL,
                        shares = shares,
                        price = asset.unitValue ?: 1.0,
                        fee = 0.0,
                        amount = amountFinal,
                        time = LocalDateTime.now(),
                        reason = "资产 ${asset.name} 占比超目标 ${(deviation * 100).format2()}%，卖出一半超出部分"
                    )
                }
            }
        }
        return list
    }

    private fun sharesAndAmount(asset: Asset, amountPlan: Double): Pair<Double, Double> {
        val price = asset.unitValue ?: 1.0
        var shares = amountPlan / price
        if (asset.type == AssetType.STOCK) {
            val lots = shares / handSize
            shares = ceil(lots) * handSize // 卖出向上取整
        }
        val amountFinal = shares * price
        return shares to amountFinal
    }

    private fun Double.format2(): String = String.format("%.2f", this)
}
