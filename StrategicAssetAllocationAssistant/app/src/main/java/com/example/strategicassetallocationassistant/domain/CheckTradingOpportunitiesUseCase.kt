package com.example.strategicassetallocationassistant.domain

import com.example.strategicassetallocationassistant.TradeType
import com.example.strategicassetallocationassistant.TradingOpportunity
import com.example.strategicassetallocationassistant.data.repository.PortfolioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

/**
 * 简单的交易机会检查用例：
 * 当前版本：固定为第一个资产生成一条示例机会。
 */
class CheckTradingOpportunitiesUseCase @Inject constructor(
    private val repository: PortfolioRepository
) {
    suspend operator fun invoke(): List<TradingOpportunity> = withContext(Dispatchers.IO) {
        val portfolio = repository.getPortfolioOnce()
        val first = portfolio.assets.firstOrNull() ?: return@withContext emptyList()
        val now = LocalDateTime.now()
        val suggestion = TradingOpportunity(
            id = UUID.randomUUID(),
            assetId = first.id,
            type = TradeType.BUY,
            shares = 10.0,
            price = (first.unitValue ?: 1.0),
            fee = 0.0,
            amount = 10.0 * (first.unitValue ?: 1.0),
            time = now,
            reason = "示例机会：为 ${first.name} 买入 10 份以测试流程"
        )
        listOf(suggestion)
    }
}


