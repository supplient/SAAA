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
        // 计算总资产=所有资产市值+现金
        val total = portfolio.assets.sumOf { it.currentMarketValue } + portfolio.cash
        // 如果总资产小于等于0，则返回空列表
        if (total <= 0) return emptyList()

        // 遍历所有资产，计算每个资产的当前权重和目标权重之间的偏差
        val list = mutableListOf<TradingOpportunity>()
        for (asset in portfolio.assets) {
            // 计算当前权重=资产市值/总资产
            val currentWeight = asset.currentMarketValue / total
            // 计算偏差=当前权重-目标权重
            val deviation = currentWeight - asset.targetWeight

            // 偏差大于等于0.4%时才考虑卖出
            if (deviation < 0.004)
                continue

            // 计划卖出金额=偏差的一半*总资产
            val amountPlan = deviation / 2 * total
            // 计算卖出份额和卖出金额
            val (shares, amountFinal) = sharesAndAmount(asset, amountPlan)

            // 如果卖出金额大于等于1000，则创建交易机会
            if (amountFinal >= 1000) {
                // 创建交易机会
                list += TradingOpportunity(
                    id = UUID.randomUUID(),
                    assetId = asset.id,
                    type = TradeType.SELL,
                    shares = shares,
                    price = asset.unitValue ?: 1.0,
                    fee = 5.0, // 默认佣金5元
                    amount = amountFinal,
                    time = LocalDateTime.now(),
                    reason = "当前占比为${(currentWeight*100).format2()}%，目标占比为${(asset.targetWeight*100).format2()}%，超出${(deviation * 100).format2()}%，卖出一半超出部分，即${(deviation/2*100).format2()}%"
                )
            }
        }
        return list
    }

    private fun sharesAndAmount(asset: Asset, amountPlan: Double): Pair<Double, Double> {
        val price = asset.unitValue ?: 1.0
        // 计划卖出份额=计划卖出金额/单价
        var shares = amountPlan / price
        // 如果资产类型是股票，因为A股是按手交易，所以需要凑整到一手的整数倍份额
        // 不过如果是卖出所有持股的话，可以不足一手
        if (asset.type == AssetType.STOCK) {
            // 向上取整到一手的整数倍
            val lots = shares / handSize
            shares = ceil(lots) * handSize
            // 当持股不足一手的整数倍时，全部卖出
            val currentShares = asset.shares ?: 0.0
            if (shares > currentShares) {
                shares = currentShares
            }
        }
        val amountFinal = shares * price
        return shares to amountFinal
    }

    private fun Double.format2(): String = String.format("%.2f", this)
}
