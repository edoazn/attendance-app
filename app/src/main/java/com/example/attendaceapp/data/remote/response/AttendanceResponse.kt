package com.example.attendaceapp.data.remote.response

import com.google.gson.annotations.SerializedName

data class AttendanceResponse(

	@field:SerializedName("distance")
	val distance: Any? = null,

	@field:SerializedName("message")
	val message: String? = null,

	@field:SerializedName("status")
	val status: String? = null
)
