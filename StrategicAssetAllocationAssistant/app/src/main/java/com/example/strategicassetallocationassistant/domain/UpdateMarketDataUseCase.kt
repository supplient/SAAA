package com.example.strategicassetallocationassistant.domain

import com.example.strategicassetallocationassistant.data.network.AShare
import com.example.strategicassetallocationassistant.data.repository.PortfolioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

data class MarketDataUpdateStats(
    val success: Int, 
    val fail: Int,
    val failedAssetIds: List<UUID> = emptyList()
)

/**
 * UseCase：遍历数据库资产并更新市场数据。
 * 直接使用 AShare 获取实时市场数据。
 * 不刷新货币基金类型的资产。
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
            val failedAssetIds = mutableListOf<UUID>()

            portfolio.assets.forEach { asset ->
                
                // 如果没有代码，计入失败
                val code = asset.code ?: run { 
                    fail++
                    failedAssetIds.add(asset.id)
                    return@forEach 
                }

                // 统一频率：按股票逻辑
                val frequency = "5m"

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
                    failedAssetIds.add(asset.id)
                }
            }

            MarketDataUpdateStats(
                success = success, 
                fail = fail,
                failedAssetIds = failedAssetIds
            )
        }
    }
}
