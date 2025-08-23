package com.example.strategicassetallocationassistant.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

/**
 * 货币基金持仓表实体类
 * 对应 money_fund_positions 表：asset_id(外键), code, shares, lastUpdateTime
 */
@Entity(
    tableName = "money_fund_positions",
    foreignKeys = [
        ForeignKey(
            entity = AssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["assetId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MoneyFundPositionEntity(
    @PrimaryKey
    val assetId: String,           // 外键，指向assets表的id
    val code: String,              // 基金代码
    val shares: Double,            // 基金份额
    val lastUpdateTime: LocalDateTime  // 最后更新时间
) {
    companion object {
        /**
         * 从UUID创建Entity的辅助方法
         */
        fun create(
            assetId: UUID,
            code: String,
            shares: Double,
            lastUpdateTime: LocalDateTime
        ): MoneyFundPositionEntity {
            return MoneyFundPositionEntity(
                assetId = assetId.toString(),
                code = code,
                shares = shares,
                lastUpdateTime = lastUpdateTime
            )
        }
    }
    
    /**
     * 获取UUID格式的assetId
     */
    fun getAssetUUID(): UUID = UUID.fromString(assetId)
    
    /**
     * 计算当前市值（货币基金份额直接等于市值）
     */
    fun calculateCurrentMarketValue(): Double = shares
}
