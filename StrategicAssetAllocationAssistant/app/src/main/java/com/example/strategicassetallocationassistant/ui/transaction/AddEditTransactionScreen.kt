package com.example.strategicassetallocationassistant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import kotlinx.coroutines.flow.collectLatest
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.strategicassetallocationassistant.GenericAssetTable
import com.example.strategicassetallocationassistant.CommonAssetColumns
import com.example.strategicassetallocationassistant.ui.common.util.MoneyUtils
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(navController: NavController) {
    val viewModel: AddEditTransactionViewModel = hiltViewModel()
    val type by viewModel.type.collectAsState()
    val shares by viewModel.sharesInput.collectAsState()
    val priceInput by viewModel.priceInput.collectAsState()
    val fee by viewModel.feeInput.collectAsState()
    val reason by viewModel.reasonInput.collectAsState()
    val previewInfos by viewModel.previewInfosDecimal.collectAsState() // 步骤7: 切换到BigDecimal版本
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val isEditing = viewModel.isEditing
    
    // 删除确认对话框状态
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        CenterAlignedTopAppBar(title = {
            Column {
                Text(if (isEditing) "编辑交易" else "新增交易")
                val assets = viewModel.assets.collectAsState().value
                val aid = viewModel.selectedAssetId.collectAsState().value
                val assetName = assets.firstOrNull { it.id == aid }?.name ?: ""
                if (assetName.isNotBlank()) {
                    Text(
                        assetName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        })
    }) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (previewInfos.size == 2) {
                GenericAssetTable(
                    analyses = previewInfos,
                    columns = listOf(
                        CommonAssetColumns.weightColumn(),
                        CommonAssetColumns.marketValueColumn()
                    ),
                    useLazy = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 行1: 买/卖切换按钮 + 份额输入
            Row(verticalAlignment = Alignment.CenterVertically) {
                val isBuy = type == TradeType.BUY
                val btnColors = if (isBuy) {
                    // 买入按钮使用主题色
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    // 卖出按钮使用错误色
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                }
                FilledTonalButton(
                    onClick = { viewModel.onTypeChange(if (isBuy) TradeType.SELL else TradeType.BUY) },
                    colors = btnColors,
                    modifier = Modifier.width(80.dp)
                ) {
                    Text(if (isBuy) "买" else "卖")
                }

                Spacer(Modifier.width(12.dp))

                val sharesError by viewModel.sharesError.collectAsState()
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = shares,
                        onValueChange = viewModel::onSharesChange,
                        label = { Text("份额") },
                        isError = sharesError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    Column {
                        IconButton(onClick = { viewModel.incrementShares() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "加100")
                        }
                        IconButton(onClick = { viewModel.decrementShares() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "减100")
                        }
                    }
                }
            }

            // 行2 单价 + 刷新
            val isRefreshing by viewModel.isRefreshing.collectAsState()
            val priceError by viewModel.priceError.collectAsState()
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = priceInput,
                    onValueChange = viewModel::onPriceChange,
                    label = { Text("单价") },
                    isError = priceError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                if (isRefreshing) {
                    CircularProgressIndicator(Modifier.size(40.dp))
                } else {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            val ok = viewModel.refreshPriceAndAnalysis()
                            Toast.makeText(context, if (ok) "刷新成功" else "刷新失败", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新单价")
                    }
                }
            }

            // 行3 手续费 & 总额 - 步骤7: 切换到BigDecimal版本显示
            val feeError by viewModel.feeError.collectAsState()
            val totalAmount by viewModel.totalAmountDecimal.collectAsState()
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = fee,
                    onValueChange = viewModel::onFeeChange,
                    label = { Text("手续费") },
                    isError = feeError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(0.3f)
                )
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value = totalAmount,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("总额") },
                    modifier = Modifier.weight(0.7f)
                )
            }

            OutlinedTextField(value = reason, onValueChange = viewModel::onReasonChange, label = { Text("交易理由 (可选)") }, modifier = Modifier.fillMaxWidth())

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { navController.navigateUp() }) { Text("取消") }

                if (isEditing) {
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("删除") }
                }

                Spacer(Modifier.weight(1f))

                val canSave by viewModel.canSave.collectAsState()
                Button(enabled = canSave, onClick = {
                    coroutineScope.launch {
                        // 步骤7: 切换到BigDecimal版本的保存方法
                        val success = viewModel.saveWithDecimal()
                        if (success) {
                            Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                            navController.navigateUp()
                        } else {
                            Toast.makeText(context, "输入不合法，无法保存", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("保存") }
            }
        }
        
        // 删除确认对话框
        if (showDeleteDialog) {
            DeleteTransactionDialog(
                onConfirm = {
                    coroutineScope.launch {
                        viewModel.delete()
                        navController.navigateUp()
                    }
                    showDeleteDialog = false
                },
                onCancel = {
                    showDeleteDialog = false
                }
            )
        }
    }
}

@Composable
private fun DeleteTransactionDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text("删除交易记录")
        },
        text = {
            Column {
                Text("确认要删除这条交易记录吗？")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ 此操作会回滚该交易对可用现金和资产份额的影响",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "此操作不可撤销！",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("确认删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("取消")
            }
        }
    )
}
