package com.hectordev.mvp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.hectordev.mvp.data.mapper.toDomain
import com.hectordev.mvp.data.mapper.toDto
import com.hectordev.mvp.data.model.EventDto
import com.hectordev.mvp.domain.Event
import com.hectordev.mvp.domain.EventStatus
import com.hectordev.mvp.domain.repository.EventsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class EventsRepositoryDefault @Inject constructor(
    private val firestore: FirebaseFirestore
) : EventsRepository {

    private val eventsCollection = firestore.collection("events")

    override suspend fun createEvent(event: Event): String {
        val docRef = eventsCollection.document()
        docRef.set(event.copy(id = docRef.id).toDto()).await()
        return docRef.id
    }

    override suspend fun getEvent(eventId: String): Event? {
        return try {
            eventsCollection.document(eventId).get().await()
                .toObject(EventDto::class.java)?.toDomain()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updateEvent(event: Event) {
        try {
            eventsCollection.document(event.id).set(event.toDto()).await()
        } catch (e: Exception) { }
    }

    override suspend fun updateStatus(eventId: String, status: EventStatus) {
        try {
            eventsCollection.document(eventId).update("status", status.name).await()
        } catch (e: Exception) { }
    }

    // Queries all events the user participates in and filters active ones in-memory
    // to avoid requiring a composite Firestore index.
    override fun observeActiveEvent(userId: String): Flow<Event?> = callbackFlow {
        val subscription = eventsCollection
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val active = snapshot?.documents
                    ?.mapNotNull { it.toObject(EventDto::class.java)?.toDomain() }
                    ?.firstOrNull { it.status == EventStatus.PRE_TRIP || it.status == EventStatus.ON_GOING }
                trySend(active)
            }
        awaitClose { subscription.remove() }
    }

    override fun observePastEvents(userId: String): Flow<List<Event>> = callbackFlow {
        val subscription = eventsCollection
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val past = snapshot?.documents
                    ?.mapNotNull { it.toObject(EventDto::class.java)?.toDomain() }
                    ?.filter { it.status == EventStatus.FINISHED }
                    ?: emptyList()
                trySend(past)
            }
        awaitClose { subscription.remove() }
    }
}