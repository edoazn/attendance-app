package com.example.attendaceapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attendaceapp.R
import com.example.attendaceapp.data.remote.response.ScheduleDto

/**
 * Kartu jadwal ringkas yang ditampilkan di HomePage.
 *
 * Bisa dipakai dengan data real [ScheduleDto] maupun nilai default (preview/fallback).
 */
@Composable
fun ScheduleCard(
    modifier: Modifier = Modifier,
    schedule: ScheduleDto? = null,
    // Fallback values untuk preview / keadaan darurat
    matkul: String = schedule?.courseName ?: "Mata Kuliah",
    time: String = if (schedule != null)
        "${schedule.startTimeShort()} - ${schedule.endTimeShort()}"
    else "—",
    room: String = schedule?.room ?: "—",
    lecturer: String = schedule?.lecturerName ?: "",
    onScheduleClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(color = colorResource(id = R.color.white))
            .border(
                border = BorderStroke(1.dp, color = colorResource(id = R.color.gray_200)),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Ikon matkul
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color = colorResource(id = R.color.primary_color).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Matkul Icon",
                    tint = colorResource(id = R.color.primary_color),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Info matkul
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = matkul,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.black),
                    maxLines = 1,
                )
                Text(
                    text = time,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = colorResource(id = R.color.gray_400),
                )
                if (lecturer.isNotBlank()) {
                    Text(
                        text = lecturer,
                        fontSize = 11.sp,
                        color = colorResource(id = R.color.gray_400),
                        maxLines = 1,
                    )
                }
            }

            // Ruangan
            Text(
                text = room,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorResource(id = R.color.primary_color),
            )
        }
    }
}