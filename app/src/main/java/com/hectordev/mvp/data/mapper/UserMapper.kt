package com.hectordev.mvp.data.mapper

import com.hectordev.mvp.data.domain.UserDataModel
import com.hectordev.mvp.domain.User

fun UserDataModel.toDomain(): User {
    return User(
        id = id,
        name = name,
        email = email,
        photoUrl = photoUrl,
        totalPoints = totalPoints,
        mvpCount = mvpCount
    )
}

fun User.toDataModel(): UserDataModel {
    return UserDataModel(
        id = id,
        name = name,
        email = email,
        photoUrl = photoUrl,
        totalPoints = totalPoints,
        mvpCount = mvpCount
    )
}