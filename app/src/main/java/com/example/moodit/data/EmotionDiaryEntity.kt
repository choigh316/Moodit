package com.example.moodit.data

import androidx.room.Entity

@Entity(
    tableName = "emotion_diaries",
    primaryKeys = ["userId", "dateText"]
)
data class EmotionDiaryEntity(
    val userId: String,
    val dateText: String,
    val mood: String,
    val note: String,
    val updatedAt: Long = System.currentTimeMillis()
)
