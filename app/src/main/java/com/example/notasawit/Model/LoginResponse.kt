package com.example.notasawit.Model

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val role: String?,
    val data: UserData?
)