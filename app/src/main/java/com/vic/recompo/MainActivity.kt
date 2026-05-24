package com.vic.recompo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vic.recompo.ui.Screen
import com.vic.recompo.ui.entreno.EntrenoScreen
import com.vic.recompo.ui.home.HomeScreen
import com.vic.recompo.ui.mediciones.MedicionesScreen
import com.vic.recompo.ui.nutricion.NutricionScreen
import com.vic.recompo.ui.settings.SettingsScreen
import com.vic.recompo.ui.theme.RecompoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecompoTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val bottomNavItems = listOf(
                    BottomNavItem(Screen.Home.route, "Home", Icons.Default.Home),
                    BottomNavItem(Screen.Nutricion.route, "Nutrición", Icons.Default.Restaurant),
                    BottomNavItem(Screen.Entreno.route, "Entreno", Icons.Default.FitnessCenter),
                    BottomNavItem(Screen.Mediciones.route, "Mediciones", Icons.Default.ShowChart),
                    BottomNavItem(Screen.Settings.route, "Ajustes", Icons.Default.Settings),
                )

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            bottomNavItems.forEach { item ->
                                NavigationBarItem(
                                    selected = currentRoute == item.route,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(Screen.Home.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Home.route) { HomeScreen() }
                        composable(Screen.Nutricion.route) { NutricionScreen() }
                        composable(Screen.Entreno.route) { EntrenoScreen() }
                        composable(Screen.Mediciones.route) { MedicionesScreen() }
                        composable(Screen.Settings.route) { SettingsScreen() }
                    }
                }
            }
        }
    }
}

private data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)
