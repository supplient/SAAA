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
    // 保存最近一次计算的完整思考过程，供 UI 展示
    var lastLog: String = ""
        private set
    private val handSize = 100.0 // 一手等于多少股
    private val minSingleAmountRatio = 0.01 // 最小单次投放金额占比
    private val minAbsoluteAmount = 1000.0 // 最小单笔交易金额
    private val maxBuyWeeks = 25 // 最大买入周期，单位为周
    private val defaultFee = 5.0 // 默认佣金

    fun calculate(portfolio: com.example.strategicassetallocationassistant.Portfolio): List<TradingOpportunity> {
        val log = StringBuilder()
        // 计算总资产=所有资产市值+现金
        val total = portfolio.assets.sumOf { it.currentMarketValue } + portfolio.cash
        log.appendLine("总资产(市值+现金)：$total")
        // 如果总资产小于等于0，则返回空列表
        if (total <= 0) return emptyList()

        // 找到除货币基金外距离目标占比最远的资产
        val eligibleAssets = portfolio.assets.filter { it.type != AssetType.MONEY_FUND }
        val targetAsset = eligibleAssets.minByOrNull { asset ->
            val currentWeight = asset.currentMarketValue / total
            currentWeight - asset.targetWeight
        } ?: return emptyList()

        val targetAssetCurrentWeight = targetAsset.currentMarketValue / total
        log.appendLine("当前最偏离目标的资产：${targetAsset.name}，当前占比=${(targetAssetCurrentWeight*100).format2()}%, 目标占比=${(targetAsset.targetWeight*100).format2()}%")

        if (targetAssetCurrentWeight > targetAsset.targetWeight) {
            log.appendLine("该资产已经高于目标占比，放弃买入")
            lastLog = log.toString()
            return emptyList()
        }

        // 获取所有货币基金的当前金额和目标金额，计算得到待投放金额
        val moneyMarketFunds = portfolio.assets.filter { it.type == AssetType.MONEY_FUND }
        val moneyMarketFundsCurrentValue = moneyMarketFunds.sumOf { it.currentMarketValue }
        val moneyMarketFundsTargetValue = moneyMarketFunds.sumOf { total * it.targetWeight }
        val pendingAmount = moneyMarketFundsCurrentValue - moneyMarketFundsTargetValue
        log.appendLine("货币基金待投放金额：$pendingAmount")

        // 计算最小单次投放金额
        val minSingleInvestmentAmount = total * minSingleAmountRatio

        // 计算计划买入金额
        val amountPlan = when {
            pendingAmount > minSingleInvestmentAmount * maxBuyWeeks -> pendingAmount / maxBuyWeeks
            pendingAmount >= minSingleInvestmentAmount -> minSingleInvestmentAmount
            else -> pendingAmount
        }

        // 对计划买入金额进行一手近似处理得到买入金额
        val (shares, amountFinal) = sharesAndAmount(targetAsset, amountPlan)
        log.appendLine("计划买入金额：$amountPlan，按一手规则实际买入金额：$amountFinal，份额：$shares")

        // 判断买入金额是否小于等于待投放金额、且大于等于最小单笔交易金额，如果不满足则放弃买入
        if (amountFinal > pendingAmount || amountFinal < minAbsoluteAmount) {
            log.appendLine("买入金额不符合条件，放弃买入")
            lastLog = log.toString()
            return emptyList()
        }

        // 计算交易费用(目前默认5元)
        val fee = defaultFee
        log.appendLine("交易费用：$fee")

        // 创建交易机会
        val currentWeight = targetAsset.currentMarketValue / total
        val opportunity = TradingOpportunity(
            id = UUID.randomUUID(),
            assetId = targetAsset.id,
            type = TradeType.BUY,
            shares = shares,
            price = targetAsset.unitValue ?: 1.0,
            fee = fee,
            amount = amountFinal + fee, // 现金实际变动值=买入金额+交易费用
            time = LocalDateTime.now(),
            reason = "当前占比为${(currentWeight*100).format2()}%, 目标占比为${(targetAsset.targetWeight*100).format2()}%，还差${((targetAsset.targetWeight-currentWeight)*100).format2()}%，买入${amountFinal/total*100}%"
        )
        log.appendLine("生成买入机会成功")
        lastLog = log.toString()
        return listOf(opportunity)
    }

    private fun sharesAndAmount(asset: com.example.strategicassetallocationassistant.Asset, amountPlan: Double): Pair<Double, Double> {
        val price = asset.unitValue ?: 1.0
        // 计划买入份额=计划买入金额/单价
        var shares = amountPlan / price
        // 如果资产类型是股票，因为A股是按手交易，所以需要凑整到一手的整数倍份额
        if (asset.type == AssetType.STOCK) {
            // 向下取整到一手的整数倍
            val lots = shares / handSize
            shares = floor(lots) * handSize
            // 如果不足一手则向上取整到一手
            if (shares <= 0) shares = handSize
        }
        val amountFinal = shares * price
        return shares to amountFinal
    }

    private fun Double.format2(): String = String.format("%.2f", this)
}
