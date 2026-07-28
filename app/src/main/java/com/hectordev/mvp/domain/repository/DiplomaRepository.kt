package com.hectordev.mvp.domain.repository

import android.net.Uri
import com.hectordev.mvp.domain.DiplomaData

interface DiplomaRepository {
    suspend fun generateAndSave(data: DiplomaData): String // returns saved filename
}