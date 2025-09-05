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
    portfolioCash: Double,
    onSaveCash: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var isIncrease by remember { mutableStateOf(true) }
    var deltaText by remember { mutableStateOf("") }
    var newCashText by remember { mutableStateOf(String.format("%.2f", portfolioCash)) }
    val newCashValid = newCashText.toDoubleOrNull()?.let { it >= 0 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑可用现金") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 第1行：+/- 按钮 与 变化量输入
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { isIncrease = !isIncrease },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isIncrease) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            contentColor = if (isIncrease) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(
                            if (isIncrease) "+" else "-",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    val deltaError = deltaText.isNotEmpty() && (deltaText.toDoubleOrNull()?.let { it < 0 } ?: true)
                    OutlinedTextField(
                        value = deltaText,
                        onValueChange = { text ->
                            deltaText = text
                            val delta = text.toDoubleOrNull()
                            if (delta != null && delta >= 0) {
                                val candidate = (if (isIncrease) portfolioCash + delta else portfolioCash - delta)
                                    .coerceAtLeast(0.0)
                                newCashText = String.format("%.2f", candidate)
                            }
                        },
                        label = { Text("变化量") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = deltaError,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 第2行：新的可用现金
                val newCashError = newCashText.isNotEmpty() && (newCashText.toDoubleOrNull()?.let { it < 0 } ?: true)
                OutlinedTextField(
                    value = newCashText,
                    onValueChange = { text ->
                        newCashText = text
                        val newCash = text.toDoubleOrNull()
                        if (newCash != null && newCash >= 0) {
                            isIncrease = newCash >= portfolioCash
                            val delta = abs(newCash - portfolioCash)
                            deltaText = String.format("%.2f", delta)
                        }
                    },
                    label = { Text("新的可用现金") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = newCashError,
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
            Button(enabled = newCashValid, onClick = {
                val newCash = newCashText.toDoubleOrNull()
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

