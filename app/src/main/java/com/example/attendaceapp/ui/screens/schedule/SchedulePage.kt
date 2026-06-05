package com.example.attendaceapp.ui.screens.schedule

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.attendaceapp.R
import com.example.attendaceapp.data.remote.response.ScheduleDto
import com.example.attendaceapp.ui.theme.PrimaryColor

// ─── Hari (urutan Senin-Jumat) ───────────────────────────────────────────────

private val DAYS = listOf(
    "Sen" to "Monday",
    "Sel" to "Tuesday",
    "Rab" to "Wednesday",
    "Kam" to "Thursday",
    "Jum" to "Friday"
)

/** Index hari saat ini (0=Senin … 4=Jumat). Kembalikan 0 jika weekend. */
private fun todayDayIndex(): Int {
    val cal = java.util.Calendar.getInstance()
    return when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
        java.util.Calendar.MONDAY    -> 0
        java.util.Calendar.TUESDAY   -> 1
        java.util.Calendar.WEDNESDAY -> 2
        java.util.Calendar.THURSDAY  -> 3
        java.util.Calendar.FRIDAY    -> 4
        else -> 0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulePage(
    onNavigateToAttendance: () -> Unit = {},
    viewModel: ScheduleViewModel = viewModel(
        factory = ScheduleViewModel.factory(LocalContext.current)
    ),
    // ── Backward-compat params (tidak lagi dipakai, keep agar navigation lama compile) ──
    @Suppress("UNUSED_PARAMETER") scheduleList: List<ScheduleItem> = emptyList(),
    @Suppress("UNUSED_PARAMETER") onPengajuanClick: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onAbsensiClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = MaterialTheme.colorScheme

    var selectedDay by remember { mutableIntStateOf(todayDayIndex()) }
    var selectedDto by remember { mutableStateOf<ScheduleDto?>(null) }
    var showPengajuanModal by remember { mutableStateOf(false) }
    var showAbsensiModal by remember { mutableStateOf(false) }

    // Muat semua jadwal sekali saat pertama kali
    LaunchedEffect(Unit) {
        viewModel.loadAllSchedules()
    }

    val schedulesForDay = uiState.schedulesForDay(DAYS[selectedDay].second)

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Jadwal", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.onBackground,
                ),
                windowInsets = WindowInsets(0, 0, 0, 0),
                actions = {
                    IconButton(onClick = { viewModel.loadAllSchedules() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh jadwal")
                    }
                }
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
                    .padding(horizontal = 20.dp)
            ) {
                // Day selector
                HorizontalDaySelector(
                    selectedIndex = selectedDay,
                    onDaySelected = { selectedDay = it }
                )
                HorizontalDivider(color = colors.outlineVariant, thickness = 1.dp)

                when {
                    // ── Loading ──────────────────────────────────────────
                    uiState.isLoadingAll -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryColor)
                        }
                    }

                    // ── Error ────────────────────────────────────────────
                    uiState.error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.no_internet),
                                    contentDescription = null,
                                    modifier = Modifier.size(160.dp)
                                )
                                Text(
                                    text = uiState.error!!,
                                    color = colors.error,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = { viewModel.loadAllSchedules() },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                                ) {
                                    Text("Coba Lagi")
                                }
                            }
                        }
                    }

                    // ── Kosong untuk hari ini ─────────────────────────────
                    schedulesForDay.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("🗓️", fontSize = 48.sp)
                                Text(
                                    "Tidak ada jadwal ${DAYS[selectedDay].first}",
                                    fontWeight = FontWeight.Medium,
                                    color = colors.onSurface
                                )
                                Text(
                                    "Hari yang tenang! 😊",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    // ── List jadwal ───────────────────────────────────────
                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(schedulesForDay) { index, dto ->
                                ScheduleTimelineEventItem(
                                    dto = dto,
                                    isFirst = index == 0,
                                    isLast = index == schedulesForDay.lastIndex,
                                    onPengajuanClick = {
                                        selectedDto = dto
                                        showPengajuanModal = true
                                    },
                                    onAbsensiClick = {
                                        selectedDto = dto
                                        showAbsensiModal = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ── Modals ────────────────────────────────────────────────────────
            if (showPengajuanModal && selectedDto != null) {
                PengajuanDtoModal(
                    dto = selectedDto!!,
                    onDismiss = { showPengajuanModal = false },
                    onSubmit = { showPengajuanModal = false }
                )
            }

            if (showAbsensiModal && selectedDto != null) {
                AbsensiDtoModal(
                    dto = selectedDto!!,
                    onDismiss = { showAbsensiModal = false },
                    onConfirm = {
                        showAbsensiModal = false
                        onNavigateToAttendance()
                    }
                )
            }
        }
    }
}

// ─── Day Selector ─────────────────────────────────────────────────────────────

@Composable
fun HorizontalDaySelector(selectedIndex: Int, onDaySelected: (Int) -> Unit) {
    val indication = LocalIndication.current
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        DAYS.forEachIndexed { index, (label, _) ->
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
                    text = label,
                    color = if (isSelected) colors.primary else colors.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                if (index == todayDayIndex()) {
                    // Titik penanda hari ini
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(
                                color = if (isSelected) colors.primary else colors.primary.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

// ─── Modal Pengajuan (DTO) ────────────────────────────────────────────────────

@Composable
fun PengajuanDtoModal(
    dto: ScheduleDto,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var alasan by remember { mutableStateOf("") }

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
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Email, contentDescription = null,
                                tint = PrimaryColor, modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Pengajuan Izin/Sakit", fontWeight = FontWeight.Bold,
                            fontSize = 18.sp, color = Color.White
                        )
                        Text(
                            dto.courseName ?: "—",
                            fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = alasan, onValueChange = { alasan = it },
                        placeholder = { Text("Deskripsi pengajuan") },
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        shape = RoundedCornerShape(12.dp), maxLines = 4
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { onSubmit(alasan) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = alasan.isNotBlank(),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                    ) { Text("Ajukan") }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// ─── Modal Absensi (DTO) ──────────────────────────────────────────────────────

@Composable
fun AbsensiDtoModal(
    dto: ScheduleDto,
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
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📋", fontSize = 28.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Konfirmasi Absensi", fontWeight = FontWeight.Bold,
                            fontSize = 18.sp, color = Color.White
                        )
                        Text(
                            dto.courseName ?: "—",
                            fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            "${dto.startTimeShort()} - ${dto.endTimeShort()} · ${dto.room ?: ""}",
                            fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (dto.hasQr == true || dto.hasActiveCode == true)
                            "Pilih metode absensi di halaman berikutnya"
                        else
                            "⚠️ Belum ada QR/Kode yang aktif untuk jadwal ini.\nHubungi dosen untuk mengaktifkan.",
                        color = colors.onSurfaceVariant,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryColor)
                        ) { Text("Batal") }

                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f).height(48.dp),
                            enabled = dto.hasQr == true || dto.hasActiveCode == true,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                        ) { Text("Lanjut Absensi") }
                    }
                }
            }
        }
    }
}

// ─── Modifier helpers ─────────────────────────────────────────────────────────

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

// ─── Backward-compat legacy types ─────────────────────────────────────────────

/**
 * [ScheduleItem] dipertahankan agar kode lama (DummyData, preview) tidak compile error.
 * Untuk screen nyata gunakan [ScheduleDto].
 */
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

// Backward-compat overload — diarahkan ke versi baru
@Composable
fun PengajuanModal(item: ScheduleItem, onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    PengajuanDtoModal(
        dto = ScheduleDto(courseName = item.subject, room = item.room),
        onDismiss = onDismiss,
        onSubmit = onSubmit
    )
}

@Composable
fun AbsensiModal(item: ScheduleItem, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AbsensiDtoModal(
        dto = ScheduleDto(courseName = item.subject, room = item.room),
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}
