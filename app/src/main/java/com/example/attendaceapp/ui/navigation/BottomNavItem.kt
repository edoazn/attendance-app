package com.example.attendaceapp.ui.navigation

sealed class BottomNavItem(
    val route: String,
    val icon: Int,
    val title: String,
) {
    data object Home : BottomNavItem(
        route = "home",
        icon = com.example.attendaceapp.R.drawable.ic_home_24,
        title = "Home",
    )

    data object Schedule : BottomNavItem(
        route = "schedule",
        icon = com.example.attendaceapp.R.drawable.ic_note_24,
        title = "Schedule",
    )

    data object Attendance : BottomNavItem(
        route = "attendance",
        icon = com.example.attendaceapp.R.drawable.baseline_document_scanner_24,
        title = "Attendance",
    )

    data object History : BottomNavItem(
        route = "history",
        icon = com.example.attendaceapp.R.drawable.ic_history_24,
        title = "History",
    )

    data object Profile : BottomNavItem(
        route = "profile",
        icon = com.example.attendaceapp.R.drawable.baseline_person_24,
        title = "Profile",
    )
}