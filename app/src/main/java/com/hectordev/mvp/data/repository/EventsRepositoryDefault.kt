package com.hectordev.mvp.data.repository

import android.net.Uri
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.hectordev.mvp.data.util.ImageCompressor
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.hectordev.mvp.data.mapper.toDomain
import com.hectordev.mvp.data.mapper.toDto
import com.hectordev.mvp.data.model.EventDto
import com.hectordev.mvp.data.model.PredictionDto
import com.hectordev.mvp.data.model.TimelineNoteDto
import com.hectordev.mvp.domain.Event
import com.hectordev.mvp.domain.EventStatus
import com.hectordev.mvp.domain.Prediction
import com.hectordev.mvp.domain.TimelineNote
import com.hectordev.mvp.domain.repository.EventsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class EventsRepositoryDefault @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val imageCompressor: ImageCompressor
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
        eventsCollection.document(event.id).set(event.toDto()).await()
    }

    override suspend fun deleteEvent(eventId: String) {
        eventsCollection.document(eventId).delete().await()
    }

    override suspend fun inviteParticipant(eventId: String, userId: String) {
        eventsCollection.document(eventId)
            .update("pendingParticipants", FieldValue.arrayUnion(userId))
            .await()
    }

    override suspend fun removeParticipant(eventId: String, userId: String) {
        eventsCollection.document(eventId)
            .update("participants", FieldValue.arrayRemove(userId))
            .await()
    }

    override suspend fun cancelInvite(eventId: String, userId: String) {
        eventsCollection.document(eventId)
            .update("pendingParticipants", FieldValue.arrayRemove(userId))
            .await()
    }

    override suspend fun invitePendingEmail(eventId: String, email: String) {
        eventsCollection.document(eventId)
            .update("pendingEmails", FieldValue.arrayUnion(email))
            .await()
    }

    override suspend fun cancelEmailInvite(eventId: String, email: String) {
        eventsCollection.document(eventId)
            .update("pendingEmails", FieldValue.arrayRemove(email))
            .await()
    }

    override suspend fun bindPendingEmailInvite(email: String, userId: String) {
        val events = eventsCollection
            .whereArrayContains("pendingEmails", email)
            .get()
            .await()
        for (doc in events.documents) {
            doc.reference.update(
                "pendingEmails", FieldValue.arrayRemove(email),
                "pendingParticipants", FieldValue.arrayUnion(userId)
            ).await()
        }
    }

    override suspend fun submitPrediction(eventId: String, userId: String, prediction: Prediction) {
        eventsCollection.document(eventId)
            .collection("predictions")
            .document(userId)
            .set(prediction.toDto())
            .await()
    }

    override suspend fun getPrediction(eventId: String, userId: String): Prediction? {
        return eventsCollection.document(eventId)
            .collection("predictions")
            .document(userId)
            .get()
            .await()
            .toObject(PredictionDto::class.java)
            ?.toDomain()
    }

    override suspend fun openPredictions(eventId: String) {
        eventsCollection.document(eventId)
            .update(
                "status", EventStatus.PREDICTION.name,
                "pendingParticipants", emptyList<String>(),
                "pendingEmails", emptyList<String>()
            ).await()
    }

    override suspend fun acceptInvitation(eventId: String, userId: String) {
        eventsCollection.document(eventId)
            .update(
                "pendingParticipants", FieldValue.arrayRemove(userId),
                "participants", FieldValue.arrayUnion(userId)
            ).await()
    }

    override suspend fun declineInvitation(eventId: String, userId: String) {
        eventsCollection.document(eventId)
            .update("pendingParticipants", FieldValue.arrayRemove(userId))
            .await()
    }

    override suspend fun updateStatus(eventId: String, status: EventStatus) {
        eventsCollection.document(eventId).update("status", status.name).await()
    }

    override fun observeEvent(eventId: String): Flow<Event?> = callbackFlow {
        val subscription = eventsCollection.document(eventId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObject(EventDto::class.java)?.toDomain())
            }
        awaitClose { subscription.remove() }
    }

    override fun observeParticipantEvents(userId: String): Flow<List<Event>> = callbackFlow {
        if (userId.isEmpty()) { trySend(emptyList()); awaitClose { }; return@callbackFlow }
        val subscription = eventsCollection
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val events = snapshot?.documents
                    ?.mapNotNull { it.toObject(EventDto::class.java)?.toDomain() }
                    ?: emptyList()
                trySend(events)
            }
        awaitClose { subscription.remove() }
    }

    override fun observeTimeline(eventId: String): Flow<List<TimelineNote>> = callbackFlow {
        val subscription = eventsCollection.document(eventId)
            .collection("points_log")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val notes = snapshot?.documents
                    ?.mapNotNull { it.toObject(TimelineNoteDto::class.java)?.toDomain() }
                    ?: emptyList()
                trySend(notes)
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun postNote(eventId: String, note: TimelineNote): String {
        val docRef = eventsCollection.document(eventId).collection("points_log").document()
        docRef.set(note.copy(id = docRef.id).toDto()).await()
        return docRef.id
    }

    override suspend fun deleteNote(eventId: String, noteId: String) {
        eventsCollection.document(eventId).collection("points_log").document(noteId).delete().await()
    }

    override suspend fun uploadNotePhoto(eventId: String, imageUri: Uri): String {
        val ref = storage.reference.child("events/$eventId/notes/${System.currentTimeMillis()}.jpg")
        ref.putBytes(imageCompressor.compress(imageUri)).await()
        return ref.downloadUrl.await().toString()
    }

    override fun observePendingInvitations(userId: String): Flow<List<Event>> = callbackFlow {
        if (userId.isEmpty()) { trySend(emptyList()); awaitClose { }; return@callbackFlow }
        val subscription = eventsCollection
            .whereArrayContains("pendingParticipants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val pending = snapshot?.documents
                    ?.mapNotNull { it.toObject(EventDto::class.java)?.toDomain() }
                    ?.filter { it.status != EventStatus.FINISHED }
                    ?: emptyList()
                trySend(pending)
            }
        awaitClose { subscription.remove() }
    }

}