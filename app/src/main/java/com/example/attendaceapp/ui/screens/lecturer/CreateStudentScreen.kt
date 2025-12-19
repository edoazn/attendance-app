package com.example.attendaceapp.ui.screens.lecturer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.attendaceapp.R
import com.example.attendaceapp.ui.components.MyTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStudentScreen(
    viewModel: LecturerViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var nimText by rememberSaveable { mutableStateOf("") }
    var nameText by rememberSaveable { mutableStateOf("") }
    var emailText by rememberSaveable { mutableStateOf("") }
    var departmentText by rememberSaveable { mutableStateOf("") }
    var passwordText by rememberSaveable { mutableStateOf("") }

    // Handle navigation on success
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage?.contains("berhasil dibuat") == true) {
            // Clear fields
            nimText = ""
            nameText = ""
            emailText = ""
            departmentText = ""
            passwordText = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tambah Mahasiswa",
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
                // Header Icon
                Text(
                    text = "👨‍🎓",
                    fontSize = 80.sp,
                    modifier = Modifier.padding(vertical = 24.dp)
                )

                // Description
                Text(
                    text = "Buat Akun Mahasiswa Baru",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Masukkan data mahasiswa untuk membuat akun",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // NIM Field
                MyTextField(
                    value = nimText,
                    onValueChange = { nimText = it },
                    placeHolder = "NIM",
                    keyboardType = KeyboardType.Number,
                    isPassword = false
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Name Field
                MyTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    placeHolder = "Nama Lengkap",
                    keyboardType = KeyboardType.Text,
                    isPassword = false
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Department Field
                MyTextField(
                    value = departmentText,
                    onValueChange = { departmentText = it },
                    placeHolder = "Jurusan",
                    keyboardType = KeyboardType.Text,
                    isPassword = false
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                MyTextField(
                    value = passwordText,
                    onValueChange = { passwordText = it },
                    placeHolder = "Password",
                    keyboardType = KeyboardType.Password,
                    isPassword = true
                )

                // Error Message
                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Success Message
                if (uiState.successMessage != null) {
                    Text(
                        text = uiState.successMessage!!,
                        color = Color(0xFF4CAF50),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Create Button
                Button(
                    onClick = {
                        viewModel.createStudent(
                            nim = nimText,
                            name = nameText,
                            email = emailText,
                            department = departmentText,
                            password = passwordText
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    enabled = !uiState.isLoading &&
                            nimText.isNotBlank() &&
                            nameText.isNotBlank() &&
                            departmentText.isNotBlank() &&
                            passwordText.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.primary_color)
                    )
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "Buat Akun",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info Text
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
                            text = "• NIM harus unik dan berupa angka\n" +
                                    "• Password minimal 6 karakter\n" +
                                    "• Mahasiswa dapat login dengan NIM dan password yang dibuat",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
