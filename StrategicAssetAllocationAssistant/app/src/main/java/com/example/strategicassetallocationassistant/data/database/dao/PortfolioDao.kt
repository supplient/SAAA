package com.example.strategicassetallocationassistant.data.database.dao

import androidx.room.*
import com.example.strategicassetallocationassistant.data.database.entities.PortfolioEntity
import kotlinx.coroutines.flow.Flow

/**
 * 投资组合表数据访问对象
 */
@Dao
interface PortfolioDao {
    
    /**
     * 获取投资组合信息（返回Flow以便观察数据变化）
     * 因为只有一条记录，所以直接返回单个对象
     */
    @Query("SELECT * FROM portfolio WHERE id = 1")
    fun getPortfolio(): Flow<PortfolioEntity?>
    
    /**
     * 获取投资组合信息（suspend版本）
     */
    @Query("SELECT * FROM portfolio WHERE id = 1")
    suspend fun getPortfolioSuspend(): PortfolioEntity?
    
    /**
     * 插入或更新投资组合信息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPortfolio(portfolio: PortfolioEntity)
    
    /**
     * 更新投资组合信息
     */
    @Update
    suspend fun updatePortfolio(portfolio: PortfolioEntity)
    
    /**
     * 更新现金金额
     */
    @Query("UPDATE portfolio SET cash = :cash WHERE id = 1")
    suspend fun updateCash(cash: Double)

    /** 更新整体资产配置备注 */
    @Query("UPDATE portfolio SET note = :note WHERE id = 1")
    suspend fun updateNote(note: String?)

    /** 更新是否顺延标记 */
    @Query("UPDATE portfolio SET isBuyOpportunityPostponed = :flag WHERE id = 1")
    suspend fun updateBuyOpportunityPostponed(flag: Boolean)

    /** 获取是否顺延标记 */
    @Query("SELECT isBuyOpportunityPostponed FROM portfolio WHERE id = 1")
    suspend fun getBuyOpportunityPostponed(): Boolean?

    /**
     * 更新上一次买入机会检查时间
     */
    @Query("UPDATE portfolio SET lastBuyOpportunityCheck = :time WHERE id = 1")
    suspend fun updateLastBuyOpportunityCheck(time: java.time.LocalDateTime?)

    /**
     * 获取上一次买入机会检查时间
     */
    @Query("SELECT lastBuyOpportunityCheck FROM portfolio WHERE id = 1")
    suspend fun getLastBuyOpportunityCheck(): java.time.LocalDateTime?
    
    /**
     * 删除投资组合信息
     */
    @Query("DELETE FROM portfolio WHERE id = 1")
    suspend fun deletePortfolio()
    
    /**
     * 获取当前现金金额
     */
    @Query("SELECT cash FROM portfolio WHERE id = 1")
    suspend fun getCash(): Double?
    
    /**
     * 获取当前现金金额（Flow版本）
     */
    @Query("SELECT cash FROM portfolio WHERE id = 1")
    fun getCashFlow(): Flow<Double?>
}
