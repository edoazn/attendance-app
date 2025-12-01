package com.example.attendaceapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attendaceapp.R

@Composable
fun ScheduleCard(
    modifier: Modifier = Modifier,
    image: Painter = painterResource(R.drawable.ic_google),
    matkul: String = "Matematika Diskrit",
    time: String = "Senin, 10.00 - 12.00",
    room: String = "U512"
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
            // Matkul Icon
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(color = colorResource(id = R.color.primary_color).copy(alpha = 0.1f))
                    .padding(12.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_google),
                    contentDescription = "Matkul Icon",
                    modifier = Modifier,
                )
            }
            // Left Side
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = "Matematika Diskrit",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.black),
                )
                Text(
                    text = "Senin, 10.00 - 12.00",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = colorResource(id = R.color.gray_400),
                )
            }
            // Right Side
            // Class Room
            Text(
                text = "U512",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorResource(id = R.color.gray_400),
            )
        }
    }
}