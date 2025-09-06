package com.example.strategicassetallocationassistant.data.database.dao

import androidx.room.*
import com.example.strategicassetallocationassistant.data.database.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * 交易记录 DAO
 */
@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY time DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    /**
     * Fetch a single transaction by primary key. Returned null if not found.
     */
    @Query("SELECT * FROM transactions WHERE id = :txId LIMIT 1")
    suspend fun getTransactionById(txId: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: TransactionEntity)

    @Update
    suspend fun updateTransaction(tx: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(tx: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :txId")
    suspend fun deleteById(txId: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
