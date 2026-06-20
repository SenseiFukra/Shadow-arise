package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hunter_profile")
data class HunterProfile(
    @PrimaryKey val id: String = "primary_profile",
    val email: String,
    val displayName: String,
    val profilePicUrl: String? = null,
    val height: Double, // in standard unit depending on unitType (cm or inches)
    val weight: Double, // in standard unit depending on unitType (kg or lbs)
    val age: Int,
    val gender: String, // Male, Female, Other
    val unitType: String = "METRIC", // METRIC, IMPERIAL
    val totalXp: Int = 0,
    val currentStreak: Int = 0,
    val lastWorkoutDateString: String? = null, // "yyyy-MM-dd"
    val totalWorkoutsCompleted: Int = 0,
    val totalCaloriesBurned: Double = 0.0,
    val notificationEnabled: Boolean = true,
    val aiChatEnabled: Boolean = true,
    val joinDateString: String = "June 19, 2026",
    val username: String? = null,
    val isRegistered: Boolean = false,
    val activityLevel: String = "Moderate"
)

@Entity(tableName = "user_account")
data class UserAccount(
    @PrimaryKey val username: String,
    val passwordHash: String,
    val nickname: String,
    val mobileNumber: String = "",
    val profileJson: String? = null,
    val workoutsJson: String? = null,
    val bmiHistoryJson: String? = null,
    val questsJson: String? = null
)

@Entity(tableName = "workout_log")
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateString: String, // "yyyy-MM-dd"
    val exerciseName: String,
    val category: String,
    val xpEarned: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_quest")
data class DailyQuest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateString: String, // "yyyy-MM-dd"
    val questTitle: String,
    val exerciseName: String,
    val setsReps: String,
    val isCompleted: Boolean = false,
    val xpEarned: Int = 50
)

@Entity(tableName = "bmi_history")
data class BmiHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateString: String, // "yyyy-MM-dd"
    val weight: Double,
    val height: Double,
    val bmi: Double,
    val timestamp: Long = System.currentTimeMillis()
)
