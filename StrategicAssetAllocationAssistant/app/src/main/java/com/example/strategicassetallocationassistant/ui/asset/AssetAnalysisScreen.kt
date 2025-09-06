@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.strategicassetallocationassistant

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 资产分析界面组件
 * 专门用于显示和分析资产的各项指标数据
 */
@Composable
fun AssetAnalysisScreen(
    viewModel: PortfolioViewModel,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onEditAsset: (java.util.UUID) -> Unit = {}
) {
    val analyses by viewModel.assetAnalyses.collectAsState()
    val portfolio by viewModel.portfolioState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 排序对话框状态
    var showSortDialog by remember { mutableStateOf(false) }
    
    // 计算目标占比总和和总资产
    val targetWeightSum = analyses.sumOf { it.asset.targetWeight }
    val totalAssets = portfolio.cash + analyses.sumOf { it.marketValue }
    val assetsValue = analyses.sumOf { it.marketValue }
    val assetWeightPct = if (totalAssets > 0) assetsValue / totalAssets else 0.0
    val targetWeightPct = targetWeightSum
    val deviationPct = assetWeightPct - targetWeightPct

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "资产分析",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 排序按钮
                    IconButton(onClick = { showSortDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "排序")
                    }
                    // 隐藏资产数目按钮
                    IconButton(onClick = { viewModel.toggleAssetAmountHidden() }) {
                        val isHidden by viewModel.isAssetAmountHidden.collectAsState()
                        Icon(
                            imageVector = if (isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isHidden) "显示资产数目" else "隐藏资产数目"
                        )
                    }
                    // 刷新分析数据按钮
                    IconButton(onClick = { viewModel.refreshAnalysisData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新分析数据")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 资产分析表格
            AssetAnalysisTable(
                analyses = viewModel.sortedAssetAnalyses.collectAsState().value,
                isHidden = viewModel.isAssetAmountHidden.collectAsState().value,
                onEditAsset = onEditAsset,
                modifier = Modifier.weight(1f)
            )
            
            // 底部信息栏
            PortfolioSummary(
                currentWeight = assetWeightPct,
                targetWeight = targetWeightPct,
                deviation = deviationPct,
                availableCash = portfolio.cash,
                riskFactor = portfolio.overallRiskFactor,
                isHidden = viewModel.isAssetAmountHidden.collectAsState().value,
                totalAssets = totalAssets,
                targetWeightSum = targetWeightSum,
                onSaveCash = { newCash -> viewModel.updateCash(newCash) }
            )
        }
        
        // 排序对话框
        if (showSortDialog) {
            AnalysisSortDialog(
                currentSortOption = viewModel.sortOption.collectAsState().value,
                isAscending = viewModel.isAscending.collectAsState().value,
                onSortOptionSelected = { option ->
                    viewModel.setSortOption(option)
                    showSortDialog = false
                },
                onDismiss = { showSortDialog = false }
            )
        }
    }
}


/**
 * 分析界面专用的排序对话框组件
 * 只显示与分析相关的排序选项
 */
@Composable
private fun AnalysisSortDialog(
    currentSortOption: PortfolioViewModel.SortOption,
    isAscending: Boolean,
    onSortOptionSelected: (PortfolioViewModel.SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    // 分析界面相关的排序选项
    val analysisRelevantOptions = listOf(
        PortfolioViewModel.SortOption.ORIGINAL,
        PortfolioViewModel.SortOption.CURRENT_WEIGHT,
        PortfolioViewModel.SortOption.TARGET_WEIGHT,
        PortfolioViewModel.SortOption.WEIGHT_DEVIATION,
        PortfolioViewModel.SortOption.WEIGHT_DEVIATION_ABS,
        PortfolioViewModel.SortOption.SEVEN_DAY_RETURN,
        PortfolioViewModel.SortOption.BUY_FACTOR,
        PortfolioViewModel.SortOption.SELL_THRESHOLD,
        PortfolioViewModel.SortOption.RELATIVE_OFFSET,
        PortfolioViewModel.SortOption.OFFSET_FACTOR,
        PortfolioViewModel.SortOption.DRAWDOWN_FACTOR,
        PortfolioViewModel.SortOption.PRE_VOLATILITY_BUY_FACTOR,
        PortfolioViewModel.SortOption.ASSET_RISK
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择排序方案") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                analysisRelevantOptions.forEach { option ->
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
                        
                        // 显示当前排序方案和升降序指示器
                        if (option == currentSortOption) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isAscending) "↑" else "↓",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (isAscending) "升序" else "降序",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
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
