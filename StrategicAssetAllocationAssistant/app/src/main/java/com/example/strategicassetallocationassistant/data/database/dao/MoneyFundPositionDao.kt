package com.example.strategicassetallocationassistant.data.database.dao

import androidx.room.*
import com.example.strategicassetallocationassistant.data.database.entities.MoneyFundPositionEntity
import kotlinx.coroutines.flow.Flow

/**
 * 货币基金持仓表数据访问对象
 */
@Dao
interface MoneyFundPositionDao {
    
    /**
     * 获取所有货币基金持仓（返回Flow以便观察数据变化）
     */
    @Query("SELECT * FROM money_fund_positions")
    fun getAllMoneyFundPositions(): Flow<List<MoneyFundPositionEntity>>
    
    /**
     * 根据资产ID获取货币基金持仓
     */
    @Query("SELECT * FROM money_fund_positions WHERE assetId = :assetId")
    suspend fun getMoneyFundPositionByAssetId(assetId: String): MoneyFundPositionEntity?
    
    /**
     * 根据资产ID获取货币基金持仓（Flow版本）
     */
    @Query("SELECT * FROM money_fund_positions WHERE assetId = :assetId")
    fun getMoneyFundPositionByAssetIdFlow(assetId: String): Flow<MoneyFundPositionEntity?>
    
    /**
     * 插入货币基金持仓
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoneyFundPosition(position: MoneyFundPositionEntity)
    
    /**
     * 插入多个货币基金持仓
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoneyFundPositions(positions: List<MoneyFundPositionEntity>)
    
    /**
     * 更新货币基金持仓
     */
    @Update
    suspend fun updateMoneyFundPosition(position: MoneyFundPositionEntity)
    
    /**
     * 删除货币基金持仓
     */
    @Delete
    suspend fun deleteMoneyFundPosition(position: MoneyFundPositionEntity)
    
    /**
     * 根据资产ID删除货币基金持仓
     */
    @Query("DELETE FROM money_fund_positions WHERE assetId = :assetId")
    suspend fun deleteMoneyFundPositionByAssetId(assetId: String)
    
    /**
     * 删除所有货币基金持仓
     */
    @Query("DELETE FROM money_fund_positions")
    suspend fun deleteAllMoneyFundPositions()
    
    /**
     * 更新货币基金的份额和更新时间
     */
    @Query("UPDATE money_fund_positions SET shares = :shares, lastUpdateTime = :lastUpdateTime WHERE assetId = :assetId")
    suspend fun updateShares(assetId: String, shares: Double, lastUpdateTime: java.time.LocalDateTime)
}
