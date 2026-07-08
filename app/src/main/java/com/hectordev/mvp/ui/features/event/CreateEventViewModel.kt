package com.hectordev.mvp.ui.features.event

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.hectordev.mvp.domain.Event
import com.hectordev.mvp.domain.EventStatus
import com.hectordev.mvp.domain.repository.EventsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val eventsRepository: EventsRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    // Form fields exposed as Compose state for direct observation in the UI
    var title by mutableStateOf("")
        private set
    var startDate by mutableLongStateOf(0L)
        private set
    var endDate by mutableLongStateOf(0L)
        private set
    var locationLabel by mutableStateOf("")
        private set
    var latitude by mutableStateOf<Double?>(null)
        private set
    var longitude by mutableStateOf<Double?>(null)
        private set

    private val isFormValid: Boolean
        get() {
            val todayMillis = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            return title.isNotBlank()
                && startDate > 0L
                && endDate > 0L
                && endDate >= startDate
                && endDate >= todayMillis
        }

    var showErrors by mutableStateOf(false)
        private set

    private val _uiState = MutableStateFlow(CreateEventUiState())
    val uiState: StateFlow<CreateEventUiState> = _uiState.asStateFlow()

    fun updateTitle(value: String) { title = value }
    fun updateStartDate(value: Long) { startDate = value }
    fun updateEndDate(value: Long) { endDate = value }
    fun updateLocationLabel(value: String) { locationLabel = value }
    fun updateLocation(lat: Double, lng: Double) { latitude = lat; longitude = lng }
    fun clearLocation() { latitude = null; longitude = null }

    fun createEvent() {
        if (!isFormValid) {
            showErrors = true
            return
        }
        val adminId = firebaseAuth.currentUser?.uid ?: return

        val event = Event(
            title = title.trim(),
            locationLabel = locationLabel.trim().ifBlank { null },
            latitude = latitude,
            longitude = longitude,
            startDate = startDate,
            endDate = endDate,
            status = EventStatus.PRE_TRIP,
            adminId = adminId,
            participants = listOf(adminId),
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Navigate immediately — don't block on Firestore write
            _uiState.update { it.copy(isLoading = false, isSuccess = true) }

            // Fire-and-forget: Firestore offline persistence will sync when ready
            try {
                eventsRepository.createEvent(event)
            } catch (e: Exception) {
                android.util.Log.e("CreateEvent", "Failed to save event: ${e.message}")
            }
        }
    }

    fun resetUiState() = _uiState.update { CreateEventUiState() }
}