package com.example.attendaceapp.ui.screens.lecturer

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.attendaceapp.R
import com.example.attendaceapp.data.model.AttendanceSession
import com.example.attendaceapp.data.model.User
import com.example.attendaceapp.ui.components.MyTextField
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRGeneratorScreen(
    currentUser: User,
    viewModel: LecturerViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var courseIdText by rememberSaveable { mutableStateOf("") }
    var courseNameText by rememberSaveable { mutableStateOf("") }
    var durationText by rememberSaveable { mutableStateOf("60") }
    var lateThresholdText by rememberSaveable { mutableStateOf("15") }

    var generatedSession by remember { mutableStateOf<AttendanceSession?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Generate QR Code",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (generatedSession == null) {
                    // Form to create session
                    SessionFormContent(
                        courseIdText = courseIdText,
                        onCourseIdChange = { courseIdText = it },
                        courseNameText = courseNameText,
                        onCourseNameChange = { courseNameText = it },
                        durationText = durationText,
                        onDurationChange = { durationText = it },
                        lateThresholdText = lateThresholdText,
                        onLateThresholdChange = { lateThresholdText = it },
                        isLoading = uiState.isLoading,
                        error = uiState.error,
                        onGenerateClick = {
                            viewModel.createAttendanceSession(
                                courseId = courseIdText,
                                courseName = courseNameText,
                                lecturerId = currentUser.id,
                                lecturerName = currentUser.name,
                                durationInMinutes = durationText.toIntOrNull() ?: 60,
                                lateThreshold = lateThresholdText.toIntOrNull() ?: 15
                            )
                        }
                    )
                } else {
                    // Show generated QR Code
                    QRCodeDisplayContent(
                        session = generatedSession!!,
                        qrBitmap = qrBitmap,
                        onGenerateNewClick = {
                            generatedSession = null
                            qrBitmap = null
                            courseIdText = ""
                            courseNameText = ""
                            durationText = "60"
                            lateThresholdText = "15"
                            viewModel.clearMessages()
                        }
                    )
                }
            }

            // Handle success - generate QR and show it
            LaunchedEffect(uiState.successMessage) {
                if (uiState.successMessage?.contains("berhasil dibuat") == true) {
                    val sessionId = uiState.activeSessions.lastOrNull()?.id
                    if (sessionId != null) {
                        val session = uiState.activeSessions.last()
                        generatedSession = session
                        qrBitmap = generateQRCode(session.qrCode)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionFormContent(
    courseIdText: String,
    onCourseIdChange: (String) -> Unit,
    courseNameText: String,
    onCourseNameChange: (String) -> Unit,
    durationText: String,
    onDurationChange: (String) -> Unit,
    lateThresholdText: String,
    onLateThresholdChange: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    onGenerateClick: () -> Unit
) {
    Text(
        text = "📲",
        fontSize = 80.sp,
        modifier = Modifier.padding(vertical = 24.dp)
    )

    Text(
        text = "Buat Sesi Presensi",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    Text(
        text = "Isi data sesi untuk generate QR Code",
        fontSize = 14.sp,
        color = Color.Gray,
        modifier = Modifier.padding(bottom = 32.dp)
    )

    // Course ID
    MyTextField(
        value = courseIdText,
        onValueChange = onCourseIdChange,
        placeHolder = "Kode Mata Kuliah (e.g. CS101)",
        keyboardType = KeyboardType.Text,
        isPassword = false
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Course Name
    MyTextField(
        value = courseNameText,
        onValueChange = onCourseNameChange,
        placeHolder = "Nama Mata Kuliah",
        keyboardType = KeyboardType.Text,
        isPassword = false
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Duration
    MyTextField(
        value = durationText,
        onValueChange = onDurationChange,
        placeHolder = "Durasi (menit)",
        keyboardType = KeyboardType.Number,
        isPassword = false
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Late Threshold
    MyTextField(
        value = lateThresholdText,
        onValueChange = onLateThresholdChange,
        placeHolder = "Batas Terlambat (menit)",
        keyboardType = KeyboardType.Number,
        isPassword = false
    )

    // Error Message
    if (error != null) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            fontSize = 14.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Generate Button
    Button(
        onClick = onGenerateClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp),
        enabled = !isLoading &&
                courseIdText.isNotBlank() &&
                courseNameText.isNotBlank() &&
                durationText.isNotBlank(),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(id = R.color.primary_color)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White
            )
        } else {
            Text(
                text = "Generate QR Code",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Info Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "ℹ️ Informasi:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "• QR Code akan aktif sesuai durasi yang ditentukan\n" +
                        "• Mahasiswa yang scan setelah batas terlambat akan tercatat terlambat\n" +
                        "• QR Code bersifat unik untuk setiap sesi",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun QRCodeDisplayContent(
    session: AttendanceSession,
    qrBitmap: Bitmap?,
    onGenerateNewClick: () -> Unit
) {
    Text(
        text = "✅",
        fontSize = 80.sp,
        modifier = Modifier.padding(vertical = 24.dp)
    )

    Text(
        text = "QR Code Berhasil Dibuat!",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 32.dp)
    )

    // Session Info Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            InfoRow(label = "Mata Kuliah", value = session.courseName)
            InfoRow(label = "Kode", value = session.courseId)
            InfoRow(label = "Dosen", value = session.lecturerName)
            InfoRow(
                label = "Berlaku Hingga",
                value = formatTimestamp(session.expiresAt)
            )
            InfoRow(
                label = "Batas Terlambat",
                value = "${session.lateThreshold} menit"
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // QR Code Image
    if (qrBitmap != null) {
        Card(
            modifier = Modifier.size(300.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Generate New Button
    Button(
        onClick = onGenerateNewClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(id = R.color.primary_color)
        )
    ) {
        Text(
            text = "Buat Sesi Baru",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Warning Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "⚠️ Perhatian:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFFD32F2F)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Simpan atau screenshot QR Code ini. Mahasiswa harus scan QR Code untuk melakukan presensi.",
                fontSize = 12.sp,
                color = Color(0xFFD32F2F)
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun generateQRCode(text: String, size: Int = 512): Bitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }

    return bitmap
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
    return sdf.format(Date(timestamp))
}
