package com.example.attendaceapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attendaceapp.R
import com.example.attendaceapp.data.local.AttendanceHistory

@Composable
fun HistoryAttendanceCard(
    modifier: Modifier = Modifier,
    attendace: AttendanceHistory
) {
    val statusColor = when (attendace.status.lowercase()) {
        "hadir" -> colorResource(R.color.teal_700)
        "izin" -> Color(0xFFFFA500)
        "sakit" -> Color(0xFF2196F3)
        else -> Color(0xFFFF6B6B)
    }

    Card(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header dengan mata kuliah dan status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = attendace.courseName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Jenis Kelas: ${attendace.classType}",
                        fontSize = 12.sp,
                        color = colorResource(R.color.gray),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Status badge
                Box(
                    modifier = Modifier
                        .background(
                            colorResource(R.color.teal_700).copy(0.15f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Hadir",
                            tint = colorResource(R.color.teal_700),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = attendace.status,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorResource(R.color.teal_700)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.LightGray.copy(alpha = 0.3f))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Info dengan icon
            InfoRow(
                icon = Icons.Default.DateRange,
                text = attendace.timeRange
            )

            Spacer(modifier = Modifier.height(8.dp))

            InfoRow(
                icon = Icons.Default.LocationOn,
                text = attendace.room
            )

            Spacer(modifier = Modifier.height(8.dp))

            InfoRow(
                icon = Icons.Default.Person,
                text = attendace.lecturer
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Jam masuk dan keluar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TimeInfo(
                    painter = painterResource(R.drawable.ic_login_24),
                    label = "Masuk",
                    time = attendace.checkInTime,
                    color = colorResource(R.color.teal_700)
                )

                TimeInfo(
                    painter = painterResource(R.drawable.ic_logout_24),
                    label = "Keluar",
                    time = attendace.checkOutTime,
                    color = Color(0xFFFF6B6B)
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colorResource(R.color.gray),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color.Black.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun TimeInfo(
    painter: Painter,
    label: String,
    time: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            colorFilter = ColorFilter.tint(color),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = label,
                fontSize = 10.sp,
                color = color.copy(alpha = 0.7f)
            )
            Text(
                text = time,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}
