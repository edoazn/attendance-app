package com.example.attendaceapp.ui.screens.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulePage(
    scheduleList: List<ScheduleItem>,
    onPengajuanClick: () -> Unit,
    onAbsensiClick: () -> Unit
) {
    val contentHorizontalPadding = 20.dp
    val colors = MaterialTheme.colorScheme

    var selectedDay by remember { mutableIntStateOf(0) }
    var selectedItem by remember { mutableStateOf<ScheduleItem?>(null) }
    var showPengajuanModal by remember { mutableStateOf(false) }
    var showAbsensiModal by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Jadwal", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.onBackground,
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(colors.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = contentHorizontalPadding)
            ) {
                HorizontalDaySelector(
                    selectedIndex = selectedDay,
                    onDaySelected = { selectedDay = it }
                )
                HorizontalDivider(color = colors.outlineVariant, thickness = 1.dp)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    itemsIndexed(scheduleList) { index, item ->
                        ScheduleTimelineEventItem(
                            item = item,
                            isFirst = index == 0,
                            isLast = index == scheduleList.lastIndex,
                            onPengajuanClick = {
                                selectedItem = item
                                showPengajuanModal = true
                            },
                            onAbsensiClick = {
                                selectedItem = item
                                showAbsensiModal = true
                            }
                        )
                    }
                }
            }

            if (showPengajuanModal && selectedItem != null) {
                PengajuanModal(
                    item = selectedItem!!,
                    onDismiss = { showPengajuanModal = false },
                    onSubmit = {
                        showPengajuanModal = false
                        onPengajuanClick()
                    }
                )
            }

            if (showAbsensiModal && selectedItem != null) {
                AbsensiModal(
                    item = selectedItem!!,
                    onDismiss = { showAbsensiModal = false },
                    onConfirm = {
                        showAbsensiModal = false
                        onAbsensiClick()
                    }
                )
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
    val indication = LocalIndication.current
    val colors = MaterialTheme.colorScheme

    LazyRow(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        itemsIndexed(days) { index, day ->
            val isSelected = index == selectedIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = indication,
                    ) { onDaySelected(index) }
                    .background(
                        if (isSelected) colors.primary.copy(alpha = 0.12f) else Color.Transparent
                    )
            ) {
                Text(
                    text = day,
                    color = if (isSelected) colors.primary else colors.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = (index + 2).toString(),
                    color = if (isSelected) colors.primary else colors.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
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
    SchedulePage(scheduleList = sampleSchedule, onPengajuanClick = {}, onAbsensiClick = {})
}

// Modal Pengajuan dan Absensi
@Composable
fun PengajuanModal(
    item: ScheduleItem,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var alasan by remember { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.scrim.copy(alpha = 0.5f))
            .modalDismissClickable(onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .modalContentClickBlocker()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Pengajuan Izin", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(item.subject, color = colors.onSurfaceVariant, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = alasan,
                    onValueChange = { alasan = it },
                    label = { Text("Alasan ketidakhadiran") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Lampiran (opsional)")
                    }

                    Button(
                        onClick = { onSubmit(alasan) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = alasan.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary
                        )
                    ) {
                        Text("Kirim")
                    }
                }
            }
        }
    }
}

@Composable
fun AbsensiModal(
    item: ScheduleItem,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.scrim.copy(alpha = 0.5f))
            .modalDismissClickable(onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .modalContentClickBlocker()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Konfirmasi Absensi", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(item.subject, fontWeight = FontWeight.SemiBold)
                Text("${item.startTime} - ${item.endTime}", color = colors.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary
                        )
                    ) {
                        Text("Lanjut")
                    }
                }
            }
        }
    }
}

@Composable
private fun Modifier.modalDismissClickable(onDismiss: () -> Unit): Modifier {
    val indication = LocalIndication.current
    return clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = indication,
        onClick = onDismiss,
    )
}

@Composable
private fun Modifier.modalContentClickBlocker(): Modifier {
    val indication = LocalIndication.current
    return clickable(
        enabled = false,
        interactionSource = remember { MutableInteractionSource() },
        indication = indication,
        onClick = {},
    )
}
