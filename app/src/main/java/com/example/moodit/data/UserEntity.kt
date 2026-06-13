package com.example.moodit.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey
    val userId: String,
    val email: String,
    val nickname: String,
    val passwordHash: String,
    val createdAt: Long = System.currentTimeMillis()
)
