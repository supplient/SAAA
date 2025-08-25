package com.example.strategicassetallocationassistant.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, viewModel: SettingsViewModel = hiltViewModel()) {
    val interval by viewModel.intervalMinutes.collectAsState()
    val options = listOf(0L, 15L, 30L, 60L, 120L)
    var showImportDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SettingsViewModel.Event.ExportSuccess -> {
                    scope.launch { snackbarHostState.showSnackbar("已复制到剪贴板") }
                }
                is SettingsViewModel.Event.ImportSuccess -> {
                    showImportDialog = false
                    scope.launch { snackbarHostState.showSnackbar("导入成功") }
                }
                is SettingsViewModel.Event.ImportError -> {
                    scope.launch { snackbarHostState.showSnackbar("导入失败: ${event.message}") }
                }
            }
        }
    }

    if (showImportDialog) {
        ImportDialog(
            onDismiss = { showImportDialog = false },
            onConfirm = { json -> viewModel.importData(json) }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("设置") }, navigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        })

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            // 下拉菜单选择后台刷新频率
            var expanded by remember { mutableStateOf(false) }
            val labels = options.map { if (it == 0L) "关闭" else "${it} 分钟" }

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                androidx.compose.material3.OutlinedTextField(
                    value = labels[options.indexOf(interval)],
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("后台刷新频率") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                androidx.compose.material3.DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEachIndexed { idx, option ->
                        DropdownMenuItem(
                            text = { Text(labels[idx]) },
                            onClick = {
                                viewModel.onIntervalSelected(option)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Button(onClick = { viewModel.exportData() }, contentPadding = PaddingValues(8.dp)) {
                Text("导出数据到剪贴板")
            }
            Button(onClick = { showImportDialog = true }, contentPadding = PaddingValues(8.dp)) {
                Text("从剪贴板导入数据")
            }
        }
    }

    SnackbarHost(hostState = snackbarHostState, modifier = Modifier.fillMaxSize())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入数据") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("在此处粘贴JSON数据") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text("导入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
