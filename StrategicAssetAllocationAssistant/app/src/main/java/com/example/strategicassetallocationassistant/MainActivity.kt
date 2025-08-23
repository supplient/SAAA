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
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

// 资产类型枚举
enum class AssetType {
    MONEY_FUND,      // 货币基金
    OFFSHORE_FUND,   // 场外基金
    STOCK,           // 场内股票
    ASSET_PACKAGE    // 资产包
}

// 货币基金持仓信息
@Serializable
data class MoneyFundPosition(
    val fundCode: String,           // 基金编号
    val shares: Double,             // 份额
    val lastUpdateTime: LocalDateTime  // 份额更新时间
)

// 场外基金持仓信息
@Serializable
data class OffshoreFundPosition(
    val fundCode: String,           // 基金编号
    val shares: Double,             // 份额
    val netValue: Double,           // 净值
    val lastUpdateTime: LocalDateTime  // 净值更新时间
)

// 场内股票持仓信息
@Serializable
data class StockPosition(
    val stockCode: String,          // 股票编号
    val shares: Double,             // 份额（股数）
    val marketValue: Double,        // 市值（每股价格）
    val lastUpdateTime: LocalDateTime  // 市值更新时间
)

// 资产数据模型
@Serializable
data class Asset(
    val id: String,                 // 资产ID
    val name: String,               // 资产名称
    val parentId: String?,          // 父级资产ID
    val type: AssetType,            // 资产类型
    val targetWeight: Double,       // 目标占比（0.0-1.0）
    val moneyFundPosition: MoneyFundPosition? = null,      // 货币基金持仓信息
    val offshoreFundPosition: OffshoreFundPosition? = null, // 场外基金持仓信息
    val stockPosition: StockPosition? = null,              // 股票持仓信息
    val cash: Double? = null        // 现金金额（仅对现金资产有效）
) {
    // 计算当前市场价值
    val currentMarketValue: Double
        get() = when (type) {
            AssetType.MONEY_FUND -> moneyFundPosition?.shares ?: 0.0
            AssetType.OFFSHORE_FUND -> {
                val position = offshoreFundPosition
                if (position != null) position.shares * position.netValue else 0.0
            }
            AssetType.STOCK -> {
                val position = stockPosition
                if (position != null) position.shares * position.marketValue else 0.0
            }
            AssetType.ASSET_PACKAGE -> cash ?: 0.0
        }
}

// 投资组合ViewModel
class PortfolioViewModel : ViewModel() {

    // 硬编码的样本资产数据
    private val _assets = MutableStateFlow<List<Asset>>(createSampleAssets())
    val assets: StateFlow<List<Asset>> = _assets.asStateFlow()

    // 创建样本资产数据
    private fun createSampleAssets(): List<Asset> {
        val now = LocalDateTime.now()

        return listOf(
            // 现金资产
            Asset(
                id = "cash_001",
                name = "现金",
                parentId = null,
                type = AssetType.ASSET_PACKAGE,
                targetWeight = 0.05, // 5%
                cash = 10000.0
            ),

            // 货币基金
            Asset(
                id = "fund_001",
                name = "余额宝货币基金",
                parentId = null,
                type = AssetType.MONEY_FUND,
                targetWeight = 0.10, // 10%
                moneyFundPosition = MoneyFundPosition(
                    fundCode = "000198",
                    shares = 50000.0,
                    lastUpdateTime = now.minusDays(1)
                )
            ),

            // 场外基金
            Asset(
                id = "offshore_fund_001",
                name = "易方达蓝筹精选混合",
                parentId = null,
                type = AssetType.OFFSHORE_FUND,
                targetWeight = 0.30, // 30%
                offshoreFundPosition = OffshoreFundPosition(
                    fundCode = "005827",
                    shares = 1000.0,
                    netValue = 2.15,
                    lastUpdateTime = now.minusDays(1)
                )
            ),

            // 场内股票
            Asset(
                id = "stock_001",
                name = "贵州茅台",
                parentId = null,
                type = AssetType.STOCK,
                targetWeight = 0.25, // 25%
                stockPosition = StockPosition(
                    stockCode = "600519",
                    shares = 100.0,
                    marketValue = 1650.0,
                    lastUpdateTime = now.minusHours(2)
                )
            ),

            // 股票资产包（包含多只股票）
            Asset(
                id = "stock_package_001",
                name = "股票组合",
                parentId = null,
                type = AssetType.ASSET_PACKAGE,
                targetWeight = 0.30, // 30%
                cash = 150000.0
            ),

            // 股票包下的子股票
            Asset(
                id = "stock_002",
                name = "腾讯控股",
                parentId = "stock_package_001",
                type = AssetType.STOCK,
                targetWeight = 0.15,
                stockPosition = StockPosition(
                    stockCode = "00700",
                    shares = 200.0,
                    marketValue = 380.0,
                    lastUpdateTime = now.minusHours(1)
                )
            ),

            Asset(
                id = "stock_003",
                name = "阿里巴巴",
                parentId = "stock_package_001",
                type = AssetType.STOCK,
                targetWeight = 0.15,
                stockPosition = StockPosition(
                    stockCode = "09988",
                    shares = 300.0,
                    marketValue = 85.0,
                    lastUpdateTime = now.minusHours(1)
                )
            )
        )
    }
}

// 显示单个资产的组件
@Composable
fun AssetItem(asset: Asset, modifier: Modifier = Modifier) {
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
                        AssetType.ASSET_PACKAGE -> "资产包"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 资产ID
            Text(
                text = "ID: ${asset.id}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 父级资产ID（如果有）
            asset.parentId?.let {
                Text(
                    text = "父级ID: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 持仓信息（根据类型显示不同信息）
            when (asset.type) {
                AssetType.MONEY_FUND -> {
                    asset.moneyFundPosition?.let { position ->
                        Text(
                            text = "基金代码: ${position.fundCode}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "份额: ${position.shares}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "更新时间: ${position.lastUpdateTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AssetType.OFFSHORE_FUND -> {
                    asset.offshoreFundPosition?.let { position ->
                        Text(
                            text = "基金代码: ${position.fundCode}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "份额: ${position.shares}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "净值: ${position.netValue}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "更新时间: ${position.lastUpdateTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AssetType.STOCK -> {
                    asset.stockPosition?.let { position ->
                        Text(
                            text = "股票代码: ${position.stockCode}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "持股数量: ${position.shares}股",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "每股价格: ¥${position.marketValue}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "更新时间: ${position.lastUpdateTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AssetType.ASSET_PACKAGE -> {
                    asset.cash?.let { cash ->
                        Text(
                            text = "现金金额: ¥${cash}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
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
                    text = "市值: ¥${String.format("%.2f", asset.currentMarketValue)}",
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
    val assets by viewModel.assets.collectAsState()

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

        // 资产列表
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(assets) { asset ->
                AssetItem(asset = asset)
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

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    StrategicAssetAllocationAssistantTheme {
        Greeting("Android")
    }
}