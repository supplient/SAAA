package com.example.strategicassetallocationassistant.domain

import com.example.strategicassetallocationassistant.TradeType
import com.example.strategicassetallocationassistant.TradingOpportunity
import com.example.strategicassetallocationassistant.ui.common.util.MoneyUtils
import java.math.BigDecimal
import java.math.RoundingMode
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
    
    // 原Double版本常量 (保留向后兼容性)
    private val handSize = 100.0 // 一手等于多少股
    private val minSingleAmountRatio = 0.01 // 最小单次投放金额占比
    private val minAbsoluteAmount = 1000.0 // 最小单笔交易金额
    private val maxBuyWeeks = 25 // 最大买入周期，单位为周
    private val defaultFee = 5.0 // 默认佣金
    
    // 新BigDecimal版本常量 (步骤4: 计算类适配层)
    private val handSizeDecimal = MoneyUtils.createShare("100") // 一手等于多少股
    private val minSingleAmountRatioDecimal = BigDecimal("0.01") // 最小单次投放金额占比
    private val minAbsoluteAmountDecimal = MoneyUtils.createMoney("1000") // 最小单笔交易金额
    private val maxBuyWeeksDecimal = BigDecimal("25") // 最大买入周期，单位为周
    private val defaultFeeDecimal = MoneyUtils.createMoney("5") // 默认佣金

    fun calculate(portfolio: com.example.strategicassetallocationassistant.Portfolio): List<TradingOpportunity> {
        val log = StringBuilder()
        // 计算总资产=所有资产市值+现金
        val total = portfolio.assets.sumOf { it.currentMarketValue } + portfolio.cash
        log.appendLine("总资产(市值+现金)：$total")
        // 如果总资产小于等于0，则返回空列表
        if (total <= 0) return emptyList()

        // 找到距离目标占比最远的资产（现所有资产均视为股票）
        val eligibleAssets = portfolio.assets
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

        // 现金即为待投放金额
        val pendingAmount = portfolio.cash
        log.appendLine("可用现金待投放金额：$pendingAmount")

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

    /**
     * BigDecimal版本的买入机会计算 (步骤4: 计算类适配层)
     * 使用BigDecimal进行精确计算，避免浮点数精度问题
     */
    fun calculateWithDecimal(portfolio: com.example.strategicassetallocationassistant.Portfolio): List<TradingOpportunity> {
        val log = StringBuilder()
        // 计算总资产=所有资产市值+现金（使用BigDecimal精确计算）
        val total = portfolio.totalAssetsValueDecimal
        log.appendLine("总资产(市值+现金)：${MoneyUtils.formatMoney(total)}")
        
        // 如果总资产小于等于0，则返回空列表
        if (total <= BigDecimal.ZERO) return emptyList()

        // 找到距离目标占比最远的资产（现所有资产均视为股票）
        val eligibleAssets = portfolio.assets
        val targetAsset = eligibleAssets.minByOrNull { asset ->
            val currentWeight = MoneyUtils.safeDivide(asset.currentMarketValueDecimal, total, 8)
            currentWeight.subtract(BigDecimal.valueOf(asset.targetWeight))
        } ?: return emptyList()

        val targetAssetCurrentWeight = MoneyUtils.safeDivide(targetAsset.currentMarketValueDecimal, total, 8)
        log.appendLine("当前最偏离目标的资产：${targetAsset.name}，当前占比=${MoneyUtils.formatPercentage(targetAssetCurrentWeight.multiply(BigDecimal("100")))}, 目标占比=${(targetAsset.targetWeight*100).format2()}%")

        if (targetAssetCurrentWeight > BigDecimal.valueOf(targetAsset.targetWeight)) {
            log.appendLine("该资产已经高于目标占比，放弃买入")
            lastLog = log.toString()
            return emptyList()
        }

        // 现金即为待投放金额
        val pendingAmount = portfolio.getCashValue()
        log.appendLine("可用现金待投放金额：${MoneyUtils.formatMoney(pendingAmount)}")

        // 计算最小单次投放金额
        val minSingleInvestmentAmount = total.multiply(minSingleAmountRatioDecimal)

        // 计算计划买入金额
        val amountPlan = when {
            pendingAmount > minSingleInvestmentAmount.multiply(maxBuyWeeksDecimal) -> 
                MoneyUtils.safeDivide(pendingAmount, maxBuyWeeksDecimal)
            pendingAmount >= minSingleInvestmentAmount -> minSingleInvestmentAmount
            else -> pendingAmount
        }

        // 对计划买入金额进行一手近似处理得到买入金额
        val (shares, amountFinal) = sharesAndAmountDecimal(targetAsset, amountPlan)
        log.appendLine("计划买入金额：${MoneyUtils.formatMoney(amountPlan)}，按一手规则实际买入金额：${MoneyUtils.formatMoney(amountFinal)}，份额：${MoneyUtils.formatShare(shares)}")

        // 判断买入金额是否小于等于待投放金额、且大于等于最小单笔交易金额，如果不满足则放弃买入
        if (amountFinal > pendingAmount || amountFinal < minAbsoluteAmountDecimal) {
            log.appendLine("买入金额不符合条件，放弃买入")
            lastLog = log.toString()
            return emptyList()
        }

        // 计算交易费用(目前默认5元)
        val fee = defaultFeeDecimal
        log.appendLine("交易费用：${MoneyUtils.formatMoney(fee)}")

        // 创建交易机会
        val currentWeight = MoneyUtils.safeDivide(targetAsset.currentMarketValueDecimal, total, 8)
        val targetWeightDecimal = BigDecimal.valueOf(targetAsset.targetWeight)
        val deviationPercent = targetWeightDecimal.subtract(currentWeight).multiply(BigDecimal("100"))
        val buyPercent = MoneyUtils.safeDivide(amountFinal, total).multiply(BigDecimal("100"))
        
        val opportunity = TradingOpportunity(
            id = UUID.randomUUID(),
            assetId = targetAsset.id,
            type = TradeType.BUY,
            shares = shares.toDouble(), // 保留Double字段兼容性
            price = targetAsset.getUnitValueValue()?.toDouble() ?: 1.0,
            fee = fee.toDouble(),
            amount = amountFinal.add(fee).toDouble(), // 现金实际变动值=买入金额+交易费用
            // BigDecimal字段 (精确版本)
            sharesDecimal = shares,
            priceDecimal = targetAsset.getUnitValueValue() ?: BigDecimal.ONE,
            feeDecimal = fee,
            amountDecimal = amountFinal.add(fee),
            time = LocalDateTime.now(),
            reason = "当前占比为${MoneyUtils.formatPercentage(currentWeight.multiply(BigDecimal("100")))}, 目标占比为${(targetAsset.targetWeight*100).format2()}%，还差${MoneyUtils.formatPercentage(deviationPercent)}，买入${MoneyUtils.formatPercentage(buyPercent)}"
        )
        log.appendLine("生成买入机会成功")
        lastLog = log.toString()
        return listOf(opportunity)
    }

    private fun sharesAndAmount(asset: com.example.strategicassetallocationassistant.Asset, amountPlan: Double): Pair<Double, Double> {
        val price = asset.unitValue ?: 1.0
        // 计划买入份额=计划买入金额/单价
        var shares = amountPlan / price
        // A股按手交易，需要凑整到一手的整数倍份额
        val lots = shares / handSize
        shares = floor(lots) * handSize
        // 如果不足一手则向上取整到一手
        if (shares <= 0) shares = handSize
        val amountFinal = shares * price
        return shares to amountFinal
    }

    /**
     * BigDecimal版本的份额和金额计算 (步骤4: 计算类适配层)
     */
    private fun sharesAndAmountDecimal(asset: com.example.strategicassetallocationassistant.Asset, amountPlan: BigDecimal): Pair<BigDecimal, BigDecimal> {
        val price = asset.getUnitValueValue() ?: BigDecimal.ONE
        // 计划买入份额=计划买入金额/单价
        var shares = MoneyUtils.safeDivide(amountPlan, price, MoneyUtils.SHARE_SCALE)
        // A股按手交易，需要凑整到一手的整数倍份额
        val lots = MoneyUtils.safeDivide(shares, handSizeDecimal, 0, RoundingMode.DOWN)
        shares = lots.multiply(handSizeDecimal)
        // 如果不足一手则向上取整到一手
        if (shares <= BigDecimal.ZERO) shares = handSizeDecimal
        val amountFinal = MoneyUtils.safeMultiply(shares, price)
        return shares to amountFinal
    }

    /**
     * 过渡期比较方法 - 用于验证新旧方法的计算结果一致性
     */
    fun validateConsistency(portfolio: com.example.strategicassetallocationassistant.Portfolio): Boolean {
        return try {
            val oldResult = calculate(portfolio)
            val newResult = calculateWithDecimal(portfolio)
            
            // 比较结果数量
            if (oldResult.size != newResult.size) return false
            
            // 比较每个交易机会的关键字段
            oldResult.zip(newResult).all { (old, new) ->
                old.assetId == new.assetId &&
                old.type == new.type &&
                MoneyUtils.isEqual(new.getAmountValue(), old.getAmountValue(), MoneyUtils.MONEY_SCALE) &&
                MoneyUtils.isEqual(new.getSharesValue(), old.getSharesValue(), MoneyUtils.SHARE_SCALE)
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun Double.format2(): String = String.format("%.2f", this)
}
