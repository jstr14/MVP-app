package com.hectordev.mvp.data.mapper

import com.hectordev.mvp.data.model.UserDto
import com.hectordev.mvp.domain.User

fun UserDto.toDomain(): User {
    return User(
        id = id,
        name = name,
        email = email,
        photoUrl = photoUrl,
        totalPoints = totalPoints,
        mvpCount = mvpCount
    )
}

fun User.toDto(): UserDto {
    return UserDto(
        id = id,
        name = name,
        email = email,
        photoUrl = photoUrl,
        totalPoints = totalPoints,
        mvpCount = mvpCount
    )
}