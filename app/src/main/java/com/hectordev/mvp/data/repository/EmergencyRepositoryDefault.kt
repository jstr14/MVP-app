package com.hectordev.mvp.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.hectordev.mvp.data.model.EmergencyRequestDto
import com.hectordev.mvp.domain.EmergencyRequest
import com.hectordev.mvp.domain.EmergencyStatus
import com.hectordev.mvp.domain.repository.EmergencyRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class EmergencyRepositoryDefault @Inject constructor(
    private val firestore: FirebaseFirestore
) : EmergencyRepository {

    private fun emergencyRef(eventId: String) =
        firestore.collection("events").document(eventId).collection("emergency_requests")

    private fun eventRef(eventId: String) =
        firestore.collection("events").document(eventId)

    override suspend fun triggerEmergency(eventId: String, request: EmergencyRequest): String {
        val docRef = emergencyRef(eventId).document()
        val dto = EmergencyRequestDto(
            id = docRef.id,
            triggeredById = request.triggeredById,
            targetUserId = request.targetUserId,
            votesAccept = request.votesAccept,
            votesDecline = emptyList(),
            status = EmergencyStatus.PENDING.name,
            expiresAt = request.expiresAt,
            timestamp = request.timestamp
        )
        firestore.runBatch { batch ->
            batch.set(docRef, dto)
            batch.update(eventRef(eventId), "activeEmergencyId", docRef.id)
        }.await()
        return docRef.id
    }

    override fun observeEmergencyRequest(eventId: String, requestId: String): Flow<EmergencyRequest?> =
        callbackFlow {
            val subscription = emergencyRef(eventId).document(requestId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) { close(error); return@addSnapshotListener }
                    val dto = snapshot?.toObject(EmergencyRequestDto::class.java)
                    trySend(dto?.toDomain())
                }
            awaitClose { subscription.remove() }
        }

    override suspend fun castVote(eventId: String, requestId: String, userId: String, accept: Boolean) {
        val field = if (accept) "votesAccept" else "votesDecline"
        emergencyRef(eventId).document(requestId)
            .update(field, FieldValue.arrayUnion(userId))
            .await()
    }

    override suspend fun resolveTimeout(eventId: String, requestId: String, triggeredById: String) {
        firestore.runBatch { batch ->
            batch.update(
                emergencyRef(eventId).document(requestId),
                "status", EmergencyStatus.TIMED_OUT.name
            )
            batch.update(
                eventRef(eventId),
                "activeEmergencyId", null,
                "usedEmergencyClause", FieldValue.arrayUnion(triggeredById)
            )
        }.await()
    }

    private fun EmergencyRequestDto.toDomain() = EmergencyRequest(
        id = id,
        triggeredById = triggeredById,
        targetUserId = targetUserId,
        votesAccept = votesAccept,
        votesDecline = votesDecline,
        status = runCatching { EmergencyStatus.valueOf(status) }.getOrDefault(EmergencyStatus.PENDING),
        expiresAt = expiresAt,
        timestamp = timestamp
    )
}