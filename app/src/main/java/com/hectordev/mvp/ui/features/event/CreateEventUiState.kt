package com.hectordev.mvp.ui.features.event

data class CreateEventUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)