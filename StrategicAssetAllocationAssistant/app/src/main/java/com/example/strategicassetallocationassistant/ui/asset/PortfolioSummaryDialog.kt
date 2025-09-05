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
import kotlin.math.abs

@Composable
fun PortfolioSummaryDialog(
    totalAssets: Double,
    portfolioCash: Double,
    targetWeightSum: Double,
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
                val nonCashTargetWeightSum = targetWeightSum
                val nonCashWeightDeviation = nonCashCurrentWeightSum - nonCashTargetWeightSum
                val deviationAbs = abs(nonCashWeightDeviation)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "除现金外占比", style = MaterialTheme.typography.bodyMedium)
                    if (deviationAbs > 0.0001) {
                        Text(
                            text = "${String.format("%.1f", nonCashCurrentWeightSum * 100)}% = ${String.format("%.1f", nonCashTargetWeightSum * 100)}% ± ${String.format("%.1f", deviationAbs * 100)}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = "${String.format("%.1f", nonCashCurrentWeightSum * 100)}% = ${String.format("%.1f", nonCashTargetWeightSum * 100)}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 可用现金
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "可用现金", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "¥${String.format("%.2f", portfolioCash)}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}
