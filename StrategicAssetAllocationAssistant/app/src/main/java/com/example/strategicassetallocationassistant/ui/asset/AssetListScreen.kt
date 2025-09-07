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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.automirrored.filled.List
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
    onOpenConfigNote: () -> Unit = {},
    onOpenAssetAnalysis: () -> Unit = {}
) {
    val portfolio by viewModel.portfolioState.collectAsState()
    val analyses by viewModel.assetAnalyses.collectAsState()

    // 计算目标占比总和
    val targetWeightSum = analyses.sumOf { it.asset.targetWeight }
    
    // 侧边栏和消息状态
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
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
                        // 资产分析按钮
                        IconButton(onClick = onOpenAssetAnalysis) {
                            Icon(Icons.Default.Analytics, contentDescription = "资产分析")
                        }
                        // 交易记录按钮
                        IconButton(onClick = onOpenTransactions) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "交易记录")
                        }
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
                    onAddAsset = onAddAsset,
                    showSortDialog = showSortDialog,
                    onSortOptionSelected = { option ->
                        viewModel.setSortOption(option)
                        showSortDialog = false
                    },
                    onDismissSortDialog = { showSortDialog = false },
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
        }
        }
    }
}



