package com.hectordev.mvp.data.mapper

import com.hectordev.mvp.data.model.EventDto
import com.hectordev.mvp.domain.Event
import com.hectordev.mvp.domain.EventStatus

fun EventDto.toDomain(): Event {
    return Event(
        id = id,
        title = title,
        locationLabel = locationLabel,
        latitude = latitude,
        longitude = longitude,
        startDate = startDate,
        endDate = endDate,
        status = runCatching { EventStatus.valueOf(status) }.getOrDefault(EventStatus.PRE_TRIP),
        adminId = adminId,
        participants = participants,
        mvpId = mvpId,
        galaPhotoUrl = galaPhotoUrl,
        activeEmergencyId = activeEmergencyId,
        createdAt = createdAt
    )
}

fun Event.toDto(): EventDto {
    return EventDto(
        id = id,
        title = title,
        locationLabel = locationLabel,
        latitude = latitude,
        longitude = longitude,
        startDate = startDate,
        endDate = endDate,
        status = status.name,
        adminId = adminId,
        participants = participants,
        mvpId = mvpId,
        galaPhotoUrl = galaPhotoUrl,
        activeEmergencyId = activeEmergencyId,
        createdAt = createdAt
    )
}