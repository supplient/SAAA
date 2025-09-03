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
 * 资产分析表格组件
 * 专门用于显示资产分析数据：占比、七日涨跌幅、波动率、买入因子、卖出阈值
 * 第一列（资产名称）固定，剩余列可横向滚动
 */
@Composable
fun AssetAnalysisTable(
    analyses: List<PortfolioViewModel.AssetAnalysis>,
    onEditAsset: (java.util.UUID) -> Unit,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()
    
    Column(modifier = modifier) {
        // 表头
        AssetAnalysisTableHeader(
            horizontalScrollState = horizontalScrollState,
            modifier = Modifier.fillMaxWidth()
        )
        
        // 数据行
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(analyses) { analysis ->
                AssetAnalysisTableRow(
                    analysis = analysis,
                    horizontalScrollState = horizontalScrollState,
                    onEditAsset = { onEditAsset(analysis.asset.id) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 资产分析表格表头组件
 */
@Composable
private fun AssetAnalysisTableHeader(
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
            // 占比列
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
            
            // 七日涨跌幅列
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "七日涨跌幅",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }

            // 波动率列
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "波动率",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }

            // 买入因子列
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "买入因子",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }

            // 卖出阈值列
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "卖出阈值",
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
 * 资产分析表格数据行组件
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun AssetAnalysisTableRow(
    analysis: PortfolioViewModel.AssetAnalysis,
    horizontalScrollState: androidx.compose.foundation.ScrollState,
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
            .combinedClickable(onLongClick = onEditAsset, onClick = {})
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
            
            // 七日涨跌幅列
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                val sevenDayReturn = analysis.sevenDayReturn
                if (sevenDayReturn != null) {
                    Text(
                        text = "${if (sevenDayReturn >= 0) "+" else ""}${String.format("%.2f", sevenDayReturn * 100)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (sevenDayReturn >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "-",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 波动率列
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = analysis.volatility?.let { String.format("%.2f%%", it * 100) } ?: "-",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            // 买入因子列
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = analysis.buyFactor?.let { String.format("%.2f%%", it * 100) } ?: "-",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            // 卖出阈值列
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = analysis.sellThreshold?.let { String.format("%.2f%%", it * 100) } ?: "-",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

