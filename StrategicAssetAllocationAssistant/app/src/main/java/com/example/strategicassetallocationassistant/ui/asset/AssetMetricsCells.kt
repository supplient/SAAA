package com.example.strategicassetallocationassistant

import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.Alignment
import java.time.format.DateTimeFormatter
import com.example.strategicassetallocationassistant.ui.common.model.AssetInfo

/**
 * 可复用的单元格组件库，用于在不同列/组合列中显示相同的资产指标数据。
 */
object AssetMetricsCells {

    /** 买入因子（百分比，颜色 Primary） */
    @Composable
    fun BuyFactor(info: AssetInfo) {
        Text(
            text = info.buyFactor?.let { "🏷️${String.format("%.2f", it * 100)}" } ?: "-",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }

    /** 卖出阈值（百分比，颜色 Tertiary） */
    @Composable
    fun SellThreshold(info: AssetInfo) {
        Text(
            text = info.sellThreshold?.let { "+${String.format("%.2f", it * info.asset.targetWeight * 100)}%卖" } ?: "-",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary,
            textAlign = TextAlign.Center
        )
    }

    /** 波动率（百分比，默认颜色） */
    @Composable
    fun Volatility(info: AssetInfo) {
        Text(
            text = info.volatility?.let { "〰️${String.format("%.2f%%", it * 100)}" } ?: "-",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }

    /** 七日涨跌幅（颜色正:Primary 负:Error） */
    @Composable
    fun SevenDayReturn(info: AssetInfo) {
        val pct = info.sevenDayReturn
        if (pct != null) {
            Text(
                text = "七${if (pct >= 0) "+" else ""}${String.format("%.2f%%", pct * 100)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (pct >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        } else {
            Text("-", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }

    /** 相对偏移 r */
    @Composable
    fun RelativeOffset(info: AssetInfo) {
        Text(
            text = info.relativeOffset?.let { String.format("R%.2f%%", it*100) } ?: "-",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }

    /** 偏移因子 E */
    @Composable
    fun OffsetFactor(info: AssetInfo) {
        Text(
            text = info.offsetFactor?.let { String.format("偏因%.2f%%", it*100) } ?: "-",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }

    /** 跌幅因子 D */
    @Composable
    fun DrawdownFactor(info: AssetInfo) {
        Text(
            text = info.drawdownFactor?.let { String.format("跌因%.2f%%", it*100) } ?: "-",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary,
            textAlign = TextAlign.Center
        )
    }

    /** 去波动买入因子 */
    @Composable
    fun PreVolatilityBuyFactor(info: AssetInfo) {
        Text(
            text = info.preVolatilityBuyFactor?.let { String.format("🏷️%.2f%%", it*100) } ?: "-",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
    }

    /** 资产风险 */
    @Composable
    fun AssetRisk(info: AssetInfo) {
        Text(
            text = info.assetRisk?.let { String.format("🎲%.6f", it) } ?: "-",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }

    /** 当前占比 */
    @Composable
    fun CurrentWeight(info: AssetInfo) {
        Text(
            text = String.format("%.2f%%", info.currentWeight * 100),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }

    /** 目标占比 */
    @Composable
    fun TargetWeight(info: AssetInfo) {
        Text(
            text = "= ${(info.asset.targetWeight * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }

    /** 占比偏差（颜色根据正负） */
    @Composable
    fun WeightDeviation(info: AssetInfo) {
        val dev = info.deviationPct
        Text(
            text = "${if (dev >= 0) "+" else ""}${String.format("%.2f%%", dev * 100)}",
            style = MaterialTheme.typography.bodySmall,
            color = if (dev >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }

    /** 单价 */
    @Composable
    fun UnitPrice(info: AssetInfo) {
        Text(
            text = "¥${String.format("%.3f", info.asset.unitValue ?: 0.0)}",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }

    /** 份额 */
    @Composable
    fun Shares(info: AssetInfo, isHidden: Boolean) {
        Text(
            text = if (isHidden) "***" else "×${String.format("%.2f", info.asset.shares ?: 0.0)}",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }

    /** 当前市值 */
    @Composable
    fun CurrentMarketValue(info: AssetInfo, isHidden: Boolean) {
        Text(
            text = if (isHidden) "***" else "¥${String.format("%.3f", info.marketValue)}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }

    /** 目标市值 */
    @Composable
    fun TargetMarketValue(info: AssetInfo, isHidden: Boolean) {
        Text(
            text = if (isHidden) "***" else "= ¥${String.format("%.3f", info.targetMarketValue)}",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }

    /** 市值偏差（颜色根据正负） */
    @Composable
    fun MarketValueDeviation(info: AssetInfo, isHidden: Boolean) {
        val dev = info.deviationValue
        Text(
            text = if (isHidden) "***" else "${if (dev >= 0) "+" else ""}¥${String.format("%.3f", dev)}",
            style = MaterialTheme.typography.bodySmall,
            color = if (dev >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }

    /** 更新时间（时分秒） */
    @Composable
    fun UpdateTimeClock(info: AssetInfo) {
        val time = info.asset.lastUpdateTime
        Text(
            text = time?.format(DateTimeFormatter.ofPattern("HH:mm:ss")) ?: "-",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            textAlign = TextAlign.Center
        )
    }

    /** 更新时间（日期） */
    @Composable
    fun UpdateTimeDate(info: AssetInfo) {
        val time = info.asset.lastUpdateTime
        if (time != null) {
            Text(
                text = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                textAlign = TextAlign.Center
            )
        }
    }

    /** 备注 */
    @Composable
    fun Note(info: AssetInfo) {
        Text(
            text = info.asset.note ?: "",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }

    /** 资产名称 + 错误指示 */
    @Composable
    fun AssetName(info: AssetInfo) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = info.asset.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            if (info.isRefreshFailed) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "刷新失败",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
    
    /**
     * 排序图标组件
     * 用于在表头显示排序方向
     */
    @Composable
    fun SortIcon(
        isAscending: Boolean,
        modifier: Modifier = Modifier
    ) {
        Icon(
            imageVector = if (isAscending) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (isAscending) "升序" else "降序",
            tint = MaterialTheme.colorScheme.primary,
            modifier = modifier.size(16.dp)
        )
    }
}


