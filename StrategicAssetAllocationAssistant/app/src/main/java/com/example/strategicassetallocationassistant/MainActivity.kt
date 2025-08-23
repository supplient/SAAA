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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDateTime
import java.util.UUID

// 投资组合ViewModel
class PortfolioViewModel : ViewModel() {

    private val _portfolio = MutableStateFlow(createSamplePortfolio())
    val portfolioState: StateFlow<Portfolio> = _portfolio.asStateFlow()

    // 从顶层Portfolio StateFlow中派生出资产列表的Flow
    private val assets: StateFlow<List<Asset>> = portfolioState.map { it.assets }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 当资产列表更新时，自动计算每个资产的市值，并创建一个ID到市值的映射
    val assetId2Value: StateFlow<Map<UUID, Double>> = assets.map { assetList ->
        assetList.associate { asset ->
            asset.id to asset.currentMarketValue
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    // 创建样本投资组合数据
    private fun createSamplePortfolio(): Portfolio {
        val now = LocalDateTime.now()

        val sampleAssets = listOf(
            // 股票组合（这是一个没有position的资产，其价值为0）
            Asset(
                id = UUID.randomUUID(),
                name = "股票组合",
                type = AssetType.STOCK,
                targetWeight = 0.30, // 30%
                position = null
            ),
            // 股票1
            Asset(
                id = UUID.randomUUID(),
                name = "腾讯控股",
                type = AssetType.STOCK,
                targetWeight = 0.15,
                position = StockPosition(
                    code = "00700",
                    lastUpdateTime = now.minusHours(1),
                    shares = 200.0,
                    marketValue = 380.0
                )
            ),
            // 股票2
            Asset(
                id = UUID.randomUUID(),
                name = "阿里巴巴",
                type = AssetType.STOCK,
                targetWeight = 0.15,
                position = StockPosition(
                    code = "09988",
                    lastUpdateTime = now.minusHours(1),
                    shares = 300.0,
                    marketValue = 85.0
                )
            ),
            // 场外基金
            Asset(
                id = UUID.randomUUID(),
                name = "易方达蓝筹精选混合",
                type = AssetType.OFFSHORE_FUND,
                targetWeight = 0.30, // 30%
                position = OffshoreFundPosition(
                    code = "005827",
                    lastUpdateTime = now.minusDays(1),
                    shares = 1000.0,
                    netValue = 2.15
                )
            ),
            // 货币基金
            Asset(
                id = UUID.randomUUID(),
                name = "余额宝货币基金",
                type = AssetType.MONEY_FUND,
                targetWeight = 0.10, // 10%
                position = MoneyFundPosition(
                    code = "000198",
                    lastUpdateTime = now.minusDays(1),
                    shares = 50000.0
                )
            )
        )

        return Portfolio(
            assets = sampleAssets,
            cash = 10000.0 // 顶层现金
        )
    }
}

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

            // 持仓信息（根据类型显示不同信息）
            asset.position?.let { position ->
                // 显示资产代码
                Text(
                    text = "资产代码: ${position.code}",
                    style = MaterialTheme.typography.bodyMedium
                )

                // 根据具体类型显示详细信息
                when (position) {
                    is MoneyFundPosition -> {
                        Text(
                            text = "份额: ${position.shares}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    is OffshoreFundPosition -> {
                        Text(
                            text = "份额: ${position.shares}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "净值: ${position.netValue}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    is StockPosition -> {
                        Text(
                            text = "持股数量: ${position.shares}股",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "每股价格: ¥${position.marketValue}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // 显示更新时间（所有Position都有的共同属性）
                Text(
                    text = "更新时间: ${position.lastUpdateTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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