package com.example.strategicassetallocationassistant

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NonCashWeightView(
    currentWeight: Double,
    targetWeight: Double,
    showLabel: Boolean,
    prefix: String? = null,
    modifier: Modifier = Modifier,
    errorThreshold: Double = 0.0001
) {
    val deviation = currentWeight - targetWeight
    val deviationAbs = kotlin.math.abs(deviation)
    val sign = if (deviation >= 0) "+" else "-"
    val color = if (deviationAbs > errorThreshold) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    val left = prefix ?: ""
    val text = "$left${String.format("%.1f", currentWeight * 100)}% = ${String.format("%.1f", targetWeight * 100)}% $sign ${String.format("%.1f", deviationAbs * 100)}%"

    if (showLabel) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "除现金外占比", style = MaterialTheme.typography.bodyMedium)
            Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color)
        }
    } else {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color, modifier = modifier)
    }
}

@Composable
fun RiskFactorView(
    riskFactor: Double?,
    showLabel: Boolean,
    modifier: Modifier = Modifier
) {
    val rfText = "🚩${String.format("%.2f%%", (riskFactor ?: 0.0) * 100)}"
    if (showLabel) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "总体风险因子", style = MaterialTheme.typography.bodyMedium)
            Text(text = rfText, style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        Text(text = rfText, style = MaterialTheme.typography.bodyMedium, modifier = modifier)
    }
}



