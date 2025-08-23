package com.example.strategicassetallocationassistant.data.database.dao

import androidx.room.*
import com.example.strategicassetallocationassistant.data.database.entities.OffshoreFundPositionEntity
import kotlinx.coroutines.flow.Flow

/**
 * 场外基金持仓表数据访问对象
 */
@Dao
interface OffshoreFundPositionDao {
    
    /**
     * 获取所有场外基金持仓（返回Flow以便观察数据变化）
     */
    @Query("SELECT * FROM offshore_fund_positions")
    fun getAllOffshoreFundPositions(): Flow<List<OffshoreFundPositionEntity>>
    
    /**
     * 根据资产ID获取场外基金持仓
     */
    @Query("SELECT * FROM offshore_fund_positions WHERE assetId = :assetId")
    suspend fun getOffshoreFundPositionByAssetId(assetId: String): OffshoreFundPositionEntity?
    
    /**
     * 根据资产ID获取场外基金持仓（Flow版本）
     */
    @Query("SELECT * FROM offshore_fund_positions WHERE assetId = :assetId")
    fun getOffshoreFundPositionByAssetIdFlow(assetId: String): Flow<OffshoreFundPositionEntity?>
    
    /**
     * 插入场外基金持仓
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffshoreFundPosition(position: OffshoreFundPositionEntity)
    
    /**
     * 插入多个场外基金持仓
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOffshoreFundPositions(positions: List<OffshoreFundPositionEntity>)
    
    /**
     * 更新场外基金持仓
     */
    @Update
    suspend fun updateOffshoreFundPosition(position: OffshoreFundPositionEntity)
    
    /**
     * 删除场外基金持仓
     */
    @Delete
    suspend fun deleteOffshoreFundPosition(position: OffshoreFundPositionEntity)
    
    /**
     * 根据资产ID删除场外基金持仓
     */
    @Query("DELETE FROM offshore_fund_positions WHERE assetId = :assetId")
    suspend fun deleteOffshoreFundPositionByAssetId(assetId: String)
    
    /**
     * 删除所有场外基金持仓
     */
    @Query("DELETE FROM offshore_fund_positions")
    suspend fun deleteAllOffshoreFundPositions()
    
    /**
     * 更新场外基金的净值和更新时间
     */
    @Query("UPDATE offshore_fund_positions SET netValue = :netValue, lastUpdateTime = :lastUpdateTime WHERE assetId = :assetId")
    suspend fun updateNetValue(assetId: String, netValue: Double, lastUpdateTime: java.time.LocalDateTime)
}
