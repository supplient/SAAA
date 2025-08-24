package com.example.strategicassetallocationassistant

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@Composable
fun AddEditAssetScreen(
    navController: NavController,
    assetId: String?
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = if (assetId.isNullOrBlank()) "添加资产（开发中）" else "编辑资产（开发中）", style = MaterialTheme.typography.headlineSmall)
    }
}
