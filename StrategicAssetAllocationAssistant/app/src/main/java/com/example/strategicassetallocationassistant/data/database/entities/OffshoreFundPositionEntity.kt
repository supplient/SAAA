package com.example.strategicassetallocationassistant.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

/**
 * 场外基金持仓表实体类
 * 对应 offshore_fund_positions 表：asset_id(外键), code, shares, netValue, lastUpdateTime
 */
@Entity(
    tableName = "offshore_fund_positions",
    foreignKeys = [
        ForeignKey(
            entity = AssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["assetId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class OffshoreFundPositionEntity(
    @PrimaryKey
    val assetId: String,           // 外键，指向assets表的id
    val code: String,              // 基金代码
    val shares: Double,            // 基金份额
    val netValue: Double,          // 基金净值
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
            netValue: Double,
            lastUpdateTime: LocalDateTime
        ): OffshoreFundPositionEntity {
            return OffshoreFundPositionEntity(
                assetId = assetId.toString(),
                code = code,
                shares = shares,
                netValue = netValue,
                lastUpdateTime = lastUpdateTime
            )
        }
    }
    
    /**
     * 获取UUID格式的assetId
     */
    fun getAssetUUID(): UUID = UUID.fromString(assetId)
    
    /**
     * 计算当前市值
     */
    fun calculateCurrentMarketValue(): Double = shares * netValue
}
