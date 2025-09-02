package com.example.strategicassetallocationassistant.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable
import com.example.strategicassetallocationassistant.data.network.LocalDateTimeSerializer
import java.time.LocalDateTime

/**
 * 资产分析表实体类
 * 对应 asset_analysis 表：存储资产的动态分析数据
 * 与 assets 表建立一对一的外键关系
 */
@Serializable
@Entity(
    tableName = "asset_analysis",
    foreignKeys = [ForeignKey(
        entity = AssetEntity::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("assetId"),
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["assetId"], unique = true)]
)
data class AssetAnalysisEntity(
    @PrimaryKey
    val assetId: String,           // 外键，关联到 assets 表的 id
    val volatility: Double? = null,        // 90日每日收益率标准差
    val sevenDayReturn: Double? = null,    // 七日涨跌幅（-1~1）
    val buyFactor: Double? = null,         // 买入因子 B
    val sellThreshold: Double? = null,     // 卖出阈值 S
    val buyFactorLog: String? = null,      // 买入因子计算过程日志
    val sellThresholdLog: String? = null,  // 卖出阈值计算过程日志
    @Serializable(with = LocalDateTimeSerializer::class)
    val lastUpdateTime: LocalDateTime? = null  // 分析数据最后更新时间
) {
    companion object {
        /**
         * 创建AssetAnalysisEntity的辅助方法
         */
        fun create(
            assetId: String,
            volatility: Double? = null,
            sevenDayReturn: Double? = null,
            buyFactor: Double? = null,
            sellThreshold: Double? = null,
            buyFactorLog: String? = null,
            sellThresholdLog: String? = null,
            lastUpdateTime: LocalDateTime? = null
        ): AssetAnalysisEntity {
            return AssetAnalysisEntity(
                assetId = assetId,
                volatility = volatility,
                sevenDayReturn = sevenDayReturn,
                buyFactor = buyFactor,
                sellThreshold = sellThreshold,
                buyFactorLog = buyFactorLog,
                sellThresholdLog = sellThresholdLog,
                lastUpdateTime = lastUpdateTime
            )
        }
    }
}
