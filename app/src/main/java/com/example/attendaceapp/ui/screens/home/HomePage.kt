package com.example.attendaceapp.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attendaceapp.R
import com.example.attendaceapp.ui.components.AnnouncementCard
import com.example.attendaceapp.ui.components.ScheduleCard
import com.example.attendaceapp.ui.components.StatisticsCard
import com.example.attendaceapp.ui.navigation.BottomNavItem
import com.example.attendaceapp.ui.navigation.BottomNavigationBar

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomePage(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            // Vector background
            Box(
                modifier = modifier
                    .height(190.dp)
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
                    // top padding 40 dp
                    .padding(top = 30.dp, start = 24.dp, end = 24.dp, bottom = 16.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                // User info and leading icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // User info
                    Column {
                        // welcome text
                        Text(
                            modifier = Modifier.padding(bottom = 4.dp),
                            text = "Selamat Pagi, User",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(id = R.color.white),
                        )
                        // NIM
                        Text(
                            text = "211420108",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = colorResource(id = R.color.gray_400),
                        )
                    }
                    // Leading icon
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Notification Icon
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(color = colorResource(id = R.color.white))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "User Icon",
                                tint = colorResource(id = R.color.primary_color),
                            )
                        }
                        // User Avatar
                        Image(
                            painter = painterResource(R.drawable.avatar),
                            contentDescription = "User Avatar",
                            modifier = Modifier
                                .height(50.dp)
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
                    .offset(
                        y = (-50).dp
                    )
            )
        }
        item {
            // Text Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Jadwal Saya",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.black),
                )
                Text(
                    text = "See all",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.primary_color),
                )
            }
        }
        // Card Jadwal
        items(4) {
            ScheduleCard(modifier = Modifier.padding(vertical = 6.dp))
        }

        item {
            // Text Pengumuman
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 16.dp),
                text = "Pengumuman Terbaru",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.black),
            )
        }
        item {
            // Card Pengumuman
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