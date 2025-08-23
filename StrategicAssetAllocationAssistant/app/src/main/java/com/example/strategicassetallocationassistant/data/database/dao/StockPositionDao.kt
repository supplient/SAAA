package com.example.strategicassetallocationassistant.data.database.dao

import androidx.room.*
import com.example.strategicassetallocationassistant.data.database.entities.StockPositionEntity
import kotlinx.coroutines.flow.Flow

/**
 * 股票持仓表数据访问对象
 */
@Dao
interface StockPositionDao {
    
    /**
     * 获取所有股票持仓（返回Flow以便观察数据变化）
     */
    @Query("SELECT * FROM stock_positions")
    fun getAllStockPositions(): Flow<List<StockPositionEntity>>
    
    /**
     * 根据资产ID获取股票持仓
     */
    @Query("SELECT * FROM stock_positions WHERE assetId = :assetId")
    suspend fun getStockPositionByAssetId(assetId: String): StockPositionEntity?
    
    /**
     * 根据资产ID获取股票持仓（Flow版本）
     */
    @Query("SELECT * FROM stock_positions WHERE assetId = :assetId")
    fun getStockPositionByAssetIdFlow(assetId: String): Flow<StockPositionEntity?>
    
    /**
     * 插入股票持仓
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockPosition(position: StockPositionEntity)
    
    /**
     * 插入多个股票持仓
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockPositions(positions: List<StockPositionEntity>)
    
    /**
     * 更新股票持仓
     */
    @Update
    suspend fun updateStockPosition(position: StockPositionEntity)
    
    /**
     * 删除股票持仓
     */
    @Delete
    suspend fun deleteStockPosition(position: StockPositionEntity)
    
    /**
     * 根据资产ID删除股票持仓
     */
    @Query("DELETE FROM stock_positions WHERE assetId = :assetId")
    suspend fun deleteStockPositionByAssetId(assetId: String)
    
    /**
     * 删除所有股票持仓
     */
    @Query("DELETE FROM stock_positions")
    suspend fun deleteAllStockPositions()
    
    /**
     * 更新股票的市值和更新时间
     */
    @Query("UPDATE stock_positions SET marketValue = :marketValue, lastUpdateTime = :lastUpdateTime WHERE assetId = :assetId")
    suspend fun updateMarketValue(assetId: String, marketValue: Double, lastUpdateTime: java.time.LocalDateTime)
}
