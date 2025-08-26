package com.example.strategicassetallocationassistant

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    val itemsWithAssets by viewModel.opportunitiesWithAssets.collectAsState()
    val reasoningLog by viewModel.reasoningLog.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("交易机会") },
                actions = {
                    IconButton(onClick = { viewModel.clearAll() }) { Icon(Icons.Default.DeleteSweep, contentDescription = "清空") }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { viewModel.checkBuy() }) { Text("检查买") }
                Button(onClick = { viewModel.checkSell() }) { Text("检查卖") }
            }
        }
    ) { inner ->
        // 弹出算法思考过程对话框
        if (reasoningLog != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearReasoningLog() },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearReasoningLog() }) { Text("确定") }
                },
                title = { Text("算法思考过程") },
                text = {
                    Box(Modifier.heightIn(min = 100.dp, max = 400.dp).verticalScroll(rememberScrollState())) {
                        Text(reasoningLog ?: "")
                    }
                }
            )
        }
        if (itemsWithAssets.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text("暂无交易机会")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(inner)) {
                items(itemsWithAssets) { itemWithAsset ->
                    OpportunityRow(
                        itemWithAsset = itemWithAsset, 
                        onExecute = { onExecute(itemWithAsset.opportunity) }, 
                        onDelete = { viewModel.deleteOne(itemWithAsset.opportunity.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OpportunityRow(
    itemWithAsset: TradingOpportunityViewModel.TradingOpportunityWithAsset, 
    onExecute: () -> Unit, 
    onDelete: () -> Unit
) {
    val op = itemWithAsset.opportunity
    val dateStr = op.time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // 交易类型和时间
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (op.type == TradeType.BUY) "买入" else "卖出",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (op.type == TradeType.BUY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text(dateStr, style = MaterialTheme.typography.bodySmall)
            }
            
            Spacer(Modifier.height(8.dp))
            
            // 资产信息
            itemWithAsset.assetName?.let { assetName ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "资产名称: $assetName",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                    itemWithAsset.assetType?.let { assetType ->
                        Text(
                            text = when (assetType) {
                                AssetType.MONEY_FUND -> "货币基金"
                                AssetType.OFFSHORE_FUND -> "场外基金"
                                AssetType.STOCK -> "股票"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(4.dp))
            
            // 交易份额和金额
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "交易份额: ${String.format("%.2f", op.shares)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "交易金额: ¥${String.format("%.2f", op.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
            }
            
            Spacer(Modifier.height(4.dp))
            
            // 交易价格和费用
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "交易价格: ¥${String.format("%.2f", op.price)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "交易费用: ¥${String.format("%.2f", op.fee)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            // 交易理由
            Text(
                text = "交易理由: ${op.reason}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(Modifier.height(12.dp))
            
            // 操作按钮
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDelete) { Text("删除") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onExecute) { Text("转为交易") }
            }
        }
    }
}


