package com.example.strategicassetallocationassistant

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    navToEdit: (java.util.UUID) -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactionsState.collectAsState()
    val scope = rememberCoroutineScope()
    
    // 清空确认对话框状态
    var showClearDialog by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(5) }
    var canConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("交易记录") },
                actions = {
                    // 只有当有交易记录时才显示清空按钮
                    if (transactions.isNotEmpty()) {
                        IconButton(
                            onClick = { 
                                showClearDialog = true
                                countdown = 5
                                canConfirm = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "清空所有交易记录",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
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
        
        // 清空确认对话框
        if (showClearDialog) {
            ClearTransactionsDialog(
                countdown = countdown,
                canConfirm = canConfirm,
                onConfirm = {
                    scope.launch {
                        viewModel.clearAllTransactions()
                        showClearDialog = false
                    }
                },
                onCancel = {
                    showClearDialog = false
                }
            )
            
            // 倒计时逻辑
            LaunchedEffect(showClearDialog) {
                if (showClearDialog) {
                    repeat(5) {
                        delay(1000)
                        countdown--
                    }
                    canConfirm = true
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

@Composable
private fun ClearTransactionsDialog(
    countdown: Int,
    canConfirm: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text("清空所有交易记录")
        },
        text = {
            Column {
                Text("此操作将删除所有交易记录，但不会影响您的资产份额和可用现金。")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ 此操作不可撤销！",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!canConfirm && countdown > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "请等待 $countdown 秒后确认",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = canConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("确认清空")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("取消")
            }
        }
    )
}
