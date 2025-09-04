package com.example.strategicassetallocationassistant

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import java.util.UUID

/**
 * 资产列表表格组件
 * 使用通用表格组件，配置AssetTable特有的列组合
 */
@Composable
fun AssetTable(
    analyses: List<com.example.strategicassetallocationassistant.ui.common.model.AssetInfo>,
    isHidden: Boolean,
    onAddTransaction: (UUID) -> Unit,
    onEditAsset: (UUID) -> Unit,
    modifier: Modifier = Modifier
) {
    val columns = remember {
        listOf(
            CommonAssetColumns.assetNameColumn(),
            CommonAssetColumns.weightColumn(),
            CommonAssetColumns.buyFactorSellThresholdCombinedColumn(),
            CommonAssetColumns.priceSharesVolatilityCombinedColumn(),
            CommonAssetColumns.marketValueColumn(),
            CommonAssetColumns.updateTimeColumn(),
            CommonAssetColumns.noteColumn()
        )
    }
    
    val behavior = remember(onAddTransaction, onEditAsset) {
        AssetTableBehavior(
            onRowClick = onAddTransaction,
            onRowLongClick = onEditAsset
        )
    }
    
    GenericAssetTable(
        analyses = analyses,
        columns = columns,
        behavior = behavior,
        isHidden = isHidden,
        modifier = modifier
    )
}