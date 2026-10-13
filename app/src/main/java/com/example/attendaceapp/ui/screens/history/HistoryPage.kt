package com.example.attendaceapp.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.attendaceapp.R
import com.example.attendaceapp.data.local.HistoryItem
import com.example.attendaceapp.ui.components.CardHistory
import com.example.attendaceapp.ui.components.SearchBar


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryPage(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModel.factory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    // Muat riwayat dari Supabase sekali saat pertama kali
    LaunchedEffect(Unit) {
        viewModel.loadHistory()
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("Riwayat", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    titleContentColor = Color.Black,
                    containerColor = Color.White
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Search Bar
            SearchBar()
            when {
                // ── Loading ──────────────────────────────────────────
                uiState.isLoading -> {
                    Box(
                        modifier = modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Memuat riwayat...")
                        }
                    }
                }

                // ── Error ────────────────────────────────────────────
                uiState.error != null -> {
                    Box(
                        modifier = modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            Text(
                                text = uiState.error!!,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.loadHistory() },
                                colors = ButtonDefaults.buttonColors()
                            ) {
                                Text("Coba Lagi")
                            }
                        }
                    }
                }

                // ── Kosong (belum pernah absen) ──────────────────────
                uiState.isEmpty -> {
                    Box(
                        modifier = modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            Text(
                                text = "Belum ada riwayat kehadiran.",
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Riwayat absensi Anda akan muncul di sini.",
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // ── List riwayat dari Supabase ───────────────────────
                else -> {
                    LazyColumn(
                        modifier = modifier.fillMaxSize()
                    ) {
                        items(uiState.history.size) { index ->
                            val record = uiState.history[index]
                            CardHistory(
                                item = HistoryItem(
                                    courseName = record.schedule?.courseName ?: "—",
                                    room = record.schedule?.room ?: "—",
                                    imageRes = R.drawable.programming
                                ),
                                onClick = {
                                    // Handle card click
                                    print("Diklik riwayat ${record.schedule?.courseName}")
                                },
                                modifier = modifier
                            )
                        }
                    }
                }
            }
        }
    }
}
