package com.example.attendaceapp.ui.screens.lecturer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.attendaceapp.R
import com.example.attendaceapp.data.model.AttendanceSession
import com.example.attendaceapp.data.model.User
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LecturerDashboardScreen(
    onNavigateToCreateStudent: () -> Unit,
    onNavigateToQRGenerator: () -> Unit,
    onNavigateToSessionDetail: (String) -> Unit,
    viewModel: LecturerViewModel = viewModel(),
    currentUser: StateFlow<User?>,
    onLogout: () -> Unit
) {
    val user by currentUser.collectAsState()
    // Menggunakan uiState dari ViewModel yang sudah ada
    val uiState by viewModel.uiState.collectAsState()

    // Mapping state dari uiState ke variabel lokal agar sesuai dengan kode UI
    val sessions = uiState.activeSessions
    val isLoading = uiState.isLoading
    val errorMessage = uiState.error

    // State untuk dialog pembuatan sesi
    var showCreateSessionDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.White // Background putih bersih
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Custom Header (Mirip HomePage)
                item {
                    Box(
                        modifier = Modifier
                            .height(220.dp) // Sedikit lebih tinggi untuk mengakomodasi menu
                            .fillMaxWidth()
                            .clip(
                                shape = RoundedCornerShape(
                                    bottomStart = 30.dp,
                                    bottomEnd = 30.dp
                                )
                            )
                            .background(color = colorResource(id = R.color.primary_color))
                            .border(
                                border = BorderStroke(
                                    2.dp,
                                    color = colorResource(id = R.color.primary_color)
                                ),
                                shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp),
                            )
                            .padding(top = 30.dp, start = 24.dp, end = 24.dp, bottom = 16.dp),
                    ) {
                        Column {
                            // Top Row: Greeting & Icons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(
                                        text = "Selamat Datang,",
                                        fontSize = 16.sp,
                                        color = colorResource(id = R.color.gray_400),
                                    )
                                    Text(
                                        text = user?.name ?: "Dosen",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // Logout Button (Custom Icon)
                                    IconButton(
                                        onClick = onLogout,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.2f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                            contentDescription = "Logout",
                                            tint = Color.White
                                        )
                                    }
                                    // Avatar
                                    Image(
                                        painter = painterResource(R.drawable.avatar),
                                        contentDescription = "User Avatar",
                                        modifier = Modifier
                                            .height(50.dp)
                                            .clip(RoundedCornerShape(25.dp))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Quick Actions Row inside Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                QuickActionItem(
                                    icon = Icons.Default.Person,
                                    label = "Tambah\nMahasiswa",
                                    onClick = onNavigateToCreateStudent
                                )
                                QuickActionItem(
                                    icon = Icons.Default.AddCircle,
                                    label = "Generate\nQR Code",
                                    onClick = onNavigateToQRGenerator
                                )
                                QuickActionItem(
                                    icon = Icons.Default.Add,
                                    label = "Buat\nSesi Baru",
                                    onClick = { showCreateSessionDialog = true }
                                )
                            }
                        }
                    }
                }

                // 2. Content Section
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sesi Aktif",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = colorResource(id = R.color.primary_color)
                            )
                        }
                    }
                }

                // Error Message
                if (errorMessage != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                // Empty State
                if (sessions.isEmpty() && !isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Belum ada sesi aktif", color = Color.Gray)
                            }
                        }
                    }
                }

                // Session List
                items(sessions) { session ->
                    SessionItem(
                        session = session,
                        onDelete = { viewModel.deleteSession(session.id) },
                        onClick = { onNavigateToSessionDetail(session.id) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp)) // Bottom padding
                }
            }
        }

        if (showCreateSessionDialog) {
            CreateSessionDialog(
                onDismiss = { showCreateSessionDialog = false },
                onCreate = { subject, description ->
                    viewModel.createAttendanceSession(subject, description)
                    showCreateSessionDialog = false
                }
            )
        }
    }
}

@Composable
fun QuickActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

@Composable
fun SessionItem(
    session: AttendanceSession,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorResource(id = R.color.primary_color).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = colorResource(id = R.color.primary_color)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.courseName, // Changed from subject to courseName
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = session.courseName, // Changed from description to courseName (or add description to model)
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                        .format(Date(session.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorResource(id = R.color.primary_color)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus Sesi",
                    tint = Color.Red.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun CreateSessionDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buat Sesi Baru") },
        text = {
            Column {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Mata Kuliah") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi (Opsional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subject.isNotBlank()) {
                        onCreate(subject, description)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.primary_color)
                )
            ) {
                Text("Buat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = Color.Gray)
            }
        },
        containerColor = Color.White
    )
}
