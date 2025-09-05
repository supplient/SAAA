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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(navController: NavController) {
    val viewModel: AddEditTransactionViewModel = hiltViewModel()
    val type by viewModel.type.collectAsState()
    val assetId by viewModel.assetIdInput.collectAsState()
    val shares by viewModel.sharesInput.collectAsState()
    val currentPrice by viewModel.currentPrice.collectAsState()
    val fee by viewModel.feeInput.collectAsState()
    val reason by viewModel.reasonInput.collectAsState()
    val previewInfos by viewModel.previewInfos.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var typeExpanded by remember { mutableStateOf(false) }
    val isEditing = viewModel.isEditing

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
                val btnColors = ButtonDefaults.filledTonalButtonColors()
                FilledTonalButton(
                    onClick = { viewModel.onTypeChange(if (isBuy) TradeType.SELL else TradeType.BUY) },
                    colors = btnColors,
                    modifier = Modifier.width(80.dp)
                ) {
                    Text(if (isBuy) "买" else "卖")
                }

                Spacer(Modifier.width(12.dp))

                val sharesError by viewModel.sharesError.collectAsState()
                OutlinedTextField(
                    value = shares,
                    onValueChange = viewModel::onSharesChange,
                    label = { Text("份额") },
                    isError = sharesError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            // 行2 单价 + 刷新
            val isRefreshing by viewModel.isRefreshing.collectAsState()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "单价: ¥${currentPrice?.let { String.format("%.4f", it) } ?: "-"}", modifier = Modifier.weight(1f))
                if (isRefreshing) {
                    CircularProgressIndicator(Modifier.size(20.dp))
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

            // 行3 手续费 & 总额
            val feeError by viewModel.feeError.collectAsState()
            val totalAmount by viewModel.totalAmount.collectAsState()
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
                    OutlinedButton(onClick = {
                        coroutineScope.launch {
                            viewModel.delete(); navController.navigateUp()
                        }
                    }) { Text("删除") }
                }

                Spacer(Modifier.weight(1f))

                val canSave by viewModel.canSave.collectAsState()
                Button(enabled = canSave, onClick = {
                    coroutineScope.launch {
                        val success = viewModel.save()
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
    }
}
