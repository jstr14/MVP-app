package com.hectordev.mvp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.hectordev.mvp.data.domain.UserDataModel
import com.hectordev.mvp.data.mapper.toDataModel
import com.hectordev.mvp.data.mapper.toDomain
import com.hectordev.mvp.domain.User
import com.hectordev.mvp.domain.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    private val usersCollection = firestore.collection("users")

    override suspend fun getCurrentUser(userId: String): User? {
        return try {
            val document = usersCollection.document(userId).get().await()
            // Convert firebase document to datamodel o object
            val dataModel = document.toObject(UserDataModel::class.java)
            // transform it to domain model before return it
            dataModel?.toDomain()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveUser(user: User) {
        try {
            // Transform model from domain to datamodel for Firebase
            val dataModel = user.toDataModel()
            usersCollection.document(dataModel.id).set(dataModel).await()
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
                doc.toObject(UserDataModel::class.java)?.toDomain()
            } ?: emptyList()

            trySend(users)
        }
        awaitClose { subscription.remove() }
    }
}