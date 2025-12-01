package com.example.attendaceapp.ui.screens.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.attendaceapp.R
import com.example.attendaceapp.data.local.HistoryItem
import com.example.attendaceapp.ui.components.CardHistory
import com.example.attendaceapp.ui.components.SearchBar


@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HistoryPage(modifier: Modifier = Modifier) {

    val historyList = remember {
        listOf(
            HistoryItem("Pemrograman Mobile", "D303", R.drawable.programming),
            HistoryItem("Kalkulus", "D201", R.drawable.kalkulus),
            HistoryItem("IT Interphenuership", "D102", R.drawable.interprener),
            HistoryItem("Pancasila", "D205", R.drawable.pancasila),
//            HistoryItem("Jaringan Komputer", "D304", R.drawable.programming),
        )
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
            // Card List of History
            LazyColumn(
                modifier = modifier.fillMaxSize()
            ) {



                items(historyList.size) { index ->
                    CardHistory(
                        item = historyList[index],
                        onClick = {
                            // Handle card click
                            print("Clicked on ${historyList[index].courseName}")
                        },
                        modifier = modifier
                    )
                }
            }
        }
    }
}