package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.ProductViewModel
import com.example.ui.screens.*
import com.example.ui.theme.PriceWalaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Turn on Edge to Edge full bleed layout support
        enableEdgeToEdge()
        
        setContent {
            val productViewModel: ProductViewModel = viewModel()
            val darkModeOn by productViewModel.darkMode.collectAsState()

            PriceWalaTheme(darkTheme = darkModeOn) {
                val navController = rememberNavController()
                
                NavHost(
                    navController = navController,
                    startDestination = "splash",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("splash") {
                        SplashScreen(navController = navController, viewModel = productViewModel)
                    }
                    composable("login") {
                        LoginScreen(navController = navController, viewModel = productViewModel)
                    }
                    composable("home") {
                        DashboardScreen(navController = navController, viewModel = productViewModel)
                    }
                    composable("scanner") {
                        ScannerScreen(navController = navController, viewModel = productViewModel)
                    }
                    composable(
                        route = "product_detail/{barcode}",
                        arguments = listOf(navArgument("barcode") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val barcode = backStackEntry.arguments?.getString("barcode") ?: ""
                        ProductDetailsScreen(barcode = barcode, navController = navController, viewModel = productViewModel)
                    }
                    composable(
                        route = "add_edit_product?barcode={barcode}",
                        arguments = listOf(
                            navArgument("barcode") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val barcode = backStackEntry.arguments?.getString("barcode")
                        AddEditProductScreen(barcodeParam = barcode, navController = navController, viewModel = productViewModel)
                    }
                    composable("reports") {
                        ReportsScreen(navController = navController, viewModel = productViewModel)
                    }
                    composable("settings") {
                        SettingsScreen(navController = navController, viewModel = productViewModel)
                    }
                }
            }
        }
    }
}
