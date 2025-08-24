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
                    }
                }
            }
        }
    }
}