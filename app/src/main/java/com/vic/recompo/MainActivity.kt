package com.vic.recompo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vic.recompo.ui.Screen
import com.vic.recompo.ui.actividad.ActividadScreen
import com.vic.recompo.ui.actividad.ActividadViewModel
import com.vic.recompo.ui.actividad.ActividadViewModelFactory
import com.vic.recompo.ui.entreno.EntrenoScreen
import com.vic.recompo.ui.entreno.EntrenoViewModel
import com.vic.recompo.ui.entreno.EntrenoViewModelFactory
import com.vic.recompo.ui.home.HomeScreen
import com.vic.recompo.ui.home.HomeViewModel
import com.vic.recompo.ui.home.HomeViewModelFactory
import com.vic.recompo.ui.mediciones.MedicionesScreen
import com.vic.recompo.ui.mediciones.MedicionesViewModel
import com.vic.recompo.ui.mediciones.MedicionesViewModelFactory
import com.vic.recompo.data.ai.DescomponerComidaUseCase
import com.vic.recompo.data.ai.ParseComidaUseCase
import com.vic.recompo.domain.usecase.ChatUseCase
import com.vic.recompo.domain.usecase.GenerarSesionUseCase
import com.vic.recompo.ui.chat.ChatScreen
import com.vic.recompo.ui.chat.ChatViewModel
import com.vic.recompo.ui.chat.ChatViewModelFactory
import com.vic.recompo.ui.nutricion.NutricionScreen
import com.vic.recompo.ui.nutricion.NutricionViewModel
import com.vic.recompo.ui.nutricion.NutricionViewModelFactory
import com.vic.recompo.ui.settings.SettingsScreen
import com.vic.recompo.ui.settings.SettingsViewModel
import com.vic.recompo.ui.settings.SettingsViewModelFactory
import com.vic.recompo.ui.theme.RecompoTheme
import com.vic.recompo.ui.wizard.WizardScreen
import com.vic.recompo.ui.wizard.WizardViewModelFactory

class MainActivity : ComponentActivity() {

    private val app get() = application as App

    private val wizardViewModel by viewModels<com.vic.recompo.ui.wizard.WizardViewModel> {
        WizardViewModelFactory(app.userSettingsStore)
    }

    private val nutricionViewModel by viewModels<NutricionViewModel> {
        val schemaJson = app.assets.open("tools/registrar_comida.json").bufferedReader().use { it.readText() }
        val schemaDescomponer = app.assets.open("tools/descomponer_comida.json").bufferedReader().use { it.readText() }
        NutricionViewModelFactory(
            app.database.entradaComidaDao(),
            app.database.comidaBaseDao(),
            app.database.plantillaIngredienteDao(),
            app.database.ingredienteDao(),
            ParseComidaUseCase(app.claudeApi, schemaJson, app.database.usoIADao()),
            DescomponerComidaUseCase(app.claudeApi, schemaDescomponer, app.database.usoIADao())
        )
    }

    private val medicionesViewModel by viewModels<MedicionesViewModel> {
        MedicionesViewModelFactory(
            app.database.medicionDao(),
            app.userSettingsStore
        )
    }

    private val homeViewModel by viewModels<HomeViewModel> {
        HomeViewModelFactory(
            app.userSettingsStore,
            app.database.entradaComidaDao(),
            app.database.sesionDao(),
            app.database.actividadDao(),
            app.database.tipoSesionDao()
        )
    }

    private val actividadViewModel by viewModels<ActividadViewModel> {
        ActividadViewModelFactory(app.database.actividadDao())
    }

    private val entrenoViewModel by viewModels<EntrenoViewModel> {
        val schemaJson = app.assets.open("tools/generar_sesion.json").bufferedReader().use { it.readText() }
        EntrenoViewModelFactory(
            applicationContext,
            app.database.sesionDao(),
            app.database.ejercicioEnSesionDao(),
            app.database.serieDao(),
            app.database.ejercicioDao(),
            app.database.tipoSesionDao(),
            GenerarSesionUseCase(app.claudeApi, schemaJson, applicationContext, app.database.usoIADao())
        )
    }

    private val settingsViewModel by viewModels<SettingsViewModel> {
        SettingsViewModelFactory(application, app.userSettingsStore, app.database)
    }

    private val chatViewModel by viewModels<ChatViewModel> {
        ChatViewModelFactory(
            app.database.conversacionDao(),
            app.database.mensajeIADao(),
            app.database.usoIADao(),
            app.userSettingsStore,
            app.database.entradaComidaDao(),
            app.database.sesionDao(),
            app.database.actividadDao(),
            app.database.medicionDao(),
            app.database.tipoSesionDao(),
            ChatUseCase(app.claudeApi),
            applicationContext
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecompoTheme {
                val setupDone by app.userSettingsStore.setupDone.collectAsState(initial = null)

                when (setupDone) {
                    null -> Surface(Modifier.fillMaxSize()) { Box(Modifier.fillMaxSize()) }
                    false -> WizardScreen(viewModel = wizardViewModel)
                    true -> MainAppContent(
                    homeViewModel = homeViewModel,
                    nutricionViewModel = nutricionViewModel,
                    medicionesViewModel = medicionesViewModel,
                    actividadViewModel = actividadViewModel,
                    entrenoViewModel = entrenoViewModel,
                    settingsViewModel = settingsViewModel,
                    chatViewModel = chatViewModel
                )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun MainAppContent(
    homeViewModel: HomeViewModel,
    nutricionViewModel: com.vic.recompo.ui.nutricion.NutricionViewModel,
    medicionesViewModel: MedicionesViewModel,
    actividadViewModel: ActividadViewModel,
    entrenoViewModel: EntrenoViewModel,
    settingsViewModel: SettingsViewModel,
    chatViewModel: ChatViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Home.route, "Home", Icons.Default.Home),
        BottomNavItem(Screen.Nutricion.route, "Nutrición", Icons.Default.Restaurant),
        BottomNavItem(Screen.Entreno.route, "Entreno", Icons.Default.FitnessCenter),
        BottomNavItem(Screen.Actividad.route, "Actividad", Icons.AutoMirrored.Filled.DirectionsRun),
        BottomNavItem(Screen.Mediciones.route, "Mediciones", Icons.AutoMirrored.Filled.ShowChart),
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
                        label = null
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
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToEntreno = {
                        navController.navigate(Screen.Entreno.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToChat = {
                        navController.navigate(Screen.Chat.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Nutricion.route) { NutricionScreen(nutricionViewModel) }
            composable(Screen.Entreno.route) { EntrenoScreen(entrenoViewModel) }
            composable(Screen.Actividad.route) { ActividadScreen(actividadViewModel) }
            composable(Screen.Mediciones.route) { MedicionesScreen(medicionesViewModel) }
            composable(Screen.Settings.route) { SettingsScreen(settingsViewModel) }
            composable(Screen.Chat.route) {
                ChatScreen(chatViewModel, onNavigateBack = { navController.navigateUp() })
            }
        }
    }
}

private data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)
