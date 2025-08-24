@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.strategicassetallocationassistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 显示单个资产的组件
@Composable
fun AssetItem(
    analysis: PortfolioViewModel.AssetAnalysis,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // 资产名称和类型
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = analysis.asset.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = when (analysis.asset.type) {
                        AssetType.MONEY_FUND -> "货币基金"
                        AssetType.OFFSHORE_FUND -> "场外基金"
                        AssetType.STOCK -> "股票"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 持仓信息显示
            analysis.asset.code?.let {
                Text("资产代码: $it", style = MaterialTheme.typography.bodyMedium)
            }
            analysis.asset.shares?.let {
                Text("份额: $it", style = MaterialTheme.typography.bodyMedium)
            }
            when (analysis.asset.type) {
                AssetType.STOCK -> analysis.asset.unitValue?.let {
                    Text("每股价格: ¥$it", style = MaterialTheme.typography.bodyMedium)
                }
                AssetType.OFFSHORE_FUND -> analysis.asset.unitValue?.let {
                    Text("净值: $it", style = MaterialTheme.typography.bodyMedium)
                }
                else -> {}
            }
            analysis.asset.lastUpdateTime?.let {
                Text("更新时间: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 目标占比和市场价值
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "目标占比: ${(analysis.asset.targetWeight * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "市值: ¥${String.format("%.2f", analysis.marketValue)}", // 使用分析中的市值
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            // 当前占比和偏离度
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "当前占比: ${(analysis.currentWeight * 100).let { String.format("%.2f", it) }}%",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "偏离: ${(analysis.deviationPct * 100).let { String.format("%.2f", it) }}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (analysis.deviationPct >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            // 目标市值与偏离市值
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "目标市值: ¥${String.format("%.2f", analysis.targetMarketValue)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "偏离市值: ¥${String.format("%.2f", analysis.deviationValue)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (analysis.deviationValue >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// 主屏幕组件
@Composable
fun AssetListScreen(
    viewModel: PortfolioViewModel,
    modifier: Modifier = Modifier,
    onAddAsset: () -> Unit = {},
    onEditAsset: (java.util.UUID) -> Unit = {},
    onOpenApiTest: () -> Unit = {}
) {
    val portfolio by viewModel.portfolioState.collectAsState() // 观察顶层Portfolio状态
    val analyses by viewModel.assetAnalyses.collectAsState()

    // 旧 assetId2Value 不再需要

    Box(modifier = modifier.fillMaxSize()) {
        // TopAppBar with Refresh
        androidx.compose.material3.TopAppBar(
            title = { Text("战略资产配置助手") },
            navigationIcon = {
                IconButton(onClick = onOpenApiTest) {
                    Icon(Icons.Default.BugReport, contentDescription = "API 测试")
                }
            },
            actions = {
                IconButton(onClick = { viewModel.refreshMarketData() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(56.dp)) // space for top bar

            // 显示总现金
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "可用现金",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "¥${String.format("%.2f", portfolio.cash)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }


            // 资产列表
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(analyses) { analysis ->
                    AssetItem(
                        analysis = analysis,
                        onClick = { onEditAsset(analysis.asset.id) },
                        modifier = Modifier,
                    )
                }
            }
        }

        // FloatingActionButton
        FloatingActionButton(
            onClick = onAddAsset,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add Asset"
            )
        }
    }
}
