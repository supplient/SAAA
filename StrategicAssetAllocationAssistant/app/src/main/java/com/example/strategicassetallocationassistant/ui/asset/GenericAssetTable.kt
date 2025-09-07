package com.example.strategicassetallocationassistant

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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
import com.example.strategicassetallocationassistant.AssetMetricsCells
import com.example.strategicassetallocationassistant.PortfolioViewModel

/**
 * 资产表格列定义
 */
data class AssetTableColumn(
    val title: String,
    val width: Dp,
    val headerAlignment: Alignment = Alignment.Center,
    val contentAlignment: Alignment = Alignment.Center,
    val content: @Composable (analysis: com.example.strategicassetallocationassistant.ui.common.model.AssetInfo, isHidden: Boolean) -> Unit,
    val sortOptions: List<com.example.strategicassetallocationassistant.PortfolioViewModel.SortOption> = emptyList()
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
    analyses: List<com.example.strategicassetallocationassistant.ui.common.model.AssetInfo>,
    columns: List<AssetTableColumn>,
    behavior: AssetTableBehavior = AssetTableBehavior(),
    isHidden: Boolean = false,
    useLazy: Boolean = true,
    showAddButton: Boolean = false,
    onAddClick: (() -> Unit)? = null,
    showSortDialog: Boolean = false,
    onSortOptionSelected: ((PortfolioViewModel.SortOption) -> Unit)? = null,
    onDismissSortDialog: (() -> Unit)? = null,
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
        if (useLazy) {
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
                
                // 底部新增资产按钮行
                if (showAddButton && onAddClick != null) {
                    item {
                        AddAssetButtonRow(
                            fixedColumn = fixedColumn.firstOrNull(),
                            scrollableColumns = scrollableColumns,
                            horizontalScrollState = horizontalScrollState,
                            onAddClick = onAddClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                analyses.forEach { analysis ->
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
                
                // 底部新增资产按钮行
                if (showAddButton && onAddClick != null) {
                    AddAssetButtonRow(
                        fixedColumn = fixedColumn.firstOrNull(),
                        scrollableColumns = scrollableColumns,
                        horizontalScrollState = horizontalScrollState,
                        onAddClick = onAddClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        // 统一的排序对话框
        if (showSortDialog && onSortOptionSelected != null && onDismissSortDialog != null) {
            UnifiedSortDialog(
                columns = columns,
                onSortOptionSelected = onSortOptionSelected,
                onDismiss = onDismissSortDialog
            )
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
            .padding(2.dp)
    ) {
        // 固定列（如果存在）
        fixedColumn?.let { column ->
            Box(
                modifier = Modifier
                    .width(column.width)
                    .padding(horizontal = 2.dp),
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
                .padding(start = 2.dp)
        ) {
            scrollableColumns.forEach { column ->
                Box(
                    modifier = Modifier
                        .width(column.width)
                        .padding(horizontal = 2.dp),
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
 * 新增资产按钮行组件
 */
@Composable
private fun AddAssetButtonRow(
    fixedColumn: AssetTableColumn?,
    scrollableColumns: List<AssetTableColumn>,
    horizontalScrollState: androidx.compose.foundation.ScrollState,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onAddClick() }
            .padding(vertical = 12.dp, horizontal = 2.dp)
    ) {
        // 固定列区域
        fixedColumn?.let { column ->
            Box(
                modifier = Modifier
                    .width(column.width)
                    .padding(horizontal = 2.dp),
                contentAlignment = column.contentAlignment
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "新增资产",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "新增资产",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // 可滚动列区域
        Row(
            modifier = Modifier
                .horizontalScroll(horizontalScrollState)
                .padding(start = 2.dp)
        ) {
            scrollableColumns.forEach { column ->
                Box(
                    modifier = Modifier
                        .width(column.width)
                        .padding(horizontal = 2.dp),
                    contentAlignment = column.contentAlignment
                ) {
                    Text(
                        text = "点击新增",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    analysis: com.example.strategicassetallocationassistant.ui.common.model.AssetInfo,
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
            .padding(2.dp)
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
                    .padding(horizontal = 2.dp),
                contentAlignment = column.contentAlignment
            ) {
                column.content(analysis, isHidden)
            }
        }
        
        // 可滚动列
        Row(
            modifier = Modifier
                .horizontalScroll(horizontalScrollState)
                .padding(start = 2.dp)
        ) {
            scrollableColumns.forEach { column ->
                Box(
                    modifier = Modifier
                        .width(column.width)
                        .padding(horizontal = 2.dp),
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
        width = 60.dp,
        headerAlignment = Alignment.CenterStart,
        contentAlignment = Alignment.CenterStart,
        content = { info, _ -> AssetMetricsCells.AssetName(info) },
        sortOptions = emptyList() // 资产名称通常不参与排序
    )
    
    /**
     * 占比列（显示：当前占比=目标占比±偏离度）
     */
    fun weightColumn() = AssetTableColumn(
        title = "占比",
        width = 60.dp,
        content = { info, _ ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AssetMetricsCells.CurrentWeight(info)
                AssetMetricsCells.TargetWeight(info)
                AssetMetricsCells.WeightDeviation(info)
            }
        },
        sortOptions = listOf(
            PortfolioViewModel.SortOption.CURRENT_WEIGHT,
            PortfolioViewModel.SortOption.TARGET_WEIGHT,
            PortfolioViewModel.SortOption.WEIGHT_DEVIATION,
            PortfolioViewModel.SortOption.WEIGHT_DEVIATION_ABS
        )
    )
    
    /**
     * 买因卖阈组合列（AssetTable专用）
     */
    fun buyFactorSellThresholdCombinedColumn() = AssetTableColumn(
        title = "买因卖阈",
        width = 70.dp,
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
        },
        sortOptions = listOf(
            PortfolioViewModel.SortOption.BUY_FACTOR,
            PortfolioViewModel.SortOption.SELL_THRESHOLD
        )
    )
    
    /**
     * 价份波组合列（AssetTable专用）
     */
    fun priceSharesVolatilityCombinedColumn() = AssetTableColumn(
        title = "价份波",
        width = 80.dp,
        content = { info, hidden ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AssetMetricsCells.UnitPrice(info)
                AssetMetricsCells.Shares(info, hidden)
                AssetMetricsCells.Volatility(info)
            }
        },
        sortOptions = listOf(
            PortfolioViewModel.SortOption.UNIT_PRICE,
            PortfolioViewModel.SortOption.SHARES,
            PortfolioViewModel.SortOption.VOLATILITY
        )
    )
    
    /**
     * 市值列
     */
    fun marketValueColumn() = AssetTableColumn(
        title = "市值",
        width = 100.dp,
        content = { info, hidden ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AssetMetricsCells.CurrentMarketValue(info, hidden)
                AssetMetricsCells.TargetMarketValue(info, hidden)
                AssetMetricsCells.MarketValueDeviation(info, hidden)
            }
        },
        sortOptions = listOf(
            PortfolioViewModel.SortOption.CURRENT_MARKET_VALUE,
            PortfolioViewModel.SortOption.TARGET_MARKET_VALUE,
            PortfolioViewModel.SortOption.MARKET_VALUE_DEVIATION,
            PortfolioViewModel.SortOption.MARKET_VALUE_DEVIATION_ABS
        )
    )
    
    /**
     * 更新时间列
     */
    fun updateTimeColumn() = AssetTableColumn(
        title = "更新时间",
        width = 100.dp,
        content = { info, _ ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AssetMetricsCells.UpdateTimeClock(info)
                AssetMetricsCells.UpdateTimeDate(info)
            }
        },
        sortOptions = emptyList() // 更新时间通常不参与排序
    )
    
    /**
     * 备注列
     */
    fun noteColumn() = AssetTableColumn(
        title = "备注",
        width = 160.dp,
        contentAlignment = Alignment.CenterStart,
        headerAlignment = Alignment.Center,
        content = { info, _ -> AssetMetricsCells.Note(info) },
        sortOptions = emptyList() // 备注通常不参与排序
    )
    
    /**
     * 七波相列
     */
    fun sevenDayReturnVolatilityRelativeOffsetCombinedColumn() = AssetTableColumn(
        title = "七波相",
        width = 80.dp,
        content = { info, _ ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AssetMetricsCells.SevenDayReturn(info)
                AssetMetricsCells.Volatility(info)
                AssetMetricsCells.RelativeOffset(info)
            }
        },
        sortOptions = listOf(
            PortfolioViewModel.SortOption.SEVEN_DAY_RETURN,
            PortfolioViewModel.SortOption.VOLATILITY,
            PortfolioViewModel.SortOption.RELATIVE_OFFSET
        )
    )

    /**
     * 偏跌去波列
     */
    fun offsetFactorDrawdownFactorPreVolatilityBuyFactorCombinedColumn() = AssetTableColumn(
        title = "偏跌去波",
        width = 80.dp,
        content = { info, _ ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AssetMetricsCells.OffsetFactor(info)
                AssetMetricsCells.DrawdownFactor(info)
                AssetMetricsCells.PreVolatilityBuyFactor(info)
            }
        },
        sortOptions = listOf(
            PortfolioViewModel.SortOption.OFFSET_FACTOR,
            PortfolioViewModel.SortOption.DRAWDOWN_FACTOR,
            PortfolioViewModel.SortOption.PRE_VOLATILITY_BUY_FACTOR
        )
    )

    /**
     * 买卖风
     */
    fun buyFactorSellThresholdAssetRiskCombinedColumn() = AssetTableColumn(
        title = "买卖风",
        width = 80.dp,
        content = { info, _ ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AssetMetricsCells.BuyFactor(info)
                AssetMetricsCells.SellThreshold(info)
                AssetMetricsCells.AssetRisk(info)
            }
        },
        sortOptions = listOf(
            PortfolioViewModel.SortOption.BUY_FACTOR,
            PortfolioViewModel.SortOption.SELL_THRESHOLD,
            PortfolioViewModel.SortOption.ASSET_RISK
        )
    )
    
    /**
     * 七日涨跌幅列
     */
    fun sevenDayReturnColumn() = AssetTableColumn(
        title = "七日涨跌幅",
        width = 80.dp,
        content = { info, _ -> AssetMetricsCells.SevenDayReturn(info) },
        sortOptions = listOf(PortfolioViewModel.SortOption.SEVEN_DAY_RETURN)
    )
    
    /**
     * 波动率列
     */
    fun volatilityColumn() = AssetTableColumn(
        title = "波动率",
        width = 80.dp,
        content = { info, _ -> AssetMetricsCells.Volatility(info) },
        sortOptions = listOf(PortfolioViewModel.SortOption.VOLATILITY)
    )
    
    /**
     * 买入因子列
     */
    fun buyFactorColumn() = AssetTableColumn(
        title = "买入因子",
        width = 80.dp,
        content = { info, _ -> AssetMetricsCells.BuyFactor(info) },
        sortOptions = listOf(PortfolioViewModel.SortOption.BUY_FACTOR)
    )
    
    /**
     * 卖出阈值列
     */
    fun sellThresholdColumn() = AssetTableColumn(
        title = "卖出阈值",
        width = 80.dp,
        content = { info, _ -> AssetMetricsCells.SellThreshold(info) },
        sortOptions = listOf(PortfolioViewModel.SortOption.SELL_THRESHOLD)
    )
    
    /**
     * 相对偏移列
     */
    fun relativeOffsetColumn() = AssetTableColumn(
        title = "相对偏移",
        width = 80.dp,
        content = { info, _ -> AssetMetricsCells.RelativeOffset(info) },
        sortOptions = listOf(PortfolioViewModel.SortOption.RELATIVE_OFFSET)
    )
    
    /**
     * 偏移因子列
     */
    fun offsetFactorColumn() = AssetTableColumn(
        title = "偏移因子",
        width = 80.dp,
        content = { info, _ -> AssetMetricsCells.OffsetFactor(info) },
        sortOptions = listOf(PortfolioViewModel.SortOption.OFFSET_FACTOR)
    )
    
    /**
     * 跌幅因子列
     */
    fun drawdownFactorColumn() = AssetTableColumn(
        title = "跌幅因子",
        width = 80.dp,
        content = { info, _ -> AssetMetricsCells.DrawdownFactor(info) },
        sortOptions = listOf(PortfolioViewModel.SortOption.DRAWDOWN_FACTOR)
    )
    
    /**
     * 去波动买入因子列
     */
    fun preVolatilityBuyFactorColumn() = AssetTableColumn(
        title = "去波动买入因子",
        width = 100.dp,
        content = { info, _ -> AssetMetricsCells.PreVolatilityBuyFactor(info) },
        sortOptions = listOf(PortfolioViewModel.SortOption.PRE_VOLATILITY_BUY_FACTOR)
    )
    
    /**
     * 资产风险列
     */
    fun assetRiskColumn() = AssetTableColumn(
        title = "资产风险",
        width = 80.dp,
        content = { info, _ -> AssetMetricsCells.AssetRisk(info) },
        sortOptions = listOf(PortfolioViewModel.SortOption.ASSET_RISK)
    )
}

/**
 * 统一的排序对话框组件
 * 从所有列中收集可用的排序选项
 */
@Composable
private fun UnifiedSortDialog(
    columns: List<AssetTableColumn>,
    onSortOptionSelected: (PortfolioViewModel.SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    // 从所有列中收集排序选项，并添加原排序选项
    val availableSortOptions = listOf(PortfolioViewModel.SortOption.ORIGINAL) + 
        columns.flatMap { it.sortOptions }.distinct()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择排序方案") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableSortOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSortOptionSelected(option) }
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = option.displayName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
