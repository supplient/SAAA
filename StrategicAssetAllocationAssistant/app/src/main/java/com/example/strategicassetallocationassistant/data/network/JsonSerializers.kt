package com.example.strategicassetallocationassistant.data.network

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalSerializationApi::class)
object LocalDateTimeSerializer : KSerializer<LocalDateTime?> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime?", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime?) {
        if (value != null) {
            encoder.encodeString(value.format(formatter))
        } else {
            encoder.encodeNull()
        }
    }

    override fun deserialize(decoder: Decoder): LocalDateTime? {
        return if (decoder.decodeNotNullMark()) {
            try {
                LocalDateTime.parse(decoder.decodeString(), formatter)
            } catch (e: Exception) {
                null
            }
        } else {
            decoder.decodeNull()
            null
        }
    }
}

/**
 * BigDecimal序列化器
 * 将BigDecimal序列化为字符串，保证精度不丢失
 */
@OptIn(ExperimentalSerializationApi::class)
object BigDecimalSerializer : KSerializer<BigDecimal?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimal?", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal?) {
        if (value != null) {
            encoder.encodeString(value.toString())
        } else {
            encoder.encodeNull()
        }
    }

    override fun deserialize(decoder: Decoder): BigDecimal? {
        return if (decoder.decodeNotNullMark()) {
            try {
                BigDecimal(decoder.decodeString())
            } catch (e: Exception) {
                null
            }
        } else {
            decoder.decodeNull()
            null
        }
    }
}

/**
 * 非空BigDecimal序列化器
 */
@OptIn(ExperimentalSerializationApi::class)
object BigDecimalNotNullSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        return BigDecimal(decoder.decodeString())
    }
}
