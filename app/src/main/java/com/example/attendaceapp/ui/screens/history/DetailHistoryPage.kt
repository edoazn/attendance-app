package com.example.attendaceapp.ui.screens.history

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.attendaceapp.R
import com.example.attendaceapp.data.local.AttendanceHistory
import com.example.attendaceapp.ui.components.HistoryAttendanceCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailHistoryPage(
    modifier: Modifier = Modifier,
    attendanceList: List<AttendanceHistory> = emptyList()
) {
    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("Riwayat", fontWeight = FontWeight.Bold) },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    titleContentColor = Color.Black,
                    containerColor = Color.White
                ),
                windowInsets = androidx.compose.foundation.layout.WindowInsets(
                    0, 0, 0, 0
                )
            )
        },
    ) { paddingValues ->
        Column(
            modifier = modifier
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Date Picker
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .height(52.dp)
                    .border(1.dp, Color.Gray, RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    //Icon
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Date Icon",
                        tint = Color.Gray,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                    )
                    // Date
                    Text(
                        text = "Pilih Tanggal",
                        color = colorResource(R.color.gray),
                        modifier = Modifier
                            .padding(start = 14.dp)
                            .align(Alignment.CenterVertically)
                    )
                }
            }
            // Card Attendance History
            LazyColumn {
                items(attendanceList) { attendance ->
                    HistoryAttendanceCard(attendace = attendance)
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DetailHistoryPagePreview() {
    DetailHistoryPage(
        attendanceList = listOf(
            AttendanceHistory(
                courseName = "Matematika Diskrit",
                classType = "Tatap Muka",
                date = "12 Juni 2024",
                timeRange = "08:00 - 09:30",
                room = "Ruang 101",
                status = "Hadir",
                lecturer = "Dr. John Doe",
                checkInTime = "08:05",
                checkOutTime = "09:25"
            ),
            AttendanceHistory(
                courseName = "Algoritma dan Struktur Data",
                classType = "Daring",
                date = "12 Juni 2024",
                timeRange = "10:00 - 11:30",
                room = "Online",
                status = "Tidak Hadir",
                lecturer = "Prof. Jane Smith",
                checkInTime = "-",
                checkOutTime = "-"
            ),
            AttendanceHistory(
                courseName = "Basis Data",
                classType = "Tatap Muka",
                date = "12 Juni 2024",
                timeRange = "13:00 - 14:30",
                room = "Ruang 202",
                status = "Hadir",
                lecturer = "Dr. Emily Davis",
                checkInTime = "13:10",
                checkOutTime = "14:20"
            )
        )
    )
}