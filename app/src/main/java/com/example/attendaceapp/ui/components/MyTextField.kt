package com.example.attendaceapp.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults.outlinedTextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.attendaceapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeHolder: String,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val actualTrailingIcon = when {
        trailingIcon != null -> trailingIcon
        isPassword -> {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Lock else Icons.Outlined.Lock,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = colorResource(id = R.color.gray_400)
                    )
                }
            }
        }

        else -> null
    }

    // Implementation of custom text field goes here
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(10.dp),
        colors = outlinedTextFieldColors(
            focusedBorderColor = colorResource(id = R.color.primary_color),
            unfocusedBorderColor = colorResource(id = R.color.text_field_background),
            containerColor = colorResource(id = R.color.text_field_background)
        ),
        placeholder = { Text(text = placeHolder, color = colorResource(R.color.gray_400)) },
        trailingIcon = actualTrailingIcon,
        visualTransformation = when {
            isPassword && !passwordVisible -> PasswordVisualTransformation()
            else -> VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else keyboardType
        ),
        singleLine = true
    )
}

@Preview(showBackground = true)
@Composable
fun MyTextFieldPreview() {
    var text by rememberSaveable { mutableStateOf("") }
    MyTextField(
        value = text,
        onValueChange = { text = it },
        placeHolder = "Masukkan teks di sini",
    )
}