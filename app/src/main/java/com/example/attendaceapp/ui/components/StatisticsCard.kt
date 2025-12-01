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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attendaceapp.R

@Composable
fun StatisticsCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                clip = false
            )
            .clip(RoundedCornerShape(20.dp))
            .background(color = colorResource(id = R.color.white))
            .border(
                border = BorderStroke(1.dp, color = colorResource(id = R.color.gray_200)),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatisticItem(
                label = "Hadir",
                percentage = 85,
                color = colorResource(id = R.color.primary_color),
            )
            StatisticItem(
                label = "Sakit",
                percentage = 5,
                color = colorResource(id = R.color.primary_color),
            )
            StatisticItem(
                label = "Izin",
                percentage = 15,
                color = colorResource(id = R.color.primary_color),
            )
        }
    }
}

@Composable
fun StatisticItem(
    label: String,
    percentage: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            CircularProgressIndicator(
                progress = { percentage / 100f },
                modifier = Modifier.size(60.dp),
                color = color,
                strokeWidth = 6.dp,
                trackColor = colorResource(id = R.color.gray_200),
            )
            Text(
                text = "$percentage%",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.black),
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(id = R.color.black),
        )
    }
}