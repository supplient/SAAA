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
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.strategicassetallocationassistant.TransactionListScreen
import com.example.strategicassetallocationassistant.AddEditTransactionScreen
import com.example.strategicassetallocationassistant.settings.SettingsScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StrategicAssetAllocationAssistantTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.AssetList.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(NavRoutes.AssetList.route) {
                            val viewModel: PortfolioViewModel = hiltViewModel()
                            AssetListScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize(),
                                onAddAsset = {
                                    navController.navigate(NavRoutes.AddAsset.route)
                                },
                                onEditAsset = { id ->
                                    navController.navigate(NavRoutes.EditAsset.createRoute(id.toString()))
                                },
                                onOpenApiTest = {
                                    navController.navigate(NavRoutes.ApiTest.route)
                                },
                                onOpenTransactions = {
                                    navController.navigate(NavRoutes.TransactionList.route)
                                },
                                onOpenSettings = {
                                    navController.navigate(NavRoutes.Settings.route)
                                },
                                onOpenOpportunities = {
                                    navController.navigate(NavRoutes.TradingOpportunities.route)
                                },
                                onAddTransactionForAsset = { assetId ->
                                    navController.navigate(NavRoutes.AddTransactionForAsset.createRoute(assetId.toString()))
                                },
                                onOpenConfigNote = {
                                    navController.navigate(NavRoutes.ConfigNote.route)
                                },
                                onOpenAssetAnalysis = {
                                    navController.navigate(NavRoutes.AssetAnalysis.route)
                                }
                            )
                        }

                        composable(NavRoutes.AddAsset.route) {
                            AddEditAssetScreen(navController = navController)
                        }

                        composable(
                            route = NavRoutes.EditAsset.route,
                            arguments = listOf(navArgument(NavRoutes.EditAsset.ARG_ASSET_ID) {
                                type = NavType.StringType
                            })
                        ) {
                            AddEditAssetScreen(navController)
                        }

                        composable(NavRoutes.ApiTest.route) {
                            ApiTestScreen(navController = navController)
                        }

                        composable(NavRoutes.TransactionList.route) {
                            TransactionListScreen(
                                navToAdd = { navController.navigate(NavRoutes.AddTransaction.route) },
                                navToEdit = { navController.navigate(NavRoutes.EditTransaction.createRoute(it.toString())) }
                            )
                        }

                        composable(NavRoutes.AddTransaction.route) {
                            AddEditTransactionScreen(navController = navController)
                        }

                        composable(
                            route = NavRoutes.EditTransaction.route,
                            arguments = listOf(navArgument(NavRoutes.EditTransaction.ARG_TX_ID) { type = NavType.StringType })
                        ) {
                            AddEditTransactionScreen(navController = navController)
                        }
                        // Add Settings composable
                        composable(NavRoutes.Settings.route) {
                            SettingsScreen(navController = navController)
                        }

                        composable(NavRoutes.TradingOpportunities.route) {
                            TradingOpportunityListScreen(
                                navController = navController,
                                onExecute = { op ->
                                    navController.navigate(NavRoutes.AddTransactionFromOpportunity.createRoute(op.id.toString()))
                                }
                            )
                        }

                        composable(
                            route = NavRoutes.AddTransactionFromOpportunity.route,
                            arguments = listOf(navArgument(NavRoutes.AddTransactionFromOpportunity.ARG_OP_ID) { type = NavType.StringType })
                        ) {
                            AddEditTransactionScreen(navController = navController)
                        }

                        composable(
                            route = NavRoutes.AddTransactionForAsset.route,
                            arguments = listOf(navArgument(NavRoutes.AddTransactionForAsset.ARG_ASSET_ID) { type = NavType.StringType })
                        ) {
                            AddEditTransactionScreen(navController = navController)
                        }
                        
                        // 配置备注界面
                        composable(NavRoutes.ConfigNote.route) {
                            val viewModel: PortfolioViewModel = hiltViewModel()
                            ConfigNoteScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                        
                        // 资产分析界面
                        composable(NavRoutes.AssetAnalysis.route) {
                            val viewModel: PortfolioViewModel = hiltViewModel()
                            AssetAnalysisScreen(
                                viewModel = viewModel,
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onEditAsset = { id ->
                                    navController.navigate(NavRoutes.EditAsset.createRoute(id.toString()))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}