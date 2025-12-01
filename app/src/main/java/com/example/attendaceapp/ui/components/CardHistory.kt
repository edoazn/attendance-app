package com.example.attendaceapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.attendaceapp.R
import com.example.attendaceapp.data.local.HistoryItem

@Composable
fun CardHistory(
    modifier: Modifier = Modifier,
    item: HistoryItem,
    onClick: () -> Unit = {},
    // TODO: TAMBAHKAN NAVIGATION KE DETAIL HISTORY SETIAP MATA KULIAH
) {
    Box(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(78.dp)
            .border(1.dp, Color.Gray, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .padding(16.dp)
        ) {
            // Image of MK
            Image(
                painter = painterResource(id = item.imageRes),
                contentDescription = "MK Image",
                modifier = modifier
                    .height(62.dp)
                    .padding(end = 12.dp),
                contentScale = ContentScale.Crop
            )
            // Column of Text
            Column {
                Text(
                    text = item.courseName,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.room
                )
            }
            Spacer(modifier = modifier.weight(1f))
            // row icon
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = "Arrow Right Icon",
                tint = Color.Gray,
                modifier = modifier
                    .padding(start = 8.dp)
            )
        }
    }
}