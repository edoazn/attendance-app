package com.example.attendaceapp.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attendaceapp.R
import com.example.attendaceapp.ui.components.MyTextField

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegisterPage(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onLoginClick: () -> Unit = {}
) {
    // State variables for text fields
    var emailText by rememberSaveable { mutableStateOf("") }
    var passwordText by rememberSaveable { mutableStateOf("") }
    var confirmPasswordText by rememberSaveable { mutableStateOf("") }

    // Background Images
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
    ) {
        Image(
            painter = painterResource(R.drawable.bg_top),
            contentDescription = "Background Image",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(280.dp),
            contentScale = ContentScale.FillBounds
        )
        Image(
            painter = painterResource(R.drawable.bg_bottom),
            contentDescription = "Background Image",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(200.dp),
            contentScale = ContentScale.FillBounds
        )
        Image(
            painter = painterResource(R.drawable.bg_bottom_2),
            contentDescription = "Background Image",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(160.dp),
            contentScale = ContentScale.FillBounds
        )

        // Content goes here
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // Text header
            Text(
                text = stringResource(R.string.register_text_header),
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                color = colorResource(id = R.color.primary_color),
                modifier = Modifier
                    .padding(top = 60.dp, bottom = 6.dp)
            )

            // Text subheader
            Text(
                text = stringResource(R.string.register_text_subheader),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
                modifier = Modifier
                    .padding(bottom = 40.dp)
            )

            // Register form will be here
            // Email TextField
            MyTextField(
                value = emailText,
                onValueChange = { emailText = it },
                keyboardType = KeyboardType.Email,
                placeHolder = "Email Address",
            )
            Spacer(modifier = modifier.height(16.dp))
            // Password TextField
            MyTextField(
                value = passwordText,
                onValueChange = { passwordText = it },
                placeHolder = "Password",
                keyboardType = KeyboardType.Password,
                isPassword = true
            )
            Spacer(modifier = modifier.height(16.dp))
            // Confirm Password TextField
            MyTextField(
                value = confirmPasswordText,
                onValueChange = { confirmPasswordText = it },
                keyboardType = KeyboardType.Password,
                placeHolder = "Confirm Password",
                isPassword = true
            )
            Spacer(modifier = modifier.height(24.dp))
            // Register Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(
                        elevation = 8.dp, RoundedCornerShape(10.dp),
                        clip = false
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .background(color = colorResource(R.color.primary_color))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        // Handle register click
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.register_button_text),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            // Login link
            Text(
                text = "Already have an account",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xff494949),
                modifier = Modifier
                    .padding(vertical = 30.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        onLoginClick()
                    }
            )
            Text(
                text = "OR",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorResource(R.color.primary_color),
                modifier = Modifier
                    .padding(bottom = 20.dp)
            )
            // Social login options
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        // Handle Google Login click
                    }
                    .background(color = Color(0xffF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_google),
                    contentDescription = "Google Login",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}