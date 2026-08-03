package com.hectordev.mvp.domain.repository

import com.hectordev.mvp.domain.EmergencyRequest
import kotlinx.coroutines.flow.Flow

interface EmergencyRepository {
    suspend fun triggerEmergency(eventId: String, request: EmergencyRequest): String
    fun observeEmergencyRequest(eventId: String, requestId: String): Flow<EmergencyRequest?>
    suspend fun castVote(eventId: String, requestId: String, userId: String, accept: Boolean)
    suspend fun resolveTimeout(eventId: String, requestId: String, triggeredById: String)
}