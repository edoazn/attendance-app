package com.example.attendaceapp.ui.navigation

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: Int,
) {
    data object Home : BottomNavItem(
        route = "student_dashboard",
        title = "Home",
        icon = com.example.attendaceapp.R.drawable.ic_home_24,
    )

    data object Schedule : BottomNavItem(
        route = "schedule",
        title = "Schedule",
        icon = com.example.attendaceapp.R.drawable.ic_note_24,
    )

    data object Attendance : BottomNavItem(
        route = "attendance",
        title = "Attendance",
        icon = com.example.attendaceapp.R.drawable.baseline_document_scanner_24,
    )

    data object History : BottomNavItem(
        route = "history",
        title = "History",
        icon = com.example.attendaceapp.R.drawable.ic_history_24,
    )

    data object Profile : BottomNavItem(
        route = "profile",
        title = "Profile",
        icon = com.example.attendaceapp.R.drawable.baseline_person_24,
    )
}