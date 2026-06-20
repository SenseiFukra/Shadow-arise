package com.example.data.database

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object UserBackupHelper {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    fun profileToJson(profile: HunterProfile): String {
        return moshi.adapter(HunterProfile::class.java).toJson(profile)
    }

    fun profileFromJson(json: String): HunterProfile? {
        return try {
            moshi.adapter(HunterProfile::class.java).fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    fun workoutsToJson(workouts: List<WorkoutLog>): String {
        val type = Types.newParameterizedType(List::class.java, WorkoutLog::class.java)
        return moshi.adapter<List<WorkoutLog>>(type).toJson(workouts)
    }

    fun workoutsFromJson(json: String): List<WorkoutLog> {
        val type = Types.newParameterizedType(List::class.java, WorkoutLog::class.java)
        return try {
            moshi.adapter<List<WorkoutLog>>(type).fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun bmiHistoryToJson(history: List<BmiHistory>): String {
        val type = Types.newParameterizedType(List::class.java, BmiHistory::class.java)
        return moshi.adapter<List<BmiHistory>>(type).toJson(history)
    }

    fun bmiHistoryFromJson(json: String): List<BmiHistory> {
        val type = Types.newParameterizedType(List::class.java, BmiHistory::class.java)
        return try {
            moshi.adapter<List<BmiHistory>>(type).fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun questsToJson(quests: List<DailyQuest>): String {
        val type = Types.newParameterizedType(List::class.java, DailyQuest::class.java)
        return moshi.adapter<List<DailyQuest>>(type).toJson(quests)
    }

    fun questsFromJson(json: String): List<DailyQuest> {
        val type = Types.newParameterizedType(List::class.java, DailyQuest::class.java)
        return try {
            moshi.adapter<List<DailyQuest>>(type).fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
