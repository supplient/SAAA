package com.example.strategicassetallocationassistant.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import com.example.strategicassetallocationassistant.data.network.BigDecimalSerializer
import java.math.BigDecimal

/**
 * 投资组合表实体类
 * 对应 portfolio 表：cash(Double)
 * 使用单例模式，表中只有一条记录
 */
@Serializable
@Entity(tableName = "portfolio")
data class PortfolioEntity(
    @PrimaryKey
    val id: Int = 1,  // 固定ID，确保表中只有一条记录
    val cash: Double,  // 现金金额
    
    // 新增BigDecimal字段 (步骤2: 双字段过渡)
    @Serializable(with = BigDecimalSerializer::class)
    val cashDecimal: BigDecimal?,
    
    val note: String? = null,
    val isBuyOpportunityPostponed: Boolean = false,
    @Serializable(with = com.example.strategicassetallocationassistant.LocalDateTimeSerializer::class)
    val lastBuyOpportunityCheck: java.time.LocalDateTime? = null,
    val pendingBuyOpportunity: Boolean = false,
    val overallRiskFactor: Double? = null,
    val overallRiskFactorLog: String? = null  // 总体风险因子计算过程日志
) {
    // ========== 双字段过渡期间的同步逻辑 ==========
    
    /**
     * 获取现金值，优先使用BigDecimal字段
     */
    fun getCashValue(): BigDecimal {
        return cashDecimal ?: BigDecimal.valueOf(cash)
    }
    
    /**
     * 同步数据到双字段，确保数据一致性
     */
    fun withSyncedFields(): PortfolioEntity {
        val syncedCash = cashDecimal ?: BigDecimal.valueOf(cash)
        
        return this.copy(
            cash = syncedCash.toDouble(),
            cashDecimal = syncedCash
        )
    }
    
    companion object {
        /**
         * 从Double创建PortfolioEntity
         */
        fun create(
            cash: Double,
            note: String? = null,
            isBuyOpportunityPostponed: Boolean = false,
            lastBuyOpportunityCheck: java.time.LocalDateTime? = null,
            pendingBuyOpportunity: Boolean = false,
            overallRiskFactor: Double? = null,
            overallRiskFactorLog: String? = null
        ): PortfolioEntity {
            return PortfolioEntity(
                cash = cash,
                cashDecimal = BigDecimal.valueOf(cash),
                note = note,
                isBuyOpportunityPostponed = isBuyOpportunityPostponed,
                lastBuyOpportunityCheck = lastBuyOpportunityCheck,
                pendingBuyOpportunity = pendingBuyOpportunity,
                overallRiskFactor = overallRiskFactor,
                overallRiskFactorLog = overallRiskFactorLog
            )
        }
        
        /**
         * 从BigDecimal创建PortfolioEntity
         */
        fun createWithDecimal(
            cashDecimal: BigDecimal,
            note: String? = null,
            isBuyOpportunityPostponed: Boolean = false,
            lastBuyOpportunityCheck: java.time.LocalDateTime? = null,
            pendingBuyOpportunity: Boolean = false,
            overallRiskFactor: Double? = null,
            overallRiskFactorLog: String? = null
        ): PortfolioEntity {
            return PortfolioEntity(
                cash = cashDecimal.toDouble(),
                cashDecimal = cashDecimal,
                note = note,
                isBuyOpportunityPostponed = isBuyOpportunityPostponed,
                lastBuyOpportunityCheck = lastBuyOpportunityCheck,
                pendingBuyOpportunity = pendingBuyOpportunity,
                overallRiskFactor = overallRiskFactor,
                overallRiskFactorLog = overallRiskFactorLog
            )
        }
    }
}
