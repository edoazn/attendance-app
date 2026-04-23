package com.example.attendaceapp.data.model

import com.google.gson.annotations.SerializedName

data class UserResponse(
    @field:SerializedName("user")
    val user: User? = null,

    @field:SerializedName("token")
    val token: String? = null
)
