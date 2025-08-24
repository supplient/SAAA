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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import com.example.strategicassetallocationassistant.data.database.AppDatabase
import com.example.strategicassetallocationassistant.data.repository.PortfolioRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAssetScreen(
    navController: NavController,
    assetId: String?
) {
    val context = LocalContext.current
    // Provide repository
    val repository = remember {
        val db = AppDatabase.getDatabase(context)
        PortfolioRepository(db.assetDao(), db.portfolioDao())
    }

    // Create ViewModel with custom factory supplying repository and assetId
    val viewModel: AddEditAssetViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return AddEditAssetViewModel(repository, assetId) as T
        }
    })

    // Collect UI state from ViewModel
    val name by viewModel.name.collectAsState()
    val assetType by viewModel.type.collectAsState()
    val targetWeight by viewModel.targetWeightInput.collectAsState()
    val code by viewModel.code.collectAsState()
    val shares by viewModel.sharesInput.collectAsState()
    val unitValue by viewModel.unitValueInput.collectAsState()

    var typeExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = if (assetId.isNullOrBlank()) "添加资产" else "编辑资产")
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(onClick = { navController.navigateUp() }) {
                    Text("取消")
                }
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

            // 资产类型选择
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = !typeExpanded }
            ) {
                OutlinedTextField(
                    value = when (assetType) {
                        AssetType.MONEY_FUND -> "货币基金"
                        AssetType.OFFSHORE_FUND -> "场外基金"
                        AssetType.STOCK -> "股票"
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("资产类型") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                DropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    AssetType.values().forEach { type ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when (type) {
                                        AssetType.MONEY_FUND -> "货币基金"
                                        AssetType.OFFSHORE_FUND -> "场外基金"
                                        AssetType.STOCK -> "股票"
                                    }
                                )
                            },
                            onClick = {
                                viewModel.onTypeChange(type)
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

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
        }
    }
}
