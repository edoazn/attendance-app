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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attendaceapp.R
import com.example.attendaceapp.ui.components.MyTextField

@Composable
fun LoginPage(
    onBackClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    // State variables for text fields
    var emailText by rememberSaveable {
        mutableStateOf("")
    }

    var passwordText by rememberSaveable { mutableStateOf("") }

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

        // login content section
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Text header
            Text(
                text = "Login here",
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                color = colorResource(id = R.color.primary_color),
                modifier = Modifier
                    .padding(top = 60.dp, bottom = 26.dp)
            )

            // Text subheader
            Text(
                text = "Welcome back you’ve\n" +
                        "been missed!",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
                modifier = Modifier
                    .padding(bottom = 40.dp)
            )
            // Text fields email
            MyTextField(
                value = emailText,
                onValueChange = { emailText = it },
                placeHolder = "Email",
                keyboardType = KeyboardType.Email,
                isPassword = false
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Text fields password
            MyTextField(
                value = passwordText,
                onValueChange = { passwordText = it },
                placeHolder = "Password",
                keyboardType = KeyboardType.Password,
                trailingIcon = null,
                isPassword = true
            )
            // Forgot password link
            Text(
                text = "Forgot Password?",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorResource(R.color.primary_color),
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(vertical = 24.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        // Handle Forgot Password click
                    }
            )

            // Login button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(10.dp),
                        clip = false
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .background(color = colorResource(R.color.primary_color))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        // Handle Login click
                        onLoginSuccess()
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
            // Sign up link
            Text(
                text = "Create a new account",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xff494949),
                modifier = Modifier
                    .padding(vertical = 24.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        onRegisterClick()
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