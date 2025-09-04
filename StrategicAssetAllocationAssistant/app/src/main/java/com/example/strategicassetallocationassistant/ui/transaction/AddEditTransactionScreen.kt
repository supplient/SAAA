package com.example.strategicassetallocationassistant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(navController: NavController) {
    val viewModel: AddEditTransactionViewModel = hiltViewModel()
    val type by viewModel.type.collectAsState()
    val assetId by viewModel.assetIdInput.collectAsState()
    val shares by viewModel.sharesInput.collectAsState()
    val price by viewModel.priceInput.collectAsState()
    val fee by viewModel.feeInput.collectAsState()
    val reason by viewModel.reasonInput.collectAsState()
    val previewInfos by viewModel.previewInfos.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var typeExpanded by remember { mutableStateOf(false) }
    val isEditing = viewModel.isEditing

    Scaffold(topBar = {
        CenterAlignedTopAppBar(title = { Text(if (isEditing) "编辑交易" else "新增交易") })
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
            // 类型
            ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = !typeExpanded }) {
                OutlinedTextField(
                    value = if (type == TradeType.BUY) "买入" else "卖出",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("交易类型") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    TradeType.values().forEach { t ->
                        DropdownMenuItem(text = { Text(if (t == TradeType.BUY) "买入" else "卖出") }, onClick = {
                            viewModel.onTypeChange(t); typeExpanded = false
                        })
                    }
                }
            }

            // Asset selection dropdown
            var assetExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = assetExpanded, onExpandedChange = { assetExpanded = !assetExpanded }) {
                OutlinedTextField(
                    value = viewModel.selectedAssetId.collectAsState().value?.let { id ->
                        viewModel.assets.collectAsState().value.firstOrNull { it.id == id }?.name ?: ""
                    } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("选择资产 (可选)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = assetExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                DropdownMenu(expanded = assetExpanded, onDismissRequest = { assetExpanded = false }) {
                    DropdownMenuItem(text = { Text("无") }, onClick = { viewModel.onAssetSelected(null); assetExpanded = false })
                    viewModel.assets.collectAsState().value.forEach { asset ->
                        DropdownMenuItem(text = { Text(asset.name) }, onClick = {
                            viewModel.onAssetSelected(asset); assetExpanded = false
                        })
                    }
                }
            }
            OutlinedTextField(value = shares, onValueChange = viewModel::onSharesChange, label = { Text("份额") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = price, onValueChange = viewModel::onPriceChange, label = { Text("价格") }, modifier = Modifier.fillMaxWidth())

            OutlinedTextField(value = fee, onValueChange = viewModel::onFeeChange, label = { Text("手续费") }, modifier = Modifier.fillMaxWidth())

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

                Button(onClick = {
                    coroutineScope.launch {
                        val success = viewModel.save()
                        if (success) {
                            navController.navigateUp()
                        } else {
                            Toast.makeText(context, "请先选择资产", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("保存") }
            }
        }
    }
}
