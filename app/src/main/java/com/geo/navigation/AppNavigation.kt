package com.geo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.geo.auth.LoginScreen
import com.geo.messaging.RequestNotificationPermission
import com.geo.screen.FenceScreen
import com.geo.screen.MainScreen


@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = AppDestination.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        composable(AppDestination.Login.route) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(AppDestination.Home.route) {
                        popUpTo(AppDestination.Login.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(AppDestination.Home.route) {
            RequestNotificationPermission()
            MainScreen(
                onOpenFence = { groupId ->
                    navController.navigate(AppDestination.Fence.createRoute(groupId))
                }
            )
        }

        // groupId reaches FenceViewModel through SavedStateHandle
        composable(AppDestination.Fence.route) {
            FenceScreen(onBack = { navController.popBackStack() })
        }
    }
}
