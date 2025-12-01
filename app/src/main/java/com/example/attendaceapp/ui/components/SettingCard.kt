package com.example.attendaceapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.attendaceapp.R

@Composable
fun SettingCard() {
    Column(
        modifier = Modifier
            .padding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(54.dp)
                .border(1.dp, Color.Gray, RoundedCornerShape(10.dp))
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // Icon Setting
                Image(
                    painter = painterResource(R.drawable.ic_help_24),
                    contentDescription = "Help Icon",
                )
                // Text Setting
                Text(
                    text = "Bantuan",
                    color = colorResource(R.color.gray),
                    modifier = Modifier
                        .padding(start = 14.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                // right arrow
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = "Arrow Right Icon",
                    tint = colorResource(R.color.gray)
                )
            }
        }
    }
}