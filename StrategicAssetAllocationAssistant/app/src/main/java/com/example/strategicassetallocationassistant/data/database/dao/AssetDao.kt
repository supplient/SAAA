package com.example.strategicassetallocationassistant.data.database.dao

import androidx.room.*
import com.example.strategicassetallocationassistant.data.database.entities.AssetEntity
import kotlinx.coroutines.flow.Flow

/**
 * 资产表数据访问对象
 */
@Dao
interface AssetDao {
    
    /**
     * 获取所有资产（返回Flow以便观察数据变化）
     */
    @Query("SELECT * FROM assets")
    fun getAllAssets(): Flow<List<AssetEntity>>
    
    /**
     * 根据ID获取资产
     */
    @Query("SELECT * FROM assets WHERE id = :assetId")
    suspend fun getAssetById(assetId: String): AssetEntity?
    
    /**
     * 插入新资产
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: AssetEntity)
    
    /**
     * 插入多个资产
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<AssetEntity>)
    
    /**
     * 更新资产
     */
    @Update
    suspend fun updateAsset(asset: AssetEntity)
    
    /**
     * 删除资产
     */
    @Delete
    suspend fun deleteAsset(asset: AssetEntity)
    
    /**
     * 根据ID删除资产
     */
    @Query("DELETE FROM assets WHERE id = :assetId")
    suspend fun deleteAssetById(assetId: String)
    
    /**
     * 删除所有资产
     */
    @Query("DELETE FROM assets")
    suspend fun deleteAllAssets()
    
    /**
     * 获取资产总数
     */
    @Query("SELECT COUNT(*) FROM assets")
    suspend fun getAssetCount(): Int
}
