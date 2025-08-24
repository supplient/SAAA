package com.example.strategicassetallocationassistant.data.database.dao

import androidx.room.*
import com.example.strategicassetallocationassistant.data.database.entities.TradingOpportunityEntity
import kotlinx.coroutines.flow.Flow

/**
 * 交易机会 DAO
 */
@Dao
interface TradingOpportunityDao {
    @Query("SELECT * FROM trading_opportunities ORDER BY time DESC")
    fun getAll(): Flow<List<TradingOpportunityEntity>>

    @Query("SELECT * FROM trading_opportunities WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TradingOpportunityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(opportunity: TradingOpportunityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(opportunities: List<TradingOpportunityEntity>)

    @Delete
    suspend fun delete(opportunity: TradingOpportunityEntity)

    @Query("DELETE FROM trading_opportunities WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM trading_opportunities")
    suspend fun deleteAll()
}


