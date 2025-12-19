package com.example.attendaceapp.data.local

import com.example.attendaceapp.ui.screens.schedule.ScheduleItem

object DummyData {
    val scheduleList = listOf(
        ScheduleItem(
            startTime = "08:00",
            endTime = "09:40",
            subject = "Web Programming",
            room = "U408",
            mode = "Tatap Muka",
            code = "24UBD02006",
            lecturer = "Ahmad Syazili M.Kom",
            isPresent = true
        ),
        ScheduleItem(
            startTime = "10:00",
            endTime = "11:40",
            subject = "Kalkulus 2",
            room = "U208",
            mode = "Daring",
            code = "24UBD02007",
            lecturer = "Diana S.Si, M.Kom",
            isPresent = false
        ),
        ScheduleItem(
            startTime = "13:00",
            endTime = "14:40",
            subject = "IT Entrepreneurship",
            room = "U308",
            mode = "Tatap Muka",
            code = "24UBD02008",
            lecturer = "Rasmila M.Kom",
            isPresent = false
        ),
        ScheduleItem(
            startTime = "15:00",
            endTime = "16:40",
            subject = "Mobile Programming",
            room = "U508",
            mode = "Tatap Muka",
            code = "24UBD02009",
            lecturer = "Budi Santoso M.Kom",
            isPresent = true
        )
    )
}


data class HistoryItem(
    val courseName: String,
    val room: String,
    val imageRes: Int,
)

data class AttendanceHistory(
    val courseName: String,
    val classType: String,
    val date: String,
    val timeRange: String,
    val room: String,
    val status: String,
    val lecturer: String,
    val checkInTime: String,
    val checkOutTime: String,
)