package com.example.strategicassetallocationassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable

/**
 * 资产列表表格组件
 * 第一列（资产名称）固定，剩余列可横向滚动
 */
@Composable
fun AssetTable(
    analyses: List<PortfolioViewModel.AssetAnalysis>,
    onAddTransaction: (java.util.UUID) -> Unit,
    onEditAsset: (java.util.UUID) -> Unit,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()
    
    Column(modifier = modifier) {
        // 表头
        AssetTableHeader(
            horizontalScrollState = horizontalScrollState,
            modifier = Modifier.fillMaxWidth()
        )
        
        // 数据行
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(analyses) { analysis ->
                AssetTableRow(
                    analysis = analysis,
                    horizontalScrollState = horizontalScrollState,
                    onAddTransaction = { onAddTransaction(analysis.asset.id) },
                    onEditAsset = { onEditAsset(analysis.asset.id) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 表格表头组件
 */
@Composable
private fun AssetTableHeader(
    horizontalScrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(8.dp)
    ) {
        // 第一列 - 资产名称（固定）
        Box(
            modifier = Modifier
                .width(120.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "资产名称",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        // 剩余列（可横向滚动）
        Row(
            modifier = Modifier
                .horizontalScroll(horizontalScrollState)
                .padding(start = 8.dp)
        ) {
            // 资产类型
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "类型",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
            
            // 目标占比
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "目标占比",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
            
            // 当前占比
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "当前占比",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
            
            // 偏离度
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "偏离度",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
            
            // 市值
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "市值",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
            
            // 目标市值
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "目标市值",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
            
            // 偏离市值
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "偏离市值",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }

            // 备注
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "备注",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * 表格数据行组件
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun AssetTableRow(
    analysis: PortfolioViewModel.AssetAnalysis,
    horizontalScrollState: androidx.compose.foundation.ScrollState,
    onAddTransaction: () -> Unit,
    onEditAsset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                if (analysis.isRefreshFailed) 
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else 
                    Color.Transparent
            )
            .padding(8.dp)
            .combinedClickable(onClick = onAddTransaction, onLongClick = onEditAsset)
    ) {
        // 第一列 - 资产名称（固定）
        Box(
            modifier = Modifier
                .width(120.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = analysis.asset.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (analysis.isRefreshFailed) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "刷新失败",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        // 剩余列（可横向滚动）
        Row(
            modifier = Modifier
                .horizontalScroll(horizontalScrollState)
                .padding(start = 8.dp)
        ) {
            // 资产类型
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (analysis.asset.type) {
                        AssetType.MONEY_FUND -> "货币基金"
                        AssetType.OFFSHORE_FUND -> "场外基金"
                        AssetType.STOCK -> "股票"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
            
            // 目标占比
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${(analysis.asset.targetWeight * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
            
            // 当前占比
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${String.format("%.2f", analysis.currentWeight * 100)}%",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
            
            // 偏离度
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${String.format("%.2f", analysis.deviationPct * 100)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (analysis.deviationPct >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
            
            // 市值
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "¥${String.format("%.2f", analysis.marketValue)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
            
            // 目标市值
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "¥${String.format("%.2f", analysis.targetMarketValue)}",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
            
            // 偏离市值
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "¥${String.format("%.2f", analysis.deviationValue)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (analysis.deviationValue >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            // 备注
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = analysis.asset.note ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

