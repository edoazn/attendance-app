package com.example.attendaceapp.ui.screens.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attendaceapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulePage(
    scheduleList: List<ScheduleItem>,
) {
    var selectedDay by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("Jadwal", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    titleContentColor = Color.Black,
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            HorizontalDaySelector(
                selectedIndex = selectedDay,
                onDaySelected = { selectedDay = it }
            )
            Divider(color = Color.LightGray, thickness = 1.dp)

            // Daftar jadwal
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp)
            ) {
                itemsIndexed(scheduleList) { index, item ->
                    ScheduleTimelineItem(
                        item = item,
                        isFirst = index == 0,
                        isLast = index == scheduleList.lastIndex
                    )
                }
            }
        }
    }
}


data class ScheduleItem(
    val startTime: String,
    val endTime: String,
    val subject: String,
    val room: String,
    val mode: String,
    val code: String,
    val lecturer: String,
    val isPresent: Boolean = false
)

@Composable
fun HorizontalDaySelector(selectedIndex: Int, onDaySelected: (Int) -> Unit) {
    val days = listOf("Sen", "Sel", "Rab", "Kam", "Jum")

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        itemsIndexed(days) { index, day ->
            val isSelected = index == selectedIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onDaySelected(index) }
                    .background(if (isSelected) Color(0xFF0066FF).copy(alpha = 0.1f) else Color.Transparent)
                    .padding(8.dp)
            ) {
                Text(
                    text = day,
                    color = if (isSelected) Color(0xFF0066FF) else Color.Gray,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = (index + 2).toString(),
                    color = if (isSelected) Color(0xFF0066FF) else Color.Black,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun ScheduleTimelineItem(
    item: ScheduleItem,
    isFirst: Boolean,
    isLast: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Kolom waktu
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(50.dp)
        ) {
            Text(item.startTime, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(item.endTime, fontSize = 11.sp, color = Color.Gray)
        }
        Spacer(modifier = Modifier.width(4.dp))

        // Titik dan garis timeline
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(IntrinsicSize.Min),
            contentAlignment = Alignment.Center
        ) {
            // Garis vertikal nyambung antar item
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(Color(0xFF0066FF))
                    .align(Alignment.Center)
            )
            // Titik biru
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color(0xFF0066FF), shape = RoundedCornerShape(6.dp))
                    .align(Alignment.Center)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        // Card konten
        ScheduleCard(item)
    }
}

@Composable
fun ScheduleCard(item: ScheduleItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.subject,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                if (item.isPresent) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Hadir",
                        tint = Color(0xFF4CAF50)
                    )
                }
            }

            Text("${item.room} - ${item.mode}", color = Color.Gray)
            Text(item.code, fontSize = 12.sp, color = Color.Gray)
            Text(item.lecturer, fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { /*TODO*/ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEDEDED))
                ) {
                    Text("Pengajuan", color = Color.Black)
                }
                Button(
                    onClick = { /*TODO*/ },
                    colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.primary_color))
                ) {
                    Text("Absensi", color = Color.White)
                }
            }
        }
    }
}

@Preview
@Composable
fun SchedulePagePreview() {
    val sampleSchedule = listOf(
        ScheduleItem(
            startTime = "08:00",
            endTime = "09:30",
            subject = "Matematika Diskrit",
            room = "Ruang 101",
            mode = "Offline",
            code = "MD101",
            lecturer = "Dr. Andi",
            isPresent = true
        ),
        ScheduleItem(
            startTime = "10:00",
            endTime = "11:30",
            subject = "Algoritma dan Pemrograman",
            room = "Ruang 202",
            mode = "Online",
            code = "AP202",
            lecturer = "Prof. Budi"
        ),
    )
    SchedulePage(scheduleList = sampleSchedule)
}

