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
}
