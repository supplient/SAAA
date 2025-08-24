package com.example.strategicassetallocationassistant.data.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
