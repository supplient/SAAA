package com.example.strategicassetallocationassistant.domain

import com.example.strategicassetallocationassistant.TradingOpportunity
import com.example.strategicassetallocationassistant.data.repository.PortfolioRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * 检查投资组合中的资产偏离情况，生成买入/卖出交易机会。
 * 规则来源：REQUIRE.md 中“交易机会规则”章节，做了简化：
 * 1. 卖出机会：当某资产占比超过目标占比 0.4% 时，卖出超出部分的一半。
 * 2. 买入机会：每周三 14:00 触发一次，买入距离目标占比最远且低于目标的资产。
 *    买入金额按规则简化实现。
 */
class CheckTradingOpportunitiesUseCase @Inject constructor(
    private val repository: PortfolioRepository,
    private val sellCalculator: SellOpportunityCalculator,
    private val buyCalculator: BuyOpportunityCalculator,
    private val windowChecker: BuyOpportunityWindowChecker
) {
    suspend operator fun invoke(): List<TradingOpportunity> = withContext(Dispatchers.IO) {
        val portfolio = repository.getPortfolioOnce()
        val opportunities = mutableListOf<TradingOpportunity>()

        // 卖出机会
        opportunities += sellCalculator.calculate(portfolio)

        // 买入机会窗口判断
        val now = LocalDateTime.now()
        if (windowChecker.shouldTrigger(now, repository.getLastBuyOpportunityCheck())) {
            opportunities += buyCalculator.calculate(portfolio)
            // 更新检查时间
            repository.updateLastBuyOpportunityCheck(now)
        }

        opportunities
    }
}


