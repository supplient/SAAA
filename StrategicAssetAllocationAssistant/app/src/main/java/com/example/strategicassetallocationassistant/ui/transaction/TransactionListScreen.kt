package com.example.strategicassetallocationassistant

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    navToEdit: (java.util.UUID) -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactionsState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("交易记录") })
        }
    ) { inner ->
        if (transactions.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text("暂无交易记录")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
            ) {
                items(transactions) { displayItem ->
                    TransactionRow(displayItem = displayItem, onClick = { navToEdit(displayItem.transaction.id) })
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(displayItem: TransactionDisplayItem, onClick: () -> Unit) {
    val tx = displayItem.transaction
    val dateStr = tx.time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    
    // 计算总额（包含手续费）
    val totalAmount = tx.amount + tx.fee
    val totalAmountStr = String.format("%.2f", totalAmount)
    val amountPrefix = if (tx.type == TradeType.BUY) "-" else "+"
    
    // 格式化数值
    val sharesStr = String.format("%.2f", tx.shares)
    val priceStr = String.format("%.2f", tx.price)
    val feeStr = String.format("%.2f", tx.fee)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // 第一行：资产名称（左对齐）+ 交易时间（右对齐，较小字体）
            Row(
                horizontalArrangement = Arrangement.SpaceBetween, 
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = displayItem.assetName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = dateStr, 
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            // 第二行：<买/卖><份额>x<单价>+<手续费>=<+/-总额>
            val tradeTypeText = if (tx.type == TradeType.BUY) "买" else "卖"
            val tradeTypeColor = if (tx.type == TradeType.BUY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            
            Text(
                text = "${tradeTypeText}${sharesStr}x${priceStr}+${feeStr}=${amountPrefix}${totalAmountStr}",
                style = MaterialTheme.typography.bodyMedium,
                color = tradeTypeColor
            )
            
            Spacer(Modifier.height(4.dp))
            
            // 第三行：交易理由（单行，较小字体，不允许换行）
            tx.reason?.takeIf { it.isNotBlank() }?.let { reason ->
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}
