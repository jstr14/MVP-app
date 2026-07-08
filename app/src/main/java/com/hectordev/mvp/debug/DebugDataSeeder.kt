package com.hectordev.mvp.debug

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hectordev.mvp.data.domain.UserDataModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugDataSeeder @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) {
    private val eventsCollection = firestore.collection("events")
    private val usersCollection = firestore.collection("users")

    companion object {
        private const val DAY_MS = 86_400_000L
        private const val FIELD_DEBUG_SEED = "debugSeed"
        private val FAKE_USERS = listOf(
            UserDataModel(id = "debug_kate", name = "Kate Austen", email = "kate@debug.com",
                photoUrl = "https://i.pravatar.cc/150?u=kate@debug.com"),
            UserDataModel(id = "debug_sawyer", name = "Sawyer Ford", email = "sawyer@debug.com",
                photoUrl = "https://i.pravatar.cc/150?u=sawyer@debug.com"),
            UserDataModel(id = "debug_locke", name = "John Locke", email = "locke@debug.com",
                photoUrl = "https://i.pravatar.cc/150?u=locke@debug.com"),
            UserDataModel(id = "debug_hurley", name = "Hurley Reyes", email = "hurley@debug.com",
                photoUrl = "https://i.pravatar.cc/150?u=hurley@debug.com")
        )
        private val FAKE_USER_IDS = FAKE_USERS.map { it.id }
    }

    private val currentUserId get() = firebaseAuth.currentUser?.uid ?: ""

    fun seedPreTripEvent() {
        val uid = currentUserId
        createFakeUsers()
        val now = System.currentTimeMillis()
        val docRef = eventsCollection.document()
        docRef.set(buildEventData(
            id = docRef.id, title = "🧪 Summer Trip [DEBUG]", status = "PRE_TRIP",
            startDate = now + 7 * DAY_MS, endDate = now + 14 * DAY_MS,
            locationLabel = "Beach House, Ibiza", adminId = uid,
            participants = listOf(uid) + FAKE_USER_IDS, createdAt = now
        )).addOnFailureListener { Log.e("DebugSeeder", "seedPreTripEvent FAILED: ${it.message}") }
    }

    fun seedOnGoingEvent() {
        val uid = currentUserId
        createFakeUsers()
        val now = System.currentTimeMillis()
        val docRef = eventsCollection.document()
        docRef.set(buildEventData(
            id = docRef.id, title = "🧪 Road Trip South [DEBUG]", status = "ON_GOING",
            startDate = now - 2 * DAY_MS, endDate = now + 5 * DAY_MS,
            locationLabel = "Andalucía", adminId = uid,
            participants = listOf(uid) + FAKE_USER_IDS.take(2), createdAt = now - 2 * DAY_MS
        )).addOnFailureListener { Log.e("DebugSeeder", "seedOnGoingEvent FAILED: ${it.message}") }
    }

    fun seedPastEventWon() {
        val uid = currentUserId
        createFakeUsers()
        val now = System.currentTimeMillis()
        val docRef = eventsCollection.document()
        docRef.set(buildEventData(
            id = docRef.id, title = "🧪 New Year Trip [DEBUG]", status = "FINISHED",
            startDate = now - 30 * DAY_MS, endDate = now - 23 * DAY_MS,
            locationLabel = "Paris, France", adminId = uid,
            participants = listOf(uid) + FAKE_USER_IDS.take(3),
            mvpId = uid, createdAt = now - 30 * DAY_MS
        )).addOnFailureListener { Log.e("DebugSeeder", "seedPastEventWon FAILED: ${it.message}") }
    }

    fun seedPastEventLost() {
        val uid = currentUserId
        createFakeUsers()
        val now = System.currentTimeMillis()
        val docRef = eventsCollection.document()
        docRef.set(buildEventData(
            id = docRef.id, title = "🧪 Birthday Weekend [DEBUG]", status = "FINISHED",
            startDate = now - 60 * DAY_MS, endDate = now - 57 * DAY_MS,
            adminId = uid, participants = listOf(uid) + FAKE_USER_IDS.take(2),
            mvpId = FAKE_USER_IDS.first(), createdAt = now - 60 * DAY_MS
        )).addOnFailureListener { Log.e("DebugSeeder", "seedPastEventLost FAILED: ${it.message}") }
    }

    fun clearSeedData() {
        val uid = currentUserId
        eventsCollection
            .whereEqualTo(FIELD_DEBUG_SEED, true)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents
                    .filter { doc ->
                        @Suppress("UNCHECKED_CAST")
                        (doc.get("participants") as? List<String>)?.contains(uid) == true
                    }
                    .forEach { doc ->
                        doc.reference.delete()
                            .addOnFailureListener { Log.e("DebugSeeder", "delete ${doc.id} FAILED: ${it.message}") }
                    }
            }
            .addOnFailureListener { Log.e("DebugSeeder", "clearSeedData query FAILED: ${it.message}") }

        FAKE_USER_IDS.forEach { id -> usersCollection.document(id).delete() }
    }

    private fun createFakeUsers() {
        FAKE_USERS.forEach { user ->
            usersCollection.document(user.id).set(user)
                .addOnFailureListener { Log.w("DebugSeeder", "createFakeUser ${user.id} failed: ${it.message}") }
        }
    }

    private fun buildEventData(
        id: String, title: String, status: String,
        startDate: Long, endDate: Long, adminId: String,
        participants: List<String>, createdAt: Long,
        locationLabel: String? = null, mvpId: String? = null
    ): HashMap<String, Any?> = hashMapOf(
        "id" to id, "title" to title, "status" to status,
        "startDate" to startDate, "endDate" to endDate,
        "locationLabel" to locationLabel, "latitude" to null, "longitude" to null,
        "adminId" to adminId, "participants" to participants,
        "mvpId" to mvpId, "galaPhotoUrl" to null, "activeEmergencyId" to null,
        "createdAt" to createdAt, FIELD_DEBUG_SEED to true
    )
}