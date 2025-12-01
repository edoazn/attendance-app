package com.example.attendaceapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attendaceapp.R

@Composable
fun AnnouncementCard(
    modifier: Modifier = Modifier,
    image: Int = R.drawable.bg_top,
    title: String = "Pengumuman Ujian Akhir Semester",
    date: String = "25 Desember 2023",
    content: String = "Diberitahukan kepada seluruh mahasiswa bahwa Ujian Akhir Semester akan dilaksanakan pada tanggal 1-10 Januari 2024. Harap mempersiapkan diri dengan baik dan membawa perlengkapan yang diperlukan.",
) {
    Box(
        modifier = modifier
            .size(width = 173.dp, height = 220.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(color = colorResource(id = R.color.white))
            .border(
                border = BorderStroke(1.dp, color = colorResource(id = R.color.gray_200)),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Image
            Image(
                painter = painterResource(id = image),
                contentDescription = "Announcement Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .padding(bottom = 8.dp),
            )
            // Title
            Text(
                modifier = Modifier.padding(bottom = 4.dp),
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.black),
                maxLines = 2,
            )
            // Date
            Text(
                text = date,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = colorResource(id = R.color.gray_400),
            )
        }
    }
}