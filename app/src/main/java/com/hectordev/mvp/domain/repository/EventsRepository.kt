package com.hectordev.mvp.domain.repository

import android.net.Uri
import com.hectordev.mvp.domain.Event
import com.hectordev.mvp.domain.EventStatus
import com.hectordev.mvp.domain.Prediction
import com.hectordev.mvp.domain.TimelineNote
import kotlinx.coroutines.flow.Flow

interface EventsRepository {
    suspend fun createEvent(event: Event): String
    suspend fun getEvent(eventId: String): Event?
    suspend fun updateEvent(event: Event)
    suspend fun updateStatus(eventId: String, status: EventStatus)
    suspend fun deleteEvent(eventId: String)
    suspend fun inviteParticipant(eventId: String, userId: String)
    suspend fun removeParticipant(eventId: String, userId: String)
    suspend fun cancelInvite(eventId: String, userId: String)
    suspend fun invitePendingEmail(eventId: String, email: String)
    suspend fun cancelEmailInvite(eventId: String, email: String)
    suspend fun bindPendingEmailInvite(email: String, userId: String)
    suspend fun openPredictions(eventId: String)
    suspend fun submitPrediction(eventId: String, userId: String, prediction: Prediction)
    suspend fun getPrediction(eventId: String, userId: String): Prediction?
    suspend fun acceptInvitation(eventId: String, userId: String)
    suspend fun declineInvitation(eventId: String, userId: String)
    fun observeEvent(eventId: String): Flow<Event?>
    fun observeParticipantEvents(userId: String): Flow<List<Event>>
    fun observePendingInvitations(userId: String): Flow<List<Event>>
    fun observeTimeline(eventId: String): Flow<List<TimelineNote>>
    suspend fun postNote(eventId: String, note: TimelineNote): String
    suspend fun deleteNote(eventId: String, noteId: String)
    suspend fun uploadNotePhoto(eventId: String, imageUri: Uri): String
}