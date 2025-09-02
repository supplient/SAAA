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

// Serialization module for LocalDateTime and UUID
val serializationModule = SerializersModule {
    contextual(LocalDateTimeSerializer)
    contextual(UUIDSerializer)
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
    @Contextual val lastUpdateTime: LocalDateTime? = null,  // 数据最后更新时间
    val note: String? = null                     // 备注
) {
    // 计算当前市场价值
    val currentMarketValue: Double
        get() = (shares ?: 0.0) * (unitValue ?: 0.0)
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
    @Contextual val lastUpdateTime: LocalDateTime? = null  // 分析数据最后更新时间
)

// 资产配置的顶层数据结构，包含所有资产和一个总的现金账户。
@Serializable
data class Portfolio(
    val assets: List<Asset>,
    val cash: Double,
    val note: String? = null,
    val overallRiskFactor: Double? = null,
    val overallRiskFactorLog: String? = null    // 总体风险因子计算过程日志
)

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
    @Contextual val time: LocalDateTime,
    val reason: String? = null
)

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
    @Contextual val time: LocalDateTime,
    val reason: String
)