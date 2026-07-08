package com.hectordev.mvp.domain.repository

import com.hectordev.mvp.domain.Event
import com.hectordev.mvp.domain.EventStatus
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
    fun observeEvent(eventId: String): Flow<Event?>
    fun observeActiveEvent(userId: String): Flow<Event?>
    fun observePastEvents(userId: String): Flow<List<Event>>
}