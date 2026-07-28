package com.hectordev.mvp.ui.features.profile

data class BadgesUiState(
    val userName: String = "",
    val userPhotoUrl: String? = null,
    val lifetimeMvps: Int = 0,
    val oracleWins: Int = 0,
    val tripleWins: Int = 0,
    val reactorWins: Int = 0,
    val totalPlayerWins: Int = 0,
    val isLoading: Boolean = true
)