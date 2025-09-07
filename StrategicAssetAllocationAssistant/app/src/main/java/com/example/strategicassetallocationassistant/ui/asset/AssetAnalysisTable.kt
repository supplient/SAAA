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
    analyses: List<com.example.strategicassetallocationassistant.ui.common.model.AssetInfo>,
    isHidden: Boolean,
    onEditAsset: (UUID) -> Unit,
    showSortDialog: Boolean = false,
    onSortOptionSelected: ((com.example.strategicassetallocationassistant.PortfolioViewModel.SortOption) -> Unit)? = null,
    onDismissSortDialog: (() -> Unit)? = null,
    currentSort: com.example.strategicassetallocationassistant.PortfolioViewModel.SortOption? = null,
    currentSortColumnTitle: String? = null,
    isAscending: Boolean = false,
    onHeaderClick: ((com.example.strategicassetallocationassistant.AssetTableColumn) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val columns = remember {
        listOf(
            CommonAssetColumns.assetNameColumn(),
            CommonAssetColumns.weightColumn(),
            CommonAssetColumns.sevenDayReturnVolatilityRelativeOffsetCombinedColumn(),
            CommonAssetColumns.offsetFactorDrawdownFactorPreVolatilityBuyFactorCombinedColumn(),
            CommonAssetColumns.buyFactorSellThresholdAssetRiskCombinedColumn()
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
        isHidden = isHidden,
        showSortDialog = showSortDialog,
        onSortOptionSelected = onSortOptionSelected,
        onDismissSortDialog = onDismissSortDialog,
        currentSort = currentSort,
        currentSortColumnTitle = currentSortColumnTitle,
        isAscending = isAscending,
        onHeaderClick = onHeaderClick,
        modifier = modifier
    )
}



