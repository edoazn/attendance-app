package com.example.attendaceapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.attendaceapp.data.local.DummyData
import com.example.attendaceapp.ui.screens.attendace.AttendancePage
import com.example.attendaceapp.ui.screens.auth.AuthViewModel
import com.example.attendaceapp.ui.screens.auth.LoginPage
import com.example.attendaceapp.ui.screens.history.HistoryPage
import com.example.attendaceapp.ui.screens.home.HomePage
import com.example.attendaceapp.ui.screens.profile.ProfilePage
import com.example.attendaceapp.ui.screens.schedule.SchedulePage

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object StudentDashboard : Screen("student_dashboard")
    data object Schedule : Screen("schedule")
    data object Attendance : Screen("attendance")
    data object History : Screen("history")
    data object Profile : Screen("profile")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel()
) {
    val bottomDestinations = remember {
        setOf(
            Screen.StudentDashboard.route,
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
            startDestination = Screen.Login.route,
            modifier = Modifier.padding(padding)
        ) {
            // AUTH
            composable(Screen.Login.route) {
                LoginPage(
                    viewModel = authViewModel,
                    onLoginSuccess = {
                        navController.navigate(Screen.StudentDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // STUDENT ROUTES
            composable(Screen.StudentDashboard.route) {
                val currentUser by authViewModel.currentUser.collectAsState()

                HomePage(
                    user = currentUser
                )
            }

            composable(Screen.Schedule.route) {
                SchedulePage(scheduleList = DummyData.scheduleList, onPengajuanClick = {}, onAbsensiClick = {})
            }
            composable(Screen.Attendance.route) {
                val currentUser by authViewModel.currentUser.collectAsState()
                currentUser?.let { user ->
                    AttendancePage(
                        currentUser = user,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
            composable(Screen.History.route) {
                HistoryPage()
            }
            composable(Screen.Profile.route) {
                ProfilePage()
            }
        }
    }
}