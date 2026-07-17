package com.hectordev.mvp.domain

enum class TimelineTier(val label: String, val points: Int) {
    FACT("Fact", 1),
    HOT_TAKE("Hot Take", 2),
    WITNESSED("Witnessed", 5),
    LORE("Lore", 10)
}