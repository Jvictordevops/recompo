package com.vic.recompo.ui

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Nutricion : Screen("nutricion")
    data object Entreno : Screen("entreno")
    data object Mediciones : Screen("mediciones")
    data object Settings : Screen("settings")
    data object Actividad : Screen("actividad")
    data object Chat : Screen("chat")
}
