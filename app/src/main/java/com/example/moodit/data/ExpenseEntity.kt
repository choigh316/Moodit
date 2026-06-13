package com.example.moodit.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Int,
    val category: String,
    val foodDetail: String = "NONE",
    val subCategory: String = "NONE",
    val mood: String,
    val memo: String,
    val paymentMethod: String = "카드",
    val createdAt: Long
)
