package com.example.strategicassetallocationassistant.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.strategicassetallocationassistant.TradeType
import java.time.LocalDateTime
import java.util.UUID

/**
 * 交易记录表
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val assetId: String?,
    val type: TradeType,
    val shares: Double,
    val price: Double,
    val fee: Double,
    val amount: Double,
    val time: LocalDateTime
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
            time: LocalDateTime = LocalDateTime.now()
        ): TransactionEntity = TransactionEntity(
            id = id.toString(),
            assetId = assetId?.toString(),
            type = type,
            shares = shares,
            price = price,
            fee = fee,
            amount = amount,
            time = time
        )
    }
}
