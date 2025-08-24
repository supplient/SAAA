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
    navToAdd: () -> Unit,
    navToEdit: (java.util.UUID) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactionsState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("交易记录") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = navToAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add Tx")
            }
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
                items(transactions) { tx ->
                    TransactionRow(tx = tx, onClick = { navToEdit(tx.id) })
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(tx: Transaction, onClick: () -> Unit) {
    val dateStr = tx.time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (tx.type == TradeType.BUY) "买入" else "卖出",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (tx.type == TradeType.BUY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text(dateStr, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(4.dp))
            Text("份额: ${tx.shares}  价格: ${tx.price}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(2.dp))
            Text("金额: ${tx.amount}  手续费: ${tx.fee}", style = MaterialTheme.typography.bodySmall)
            tx.reason?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(2.dp))
                Text(text = "理由: $it", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
