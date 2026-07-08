package com.hectordev.mvp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.hectordev.mvp.data.model.UserDto
import com.hectordev.mvp.data.mapper.toDto
import com.hectordev.mvp.data.mapper.toDomain
import com.hectordev.mvp.domain.User
import com.hectordev.mvp.domain.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryDefault @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    private val usersCollection = firestore.collection("users")

    override suspend fun getCurrentUser(userId: String): User? {
        return try {
            val document = usersCollection.document(userId).get().await()
            document.toObject(UserDto::class.java)?.toDomain()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveUser(user: User) {
        try {
            val dto = user.toDto()
            // merge = true preserves existing stats fields on returning users
            usersCollection.document(dto.id).set(dto, SetOptions.merge()).await()
        } catch (e: Exception) {
        }
    }

    override fun getAllUsers(): Flow<List<User>> = callbackFlow {
        val subscription = usersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val users = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(UserDto::class.java)?.toDomain()
            } ?: emptyList()

            trySend(users)
        }
        awaitClose { subscription.remove() }
    }
}