package com.geo.navigation


sealed interface AppDestination {

    val route: String

    data object Login : AppDestination {
        override val route = "login"
    }

    data object Home : AppDestination {
        override val route = "home"
    }


}