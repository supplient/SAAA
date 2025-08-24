package com.example.strategicassetallocationassistant

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradingOpportunityListScreen(
    navController: NavController,
    viewModel: TradingOpportunityViewModel = hiltViewModel(),
    onExecute: (TradingOpportunity) -> Unit
) {
    val items by viewModel.opportunities.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("交易机会") },
                actions = {
                    IconButton(onClick = { viewModel.checkNow() }) { Text("检查") }
                    IconButton(onClick = { viewModel.clearAll() }) { Icon(Icons.Default.DeleteSweep, contentDescription = "清空") }
                }
            )
        }
    ) { inner ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text("暂无交易机会")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(inner)) {
                items(items) { op ->
                    OpportunityRow(op, onExecute = { onExecute(op) }, onDelete = { viewModel.deleteOne(op.id) })
                }
            }
        }
    }
}

@Composable
private fun OpportunityRow(op: TradingOpportunity, onExecute: () -> Unit, onDelete: () -> Unit) {
    val dateStr = op.time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (op.type == TradeType.BUY) "买入" else "卖出",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (op.type == TradeType.BUY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text(dateStr, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = op.reason,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDelete) { Text("删除") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onExecute) { Text("转为交易") }
            }
        }
    }
}


