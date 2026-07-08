package com.hectordev.mvp.data.model

data class UserDto(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val totalPoints: Float = 0f,
    val mvpCount: Int = 0
)