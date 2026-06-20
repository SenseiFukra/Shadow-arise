package com.example.data.repository

import com.example.data.database.BmiHistory
import com.example.data.database.DailyQuest
import com.example.data.database.HunterDao
import com.example.data.database.HunterProfile
import com.example.data.database.WorkoutLog
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HunterRepository(private val hunterDao: HunterDao) {

    val profile: Flow<HunterProfile?> = hunterDao.getProfile()
    val allWorkouts: Flow<List<WorkoutLog>> = hunterDao.getAllWorkouts()
    val bmiHistory: Flow<List<BmiHistory>> = hunterDao.getBmiHistory()
    val allQuests: Flow<List<DailyQuest>> = hunterDao.getAllQuestsFlow()

    fun getRecentWorkouts(limit: Int): Flow<List<WorkoutLog>> = hunterDao.getRecentWorkouts(limit)
    fun getQuestsForDate(date: String): Flow<List<DailyQuest>> = hunterDao.getQuestsForDate(date)
    suspend fun getQuestsForDateSync(date: String): List<DailyQuest> = hunterDao.getQuestsForDateSync(date)

    suspend fun getProfileSync(): HunterProfile? = hunterDao.getProfileSync()

    suspend fun saveProfile(profile: HunterProfile) {
        hunterDao.insertProfile(profile)
        backupCurrentProfile()
    }

    // Convert imperial values to metric for calculations
    fun convertToMetric(weight: Double, height: Double, unitType: String): Pair<Double, Double> {
        return if (unitType == "IMPERIAL") {
            val weightKg = weight * 0.45359237
            val heightCm = height * 2.54
            Pair(weightKg, heightCm)
        } else {
            Pair(weight, height)
        }
    }

    // Calculation formulas
    fun calculateBmi(weightKg: Double, heightCm: Double): Double {
        if (heightCm == 0.0) return 0.0
        val heightM = heightCm / 100.0
        return weightKg / (heightM * heightM)
    }

    fun getBmiLabel(bmi: Double): String {
        return when {
            bmi < 18.5 -> "Underweight — Train harder, Hunter"
            bmi < 25.0 -> "Normal — Peak condition"
            bmi < 30.0 -> "Overweight — The dungeon awaits"
            else -> "Obese — Rise, Hunter. Begin your journey."
        }
    }

    fun calculateDailyCalories(weightKg: Double, heightCm: Double, age: Int, gender: String, activityLevel: String = "Moderate"): Double {
        val bmr = when (gender.uppercase(Locale.ROOT)) {
            "MALE" -> (10 * weightKg) + (6.25 * heightCm) - (5 * age) + 5
            "FEMALE" -> (10 * weightKg) + (6.25 * heightCm) - (5 * age) - 161
            else -> {
                val male = (10 * weightKg) + (6.25 * heightCm) - (5 * age) + 5
                val female = (10 * weightKg) + (6.25 * heightCm) - (5 * age) - 161
                (male + female) / 2.0
            }
        }
        val multiplier = when (activityLevel.lowercase(Locale.ROOT).trim()) {
            "sedentary" -> 1.2
            "light", "lightly active" -> 1.375
            "moderate", "moderately active" -> 1.55
            "very", "very active" -> 1.725
            "extra", "extra active" -> 1.9
            else -> 1.55
        }
        return bmr * multiplier
    }

    fun calculateWaterIntake(weightKg: Double): Double {
        return weightKg * 0.035
    }

    // Logging workouts & XP awards
    suspend fun logWorkout(exerciseName: String, category: String, customXp: Int = 50) {
        val p = getProfileSync() ?: return
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Save log
        val winXp = customXp
        val workoutLog = WorkoutLog(
            dateString = dateString,
            exerciseName = exerciseName,
            category = category,
            xpEarned = winXp
        )
        hunterDao.insertWorkoutLog(workoutLog)

        // Calculate and update streak / stats
        var currentStreak = p.currentStreak
        val lastDateString = p.lastWorkoutDateString

        val yesterdayString = getYesterdayString()

        if (lastDateString == null) {
            currentStreak = 1
        } else if (lastDateString == yesterdayString) {
            currentStreak += 1
        } else if (lastDateString != dateString) {
            // Not today and not yesterday
            currentStreak = 1
        }

        // Specific Solo Leveling Streak bonuses:
        val streakBonus = when (currentStreak) {
            3 -> 150
            7 -> 400
            else -> 10 // small standard streak bonus
        }
        val isFirstWorkout = p.totalWorkoutsCompleted == 0
        val welcomeBonus = if (isFirstWorkout) 200 else 0
        val updatedXp = p.totalXp + winXp + streakBonus + welcomeBonus

        val updatedProfile = p.copy(
            totalXp = updatedXp,
            currentStreak = currentStreak,
            lastWorkoutDateString = dateString,
            totalWorkoutsCompleted = p.totalWorkoutsCompleted + 1,
            totalCaloriesBurned = p.totalCaloriesBurned + 150.0 // Estimation per workout
        )
        hunterDao.insertProfile(updatedProfile)
        backupCurrentProfile()
    }

    // Completing Daily Quest + bonus XP
    suspend fun completeQuestItem(quest: DailyQuest) {
        hunterDao.updateDailyQuest(quest.copy(isCompleted = true))

        // Check if all quests for this date are completed
        val date = quest.dateString
        val quests = hunterDao.getQuestsForDateSync(date)
        val allDone = quests.isNotEmpty() && quests.all { it.isCompleted || it.id == quest.id }

        if (allDone) {
            val p = getProfileSync()
            if (p != null) {
                // Award whole quest completion bonus
                val bonus = 100
                hunterDao.insertProfile(
                    p.copy(totalXp = p.totalXp + bonus)
                )
            }
        }
        backupCurrentProfile()
    }

    // Auto-generate Daily Quests
    suspend fun generateQuestsForToday(dayOfWeek: String, dateString: String) {
        val existing = hunterDao.getQuestsForDateSync(dateString)
        if (existing.isNotEmpty()) return

        val exercises = ExerciseData.getExercisesForDay(dayOfWeek)
        val quests = exercises.map { exercise ->
            val xpVal = ExerciseData.getExerciseExpValue(exercise.name)
            DailyQuest(
                dateString = dateString,
                questTitle = "DAILY QUEST",
                exerciseName = exercise.name,
                setsReps = if (exercise.category == "CARDIO" || exercise.category == "FLEXIBILITY") "20 min" else "3 Sets × 12 Reps",
                xpEarned = xpVal
            )
        }.toMutableList()

        // Formulate standard system daily quests requested in prompt
        quests.add(DailyQuest(dateString = dateString, questTitle = "SYSTEM QUEST", exerciseName = "Drink daily water goal", setsReps = "8 glasses", xpEarned = 50))
        quests.add(DailyQuest(dateString = dateString, questTitle = "SYSTEM QUEST", exerciseName = "Complete morning workout", setsReps = "Before 9:00 AM", xpEarned = 80))
        quests.add(DailyQuest(dateString = dateString, questTitle = "SYSTEM QUEST", exerciseName = "Walk 5,000 steps", setsReps = "Daily steps", xpEarned = 60))
        quests.add(DailyQuest(dateString = dateString, questTitle = "SYSTEM QUEST", exerciseName = "No junk food today", setsReps = "Clean choices", xpEarned = 30))
        quests.add(DailyQuest(dateString = dateString, questTitle = "SYSTEM QUEST", exerciseName = "Sleep 7+ hours", setsReps = "Night rest", xpEarned = 40))
        quests.add(DailyQuest(dateString = dateString, questTitle = "SYSTEM QUEST", exerciseName = "Log meals for the day", setsReps = "Food diary", xpEarned = 25))

        hunterDao.insertDailyQuests(quests)
    }

    // Generate Bmi History entry
    suspend fun recordBmiMeasure(weight: Double, height: Double, bmi: Double, dateString: String) {
        val entry = BmiHistory(
            dateString = dateString,
            weight = weight,
            height = height,
            bmi = bmi
        )
        hunterDao.insertBmiHistory(entry)
        backupCurrentProfile()
    }

    private fun getYesterdayString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    fun getRankName(xp: Int): String {
        return when {
            xp < 1000 -> "E"
            xp < 5000 -> "D"
            xp < 15000 -> "C"
            xp < 40000 -> "B"
            xp < 100000 -> "A"
            else -> "S"
        }
    }

    fun getRankTagline(xp: Int): String {
        return when {
            xp < 1000 -> "Beginner Hunter"
            xp < 5000 -> "Awakened Hunter"
            xp < 15000 -> "Shadow Apprentice"
            xp < 40000 -> "Shadow Knight"
            xp < 100000 -> "Shadow Monarch's Guard"
            else -> "Arise — Shadow Monarch"
        }
    }

    fun getXpProgress(xp: Int): Pair<Int, Int> {
        // Returns the (currentXpInLevel, maxLevelXp)
        return when {
            xp < 1000 -> Pair(xp, 1000)
            xp < 5000 -> Pair(xp - 1000, 4000)
            xp < 15000 -> Pair(xp - 5000, 10000)
            xp < 40000 -> Pair(xp - 15000, 25000)
            xp < 100000 -> Pair(xp - 40000, 60000)
            else -> Pair((xp - 100000).coerceAtLeast(0), 500000)
        }
    }

    suspend fun getUserAccount(username: String) = hunterDao.getUserAccountSync(username)

    suspend fun saveUserAccount(account: com.example.data.database.UserAccount) = hunterDao.insertUserAccount(account)

    suspend fun clearActiveSession() {
        hunterDao.clearProfileTable()
        hunterDao.clearWorkoutsTable()
        hunterDao.clearQuestsTable()
        hunterDao.clearBmiHistoryTable()
    }

    suspend fun backupCurrentProfile() {
        val currentProfile = getProfileSync() ?: return
        if (currentProfile.isRegistered && currentProfile.username != null) {
            val workouts = hunterDao.getAllWorkoutsSync()
            val bmiHistory = hunterDao.getBmiHistorySync()
            val quests = hunterDao.getAllQuestsSync()

            val workoutsJson = com.example.data.database.UserBackupHelper.workoutsToJson(workouts)
            val bmiHistoryJson = com.example.data.database.UserBackupHelper.bmiHistoryToJson(bmiHistory)
            val questsJson = com.example.data.database.UserBackupHelper.questsToJson(quests)
            val profileJson = com.example.data.database.UserBackupHelper.profileToJson(currentProfile)

            val existingAccount = hunterDao.getUserAccountSync(currentProfile.username)
            if (existingAccount != null) {
                val updatedAccount = existingAccount.copy(
                    profileJson = profileJson,
                    workoutsJson = workoutsJson,
                    bmiHistoryJson = bmiHistoryJson,
                    questsJson = questsJson
                )
                hunterDao.insertUserAccount(updatedAccount)
            }
        }
    }

    suspend fun restoreUserProfile(username: String): Boolean {
        val account = hunterDao.getUserAccountSync(username) ?: return false
        
        // Clear old database tables
        clearActiveSession()

        // Restore Profile
        val restoredProfile = if (account.profileJson != null) {
            com.example.data.database.UserBackupHelper.profileFromJson(account.profileJson)
        } else null

        val finalProfile = restoredProfile ?: HunterProfile(
            email = "$username@shadow.arise",
            displayName = account.nickname,
            username = username,
            isRegistered = true,
            height = 0.0,
            weight = 0.0,
            age = 0,
            gender = "Other"
        )
        
        hunterDao.insertProfile(finalProfile)

        // Restore Workouts
        if (account.workoutsJson != null) {
            val workouts = com.example.data.database.UserBackupHelper.workoutsFromJson(account.workoutsJson)
            workouts.forEach {
                hunterDao.insertWorkoutLog(it)
            }
        }

        // Restore BMI History
        if (account.bmiHistoryJson != null) {
            val bmiHistory = com.example.data.database.UserBackupHelper.bmiHistoryFromJson(account.bmiHistoryJson)
            bmiHistory.forEach {
                hunterDao.insertBmiHistory(it)
            }
        }

        // Restore Quests
        if (account.questsJson != null) {
            val quests = com.example.data.database.UserBackupHelper.questsFromJson(account.questsJson)
            hunterDao.insertDailyQuests(quests)
        }
        
        return true
    }

    suspend fun loginAsGuest(nickname: String) {
        // Clear old database tables
        clearActiveSession()

        val defaultProfile = HunterProfile(
            email = "guest@shadow.arise",
            displayName = nickname,
            username = "guest_${System.currentTimeMillis()}",
            isRegistered = false,
            height = 0.0,
            weight = 0.0,
            age = 0,
            gender = "Other"
        )
        hunterDao.insertProfile(defaultProfile)
    }

    suspend fun registerNewUser(username: String, passwordHash: String, nickname: String, mobileNumber: String): Boolean {
        // Check if username existing database
        val existing = hunterDao.getUserAccountSync(username)
        if (existing != null) return false

        // Create new account
        val newAccount = com.example.data.database.UserAccount(
            username = username,
            passwordHash = passwordHash,
            nickname = nickname,
            mobileNumber = mobileNumber
        )
        hunterDao.insertUserAccount(newAccount)

        // Clear active session and log in as this new user
        clearActiveSession()

        val defaultProfile = HunterProfile(
            email = "$username@shadow.arise",
            displayName = nickname,
            username = username,
            isRegistered = true,
            height = 0.0,
            weight = 0.0,
            age = 0,
            gender = "Other"
        )
        hunterDao.insertProfile(defaultProfile)
        
        return true
    }
}
