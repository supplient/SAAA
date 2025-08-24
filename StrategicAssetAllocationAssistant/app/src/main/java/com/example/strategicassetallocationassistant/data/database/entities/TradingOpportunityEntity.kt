package com.example.strategicassetallocationassistant.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.strategicassetallocationassistant.TradeType
import java.time.LocalDateTime
import java.util.UUID

/**
 * 交易机会表
 */
@Entity(tableName = "trading_opportunities")
data class TradingOpportunityEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val assetId: String?,
    val type: TradeType,
    val shares: Double,
    val price: Double,
    val fee: Double,
    val amount: Double,
    val time: LocalDateTime,
    val reason: String
) {
    companion object {
        fun create(
            id: UUID = UUID.randomUUID(),
            assetId: UUID?,
            type: TradeType,
            shares: Double,
            price: Double,
            fee: Double,
            amount: Double,
            time: LocalDateTime = LocalDateTime.now(),
            reason: String
        ): TradingOpportunityEntity = TradingOpportunityEntity(
            id = id.toString(),
            assetId = assetId?.toString(),
            type = type,
            shares = shares,
            price = price,
            fee = fee,
            amount = amount,
            time = time,
            reason = reason
        )
    }
}


