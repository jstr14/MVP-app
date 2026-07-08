package com.hectordev.mvp.ui.core.extensions

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.toFormattedDate(): String {
    if (this == 0L) return ""
    return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(this))
}