package com.example.strategicassetallocationassistant.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.strategicassetallocationassistant.AssetType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.serialization.Serializable
import com.example.strategicassetallocationassistant.data.network.LocalDateTimeSerializer

/**
 * 资产表实体类
 * 对应 assets 表：id(UUID), name, type, targetWeight
 */
@Serializable
@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey
    val id: String,           // UUID转换为String存储
    val name: String,         // 资产名称
    val type: AssetType,      // 资产类型
    val targetWeight: Double,  // 目标占比

    // 持仓信息
    val code: String?,
    val shares: Double?,
    val unitValue: Double?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val lastUpdateTime: java.time.LocalDateTime?,
    val note: String? = null
) {
    companion object {
        /**
         * 从UUID创建Entity的辅助方法
         */
        fun create(
            id: UUID,
            name: String,
            type: AssetType,
            targetWeight: Double,
            code: String?,
            shares: Double?,
            unitValue: Double?,
            lastUpdateTime: LocalDateTime?,
            note: String? = null
        ): AssetEntity {
            return AssetEntity(
                id = id.toString(),
                name = name,
                type = type,
                targetWeight = targetWeight,
                code = code,
                shares = shares,
                unitValue = unitValue,
                lastUpdateTime = lastUpdateTime,
                note = note
            )
        }
    }
    
    /**
     * 获取UUID格式的ID
     */
    fun getUUID(): UUID = UUID.fromString(id)
}
