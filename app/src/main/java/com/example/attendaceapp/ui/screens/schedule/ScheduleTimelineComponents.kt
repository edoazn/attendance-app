package com.example.attendaceapp.ui.screens.schedule

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attendaceapp.ui.theme.BlackColor
import com.example.attendaceapp.ui.theme.Gray200
import com.example.attendaceapp.ui.theme.Gray400
import com.example.attendaceapp.ui.theme.PrimaryColor
import com.example.attendaceapp.ui.theme.WhiteColor
import java.util.Calendar

private val TimelineItemVerticalSpacing = 8.dp
private val TimelineBlue = PrimaryColor
private val TimelineGray = Gray400
private val TimelineUpcoming = PrimaryColor.copy(alpha = 0.45f)

private enum class TimelineStatus {
    PAST,
    ONGOING,
    UPCOMING,
}

@Composable
fun ScheduleTimelineEventItem(
    item: ScheduleItem,
    isFirst: Boolean,
    isLast: Boolean,
    onPengajuanClick: () -> Unit,
    onAbsensiClick: () -> Unit,
) {
    val timelineStatus = rememberTimelineStatus(item.startTime, item.endTime)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        ScheduleTimeColumn(
            startTime = item.startTime,
            endTime = item.endTime
        )

        Spacer(modifier = Modifier.width(4.dp))

        ScheduleTimelineAxis(
            isFirst = isFirst,
            isLast = isLast,
            status = timelineStatus
        )

        Spacer(modifier = Modifier.width(12.dp))

        ScheduleTimelineCard(
            item = item,
            onPengajuanClick = onPengajuanClick,
            onAbsensiClick = onAbsensiClick
        )
    }
}

@Composable
private fun ScheduleTimeColumn(startTime: String, endTime: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(50.dp)
    ) {
        Text(startTime, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(endTime, fontSize = 11.sp, color = Gray400)
    }
}

@Composable
private fun ScheduleTimelineAxis(
    isFirst: Boolean,
    isLast: Boolean,
    status: TimelineStatus,
) {
    val dotColor = when (status) {
        TimelineStatus.PAST -> TimelineGray
        TimelineStatus.ONGOING -> TimelineBlue
        TimelineStatus.UPCOMING -> TimelineUpcoming
    }
    val lineColor = TimelineBlue

    val pulseScale = if (status == TimelineStatus.ONGOING) {
        rememberInfiniteTransition(label = "timelinePulse").animateFloat(
            initialValue = 1f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 950),
                repeatMode = RepeatMode.Reverse
            ),
            label = "timelinePulseScale"
        ).value
    } else {
        1f
    }

    Box(
        modifier = Modifier
            .width(20.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        if (!isFirst) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxHeight(0.5f)
                    .width(2.dp)
                    .background(lineColor)
            )
        }

        if (!isLast) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxHeight(0.5f)
                    .width(2.dp)
                    .background(lineColor)
            )
        }

        if (status == TimelineStatus.ONGOING) {
            Box(
                modifier = Modifier
                    .size(20.dp * pulseScale)
                    .background(dotColor.copy(alpha = 0.22f), CircleShape)
            )
        }

        Box(
            modifier = Modifier
                .size(12.dp)
                .background(dotColor, CircleShape)
        )
    }
}

@Composable
private fun rememberTimelineStatus(startTime: String, endTime: String): TimelineStatus {
    val now = Calendar.getInstance()
    val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    return classifyTimelineStatus(startTime, endTime, currentMinutes)
}

private fun classifyTimelineStatus(
    startTime: String,
    endTime: String,
    currentMinutes: Int,
): TimelineStatus {
    val startMinutes = parseTimeToMinutes(startTime) ?: return TimelineStatus.UPCOMING
    val endMinutes = parseTimeToMinutes(endTime) ?: return TimelineStatus.UPCOMING

    return when {
        currentMinutes < startMinutes -> TimelineStatus.UPCOMING
        currentMinutes > endMinutes -> TimelineStatus.PAST
        else -> TimelineStatus.ONGOING
    }
}

private fun parseTimeToMinutes(time: String): Int? {
    val parts = time.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

@Composable
fun ScheduleTimelineCard(
    item: ScheduleItem,
    onPengajuanClick: () -> Unit,
    onAbsensiClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = TimelineItemVerticalSpacing, bottom = TimelineItemVerticalSpacing, end = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor)
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
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Hadir",
                        tint = Color(0xFF4CAF50)
                    )
                }
            }

            Text("${item.room} - ${item.mode}", color = Gray400)
            Text(item.code, fontSize = 12.sp, color = Gray400)
            Text(item.lecturer, fontSize = 12.sp, color = Gray400)
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPengajuanClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Gray200)
                ) {
                    Text("Pengajuan", color = BlackColor)
                }
                Button(
                    onClick = onAbsensiClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                ) {
                    Text("Absensi", color = WhiteColor)
                }
            }
        }
    }
}

