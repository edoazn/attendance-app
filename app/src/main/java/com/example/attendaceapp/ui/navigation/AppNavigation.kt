package com.example.attendaceapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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
import com.example.attendaceapp.ui.screens.auth.AuthViewModel
import com.example.attendaceapp.ui.screens.auth.LoginPage
import com.example.attendaceapp.ui.screens.history.HistoryPage
import com.example.attendaceapp.ui.screens.lecturer.CreateStudentScreen
import com.example.attendaceapp.ui.screens.lecturer.LecturerDashboardScreen
import com.example.attendaceapp.ui.screens.profile.ProfilePage
import com.example.attendaceapp.ui.screens.schedule.SchedulePage
import com.example.attendaceapp.ui.screens.student.StudentDashboardScreen

sealed class Screen(val route: String) {

    // Auth
    data object Login : Screen("login")

    // Student Routes
    data object StudentDashboard : Screen("student_dashboard")
    data object Schedule : Screen("schedule")
    data object Attendance : Screen("attendance")
    data object History : Screen("history")
    data object Profile : Screen("profile")

    // Lecturer Routes
    data object LecturerDashboard : Screen("lecturer_dashboard")
    data object CreateStudent : Screen("create_student")
    data object QRGenerator : Screen("qr_generator")
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
                    onNavigateToLecturerDashboard = {
                        navController.navigate(Screen.LecturerDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToStudentDashboard = {
                        navController.navigate(Screen.StudentDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // STUDENT ROUTES
            composable(Screen.StudentDashboard.route) {
                StudentDashboardScreen(
                    authViewModel = authViewModel,
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Schedule.route) {
                SchedulePage(scheduleList = DummyData.scheduleList)
            }
//            composable(Screen.Attendance.route) {
//                AttendancePage(
//                    onNavigateBack = { navController.popBackStack() },
//                    currentUser = { authViewModel.currentUser }
//                )
//            }
            composable(Screen.History.route) {
                HistoryPage()
            }
            composable(Screen.Profile.route) {
                ProfilePage()
            }

            // LECTURER ROUTES
            composable(Screen.LecturerDashboard.route) {
                LecturerDashboardScreen(
                    onNavigateToCreateStudent = {
                        navController.navigate(Screen.CreateStudent.route)
                    },
                    onNavigateToQRGenerator = {
                        navController.navigate(Screen.QRGenerator.route)
                    },
                    onNavigateToSessionDetail = { sessionId ->
                        // navController.navigate("session_detail/$sessionId")
                    },
                    viewModel = viewModel(),
                    currentUser = authViewModel.currentUser,
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.CreateStudent.route) {
                CreateStudentScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
//            composable(Screen.QRGenerator.route) {
//                QRGeneratorScreen(
//                    onNavigateBack = { navController.popBackStack() }
//                )
//            }
        }
    }
}