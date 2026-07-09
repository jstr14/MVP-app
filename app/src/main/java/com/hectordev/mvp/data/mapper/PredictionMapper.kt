package com.hectordev.mvp.data.mapper

import com.hectordev.mvp.data.model.PredictionDto
import com.hectordev.mvp.domain.Prediction

fun PredictionDto.toDomain() = Prediction(
    projectedMvpId = projectedMvpId,
    tripleParticipantId = tripleParticipantId
)

fun Prediction.toDto() = PredictionDto(
    projectedMvpId = projectedMvpId,
    tripleParticipantId = tripleParticipantId
)