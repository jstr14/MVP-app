package com.hectordev.mvp.data.mapper

import com.hectordev.mvp.data.model.TimelineNoteDto
import com.hectordev.mvp.domain.NoteType
import com.hectordev.mvp.domain.TimelineNote
import com.hectordev.mvp.domain.TimelineTier

fun TimelineNoteDto.toDomain(): TimelineNote {
    return TimelineNote(
        id = id,
        authorId = authorId,
        targetUserId = targetUserId,
        type = runCatching { NoteType.valueOf(type) }.getOrDefault(NoteType.TEXT),
        textContent = textContent,
        contentUrl = contentUrl,
        tier = TimelineTier.entries.find { it.label == tierLabel } ?: TimelineTier.FACT,
        pointsAwarded = pointsAwarded,
        timestamp = timestamp,
        reactions = reactions
    )
}

fun TimelineNote.toDto(): TimelineNoteDto {
    return TimelineNoteDto(
        id = id,
        authorId = authorId,
        targetUserId = targetUserId,
        type = type.name,
        textContent = textContent,
        contentUrl = contentUrl,
        tierLabel = tier.label,
        pointsAwarded = tier.points,
        timestamp = timestamp,
        reactions = reactions
    )
}