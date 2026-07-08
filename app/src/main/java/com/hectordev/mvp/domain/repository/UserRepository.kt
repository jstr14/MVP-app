package com.hectordev.mvp.domain.repository

import com.hectordev.mvp.domain.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getCurrentUser(userId: String): User?
    suspend fun saveUser(user: User)
    suspend fun getUsersByIds(ids: List<String>): List<User>
    fun getAllUsers(): Flow<List<User>>
}