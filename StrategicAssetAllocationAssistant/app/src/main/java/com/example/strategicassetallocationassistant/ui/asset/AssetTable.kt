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
import java.time.format.DateTimeFormatter

/**
 * 资产列表表格组件
 * 第一列（资产名称）固定，剩余列可横向滚动
 */
@Composable
fun AssetTable(
    analyses: List<PortfolioViewModel.AssetAnalysis>,
    isHidden: Boolean,
    onAddTransaction: (java.util.UUID) -> Unit,
    onEditAsset: (java.util.UUID) -> Unit,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()
    
    Column(modifier = modifier) {
        // 表头
        AssetTableHeader(
            isHidden = isHidden,
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
                    isHidden = isHidden,
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
    isHidden: Boolean,
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
                .width(80.dp)
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
            // 占比列（合并目标占比、当前占比、偏离度）
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "占比",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }

            // 买因卖阈
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "买因卖阈",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
            
            // 价份波
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "价份波",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
            
            // 市值列（合并目标市值、当前市值、市值偏离）
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

            // 更新时间
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "更新时间",
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
    isHidden: Boolean,
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
                .width(80.dp)
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
            // 占比列（显示：当前占比=目标占比±偏离度）
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${String.format("%.2f", analysis.currentWeight * 100)}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "= ${(analysis.asset.targetWeight * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${if (analysis.deviationPct >= 0) "+" else ""}${String.format("%.2f", analysis.deviationPct * 100)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (analysis.deviationPct >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 买因卖阈
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = analysis.buyFactor?.let { "🏷️${String.format("%.2f", it * 100)}" } ?: "-",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = analysis.sellThreshold?.let { "+${String.format("%.2f", it * analysis.asset.targetWeight * 100)}%卖" } ?: "-",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            
            // 价份波
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isHidden) "***" else "¥${String.format("%.4f", analysis.asset.unitValue ?: 0.0)}",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (isHidden) "***" else "×${String.format("%.2f", analysis.asset.shares ?: 0.0)}",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = analysis.volatility?.let { "〰️${String.format("%.2f%%", it * 100)}" } ?: "-",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // 市值列（显示：当前市值=目标市值±偏离市值）
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isHidden) "***" else "¥${String.format("%.2f", analysis.marketValue)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (isHidden) "***" else "= ¥${String.format("%.2f", analysis.targetMarketValue)}",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (isHidden) "***" else "${if (analysis.deviationValue >= 0) "+" else ""}¥${String.format("%.2f", analysis.deviationValue)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (analysis.deviationValue >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 更新时间（两行显示：时分秒 + 年月日）
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                analysis.asset.lastUpdateTime?.let { time ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = time.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            textAlign = TextAlign.Center
                        )
                    }
                } ?: Text(
                    text = "-",
                    style = MaterialTheme.typography.bodySmall,
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

