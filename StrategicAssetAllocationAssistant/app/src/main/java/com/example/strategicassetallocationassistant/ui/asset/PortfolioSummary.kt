package com.example.strategicassetallocationassistant

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 资产配置总体信息栏组件
 * 支持展开/折叠功能，显示总资产和可用现金信息
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioSummary(
    currentWeight: Double,
    targetWeight: Double,
    deviation: Double,
    availableCash: Double,
    riskFactor: Double?,
    isHidden: Boolean = false,
    totalAssets: Double,
    targetWeightSum: Double,
    onSaveCash: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCashEditDialog by remember { mutableStateOf(false) }
    var showSummaryDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
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
            // 左：可用现金
            Text(
                text = if (isHidden) "***" else "¥${String.format("%.2f", availableCash)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { showCashEditDialog = true }
            )

            // 中：总体风险因子（复用组件）
            com.example.strategicassetallocationassistant.RiskFactorView(
                riskFactor = riskFactor,
                showLabel = false,
                modifier = Modifier.clickable { showSummaryDialog = true }
            )

            // 右：资产占比汇总（复用组件）
            com.example.strategicassetallocationassistant.NonCashWeightView(
                currentWeight = currentWeight,
                targetWeight = targetWeight,
                showLabel = false,
                prefix = "Σ",
                modifier = Modifier.clickable { showSummaryDialog = true }
            )
        }
    }

    if (showCashEditDialog) {
        CashEditDialog(
            portfolioCash = availableCash,
            onSaveCash = onSaveCash,
            onDismiss = { showCashEditDialog = false }
        )
    }

    if (showSummaryDialog) {
        PortfolioSummaryDialog(
            totalAssets = totalAssets,
            portfolioCash = availableCash,
            targetWeightSum = targetWeightSum,
            riskFactor = riskFactor,
            onDismiss = { showSummaryDialog = false }
        )
    }
}
