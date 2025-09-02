package com.example.strategicassetallocationassistant.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

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
    val note: String? = null
    ,val isBuyOpportunityPostponed: Boolean = false
    ,@Serializable(with = com.example.strategicassetallocationassistant.LocalDateTimeSerializer::class)
    val lastBuyOpportunityCheck: java.time.LocalDateTime? = null,
    val pendingBuyOpportunity: Boolean = false,
    val overallRiskFactor: Double? = null,
    val overallRiskFactorLog: String? = null  // 总体风险因子计算过程日志
)
