package com.hectordev.mvp.data.repository

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.hectordev.mvp.data.domain.UserDataModel
import com.hectordev.mvp.data.mapper.toDataModel
import com.hectordev.mvp.data.mapper.toDomain
import com.hectordev.mvp.domain.User
import com.hectordev.mvp.domain.repository.UserRepository
import android.util.Log
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
            // Convert firebase document to datamodel o object
            val dataModel = document.toObject(UserDataModel::class.java)
            // transform it to domain model before return it
            dataModel?.toDomain()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun saveUser(user: User) {
        val dataModel = user.toDataModel()
        usersCollection.document(dataModel.id).set(dataModel)
            .addOnFailureListener { Log.e("UserRepo", "saveUser failed: ${it.message}") }
    }

    override suspend fun getUsersByIds(ids: List<String>): List<User> {
        if (ids.isEmpty()) return emptyList()
        return try {
            usersCollection
                .whereIn(FieldPath.documentId(), ids)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(UserDataModel::class.java)?.toDomain() }
        } catch (e: Exception) {
            emptyList()
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