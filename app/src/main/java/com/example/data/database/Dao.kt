package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HunterDao {

    // Profile Transactions
    @Query("SELECT * FROM hunter_profile WHERE id = 'primary_profile' LIMIT 1")
    fun getProfile(): Flow<HunterProfile?>

    @Query("SELECT * FROM hunter_profile WHERE id = 'primary_profile' LIMIT 1")
    suspend fun getProfileSync(): HunterProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: HunterProfile)

    @Update
    suspend fun updateProfile(profile: HunterProfile)

    // User Account Transactions
    @Query("SELECT * FROM user_account WHERE username = :username LIMIT 1")
    suspend fun getUserAccountSync(username: String): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAccount(account: UserAccount)

    @Query("DELETE FROM user_account WHERE username = :username")
    suspend fun deleteUserAccount(username: String)

    // Workout Log Transactions
    @Query("SELECT * FROM workout_log ORDER BY timestamp DESC")
    fun getAllWorkouts(): Flow<List<WorkoutLog>>

    @Query("SELECT * FROM workout_log ORDER BY timestamp DESC")
    suspend fun getAllWorkoutsSync(): List<WorkoutLog>

    @Query("SELECT * FROM workout_log ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentWorkouts(limit: Int): Flow<List<WorkoutLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutLog(log: WorkoutLog)

    // Daily Quest Transactions
    @Query("SELECT * FROM daily_quest WHERE dateString = :date")
    fun getQuestsForDate(date: String): Flow<List<DailyQuest>>

    @Query("SELECT * FROM daily_quest WHERE dateString = :date")
    suspend fun getQuestsForDateSync(date: String): List<DailyQuest>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyQuests(quests: List<DailyQuest>)

    @Update
    suspend fun updateDailyQuest(quest: DailyQuest)

    @Query("SELECT * FROM daily_quest")
    fun getAllQuestsFlow(): Flow<List<DailyQuest>>

    @Query("SELECT * FROM daily_quest")
    suspend fun getAllQuestsSync(): List<DailyQuest>

    @Query("SELECT COUNT(*) FROM workout_log WHERE dateString = :dateString")
    suspend fun getWorkoutsCountForDate(dateString: String): Int

    // BMI History Transactions
    @Query("SELECT * FROM bmi_history ORDER BY timestamp ASC")
    fun getBmiHistory(): Flow<List<BmiHistory>>

    @Query("SELECT * FROM bmi_history ORDER BY timestamp ASC")
    suspend fun getBmiHistorySync(): List<BmiHistory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBmiHistory(history: BmiHistory)

    // Table Clear Transactions (for user session switches)
    @Query("DELETE FROM hunter_profile")
    suspend fun clearProfileTable()

    @Query("DELETE FROM workout_log")
    suspend fun clearWorkoutsTable()

    @Query("DELETE FROM daily_quest")
    suspend fun clearQuestsTable()

    @Query("DELETE FROM bmi_history")
    suspend fun clearBmiHistoryTable()
}
