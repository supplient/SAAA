package com.example.strategicassetallocationassistant

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * 资产表格列定义
 */
data class AssetTableColumn(
    val title: String,
    val width: Dp,
    val headerAlignment: Alignment = Alignment.Center,
    val contentAlignment: Alignment = Alignment.Center,
    val content: @Composable (analysis: PortfolioViewModel.AssetInfo, isHidden: Boolean) -> Unit
)

/**
 * 资产表格行为配置
 */
data class AssetTableBehavior(
    val onRowClick: ((UUID) -> Unit)? = null,
    val onRowLongClick: ((UUID) -> Unit)? = null
)

/**
 * 通用资产表格组件
 * 支持固定第一列 + 横向滚动其余列的布局
 */
@Composable
fun GenericAssetTable(
    analyses: List<PortfolioViewModel.AssetInfo>,
    columns: List<AssetTableColumn>,
    behavior: AssetTableBehavior = AssetTableBehavior(),
    isHidden: Boolean = false,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()
    val (fixedColumn, scrollableColumns) = columns.partition { it.title == "资产名称" }
    
    Column(modifier = modifier) {
        // 表头
        GenericAssetTableHeader(
            fixedColumn = fixedColumn.firstOrNull(),
            scrollableColumns = scrollableColumns,
            horizontalScrollState = horizontalScrollState,
            modifier = Modifier.fillMaxWidth()
        )
        
        // 数据行
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(analyses) { analysis ->
                GenericAssetTableRow(
                    analysis = analysis,
                    isHidden = isHidden,
                    fixedColumn = fixedColumn.firstOrNull(),
                    scrollableColumns = scrollableColumns,
                    horizontalScrollState = horizontalScrollState,
                    behavior = behavior,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 通用表格表头组件
 */
@Composable
private fun GenericAssetTableHeader(
    fixedColumn: AssetTableColumn?,
    scrollableColumns: List<AssetTableColumn>,
    horizontalScrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(8.dp)
    ) {
        // 固定列（如果存在）
        fixedColumn?.let { column ->
            Box(
                modifier = Modifier
                    .width(column.width)
                    .padding(horizontal = 8.dp),
                contentAlignment = column.headerAlignment
            ) {
                Text(
                    text = column.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        // 可滚动列
        Row(
            modifier = Modifier
                .horizontalScroll(horizontalScrollState)
                .padding(start = 8.dp)
        ) {
            scrollableColumns.forEach { column ->
                Box(
                    modifier = Modifier
                        .width(column.width)
                        .padding(horizontal = 4.dp),
                    contentAlignment = column.headerAlignment
                ) {
                    Text(
                        text = column.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * 通用表格数据行组件
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun GenericAssetTableRow(
    analysis: PortfolioViewModel.AssetInfo,
    isHidden: Boolean,
    fixedColumn: AssetTableColumn?,
    scrollableColumns: List<AssetTableColumn>,
    horizontalScrollState: androidx.compose.foundation.ScrollState,
    behavior: AssetTableBehavior,
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
            .combinedClickable(
                onClick = { behavior.onRowClick?.invoke(analysis.asset.id) },
                onLongClick = { behavior.onRowLongClick?.invoke(analysis.asset.id) }
            )
    ) {
        // 固定列（如果存在）
        fixedColumn?.let { column ->
            Box(
                modifier = Modifier
                    .width(column.width)
                    .padding(horizontal = 8.dp),
                contentAlignment = column.contentAlignment
            ) {
                column.content(analysis, isHidden)
            }
        }
        
        // 可滚动列
        Row(
            modifier = Modifier
                .horizontalScroll(horizontalScrollState)
                .padding(start = 8.dp)
        ) {
            scrollableColumns.forEach { column ->
                Box(
                    modifier = Modifier
                        .width(column.width)
                        .padding(horizontal = 4.dp),
                    contentAlignment = column.contentAlignment
                ) {
                    column.content(analysis, isHidden)
                }
            }
        }
    }
}

/**
 * 所有资产表格列定义的统一库
 * 包含了AssetTable和AssetAnalysisTable中使用的所有列
 */
object CommonAssetColumns {
    
    /**
     * 资产名称列（固定列，带错误指示）
     */
    fun assetNameColumn() = AssetTableColumn(
        title = "资产名称",
        width = 80.dp,
        headerAlignment = Alignment.CenterStart,
        contentAlignment = Alignment.CenterStart,
        content = { analysis, _ ->
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
    )
    
    /**
     * 占比列（显示：当前占比=目标占比±偏离度）
     */
    fun weightColumn() = AssetTableColumn(
        title = "占比",
        width = 80.dp,
        content = { analysis, _ ->
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
    )
    
    /**
     * 买因卖阈组合列（AssetTable专用）
     */
    fun buyFactorSellThresholdCombinedColumn() = AssetTableColumn(
        title = "买因卖阈",
        width = 80.dp,
        content = { analysis, _ ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
    )
    
    /**
     * 价份波组合列（AssetTable专用）
     */
    fun priceSharesVolatilityCombinedColumn() = AssetTableColumn(
        title = "价份波",
        width = 80.dp,
        content = { analysis, isHidden ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
    )
    
    /**
     * 市值列
     */
    fun marketValueColumn() = AssetTableColumn(
        title = "市值",
        width = 100.dp,
        content = { analysis, isHidden ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
    )
    
    /**
     * 更新时间列
     */
    fun updateTimeColumn() = AssetTableColumn(
        title = "更新时间",
        width = 120.dp,
        content = { analysis, _ ->
            analysis.asset.lastUpdateTime?.let { time ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
    )
    
    /**
     * 备注列
     */
    fun noteColumn() = AssetTableColumn(
        title = "备注",
        width = 160.dp,
        contentAlignment = Alignment.CenterStart,
        headerAlignment = Alignment.Center,
        content = { analysis, _ ->
            Text(
                text = analysis.asset.note ?: "",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
    
    /**
     * 七日涨跌幅列
     */
    fun sevenDayReturnColumn() = AssetTableColumn(
        title = "七日涨跌幅",
        width = 80.dp,
        content = { analysis, _ ->
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
    )
    
    /**
     * 波动率列
     */
    fun volatilityColumn() = AssetTableColumn(
        title = "波动率",
        width = 80.dp,
        content = { analysis, _ ->
            Text(
                text = analysis.volatility?.let { String.format("%.2f%%", it * 100) } ?: "-",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    )
    
    /**
     * 买入因子列
     */
    fun buyFactorColumn() = AssetTableColumn(
        title = "买入因子",
        width = 80.dp,
        content = { analysis, _ ->
            Text(
                text = analysis.buyFactor?.let { String.format("%.2f%%", it * 100) } ?: "-",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    )
    
    /**
     * 卖出阈值列
     */
    fun sellThresholdColumn() = AssetTableColumn(
        title = "卖出阈值",
        width = 80.dp,
        content = { analysis, _ ->
            Text(
                text = analysis.sellThreshold?.let { String.format("%.2f%%", it * 100) } ?: "-",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    )
    
    /**
     * 相对偏移列
     */
    fun relativeOffsetColumn() = AssetTableColumn(
        title = "相对偏移",
        width = 80.dp,
        content = { analysis, _ ->
            val relativeOffset = analysis.relativeOffset
            if (relativeOffset != null) {
                Text(
                    text = String.format("%.3f", relativeOffset),
                    style = MaterialTheme.typography.bodySmall,
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
    )
    
    /**
     * 偏移因子列
     */
    fun offsetFactorColumn() = AssetTableColumn(
        title = "偏移因子",
        width = 80.dp,
        content = { analysis, _ ->
            val offsetFactor = analysis.offsetFactor
            if (offsetFactor != null) {
                Text(
                    text = String.format("%.3f", offsetFactor),
                    style = MaterialTheme.typography.bodySmall,
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
    )
    
    /**
     * 跌幅因子列
     */
    fun drawdownFactorColumn() = AssetTableColumn(
        title = "跌幅因子",
        width = 80.dp,
        content = { analysis, _ ->
            val drawdownFactor = analysis.drawdownFactor
            if (drawdownFactor != null) {
                Text(
                    text = String.format("%.3f", drawdownFactor),
                    style = MaterialTheme.typography.bodySmall,
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
    )
    
    /**
     * 去波动买入因子列
     */
    fun preVolatilityBuyFactorColumn() = AssetTableColumn(
        title = "去波动买入因子",
        width = 100.dp,
        content = { analysis, _ ->
            val preVolatilityBuyFactor = analysis.preVolatilityBuyFactor
            if (preVolatilityBuyFactor != null) {
                Text(
                    text = String.format("%.3f", preVolatilityBuyFactor),
                    style = MaterialTheme.typography.bodySmall,
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
    )
    
    /**
     * 资产风险列
     */
    fun assetRiskColumn() = AssetTableColumn(
        title = "资产风险",
        width = 80.dp,
        content = { analysis, _ ->
            val assetRisk = analysis.assetRisk
            if (assetRisk != null) {
                Text(
                    text = String.format("%.6f", assetRisk),
                    style = MaterialTheme.typography.bodySmall,
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
    )
}
