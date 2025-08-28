package com.example.strategicassetallocationassistant.data.database.converters

import androidx.room.TypeConverter
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
}
