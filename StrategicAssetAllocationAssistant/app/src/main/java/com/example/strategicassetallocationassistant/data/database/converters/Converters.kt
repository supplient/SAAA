package com.example.strategicassetallocationassistant.data.database.converters

import androidx.room.TypeConverter
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Room数据库类型转换器
 * 用于转换Room不直接支持的数据类型
 */
class Converters {
    
    /**
     * LocalDateTime转换器
     */
    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? {
        return dateTime?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
    
    @TypeConverter
    fun toLocalDateTime(dateTimeString: String?): LocalDateTime? {
        return dateTimeString?.let {
            LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        }
    }
    
    /**
     * TradeType枚举转换器
     */
    @TypeConverter
    fun fromTradeType(tradeType: com.example.strategicassetallocationassistant.TradeType): String {
        return tradeType.name
    }

    @TypeConverter
    fun toTradeType(tradeTypeString: String): com.example.strategicassetallocationassistant.TradeType {
        return com.example.strategicassetallocationassistant.TradeType.valueOf(tradeTypeString)
    }
    
    /**
     * BigDecimal转换器
     * 将BigDecimal转换为String存储到数据库，保证精度不丢失
     */
    @TypeConverter
    fun fromBigDecimal(bigDecimal: BigDecimal?): String? {
        return bigDecimal?.toString()
    }

    @TypeConverter
    fun toBigDecimal(bigDecimalString: String?): BigDecimal? {
        return bigDecimalString?.let { 
            try {
                BigDecimal(it)
            } catch (e: NumberFormatException) {
                null
            }
        }
    }
}
