@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.example.attendaceapp.ui.screens.attendace

import android.Manifest
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.attendaceapp.data.model.User
import com.example.attendaceapp.data.remote.response.ScheduleDto
import com.example.attendaceapp.ui.components.QRScannerOverlay
import com.example.attendaceapp.ui.screens.student.StudentViewModel
import com.example.attendaceapp.ui.state.AttendanceTab
import com.example.attendaceapp.utils.QRCodeAnalyzer
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.Executors

// ─── Palet warna lokal ────────────────────────────────────────────────────────
private val DarkBg = Color(0xFF0D0D0D)
private val CardBg = Color(0xFF1A1A2E)
private val AccentBlue = Color(0xFF4FC3F7)
private val AccentPurple = Color(0xFFCE93D8)
private val TabActive = Color(0xFF4FC3F7)
private val TabInactive = Color(0xFF424242)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AttendancePage(
    currentUser: User,
    viewModel: StudentViewModel = viewModel(
        factory = StudentViewModel.factory(LocalContext.current)
    ),
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val uiState by viewModel.uiState.collectAsState()

    var scannedCode by remember { mutableStateOf<String?>(null) }
    var showResultDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Muat jadwal saat pertama kali composable muncul
    LaunchedEffect(Unit) {
        viewModel.loadTodaySchedules()
    }

    // Handle QR scan result → submit ke API
    LaunchedEffect(scannedCode) {
        if (scannedCode != null && !isProcessing) {
            isProcessing = true
            viewModel.submitQrAttendance(scannedCode!!)
        }
    }

    // Tampilkan dialog hasil absensi
    LaunchedEffect(uiState.successMessage, uiState.error) {
        if (uiState.successMessage != null || uiState.error != null) {
            showResultDialog = true
            isProcessing = false
        }
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Absensi", fontWeight = FontWeight.Bold, color = Color.White)
                        uiState.selectedSchedule?.let { s ->
                            Text(
                                text = s.courseName ?: "—",
                                fontSize = 12.sp,
                                color = AccentBlue.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardBg,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // ── Loading jadwal ──────────────────────────────────────────
                uiState.isLoading && uiState.todaySchedules.isEmpty() -> {
                    CircularProgressIndicator(
                        color = AccentBlue,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // ── Tidak ada jadwal hari ini ───────────────────────────────
                uiState.todaySchedules.isEmpty() -> {
                    EmptyScheduleState(modifier = Modifier.align(Alignment.Center))
                }

                // ── Main content ────────────────────────────────────────────
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {

                        // Pilihan jadwal (jika lebih dari satu)
                        if (uiState.todaySchedules.size > 1) {
                            ScheduleSelector(
                                schedules = uiState.todaySchedules,
                                selected = uiState.selectedSchedule,
                                onSelect = { viewModel.selectSchedule(it) }
                            )
                        }

                        // Tab row
                        AttendanceTabRow(
                            activeTab = uiState.activeTab,
                            schedule = uiState.selectedSchedule,
                            onTabChange = { viewModel.switchTab(it) }
                        )

                        // Tab content
                        AnimatedContent(
                            targetState = uiState.activeTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "tab_content"
                        ) { tab ->
                            when (tab) {
                                AttendanceTab.QR_SCAN -> QrScanTab(
                                    cameraPermissionState = cameraPermissionState,
                                    isProcessing = isProcessing,
                                    cameraExecutor = cameraExecutor,
                                    lifecycleOwner = lifecycleOwner,
                                    onQrScanned = { scannedCode = it }
                                )

                                AttendanceTab.MANUAL_CODE -> ManualCodeTab(
                                    isLoading = uiState.isLoading,
                                    onSubmit = { code -> viewModel.submitCodeAttendance(code) }
                                )
                            }
                        }
                    }
                }
            }

            // ── Result dialog ───────────────────────────────────────────────
            if (showResultDialog) {
                AttendanceResultDialog(
                    success = uiState.successMessage != null,
                    message = uiState.successMessage ?: uiState.error ?: "Terjadi kesalahan",
                    onDismiss = {
                        showResultDialog = false
                        scannedCode = null
                        viewModel.clearMessages()
                    },
                    onConfirm = {
                        showResultDialog = false
                        scannedCode = null
                        viewModel.clearMessages()
                        if (uiState.successMessage != null) onNavigateBack()
                    },
                    onRetry = {
                        showResultDialog = false
                        scannedCode = null
                        viewModel.clearMessages()
                    }
                )
            }
        }
    }

    // Cleanup executor saat composable di-dispose
    DisposableEffect(lifecycleOwner) {
        onDispose { cameraExecutor.shutdown() }
    }
}

// ─── Tab Row ─────────────────────────────────────────────────────────────────

@Composable
private fun AttendanceTabRow(
    activeTab: AttendanceTab,
    schedule: ScheduleDto?,
    onTabChange: (AttendanceTab) -> Unit
) {
    val qrEnabled = schedule?.hasQr == true
    val codeEnabled = schedule?.hasActiveCode == true

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TabButton(
            label = "📷  Scan QR",
            isActive = activeTab == AttendanceTab.QR_SCAN,
            enabled = qrEnabled,
            modifier = Modifier.weight(1f)
        ) { if (qrEnabled) onTabChange(AttendanceTab.QR_SCAN) }

        TabButton(
            label = "🔑  Kode Manual",
            isActive = activeTab == AttendanceTab.MANUAL_CODE,
            enabled = codeEnabled,
            modifier = Modifier.weight(1f)
        ) { if (codeEnabled) onTabChange(AttendanceTab.MANUAL_CODE) }
    }

    // Info jika tidak ada metode yang tersedia
    if (!qrEnabled && !codeEnabled) {
        Text(
            text = "⚠️ Belum ada QR Code atau Kode Absensi yang aktif untuk jadwal ini.",
            color = Color(0xFFFFB74D),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun TabButton(
    label: String,
    isActive: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = when {
        isActive -> TabActive
        !enabled -> TabInactive.copy(alpha = 0.3f)
        else -> TabInactive
    }
    val textColor = if (isActive) Color.Black else Color.White.copy(alpha = if (enabled) 0.8f else 0.3f)

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = bg,
            contentColor = textColor,
            disabledContainerColor = bg,
            disabledContentColor = textColor
        )
    ) {
        Text(label, fontSize = 13.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
    }
}

// ─── QR Scan Tab ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun QrScanTab(
    cameraPermissionState: com.google.accompanist.permissions.PermissionState,
    isProcessing: Boolean,
    cameraExecutor: java.util.concurrent.ExecutorService,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onQrScanned: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            !cameraPermissionState.status.isGranted -> {
                CameraPermissionRequest(onRequest = { cameraPermissionState.launchPermissionRequest() })
            }

            isProcessing -> {
                // Freeze preview saat sedang proses
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = AccentBlue)
                    Spacer(Modifier.height(16.dp))
                    Text("Memproses absensi...", color = Color.White)
                }
            }

            else -> {
                CameraPreview(
                    cameraExecutor = cameraExecutor,
                    lifecycleOwner = lifecycleOwner,
                    onQrScanned = onQrScanned
                )
                QRScannerOverlay()
                // Instruksi bawah layar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Arahkan kamera ke QR Code",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "QR akan otomatis terdeteksi",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    cameraExecutor: java.util.concurrent.ExecutorService,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onQrScanned: (String) -> Unit
) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, QRCodeAnalyzer { qrValue ->
                            onQrScanned(qrValue)
                        })
                    }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Toast.makeText(ctx, "Kamera gagal dimulai: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun CameraPermissionRequest(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Close,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = AccentBlue
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Izin Kamera Diperlukan",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Izinkan akses kamera untuk scan QR Code absensi",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
        ) {
            Text("Izinkan Kamera", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Manual Code Tab ──────────────────────────────────────────────────────────

@Composable
private fun ManualCodeTab(
    isLoading: Boolean,
    onSubmit: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Ilustrasi
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CardBg)
                .border(2.dp, AccentPurple.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("🔑", fontSize = 48.sp)
        }

        Spacer(Modifier.height(28.dp))
        Text(
            text = "Masukkan Kode Absensi",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Kode 6 karakter dari dosen / proyektor",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.55f)
        )

        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 6) code = it.uppercase() },
            label = { Text("Kode Absensi", color = AccentPurple.copy(alpha = 0.8f)) },
            placeholder = { Text("Contoh: AB12CD", color = Color.Gray) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentPurple,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = AccentPurple
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    if (code.length == 6) onSubmit(code)
                }
            ),
            trailingIcon = {
                if (code.isNotEmpty()) {
                    IconButton(onClick = { code = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            }
        )

        Spacer(Modifier.height(8.dp))

        // Counter karakter
        Text(
            text = "${code.length}/6",
            color = if (code.length == 6) AccentPurple else Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                keyboardController?.hide()
                onSubmit(code)
            },
            enabled = code.length == 6 && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentPurple,
                contentColor = Color.White,
                disabledContainerColor = AccentPurple.copy(alpha = 0.35f),
                disabledContentColor = Color.White.copy(alpha = 0.4f)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(10.dp))
                Text("Memproses...")
            } else {
                Text("Submit Absensi", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// ─── Empty State ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyScheduleState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("📅", fontSize = 64.sp)
        Text(
            "Tidak Ada Jadwal Hari Ini",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            "Jadwal absensi akan muncul di sini\nsaat kelas dijadwalkan hari ini.",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.55f),
            textAlign = TextAlign.Center
        )
    }
}

// ─── Schedule Selector ────────────────────────────────────────────────────────

@Composable
private fun ScheduleSelector(
    schedules: List<ScheduleDto>,
    selected: ScheduleDto?,
    onSelect: (ScheduleDto) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Pilih Jadwal:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            schedules.forEach { schedule ->
                val isSelected = selected?.id == schedule.id
                Button(
                    onClick = { onSelect(schedule) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) AccentBlue else CardBg,
                        contentColor = if (isSelected) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = schedule.courseName?.take(12) ?: "—",
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ─── Result Dialog ────────────────────────────────────────────────────────────

@Composable
private fun AttendanceResultDialog(
    success: Boolean,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onRetry: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (success) "✅ Berhasil!" else "❌ Gagal",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(text = message, fontSize = 14.sp)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(if (success) "OK" else "Tutup")
            }
        },
        dismissButton = if (!success) {
            {
                TextButton(onClick = onRetry) { Text("Coba Lagi") }
            }
        } else null
    )
}