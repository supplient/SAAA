// New file: defines navigation routes for the app
package com.example.strategicassetallocationassistant.navigation

sealed class NavRoutes(val route: String) {
    object AssetList : NavRoutes("asset_list")
    object AddAsset : NavRoutes("add_asset")
    object EditAsset : NavRoutes("edit_asset/{assetId}") {
        const val ARG_ASSET_ID = "assetId"
        fun createRoute(assetId: String): String = "edit_asset/$assetId"
    }
    object ApiTest : NavRoutes("api_test")
    object TransactionList : NavRoutes("transaction_list")
    object AddTransaction : NavRoutes("add_transaction")
    object EditTransaction : NavRoutes("edit_transaction/{transactionId}") {
        const val ARG_TX_ID = "transactionId"
        fun createRoute(txId: String): String = "edit_transaction/$txId"
    }
}
