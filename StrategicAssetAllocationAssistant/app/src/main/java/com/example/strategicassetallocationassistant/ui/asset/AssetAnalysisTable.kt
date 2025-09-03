package com.example.strategicassetallocationassistant

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import java.util.UUID

/**
 * 资产分析表格组件
 * 使用通用表格组件，配置分析表特有的列组合
 */
@Composable
fun AssetAnalysisTable(
    analyses: List<PortfolioViewModel.AssetInfo>,
    onEditAsset: (UUID) -> Unit,
    modifier: Modifier = Modifier
) {
    val columns = remember {
        listOf(
            CommonAssetColumns.assetNameColumn(),
            CommonAssetColumns.weightColumn(),
            CommonAssetColumns.sevenDayReturnColumn(),
            CommonAssetColumns.volatilityColumn(),
            CommonAssetColumns.buyFactorColumn(),
            CommonAssetColumns.sellThresholdColumn(),
            CommonAssetColumns.relativeOffsetColumn(),
            CommonAssetColumns.offsetFactorColumn(),
            CommonAssetColumns.drawdownFactorColumn(),
            CommonAssetColumns.preVolatilityBuyFactorColumn(),
            CommonAssetColumns.assetRiskColumn()
        )
    }
    
    val behavior = remember(onEditAsset) {
        AssetTableBehavior(
            onRowLongClick = onEditAsset
        )
    }
    
    GenericAssetTable(
        analyses = analyses,
        columns = columns,
        behavior = behavior,
        modifier = modifier
    )
}



