package com.example.attendaceapp.data.remote.response

import com.google.gson.annotations.SerializedName

data class LoginResponse(

	@field:SerializedName("data")
	val data: LoginData? = null,

	@field:SerializedName("success")
	val success: Boolean? = null,

	@field:SerializedName("message")
	val message: String? = null
)

data class UserDto(

	@field:SerializedName("role")
	val role: String? = null,

	@field:SerializedName("updated_at")
	val updatedAt: String? = null,

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("created_at")
	val createdAt: String? = null,

	@field:SerializedName("id")
	val id: Int? = null,

	@field:SerializedName("identity_number")
	val identityNumber: String? = null,

	@field:SerializedName("email")
	val email: String? = null
)

data class LoginData(

	@field:SerializedName("user")
	val user: UserDto? = null,

	@field:SerializedName("token")
	val token: String? = null
)
