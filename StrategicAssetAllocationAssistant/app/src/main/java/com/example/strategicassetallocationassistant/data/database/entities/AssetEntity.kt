package com.example.strategicassetallocationassistant.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.strategicassetallocationassistant.AssetType
import java.util.UUID

/**
 * 资产表实体类
 * 对应 assets 表：id(UUID), name, type, targetWeight
 */
@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey
    val id: String,           // UUID转换为String存储
    val name: String,         // 资产名称
    val type: AssetType,      // 资产类型
    val targetWeight: Double  // 目标占比
) {
    companion object {
        /**
         * 从UUID创建Entity的辅助方法
         */
        fun create(
            id: UUID,
            name: String,
            type: AssetType,
            targetWeight: Double
        ): AssetEntity {
            return AssetEntity(
                id = id.toString(),
                name = name,
                type = type,
                targetWeight = targetWeight
            )
        }
    }
    
    /**
     * 获取UUID格式的ID
     */
    fun getUUID(): UUID = UUID.fromString(id)
}
