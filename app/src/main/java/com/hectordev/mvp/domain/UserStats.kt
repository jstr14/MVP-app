package com.hectordev.mvp.domain

data class UserStats(
    val lifetimeMvps: Int = 0,
    val oracleWins: Int = 0,
    val tripleWins: Int = 0,
    val reactorWins: Int = 0,
    val totalPlayerWins: Int = 0
)