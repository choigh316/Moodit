package com.example.moodit.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EmotionDiaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDiary(diary: EmotionDiaryEntity)

    @Query("SELECT * FROM emotion_diaries WHERE userId = :userId AND dateText = :dateText LIMIT 1")
    fun getDiaryForDate(userId: String, dateText: String): Flow<EmotionDiaryEntity?>

    @Query("SELECT * FROM emotion_diaries WHERE userId = :userId ORDER BY dateText DESC")
    fun getDiariesForUser(userId: String): Flow<List<EmotionDiaryEntity>>

    @Query("DELETE FROM emotion_diaries WHERE userId = :userId")
    suspend fun deleteDiariesForUser(userId: String)
}
