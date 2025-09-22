package com.example.strategicassetallocationassistant.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.serialization.Serializable
import com.example.strategicassetallocationassistant.data.network.LocalDateTimeSerializer
import com.example.strategicassetallocationassistant.data.network.BigDecimalSerializer

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
    
    // 新增BigDecimal字段 (步骤2: 双字段过渡)
    @Serializable(with = BigDecimalSerializer::class)
    val sharesDecimal: BigDecimal?,
    @Serializable(with = BigDecimalSerializer::class) 
    val unitValueDecimal: BigDecimal?,
    
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
                targetWeight = targetWeight,
                code = code,
                shares = shares,
                unitValue = unitValue,
                // 同步创建BigDecimal字段
                sharesDecimal = shares?.let { BigDecimal.valueOf(it) },
                unitValueDecimal = unitValue?.let { BigDecimal.valueOf(it) },
                lastUpdateTime = lastUpdateTime,
                note = note
            )
        }
        
        /**
         * 从BigDecimal创建Entity的新方法
         */
        fun createWithDecimal(
            id: UUID,
            name: String,
            targetWeight: Double,
            code: String?,
            sharesDecimal: BigDecimal?,
            unitValueDecimal: BigDecimal?,
            lastUpdateTime: LocalDateTime?,
            note: String? = null
        ): AssetEntity {
            return AssetEntity(
                id = id.toString(),
                name = name,
                targetWeight = targetWeight,
                code = code,
                // 同步创建Double字段 (向后兼容)
                shares = sharesDecimal?.toDouble(),
                unitValue = unitValueDecimal?.toDouble(),
                sharesDecimal = sharesDecimal,
                unitValueDecimal = unitValueDecimal,
                lastUpdateTime = lastUpdateTime,
                note = note
            )
        }
    }
    
    /**
     * 获取UUID格式的ID
     */
    fun getUUID(): UUID = UUID.fromString(id)
    
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
    fun getCurrentMarketValueDecimal(): BigDecimal {
        val shares = getSharesValue() ?: return BigDecimal.ZERO
        val unitValue = getUnitValueValue() ?: return BigDecimal.ZERO
        return shares.multiply(unitValue)
    }
    
    /**
     * 同步数据到双字段，确保数据一致性
     */
    fun withSyncedFields(): AssetEntity {
        // 如果BigDecimal字段存在，用它更新Double字段
        // 如果BigDecimal字段不存在，用Double字段创建BigDecimal字段
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
