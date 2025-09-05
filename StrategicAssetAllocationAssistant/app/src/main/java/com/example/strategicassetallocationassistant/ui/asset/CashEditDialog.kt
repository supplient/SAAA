@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.strategicassetallocationassistant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun CashEditDialog(
    totalAssets: Double,
    portfolioCash: Double,
    targetWeightSum: Double,
    onSaveCash: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var cashInputValue by remember { mutableStateOf(portfolioCash.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑可用现金") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "总资产", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "¥${String.format("%.2f", totalAssets)}", style = MaterialTheme.typography.bodyMedium)
                }

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

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = cashInputValue,
                    onValueChange = { cashInputValue = it },
                    label = { Text("可用现金") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        confirmButton = {
            Button(onClick = {
                val newCash = cashInputValue.toDoubleOrNull()
                if (newCash != null && newCash >= 0) {
                    onSaveCash(newCash)
                }
                onDismiss()
            }) {
                Text("保存")
            }
        }
    )
}

