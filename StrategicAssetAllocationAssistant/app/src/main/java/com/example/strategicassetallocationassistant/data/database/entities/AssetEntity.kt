package com.example.strategicassetallocationassistant.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.serialization.Serializable
import com.example.strategicassetallocationassistant.data.network.LocalDateTimeSerializer

/**
 * 资产表实体类
 * 对应 assets 表：id(UUID), name, targetWeight
 */
@Serializable
@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey
    val id: String,           // UUID转换为String存储
    val name: String,         // 资产名称
    val targetWeight: Double,  // 目标占比

    // 持仓信息
    val code: String?,
    val shares: Double?,
    val unitValue: Double?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val lastUpdateTime: java.time.LocalDateTime?,
    val note: String? = null,
    val volatility: Double? = null,
    val sevenDayReturn: Double? = null,
    val offsetFactor: Double? = null,
    val drawdownFactor: Double? = null,
    val buyFactor: Double? = null
) {
    companion object {
        /**
         * 从UUID创建Entity的辅助方法
         */
        fun create(
            id: UUID,
            name: String,
            targetWeight: Double,
            code: String?,
            shares: Double?,
            unitValue: Double?,
            lastUpdateTime: LocalDateTime?,
            note: String? = null,
            volatility: Double? = null,
            sevenDayReturn: Double? = null,
            offsetFactor: Double? = null,
            drawdownFactor: Double? = null,
            buyFactor: Double? = null
        ): AssetEntity {
            return AssetEntity(
                id = id.toString(),
                name = name,
                targetWeight = targetWeight,
                code = code,
                shares = shares,
                unitValue = unitValue,
                lastUpdateTime = lastUpdateTime,
                note = note,
                volatility = volatility,
                sevenDayReturn = sevenDayReturn,
                offsetFactor = offsetFactor,
                drawdownFactor = drawdownFactor,
                buyFactor = buyFactor
            )
        }
    }
    
    /**
     * 获取UUID格式的ID
     */
    fun getUUID(): UUID = UUID.fromString(id)
}
