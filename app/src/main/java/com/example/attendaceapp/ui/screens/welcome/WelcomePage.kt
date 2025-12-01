package com.example.attendaceapp.ui.screens.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attendaceapp.R

@Preview
@Composable
fun WelcomePage(onLoginClick: () -> Unit = {}, onRegisterClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
    ) {
        // Background Image layer
        Image(
            painter = painterResource(id = R.drawable.bg_top),
            contentDescription = "image background",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(280.dp),
            contentScale = ContentScale.FillBounds
        )

        Image(
            painter = painterResource(id = R.drawable.bg_bottom),
            contentDescription = "image background",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size((200.dp)),
            contentScale = ContentScale.FillBounds
        )

        Image(
            painter = painterResource(id = R.drawable.bg_bottom_2),
            contentDescription = "image background",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size((160.dp)),
            contentScale = ContentScale.FillBounds
        )

        // Content layer
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Image Section
            Image(
                painter = painterResource(id = R.drawable.welcome_image),
                contentDescription = "Welcome Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 50.dp, bottom = 47.dp),
                contentScale = ContentScale.FillWidth
            )

            // Text Header Section
            Text(
                text = stringResource(R.string.welcome_message),
                fontSize = 35.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 48.sp,
                letterSpacing = 0.25.sp,
                textAlign = TextAlign.Center,
                color = colorResource(id = R.color.primary_color),
                modifier = Modifier.padding(bottom = 23.dp)
            )

            // Text Subhead Section
            Text(
                text = stringResource(R.string.welcome_subtitle),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 40.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons Section login and register
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            )
            {
                // Login Button
                Box(
                    modifier = Modifier
                        .height(60.dp)
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color = colorResource(R.color.primary_color))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            // Handle Login click
                            onLoginClick()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Login",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                // Register Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color.Blue.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            // Handle Register click
                            onRegisterClick()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Register",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xff0A0A0A)
                    )
                }
            }
        }
    }
}