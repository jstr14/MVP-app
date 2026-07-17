package com.hectordev.mvp.ui.features.gala

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.hectordev.mvp.domain.repository.EventsRepository
import com.hectordev.mvp.ui.navigation.AppDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class GalaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val eventsRepository: EventsRepository
) : ViewModel() {

    val eventId: String = savedStateHandle.toRoute<AppDestination.Gala>().eventId

    private val _uiState = MutableStateFlow(GalaUiState())
    val uiState: StateFlow<GalaUiState> = _uiState.asStateFlow()
}