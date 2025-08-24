package com.example.strategicassetallocationassistant.domain

import com.example.strategicassetallocationassistant.data.network.AShare
import com.example.strategicassetallocationassistant.data.repository.PortfolioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject

data class MarketDataUpdateStats(val success: Int, val fail: Int)

/**
 * UseCase：遍历数据库资产并更新市场数据。
 * 直接使用 AShare 获取实时市场数据。
 */
class UpdateMarketDataUseCase @Inject constructor(
    private val repository: PortfolioRepository
) {
    suspend operator fun invoke(): MarketDataUpdateStats {
        return withContext(Dispatchers.IO) {
            // 获取当前资产列表
            val portfolio = repository.getPortfolioOnce()

            var success = 0
            var fail = 0

            portfolio.assets.forEach { asset ->
                // 如果没有代码，计入失败
                val code = asset.code ?: run { fail++; return@forEach }

                // 根据资产类型选择合适的频率
                val frequency = when (asset.type) {
                    com.example.strategicassetallocationassistant.AssetType.STOCK -> "5m"
                    else -> "1d" // MONEY_FUND / OFFSHORE_FUND 使用日频即可
                }

                val latest = runCatching {
                    AShare.getPrice(code = code, count = 1, frequency = frequency)
                        .lastOrNull()
                }.getOrNull()

                if (latest != null && latest.close > 0f) {
                    val updated = asset.copy(
                        unitValue = latest.close.toDouble(),
                        lastUpdateTime = LocalDateTime.now()
                    )
                    repository.updateAsset(updated)
                    success++
                } else {
                    fail++
                }
            }

            MarketDataUpdateStats(success = success, fail = fail)
        }
    }
}
