package com.hectordev.mvp.data.repository

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.hectordev.mvp.data.domain.UserDataModel
import com.hectordev.mvp.data.mapper.toDataModel
import com.hectordev.mvp.data.mapper.toDomain
import com.hectordev.mvp.domain.User
import com.hectordev.mvp.domain.UserStats
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
        usersCollection.document(dataModel.id).set(dataModel).await()
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

    override suspend fun getUserByEmail(email: String): User? {
        return usersCollection
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.toObject(UserDataModel::class.java)
            ?.toDomain()
    }

    override fun observeUserStats(userId: String): Flow<UserStats> = callbackFlow {
        val subscription = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val stats = snapshot?.get("stats") as? Map<*, *>
                trySend(UserStats(
                    lifetimeMvps = (stats?.get("lifetimeMvps") as? Long)?.toInt() ?: 0,
                    oracleWins = (stats?.get("oraclePredictionsCorrect") as? Long)?.toInt() ?: 0,
                    tripleWins = (stats?.get("tripleBetsCorrect") as? Long)?.toInt() ?: 0,
                    reactorWins = (stats?.get("reactorWins") as? Long)?.toInt() ?: 0,
                    totalPlayerWins = (stats?.get("totalPlayerWins") as? Long)?.toInt() ?: 0
                ))
            }
        awaitClose { subscription.remove() }
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