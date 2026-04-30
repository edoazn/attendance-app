package com.example.attendaceapp.ui.screens.schedule

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attendaceapp.ui.theme.PrimaryColor

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

// Gunakan component DaySelector untuk memilih hari
@Composable
fun HorizontalDaySelector(selectedIndex: Int, onDaySelected: (Int) -> Unit) {
    val days = listOf("Sen", "Sel", "Rab", "Kam", "Jum")
    val indication = LocalIndication.current
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        days.forEachIndexed { index, day ->
            val isSelected = index == selectedIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = indication,
                    ) { onDaySelected(index) }
                    .background(
                        if (isSelected) colors.primary.copy(alpha = 0.12f) else colors.background,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = day,
                    color = if (isSelected) colors.primary else colors.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = (index + 2).toString(),
                    color = if (isSelected) colors.primary else colors.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp
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
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .modalContentClickBlocker()
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryColor)
                        .padding(top = 24.dp, bottom = 56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pengajuan Izin/Sakit",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .offset(y = (-36).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = PrimaryColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Masukkan bukti izin dan deskripsi pengajuan",
                        color = colors.onSurfaceVariant,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = alasan,
                        onValueChange = { alasan = it },
                        placeholder = { Text("Deskripsi pengajuan") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PrimaryColor
                        )
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cari File")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { onSubmit(alasan) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = alasan.isNotBlank(),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryColor
                        )
                    ) {
                        Text("Ajukan")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
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
    var otpCode by remember { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.scrim.copy(alpha = 0.5f))
            .modalDismissClickable(onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .modalContentClickBlocker()
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryColor)
                        .padding(top = 24.dp, bottom = 56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Konfirmasi Absensi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .offset(y = (-36).dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = PrimaryColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Masukkan kode OTP untuk konfirmasi absensi",
                        color = colors.onSurfaceVariant,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OtpInput(
                        code = otpCode,
                        onCodeChange = { otpCode = it },
                        length = 6
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PrimaryColor
                            )
                        ) {
                            Text("Batal")
                        }
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            enabled = otpCode.isNotBlank(),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryColor
                            )
                        ) {
                            Text("Konfirmasi")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun OtpInput(
    code: String,
    onCodeChange: (String) -> Unit,
    length: Int
) {
    val normalizedCode = code.take(length)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(length) { index ->
            val char = normalizedCode.getOrNull(index)?.toString() ?: ""
            OutlinedTextField(
                value = char,
                onValueChange = { value ->
                    val digits = value.filter { it.isDigit() }
                    val updated = normalizedCode.padEnd(length, ' ').toCharArray()
                    if (digits.length > 1) {
                        var targetIndex = index
                        for (digit in digits) {
                            if (targetIndex >= length) break
                            updated[targetIndex] = digit
                            targetIndex++
                        }
                    } else {
                        updated[index] = digits.firstOrNull() ?: ' '
                    }
                    onCodeChange(updated.concatToString().trimEnd())
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                textStyle = TextStyle(textAlign = TextAlign.Center)
            )
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

// Modal pengajuan preview
@Preview
@Composable
fun PengajuanModalPreview() {
    val sampleItem = ScheduleItem(
        startTime = "08:00",
        endTime = "09:30",
        subject = "Matematika Diskrit",
        room = "Ruang 101",
        mode = "Offline",
        code = "MD101",
        lecturer = "Dr. Andi",
        isPresent = true
    )
    PengajuanModal(item = sampleItem, onDismiss = {}, onSubmit = {})
}

// Modal absensi preview
@Preview
@Composable
fun AbsensiModalPreview() {
    val sampleItem = ScheduleItem(
        startTime = "08:00",
        endTime = "09:30",
        subject = "Matematika Diskrit",
        room = "Ruang 101",
        mode = "Offline",
        code = "MD101",
        lecturer = "Dr. Andi",
        isPresent = true
    )
    AbsensiModal(item = sampleItem, onDismiss = {}, onConfirm = {})
}
