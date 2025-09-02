package com.example.strategicassetallocationassistant

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

/**
 * 配置备注界面
 * 用于编辑资产配置备注
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigNoteScreen(
    navController: NavController,
    viewModel: PortfolioViewModel,
    modifier: Modifier = Modifier
) {
    val portfolio by viewModel.portfolioState.collectAsState()
    var noteInput by remember(portfolio.note) { mutableStateOf(portfolio.note ?: "") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("配置备注") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = noteInput,
                onValueChange = { noteInput = it },
                label = { Text("资产配置备注") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                minLines = 6
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 总体风险因子计算过程展示
            OverallRiskFactorSection(
                overallRiskFactorLog = portfolio.overallRiskFactorLog,
                overallRiskFactor = portfolio.overallRiskFactor,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = {
                    viewModel.updateNote(noteInput)
                    navController.navigateUp()
                }) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("保存")
                }
            }
        }
    }
}

/**
 * 总体风险因子计算过程展示组件
 */
@Composable
private fun OverallRiskFactorSection(
    overallRiskFactorLog: String?,
    overallRiskFactor: Double?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 标题
        Text(
            text = "总体风险因子",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        // 当前风险因子值
        if (overallRiskFactor != null) {
            Text(
                text = "当前值: ${String.format("%.3f", overallRiskFactor)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        // 计算过程
        if (!overallRiskFactorLog.isNullOrBlank()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "计算过程:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                OutlinedTextField(
                    value = overallRiskFactorLog.replace("; ", "\n"),
                    onValueChange = { /* 只读 */ },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                )
            }
        } else {
            Text(
                text = "暂无计算过程数据。请先刷新市场数据。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

