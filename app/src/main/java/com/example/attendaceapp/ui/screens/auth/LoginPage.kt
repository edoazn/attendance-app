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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.attendaceapp.R
import com.example.attendaceapp.data.model.UserRole
import com.example.attendaceapp.ui.components.MyTextField
import com.example.attendaceapp.ui.state.AuthState
import kotlinx.coroutines.launch

@Composable
fun LoginPage(
    viewModel: AuthViewModel = viewModel(),
    onNavigateToLecturerDashboard: () -> Unit,
    onNavigateToStudentDashboard: () -> Unit,
) {
    val authState by viewModel.authState.collectAsState()

    var nimText by rememberSaveable { mutableStateOf("") }
    var passwordText by rememberSaveable { mutableStateOf("") }

    // Handle navigation based on auth state
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            val user = (authState as AuthState.Success).user
            when (user.role) {
                UserRole.LECTURER -> onNavigateToLecturerDashboard()
                UserRole.STUDENT -> onNavigateToStudentDashboard()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
    ) {
        // Background images
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
                text = "Welcome back you’ve\nbeen missed!",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
                modifier = Modifier
                    .padding(bottom = 40.dp)
            )
            // NIM field
            MyTextField(
                value = nimText,
                onValueChange = { nimText = it },
                placeHolder = "NIM",
                keyboardType = KeyboardType.Number,
                isPassword = false
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Password field
            MyTextField(
                value = passwordText,
                onValueChange = { passwordText = it },
                placeHolder = "Password",
                keyboardType = KeyboardType.Password,
                trailingIcon = null,
                isPassword = true
            )

            // Error message
            if (authState is AuthState.Error) {
                Text(
                    text = (authState as AuthState.Error).error,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                    .background(
                        color = if (authState is AuthState.Loading)
                            colorResource(R.color.primary_color).copy(alpha = 0.6f)
                        else
                            colorResource(R.color.primary_color)
                    )
                    .clickable(
                        enabled = authState !is AuthState.Loading,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        if (nimText.isNotBlank() && passwordText.isNotBlank()) {
                            viewModel.login(nimText, passwordText)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                    )
                } else {
                    Text(
                        text = "Login",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }

            //Info Text
            Text(
                text = "Hubungi dosen untuk membuat akun",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xff494949),
                modifier = Modifier
                    .padding(vertical = 24.dp)
            )
        }
    }

    // Reset auth state when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetAuthState()
        }
    }
}