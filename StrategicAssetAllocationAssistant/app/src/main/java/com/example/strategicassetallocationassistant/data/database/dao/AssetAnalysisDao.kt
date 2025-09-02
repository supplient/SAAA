package com.example.strategicassetallocationassistant.data.database.dao

import androidx.room.*
import com.example.strategicassetallocationassistant.data.database.entities.AssetAnalysisEntity
import kotlinx.coroutines.flow.Flow

/**
 * 资产分析表数据访问对象
 */
@Dao
interface AssetAnalysisDao {
    
    /**
     * 获取所有资产分析数据（返回Flow以便观察数据变化）
     */
    @Query("SELECT * FROM asset_analysis")
    fun getAllAssetAnalysis(): Flow<List<AssetAnalysisEntity>>
    
    /**
     * 根据资产ID获取分析数据
     */
    @Query("SELECT * FROM asset_analysis WHERE assetId = :assetId")
    suspend fun getAssetAnalysisById(assetId: String): AssetAnalysisEntity?
    
    /**
     * 根据资产ID获取分析数据（Flow版本）
     */
    @Query("SELECT * FROM asset_analysis WHERE assetId = :assetId")
    fun getAssetAnalysisByIdFlow(assetId: String): Flow<AssetAnalysisEntity?>
    
    /**
     * 插入新的资产分析数据
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssetAnalysis(assetAnalysis: AssetAnalysisEntity)
    
    /**
     * 插入多个资产分析数据
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssetAnalysisList(assetAnalysisList: List<AssetAnalysisEntity>)
    
    /**
     * 更新资产分析数据
     */
    @Update
    suspend fun updateAssetAnalysis(assetAnalysis: AssetAnalysisEntity)
    
    /**
     * 删除资产分析数据
     */
    @Delete
    suspend fun deleteAssetAnalysis(assetAnalysis: AssetAnalysisEntity)
    
    /**
     * 根据资产ID删除分析数据
     */
    @Query("DELETE FROM asset_analysis WHERE assetId = :assetId")
    suspend fun deleteAssetAnalysisById(assetId: String)
    
    /**
     * 删除所有资产分析数据
     */
    @Query("DELETE FROM asset_analysis")
    suspend fun deleteAllAssetAnalysis()
    
    /**
     * 获取资产分析数据总数
     */
    @Query("SELECT COUNT(*) FROM asset_analysis")
    suspend fun getAssetAnalysisCount(): Int

    /**
     * 批量更新买入因子
     */
    @Query("UPDATE asset_analysis SET buyFactor = :buyFactor WHERE assetId = :assetId")
    suspend fun updateBuyFactor(assetId: String, buyFactor: Double)

    /**
     * 批量更新卖出阈值
     */
    @Query("UPDATE asset_analysis SET sellThreshold = :sellThreshold WHERE assetId = :assetId")
    suspend fun updateSellThreshold(assetId: String, sellThreshold: Double)

    /**
     * 更新市场数据（波动率和七日收益率）
     */
    @Query("UPDATE asset_analysis SET volatility = :volatility, sevenDayReturn = :sevenDayReturn, lastUpdateTime = :lastUpdateTime WHERE assetId = :assetId")
    suspend fun updateMarketData(assetId: String, volatility: Double?, sevenDayReturn: Double?, lastUpdateTime: java.time.LocalDateTime)
}
