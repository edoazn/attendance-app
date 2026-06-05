package com.example.attendaceapp.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.attendaceapp.R
import com.example.attendaceapp.data.model.User
import com.example.attendaceapp.ui.components.AnnouncementCard
import com.example.attendaceapp.ui.components.ScheduleCard
import com.example.attendaceapp.ui.components.StatisticsCard
import com.example.attendaceapp.ui.navigation.Screen
import com.example.attendaceapp.ui.screens.schedule.ScheduleViewModel
import java.util.Calendar

@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    user: User? = null,
    onScheduleClick: (String) -> Unit = {},
    onSeeAllSchedules: () -> Unit = {},
    scheduleViewModel: ScheduleViewModel = viewModel(
        factory = ScheduleViewModel.factory(LocalContext.current)
    ),
) {
    val uiState by scheduleViewModel.uiState.collectAsState()

    // Muat jadwal hari ini saat pertama kali composable muncul
    LaunchedEffect(Unit) {
        scheduleViewModel.loadTodaySchedules()
    }

    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            // Header dengan warna primary
            Box(
                modifier = modifier
                    .height(158.dp)
                    .fillMaxWidth()
                    .clip(shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
                    .background(color = colorResource(id = R.color.primary_color))
                    .border(
                        border = BorderStroke(
                            2.dp,
                            color = colorResource(id = R.color.primary_color)
                        ),
                        shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp),
                    )
                    .padding(
                        top = 30.dp,
                        start = 24.dp,
                        end = 24.dp,
                        bottom = 16.dp
                    ),
                contentAlignment = Alignment.TopCenter,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            modifier = Modifier.padding(bottom = 4.dp),
                            text = "${greeting()}, ${user?.name ?: "Mahasiswa"}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(id = R.color.white),
                        )
                        Text(
                            text = user?.identityNumber ?: "-",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = colorResource(id = R.color.gray_400),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(color = colorResource(id = R.color.white))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifikasi",
                            tint = colorResource(id = R.color.primary_color),
                        )
                    }
                }
            }
        }

        // Statistics Card
        item {
            StatisticsCard(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .offset(y = (-50).dp)
            )
        }

        // Header "Jadwal Saya" + "See all"
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .offset(y = (-18).dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Jadwal Hari Ini",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Lihat semua",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.primary_color),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        // Konten jadwal hari ini
        when {
            uiState.isLoadingToday -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = colorResource(id = R.color.primary_color),
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    }
                }
            }

            uiState.todaySchedules.isEmpty() && !uiState.isLoadingToday -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Ilustrasi kalender kosong
                        Image(
                            painter = painterResource(id = R.drawable.no_schedule),
                            contentDescription = "Tidak ada jadwal",
                            modifier = Modifier.size(140.dp)
                        )

                        Text(
                            text = "Tidak ada jadwal hari ini",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            else -> {
                // Tampilkan maks 3 jadwal di home, sisanya bisa dilihat di SchedulePage
                items(uiState.todaySchedules.take(3)) { schedule ->
                    ScheduleCard(
                        modifier = Modifier.padding(vertical = 6.dp),
                        schedule = schedule,
                        onScheduleClick = {
                            val day = schedule.day ?: "Monday"
                            onScheduleClick(day)
                        }
                    )
                }
                if (uiState.todaySchedules.size > 3) {
                    item {
                        Text(
                            text = "+${uiState.todaySchedules.size - 3} jadwal lagi — lihat semua",
                            fontSize = 13.sp,
                            color = colorResource(id = R.color.primary_color),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        }

        // Spacer antara jadwal dan pengumuman
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 16.dp),
                text = "Pengumuman Terbaru",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        item {
            LazyRow(
                modifier = Modifier.padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(5) {
                    AnnouncementCard()
                }
            }
        }
    }
}

/** Salam berdasarkan waktu lokal */
private fun greeting(): String {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..10  -> "Selamat Pagi"
        in 11..14 -> "Selamat Siang"
        in 15..17 -> "Selamat Sore"
        else      -> "Selamat Malam"
    }
}