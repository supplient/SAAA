package com.example.strategicassetallocationassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import com.example.strategicassetallocationassistant.ui.theme.StrategicAssetAllocationAssistantTheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.strategicassetallocationassistant.data.database.AppDatabase
import com.example.strategicassetallocationassistant.data.repository.PortfolioRepository
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.strategicassetallocationassistant.navigation.NavRoutes
import androidx.navigation.NavType
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StrategicAssetAllocationAssistantTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // 手动构造 Repository -> ViewModel
                    val context = this@MainActivity
                    val db = remember { AppDatabase.getDatabase(context) }
                    val repository = remember { PortfolioRepository(db.assetDao(), db.portfolioDao()) }

                    // 自定义 ViewModelFactory
                    val vmFactory = remember(repository) {
                        object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                if (modelClass.isAssignableFrom(PortfolioViewModel::class.java)) {
                                    return PortfolioViewModel(repository) as T
                                }
                                throw IllegalArgumentException("Unknown ViewModel class")
                            }
                        }
                    }

                    val viewModel: PortfolioViewModel = viewModel(factory = vmFactory)

                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.AssetList.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(NavRoutes.AssetList.route) {
                            AssetListScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize(),
                                onAddAsset = {
                                    navController.navigate(NavRoutes.AddAsset.route)
                                },
                                onEditAsset = { id ->
                                    navController.navigate(NavRoutes.EditAsset.createRoute(id.toString()))
                                }
                            )
                        }

                        composable(NavRoutes.AddAsset.route) {
                            AddEditAssetScreen(navController = navController, assetId = null)
                        }

                        composable(
                            route = NavRoutes.EditAsset.route,
                            arguments = listOf(navArgument(NavRoutes.EditAsset.ARG_ASSET_ID) {
                                type = NavType.StringType
                            })
                        ) { backStackEntry ->
                            val assetIdArg = backStackEntry.arguments?.getString(NavRoutes.EditAsset.ARG_ASSET_ID)
                            AddEditAssetScreen(navController, assetIdArg)
                        }
                    }
                }
            }
        }
    }
}