@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.example.strategicassetallocationassistant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Sort
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign

/**
 * 主屏幕组件
 * 重构后的资产列表界面，支持侧边栏、资产配置总体信息栏和表格化资产列表
 */
@Composable
fun AssetListScreen(
    viewModel: PortfolioViewModel,
    modifier: Modifier = Modifier,
    onAddAsset: () -> Unit = {},
    onEditAsset: (java.util.UUID) -> Unit = {},
    onAddTransactionForAsset: (java.util.UUID) -> Unit = {},
    onOpenApiTest: () -> Unit = {},
    onOpenTransactions: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenOpportunities: () -> Unit = {},
    onOpenConfigNote: () -> Unit = {}
) {
    val portfolio by viewModel.portfolioState.collectAsState()
    val analyses by viewModel.assetAnalyses.collectAsState()

    // 计算目标占比总和
    val targetWeightSum = analyses.sumOf { it.asset.targetWeight }
    
    // 侧边栏和消息状态
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // 现金编辑状态
    var showCashEditDialog by remember { mutableStateOf(false) }
    var cashInputValue by remember { mutableStateOf("") }
    
    // 排序对话框状态
    var showSortDialog by remember { mutableStateOf(false) }
    
    // 计算总资产
    val totalAssets = portfolio.cash + analyses.sumOf { it.marketValue }
    val assetsValue = analyses.sumOf { it.marketValue }
    val assetWeightPct = if (totalAssets > 0) assetsValue / totalAssets else 0.0
    val targetWeightPct = targetWeightSum
    val deviationPct = assetWeightPct - targetWeightPct

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
                    drawerContent = {
                AppDrawer(
                    onClose = { scope.launch { drawerState.close() } },
                    onNavigateToConfigNote = onOpenConfigNote,
                    onNavigateToApiTest = onOpenApiTest,
                    onNavigateToTransactions = onOpenTransactions,
                    onNavigateToSettings = onOpenSettings
                )
            }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "菜单")
                        }
                    },
                    actions = {
                        // 新增资产按钮
                        IconButton(onClick = onAddAsset) {
                            Icon(Icons.Default.Add, contentDescription = "新增资产")
                        }
                        // 排序按钮
                        IconButton(onClick = { showSortDialog = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "排序")
                        }
                        // 隐藏资产数目按钮
                        IconButton(onClick = { viewModel.toggleAssetAmountHidden() }) {
                            val isHidden by viewModel.isAssetAmountHidden.collectAsState()
                            Icon(
                                imageVector = if (isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isHidden) "显示资产数目" else "隐藏资产数目"
                            )
                        }
                        IconButton(onClick = onOpenOpportunities) {
                            Icon(imageVector = Icons.Default.Notifications, contentDescription = "交易机会")
                        }
                        IconButton(onClick = { viewModel.refreshMarketData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // 资产列表表格（使用weight让底部信息栏固定）
                AssetTable(
                    analyses = viewModel.sortedAssetAnalyses.collectAsState().value,
                    isHidden = viewModel.isAssetAmountHidden.collectAsState().value,
                    onAddTransaction = onAddTransactionForAsset,
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
                    onCashClick = { showCashEditDialog = true }
                )
            }
            
            // 现金编辑对话框
            if (showCashEditDialog) {
                AlertDialog(
                    onDismissRequest = { showCashEditDialog = false },
                    title = { Text("编辑可用现金") },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // 总资产展示（只读，始终显示具体数值）
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "总资产", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "¥${String.format("%.2f", totalAssets)}", style = MaterialTheme.typography.bodyMedium)
                            }

                            // 除现金外占比
                            val nonCashAssetsValue = totalAssets - portfolio.cash
                            val nonCashCurrentWeightSum = if (totalAssets > 0) nonCashAssetsValue / totalAssets else 0.0
                            val nonCashTargetWeightSum = targetWeightSum
                            val nonCashWeightDeviation = nonCashCurrentWeightSum - nonCashTargetWeightSum
                            val deviationAbs = kotlin.math.abs(nonCashWeightDeviation)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "除现金外占比", style = MaterialTheme.typography.bodyMedium)
                                if (deviationAbs > 0.0001) {
                                    Text(
                                        text = "${String.format("%.1f", nonCashCurrentWeightSum * 100)}% = ${String.format("%.1f", nonCashTargetWeightSum * 100)}% ± ${String.format("%.1f", deviationAbs * 100)}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    Text(
                                        text = "${String.format("%.1f", nonCashCurrentWeightSum * 100)}% = ${String.format("%.1f", nonCashTargetWeightSum * 100)}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            // 可用现金编辑（始终显示具体数值）
                            OutlinedTextField(
                                value = cashInputValue,
                                onValueChange = { cashInputValue = it },
                                label = { Text("可用现金") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCashEditDialog = false }) {
                            Text("取消")
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            val newCash = cashInputValue.toDoubleOrNull()
                            if (newCash != null && newCash >= 0) {
                                viewModel.updateCash(newCash)
                            }
                            showCashEditDialog = false
                        }) {
                            Text("保存")
                        }
                    }
                )
            }
            
            // 排序对话框
            if (showSortDialog) {
                SortDialog(
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
    }
}

/**
 * 底部信息栏组件
 * 显示可用现金和资产占比信息，固定在下部
 */
@Composable
private fun BottomInfoBar(
    totalAssets: Double,
    availableCash: Double,
    targetWeightSum: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 计算除现金外的资产市值和占比
    val nonCashAssetsValue = totalAssets - availableCash
    val nonCashCurrentWeightSum = if (totalAssets > 0) nonCashAssetsValue / totalAssets else 0.0
    val nonCashTargetWeightSum = targetWeightSum
    val nonCashWeightDeviation = nonCashCurrentWeightSum - nonCashTargetWeightSum
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：可用现金
            Text(
                text = "¥${String.format("%.2f", availableCash)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            // 右侧：除现金外资产占比信息
            val deviationAbs = kotlin.math.abs(nonCashWeightDeviation)
            if (deviationAbs > 0.0001) {
                // 根据实际偏差情况使用+或-符号
                val deviationSymbol = if (nonCashWeightDeviation > 0) "+" else "-"
                Text(
                    text = "∑${String.format("%.1f", nonCashCurrentWeightSum * 100)}% = ${String.format("%.1f", nonCashTargetWeightSum * 100)}% $deviationSymbol ${String.format("%.1f", deviationAbs * 100)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "∑${String.format("%.1f", nonCashCurrentWeightSum * 100)}% = ${String.format("%.1f", nonCashTargetWeightSum * 100)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 排序对话框组件
 */
@Composable
private fun SortDialog(
    currentSortOption: PortfolioViewModel.SortOption,
    isAscending: Boolean,
    onSortOptionSelected: (PortfolioViewModel.SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择排序方案") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PortfolioViewModel.SortOption.values().forEach { option ->
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


