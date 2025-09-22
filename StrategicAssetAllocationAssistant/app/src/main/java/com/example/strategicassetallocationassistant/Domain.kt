package com.example.strategicassetallocationassistant

import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.Contextual
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.json.Json
import com.example.strategicassetallocationassistant.data.network.BigDecimalSerializer
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

// Custom serializer for LocalDateTime
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
}

// Custom serializer for UUID
object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}

// Serialization module for LocalDateTime, UUID and BigDecimal
val serializationModule = SerializersModule {
    contextual(LocalDateTimeSerializer)
    contextual(UUIDSerializer)
    // 注意：BigDecimal序列化器通过@Serializable(with = BigDecimalSerializer::class)注解直接使用
}

// JSON instance with custom LocalDateTime serializer
val json = Json {
    serializersModule = serializationModule
    prettyPrint = true
    ignoreUnknownKeys = true
}

// 新增交易类型枚举
enum class TradeType {
    BUY,  // 买入
    SELL  // 卖出
}

// 资产数据模型
@Serializable
data class Asset(
    @Contextual val id: UUID,       // 资产ID (UUID)
    val name: String,               // 资产名称
    val targetWeight: Double,       // 目标占比（0.0-1.0）
    val code: String? = null,                       // 资产代码：基金编号或股票编号
    val shares: Double? = null,                    // 份额/股数
    val unitValue: Double? = null,                 // 单位价格：货币基金恒为1，股票为每股价格，基金为净值
    
    // 新增BigDecimal字段 (步骤3: 域模型双字段过渡)
    @Serializable(with = BigDecimalSerializer::class)
    val sharesDecimal: BigDecimal? = null,         // 份额/股数 (BigDecimal版本)
    @Serializable(with = BigDecimalSerializer::class)
    val unitValueDecimal: BigDecimal? = null,      // 单位价格 (BigDecimal版本)
    
    @Contextual val lastUpdateTime: LocalDateTime? = null,  // 数据最后更新时间
    val note: String? = null                     // 备注
) {
    // 计算当前市场价值 (Double版本，向后兼容)
    val currentMarketValue: Double
        get() = (shares ?: 0.0) * (unitValue ?: 0.0)
    
    // ========== 双字段过渡期间的同步逻辑 ==========
    
    /**
     * 获取份额值，优先使用BigDecimal字段
     */
    fun getSharesValue(): BigDecimal? {
        return sharesDecimal ?: shares?.let { BigDecimal.valueOf(it) }
    }
    
    /**
     * 获取单位价值，优先使用BigDecimal字段
     */
    fun getUnitValueValue(): BigDecimal? {
        return unitValueDecimal ?: unitValue?.let { BigDecimal.valueOf(it) }
    }
    
    /**
     * 计算当前市场价值 (BigDecimal版本)
     */
    val currentMarketValueDecimal: BigDecimal
        get() {
            val shares = getSharesValue() ?: return BigDecimal.ZERO
            val unitValue = getUnitValueValue() ?: return BigDecimal.ZERO
            return shares.multiply(unitValue)
        }
    
    /**
     * 同步数据到双字段，确保数据一致性
     */
    fun withSyncedFields(): Asset {
        val syncedShares = sharesDecimal ?: shares?.let { BigDecimal.valueOf(it) }
        val syncedUnitValue = unitValueDecimal ?: unitValue?.let { BigDecimal.valueOf(it) }
        
        return this.copy(
            shares = syncedShares?.toDouble(),
            unitValue = syncedUnitValue?.toDouble(),
            sharesDecimal = syncedShares,
            unitValueDecimal = syncedUnitValue
        )
    }
}

// 资产分析数据模型
@Serializable
data class AssetAnalysis(
    @Contextual val assetId: UUID,               // 对应的资产ID
    val volatility: Double? = null,              // 90日每日收益率标准差
    val sevenDayReturn: Double? = null,          // 七日涨跌幅（-1~1）
    val buyFactor: Double? = null,               // 买入因子 B
    val sellThreshold: Double? = null,           // 卖出阈值 S
    val buyFactorLog: String? = null,            // 买入因子计算过程日志
    val sellThresholdLog: String? = null,        // 卖出阈值计算过程日志

    // 买入因子计算中间结果
    val relativeOffset: Double? = null,          // 相对偏移 r
    val offsetFactor: Double? = null,            // 偏移因子 E
    val drawdownFactor: Double? = null,          // 跌幅因子 D
    val preVolatilityBuyFactor: Double? = null,  // 去波动率的买入因子

    // 卖出阈值计算中间结果
    val assetRisk: Double? = null,               // 资产风险 k_i * a_i

    @Contextual val lastUpdateTime: LocalDateTime? = null  // 分析数据最后更新时间
)

// 资产配置的顶层数据结构，包含所有资产和一个总的现金账户。
@Serializable
data class Portfolio(
    val assets: List<Asset>,
    val cash: Double,
    
    // 新增BigDecimal字段 (步骤3: 域模型双字段过渡)
    @Serializable(with = BigDecimalSerializer::class)
    val cashDecimal: BigDecimal? = null,        // 现金金额 (BigDecimal版本)
    
    val note: String? = null,
    val overallRiskFactor: Double? = null,
    val overallRiskFactorLog: String? = null    // 总体风险因子计算过程日志
) {
    // ========== 双字段过渡期间的同步逻辑 ==========
    
    /**
     * 获取现金值，优先使用BigDecimal字段
     */
    fun getCashValue(): BigDecimal {
        return cashDecimal ?: BigDecimal.valueOf(cash)
    }
    
    /**
     * 计算总资产值 (BigDecimal版本)
     */
    val totalAssetsValueDecimal: BigDecimal
        get() = getCashValue() + assets.sumOf { it.currentMarketValueDecimal }
    
    /**
     * 同步数据到双字段，确保数据一致性
     */
    fun withSyncedFields(): Portfolio {
        val syncedCash = cashDecimal ?: BigDecimal.valueOf(cash)
        
        return this.copy(
            cash = syncedCash.toDouble(),
            cashDecimal = syncedCash,
            assets = assets.map { it.withSyncedFields() }
        )
    }
}

// 交易记录
@Serializable
data class Transaction(
    @Contextual val id: UUID,
    @Contextual val assetId: UUID?,
    val type: TradeType,
    val shares: Double,
    val price: Double,
    val fee: Double,
    val amount: Double,
    
    // 新增BigDecimal字段 (步骤3: 域模型双字段过渡)
    @Serializable(with = BigDecimalSerializer::class)
    val sharesDecimal: BigDecimal? = null,      // 份额 (BigDecimal版本)
    @Serializable(with = BigDecimalSerializer::class)
    val priceDecimal: BigDecimal? = null,       // 价格 (BigDecimal版本)
    @Serializable(with = BigDecimalSerializer::class)
    val feeDecimal: BigDecimal? = null,         // 手续费 (BigDecimal版本)
    @Serializable(with = BigDecimalSerializer::class)
    val amountDecimal: BigDecimal? = null,      // 金额 (BigDecimal版本)
    
    @Contextual val time: LocalDateTime,
    val reason: String? = null
) {
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
    fun withSyncedFields(): Transaction {
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

// 交易机会（和 Transaction 字段一致，额外包含触发理由）
@Serializable
data class TradingOpportunity(
    @Contextual val id: UUID,
    @Contextual val assetId: UUID?,
    val type: TradeType,
    val shares: Double,
    val price: Double,
    val fee: Double,
    val amount: Double,
    
    // 新增BigDecimal字段 (步骤3: 域模型双字段过渡)
    @Serializable(with = BigDecimalSerializer::class)
    val sharesDecimal: BigDecimal? = null,      // 份额 (BigDecimal版本)
    @Serializable(with = BigDecimalSerializer::class)
    val priceDecimal: BigDecimal? = null,       // 价格 (BigDecimal版本)
    @Serializable(with = BigDecimalSerializer::class)
    val feeDecimal: BigDecimal? = null,         // 手续费 (BigDecimal版本)
    @Serializable(with = BigDecimalSerializer::class)
    val amountDecimal: BigDecimal? = null,      // 金额 (BigDecimal版本)
    
    @Contextual val time: LocalDateTime,
    val reason: String
) {
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
    fun withSyncedFields(): TradingOpportunity {
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

// 交易记录显示项（包含资产名称）
data class TransactionDisplayItem(
    val transaction: Transaction,
    val assetName: String
)