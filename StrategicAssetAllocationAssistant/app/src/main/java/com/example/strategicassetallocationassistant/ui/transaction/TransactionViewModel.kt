package com.example.strategicassetallocationassistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel that exposes all transaction records for UI.
 */
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: com.example.strategicassetallocationassistant.data.repository.PortfolioRepository
) : ViewModel() {

    val transactionsState: StateFlow<List<TransactionDisplayItem>> = combine(
        repository.transactionsFlow,
        repository.assetsFlow
    ) { transactions, assets ->
        transactions.map { transaction ->
            val assetName = transaction.assetId?.let { assetId ->
                assets.firstOrNull { it.id == assetId }?.name ?: "未知资产"
            } ?: "现金交易"
            TransactionDisplayItem(transaction, assetName)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    suspend fun deleteTransaction(tx: Transaction) {
        repository.deleteTransaction(tx)
    }

    /**
     * 清空所有交易记录，但不影响资产份额和现金
     */
    suspend fun clearAllTransactions() {
        repository.clearAllTransactions()
    }
}
