package com.example.strategicassetallocationassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.strategicassetallocationassistant.ui.theme.StrategicAssetAllocationAssistantTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

// 显示单个资产的组件
@Composable
fun AssetItem(
    asset: Asset,
    marketValue: Double, // 直接接收预先计算好的市值
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // 资产名称和类型
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = asset.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = when (asset.type) {
                        AssetType.MONEY_FUND -> "货币基金"
                        AssetType.OFFSHORE_FUND -> "场外基金"
                        AssetType.STOCK -> "股票"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 持仓信息显示
            asset.code?.let {
                Text("资产代码: $it", style = MaterialTheme.typography.bodyMedium)
            }
            asset.shares?.let {
                Text("份额: $it", style = MaterialTheme.typography.bodyMedium)
            }
            when (asset.type) {
                AssetType.STOCK -> asset.unitValue?.let {
                    Text("每股价格: ¥$it", style = MaterialTheme.typography.bodyMedium)
                }
                AssetType.OFFSHORE_FUND -> asset.unitValue?.let {
                    Text("净值: $it", style = MaterialTheme.typography.bodyMedium)
                }
                else -> {}
            }
            asset.lastUpdateTime?.let {
                Text("更新时间: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 目标占比和市场价值
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "目标占比: ${(asset.targetWeight * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "市值: ¥${String.format("%.2f", marketValue)}", // 使用传入的市值
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// 主屏幕组件
@Composable
fun AssetListScreen(
    viewModel: PortfolioViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val portfolio by viewModel.portfolioState.collectAsState() // 观察顶层Portfolio状态
    val assetId2Value by viewModel.assetId2Value.collectAsState() // 获取市值映射表

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = "战略资产配置助手",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 显示总现金
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "可用现金",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "¥${String.format("%.2f", portfolio.cash)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }


        // 资产列表
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(portfolio.assets) { asset -> // 使用从Portfolio中解构出的assets列表
                // 从映射表中查找当前资产的市值，如果找不到则默认为0.0
                val value = assetId2Value[asset.id] ?: 0.0
                AssetItem(asset = asset, marketValue = value)
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StrategicAssetAllocationAssistantTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AssetListScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}