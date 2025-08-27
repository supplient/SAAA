@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.example.strategicassetallocationassistant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
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
    
    // 计算总资产
    val totalAssets = portfolio.cash + analyses.sumOf { it.marketValue }

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
                    title = {
                        SummaryBar(
                            totalAssets = totalAssets,
                            availableCash = portfolio.cash,
                            targetWeightSum = targetWeightSum,
                            onClick = {
                                cashInputValue = String.format("%.2f", portfolio.cash)
                                showCashEditDialog = true
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "菜单")
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenTransactions) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = "交易")
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

                // 资产列表表格
                AssetTable(
                    analyses = analyses,
                    onAddTransaction = onAddTransactionForAsset,
                    onEditAsset = onEditAsset,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // 现金编辑对话框
            if (showCashEditDialog) {
                AlertDialog(
                    onDismissRequest = { showCashEditDialog = false },
                    title = { Text("编辑可用现金") },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // 总资产展示（只读）
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "总资产", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "¥${String.format("%.2f", totalAssets)}", style = MaterialTheme.typography.bodyMedium)
                            }

                            // 合计目标占比
                            val diff = kotlin.math.abs(targetWeightSum - 1.0)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "合计目标占比", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${String.format("%.2f", targetWeightSum * 100)}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (diff > 0.0001) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            // 可用现金编辑
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
            
            // FloatingActionButton
            FloatingActionButton(
                onClick = onAddAsset,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "添加资产"
                )
            }
        }
        }
    }
}

// 顶部紧凑型资产概览栏，可点击弹出明细/编辑对话框
@Composable
private fun SummaryBar(
    totalAssets: Double,
    availableCash: Double,
    targetWeightSum: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "¥${String.format("%.2f", availableCash)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start
            )
            Text(
                text = "¥${String.format("%.2f", totalAssets)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        val diff = kotlin.math.abs(targetWeightSum - 1.0)
        if (diff > 0.0001) {
            Text(
                text = "Σ ${(targetWeightSum * 100).let { String.format("%.0f", it) }}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End
            )
        }
    }
}
