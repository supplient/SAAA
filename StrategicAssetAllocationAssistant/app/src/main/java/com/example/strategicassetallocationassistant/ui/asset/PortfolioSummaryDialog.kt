@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.strategicassetallocationassistant

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.strategicassetallocationassistant.NonCashWeightView
import com.example.strategicassetallocationassistant.RiskFactorView

@Composable
fun PortfolioSummaryDialog(
    totalAssets: Double,
    portfolioCash: Double,
    targetWeightSum: Double,
    riskFactor: Double?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("资产总览") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 总资产
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "总资产", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "¥${String.format("%.2f", totalAssets)}", style = MaterialTheme.typography.bodyMedium)
                }

                // 除现金外占比
                val nonCashAssetsValue = totalAssets - portfolioCash
                val nonCashCurrentWeightSum = if (totalAssets > 0) nonCashAssetsValue / totalAssets else 0.0
                NonCashWeightView(
                    currentWeight = nonCashCurrentWeightSum,
                    targetWeight = targetWeightSum,
                    showLabel = true
                )

                // 可用现金
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "可用现金", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "¥${String.format("%.2f", portfolioCash)}", style = MaterialTheme.typography.bodyMedium)
                }

                // 总体风险因子
                RiskFactorView(
                    riskFactor = riskFactor,
                    showLabel = true
                )
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}
