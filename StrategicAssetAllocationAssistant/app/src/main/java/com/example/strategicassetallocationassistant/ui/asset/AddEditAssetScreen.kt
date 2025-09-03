package com.example.strategicassetallocationassistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import com.example.strategicassetallocationassistant.data.database.AppDatabase
import com.example.strategicassetallocationassistant.data.repository.PortfolioRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAssetScreen(
    navController: NavController
) {
    // Create ViewModel with Hilt
    val viewModel: AddEditAssetViewModel = hiltViewModel()
    val portfolioViewModel: PortfolioViewModel = hiltViewModel()

    // Collect UI state from ViewModel
    val name by viewModel.name.collectAsState()
    
    // 获取当前编辑资产的分析数据
    val assetAnalyses by portfolioViewModel.assetAnalyses.collectAsState()
    val currentAssetAnalysis = viewModel.editingAssetId?.let { assetId ->
        assetAnalyses.find { it.asset.id == assetId }
    }
    
    val targetWeight by viewModel.targetWeightInput.collectAsState()
    val code by viewModel.code.collectAsState()
    val shares by viewModel.sharesInput.collectAsState()
    val unitValue by viewModel.unitValueInput.collectAsState()

    
    val coroutineScope = rememberCoroutineScope()

    // 删除确认对话框状态
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isEditing = viewModel.editingAssetId != null

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = if (isEditing) "编辑资产" else "添加资产")
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "删除资产"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：取消按钮始终存在
                OutlinedButton(onClick = { navController.navigateUp() }) {
                    Text("取消")
                }

                // 右侧：保存
                Button(onClick = {
                    coroutineScope.launch {
                        if (viewModel.save()) {
                            navController.navigateUp()
                        }
                    }
                }) {
                    Text("保存")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 资产名称
            OutlinedTextField(
                value = name,
                onValueChange = { viewModel.onNameChange(it) },
                label = { Text("资产名称") },
                modifier = Modifier.fillMaxWidth()
            )

            

            // 目标占比
            OutlinedTextField(
                value = targetWeight,
                onValueChange = { viewModel.onTargetWeightChange(it) },
                label = { Text("目标占比 (%)") },
                modifier = Modifier.fillMaxWidth()
            )

            // 资产代码
            OutlinedTextField(
                value = code,
                onValueChange = { viewModel.onCodeChange(it) },
                label = { Text("资产代码") },
                modifier = Modifier.fillMaxWidth()
            )

            // 份额/股数
            OutlinedTextField(
                value = shares,
                onValueChange = { viewModel.onSharesChange(it) },
                label = { Text("份额 / 股数") },
                modifier = Modifier.fillMaxWidth()
            )

            // 单位价格 / 净值
            OutlinedTextField(
                value = unitValue,
                onValueChange = { viewModel.onUnitValueChange(it) },
                label = { Text("单位价格 / 净值") },
                modifier = Modifier.fillMaxWidth()
            )

            // 备注
            OutlinedTextField(
                value = viewModel.note.collectAsState().value,
                onValueChange = { viewModel.onNoteChange(it) },
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // 计算过程展示
            if (currentAssetAnalysis != null && viewModel.editingAssetId != null) {
                CalculationProcessSection(
                    buyFactorLog = currentAssetAnalysis.buyFactorLog,
                    sellThresholdLog = currentAssetAnalysis.sellThresholdLog,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除该资产吗？此操作无法撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        coroutineScope.launch {
                            viewModel.delete()
                            navController.navigateUp()
                        }
                    },
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 计算过程展示组件
 */
@Composable
private fun CalculationProcessSection(
    buyFactorLog: String?,
    sellThresholdLog: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 标题
        Text(
            text = "计算过程",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        // 买入因子计算过程
        if (!buyFactorLog.isNullOrBlank()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "买入因子计算过程:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                OutlinedTextField(
                    value = buyFactorLog.replace("; ", "\n"),
                    onValueChange = { /* 只读 */ },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                )
            }
        }
        
        // 卖出阈值计算过程
        if (!sellThresholdLog.isNullOrBlank()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "卖出阈值计算过程:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                OutlinedTextField(
                    value = sellThresholdLog.replace("; ", "\n"),
                    onValueChange = { /* 只读 */ },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                )
            }
        }
        
        // 如果没有计算过程数据，显示提示
        if (buyFactorLog.isNullOrBlank() && sellThresholdLog.isNullOrBlank()) {
            Text(
                text = "暂无计算过程数据。请先刷新市场数据。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
