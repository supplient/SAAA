package com.example.strategicassetallocationassistant.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.strategicassetallocationassistant.TradeType
import com.example.strategicassetallocationassistant.data.network.BigDecimalSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * 交易机会表
 */
@Serializable
@Entity(tableName = "trading_opportunities")
data class TradingOpportunityEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val assetId: String?,
    val type: TradeType,
    val shares: Double,
    val price: Double,
    val fee: Double,
    val amount: Double,
    
    // 新增BigDecimal字段 (步骤2: 双字段过渡)
    @Serializable(with = BigDecimalSerializer::class)
    val sharesDecimal: BigDecimal?,
    @Serializable(with = BigDecimalSerializer::class)
    val priceDecimal: BigDecimal?,
    @Serializable(with = BigDecimalSerializer::class)
    val feeDecimal: BigDecimal?,
    @Serializable(with = BigDecimalSerializer::class)
    val amountDecimal: BigDecimal?,
    
    @Contextual val time: LocalDateTime,
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
            // 同步创建BigDecimal字段
            sharesDecimal = BigDecimal.valueOf(shares),
            priceDecimal = BigDecimal.valueOf(price),
            feeDecimal = BigDecimal.valueOf(fee),
            amountDecimal = BigDecimal.valueOf(amount),
            time = time,
            reason = reason
        )
        
        /**
         * 从BigDecimal创建TradingOpportunityEntity的新方法
         */
        fun createWithDecimal(
            id: UUID = UUID.randomUUID(),
            assetId: UUID?,
            type: TradeType,
            sharesDecimal: BigDecimal,
            priceDecimal: BigDecimal,
            feeDecimal: BigDecimal,
            amountDecimal: BigDecimal,
            time: LocalDateTime = LocalDateTime.now(),
            reason: String
        ): TradingOpportunityEntity = TradingOpportunityEntity(
            id = id.toString(),
            assetId = assetId?.toString(),
            type = type,
            // 同步创建Double字段 (向后兼容)
            shares = sharesDecimal.toDouble(),
            price = priceDecimal.toDouble(),
            fee = feeDecimal.toDouble(),
            amount = amountDecimal.toDouble(),
            sharesDecimal = sharesDecimal,
            priceDecimal = priceDecimal,
            feeDecimal = feeDecimal,
            amountDecimal = amountDecimal,
            time = time,
            reason = reason
        )
    }
    
    // ========== 双字段过渡期间的同步逻辑 ==========
    
    /**
     * 获取份额值，优先使用BigDecimal字段
     */
    fun getSharesValue(): BigDecimal {
        return sharesDecimal ?: BigDecimal.valueOf(shares)
    }
    
    /**
     * 获取价格值，优先使用BigDecimal字段
     */
    fun getPriceValue(): BigDecimal {
        return priceDecimal ?: BigDecimal.valueOf(price)
    }
    
    /**
     * 获取手续费值，优先使用BigDecimal字段
     */
    fun getFeeValue(): BigDecimal {
        return feeDecimal ?: BigDecimal.valueOf(fee)
    }
    
    /**
     * 获取金额值，优先使用BigDecimal字段
     */
    fun getAmountValue(): BigDecimal {
        return amountDecimal ?: BigDecimal.valueOf(amount)
    }
    
    /**
     * 同步数据到双字段，确保数据一致性
     */
    fun withSyncedFields(): TradingOpportunityEntity {
        val syncedShares = sharesDecimal ?: BigDecimal.valueOf(shares)
        val syncedPrice = priceDecimal ?: BigDecimal.valueOf(price)
        val syncedFee = feeDecimal ?: BigDecimal.valueOf(fee)
        val syncedAmount = amountDecimal ?: BigDecimal.valueOf(amount)
        
        return this.copy(
            shares = syncedShares.toDouble(),
            price = syncedPrice.toDouble(),
            fee = syncedFee.toDouble(),
            amount = syncedAmount.toDouble(),
            sharesDecimal = syncedShares,
            priceDecimal = syncedPrice,
            feeDecimal = syncedFee,
            amountDecimal = syncedAmount
        )
    }
}


