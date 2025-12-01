package com.example.attendaceapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.attendaceapp.data.local.DummyData
import com.example.attendaceapp.ui.screens.attendace.AttendancePage
import com.example.attendaceapp.ui.screens.auth.LoginPage
import com.example.attendaceapp.ui.screens.auth.RegisterPage
import com.example.attendaceapp.ui.screens.history.HistoryPage
import com.example.attendaceapp.ui.screens.home.HomePage
import com.example.attendaceapp.ui.screens.profile.ProfilePage
import com.example.attendaceapp.ui.screens.schedule.SchedulePage
import com.example.attendaceapp.ui.screens.welcome.WelcomePage

sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Home : Screen("home")
    data object Schedule : Screen("schedule")
    data object Attendance : Screen("attendance")
    data object History : Screen("history")
    data object Profile : Screen("profile")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val bottomDestinations = remember {
        setOf(
            Screen.Home.route,
            Screen.Schedule.route,
            Screen.Attendance.route,
            Screen.History.route,
            Screen.Profile.route
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination: NavDestination? = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route in bottomDestinations

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    items = listOf(
                        BottomNavItem.Home,
                        BottomNavItem.Schedule,
                        BottomNavItem.Attendance,
                        BottomNavItem.History,
                        BottomNavItem.Profile
                    ),
                    currentDestination = currentDestination,
                    onItemClick = { item ->
                        navController.navigate(item.route) {
                            // jaga state tiap tab
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Welcome.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Welcome.route) {
                WelcomePage(
                    onLoginClick = { navController.navigate(Screen.Login.route) },
                    onRegisterClick = { navController.navigate(Screen.Register.route) }
                )
            }
            composable(Screen.Login.route) {
                LoginPage(
                    onBackClick = { navController.popBackStack() },
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onRegisterClick = { navController.navigate(Screen.Register.route) }
                )
            }
            composable(Screen.Register.route) {
                RegisterPage(
                    onBackClick = { navController.popBackStack() },
                    onLoginClick = { navController.navigate(Screen.Login.route) }
                )
            }

            // Bottom tabs
            composable(Screen.Home.route) { HomePage() }
            composable(Screen.Schedule.route) {
                SchedulePage(
                    scheduleList = DummyData.scheduleList
                )
            }
            composable(Screen.Attendance.route) { AttendancePage() }
            composable(Screen.History.route) { HistoryPage() }
            composable(Screen.Profile.route) { ProfilePage() }
        }
    }
}