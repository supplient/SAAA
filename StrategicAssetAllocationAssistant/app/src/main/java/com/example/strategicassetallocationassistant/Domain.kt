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

// 资产类型枚举
enum class AssetType {
    MONEY_FUND,      // 货币基金
    OFFSHORE_FUND,   // 场外基金
    STOCK            // 场内股票
}

// 持仓信息基类
@Serializable
abstract class Position() {
	abstract val code: String
	abstract @Contextual val lastUpdateTime: LocalDateTime
    // 计算当前市值（由具体子类实现）
    abstract fun calculateCurrentMarketValue(): Double
}

// 货币基金持仓信息
@Serializable
data class MoneyFundPosition(
    override val code: String,                   // 基金代码
    override @Contextual val lastUpdateTime: LocalDateTime, // 份额更新时间
    val shares: Double             // 份额
) : Position() {
    override fun calculateCurrentMarketValue(): Double = shares  // 货币基金份额直接等于市值
}

// 场外基金持仓信息
@Serializable
data class OffshoreFundPosition(
    override val code: String,                   // 基金代码
    override @Contextual val lastUpdateTime: LocalDateTime, // 净值更新时间
    val shares: Double,             // 份额
    val netValue: Double           // 净值
) : Position() {
    override fun calculateCurrentMarketValue(): Double = shares * netValue
}

// 场内股票持仓信息
@Serializable
data class StockPosition(
    override val code: String,                   // 股票代码
    override @Contextual val lastUpdateTime: LocalDateTime, // 市值更新时间
    val shares: Double,             // 份额（股数）
    val marketValue: Double        // 市值（每股价格）
) : Position() {
    override fun calculateCurrentMarketValue(): Double = shares * marketValue
}

// 资产数据模型
@Serializable
data class Asset(
    @Contextual val id: UUID,       // 资产ID (UUID)
    val name: String,               // 资产名称
    val type: AssetType,            // 资产类型
    val targetWeight: Double,       // 目标占比（0.0-1.0）
    val position: Position?         // 持仓信息
) {
    // 计算当前市场价值，直接转发调用Position的计算方法
    val currentMarketValue: Double
        get() = position?.calculateCurrentMarketValue() ?: 0.0
}

// 资产配置的顶层数据结构，包含所有资产和一个总的现金账户。
@Serializable
data class Portfolio(
    val assets: List<Asset>,
    val cash: Double
)