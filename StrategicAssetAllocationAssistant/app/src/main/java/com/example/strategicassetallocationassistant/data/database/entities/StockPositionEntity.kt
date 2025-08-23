package com.example.strategicassetallocationassistant.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

/**
 * 股票持仓表实体类
 * 对应 stock_positions 表：asset_id(外键), code, shares, marketValue, lastUpdateTime
 */
@Entity(
    tableName = "stock_positions",
    foreignKeys = [
        ForeignKey(
            entity = AssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["assetId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class StockPositionEntity(
    @PrimaryKey
    val assetId: String,           // 外键，指向assets表的id
    val code: String,              // 股票代码
    val shares: Double,            // 股数
    val marketValue: Double,       // 每股价格
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
            marketValue: Double,
            lastUpdateTime: LocalDateTime
        ): StockPositionEntity {
            return StockPositionEntity(
                assetId = assetId.toString(),
                code = code,
                shares = shares,
                marketValue = marketValue,
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
    fun calculateCurrentMarketValue(): Double = shares * marketValue
}
