package com.example

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.RetrofitGeminiClient
import com.example.data.database.AppDatabase
import com.example.data.database.BmiHistory
import com.example.data.database.DailyQuest
import com.example.data.database.HunterProfile
import com.example.data.database.WorkoutLog
import com.example.data.repository.HunterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ChatMessage(
    val sender: Sender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val illustResId: Int? = null,
    val isSavedToGallery: Boolean = false
)

enum class Sender {
    USER, SHADOW
}

class HunterViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = HunterRepository(db.hunterDao())

    // UI flows from Room Database
    val profile: StateFlow<HunterProfile?> = repository.profile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val workouts: StateFlow<List<WorkoutLog>> = repository.allWorkouts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val bmiHistory: StateFlow<List<BmiHistory>> = repository.bmiHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allQuests: StateFlow<List<DailyQuest>> = repository.allQuests.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dynamic Daily Quests (observed based on today's dateString)
    private val _todayDateString = MutableStateFlow(getTodayDateString())
    val todayDateString = _todayDateString.asStateFlow()

    private val _quests = MutableStateFlow<List<DailyQuest>>(emptyList())
    val quests = _quests.asStateFlow()

    // Level-up Event
    private val _levelUpEvent = MutableStateFlow<Pair<String, String>?>(null)
    val levelUpEvent = _levelUpEvent.asStateFlow()

    // Online Connectivity
    private val _isOnline = MutableStateFlow(false)
    val isOnline = _isOnline.asStateFlow()

    // AI Companion Chat history (session-based)
    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory = _chatHistory.asStateFlow()

    private val _chatLoading = MutableStateFlow(false)
    val chatLoading = _chatLoading.asStateFlow()

    private val _savedVisuals = MutableStateFlow<Set<Int>>(emptySet())
    val savedVisuals: StateFlow<Set<Int>> = _savedVisuals.asStateFlow()

    // Weekly Completion State for Streak tracker
    private val _weeklyCompletion = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val weeklyCompletion: StateFlow<Map<String, Boolean>> = _weeklyCompletion.asStateFlow()

    fun refreshWeeklyCompletion() {
        viewModelScope.launch {
            try {
                val dates = mutableListOf<String>()
                val cal = java.util.Calendar.getInstance()
                cal.firstDayOfWeek = java.util.Calendar.MONDAY
                cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
                val sdf = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                for (i in 0..6) {
                    dates.add(sdf.format(cal.time))
                    cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
                }
                
                val resultMap = mutableMapOf<String, Boolean>()
                for (date in dates) {
                    val list = repository.getQuestsForDateSync(date)
                    val allCompleted = list.isNotEmpty() && list.all { it.isCompleted }
                    resultMap[date] = allCompleted
                }
                _weeklyCompletion.value = resultMap
            } catch (e: Exception) {
                // Safeguard against calendar errors
            }
        }
    }

    // Penalty Zone State
    private val _penaltyActive = MutableStateFlow(false)
    val penaltyActive = _penaltyActive.asStateFlow()

    fun triggerPenalty() {
        viewModelScope.launch {
            _penaltyActive.value = true
            val current = repository.getProfileSync() ?: return@launch
            val deduction = kotlin.random.Random.nextInt(60, 161)
            val newXp = (current.totalXp - deduction).coerceAtLeast(0)
            repository.saveProfile(current.copy(totalXp = newXp))
        }
    }

    fun clearPenalty() {
        viewModelScope.launch {
            _penaltyActive.value = false
            val current = repository.getProfileSync() ?: return@launch
            // Completing penalties grants NO points
            repository.saveProfile(current.copy(totalXp = current.totalXp))
        }
    }

    fun updateProfileNameAndAvatar(displayName: String, avatarPreset: String) {
        viewModelScope.launch {
            val current = repository.getProfileSync() ?: return@launch
            repository.saveProfile(
                current.copy(
                    displayName = displayName,
                    profilePicUrl = avatarPreset
                )
            )
        }
    }

    fun saveToGallery(resId: Int) {
        _savedVisuals.value = _savedVisuals.value + resId
    }

    private val connectivityManager =
        application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    init {
        checkInternetConnection()
        registerNetworkCallback()
        generateQuestsForDate(getTodayDateString())
        refreshWeeklyCompletion()
    }

    private fun checkInternetConnection() {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        _isOnline.value = capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
    }

    private fun registerNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
            }

            override fun onLost(network: Network) {
                _isOnline.value = false
            }
        })
    }

    fun generateQuestsForDate(dateStr: String) {
        viewModelScope.launch {
            val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
            repository.generateQuestsForToday(dayOfWeek, dateStr)
            
            // Start collecting live updates for today's quests
            repository.getQuestsForDate(dateStr).collect {
                _quests.value = it
                refreshWeeklyCompletion()
            }
        }
    }

    fun loginAsGuest(nickname: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.loginAsGuest(nickname)
            // Generate standard quests for today's date
            generateQuestsForDate(getTodayDateString())
            onComplete()
        }
    }

    fun registerUser(username: String, passwordHash: String, nickname: String, mobileNumber: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val success = repository.registerNewUser(username, passwordHash, nickname, mobileNumber)
            if (success) {
                generateQuestsForDate(getTodayDateString())
                onSuccess()
            } else {
                onError("Username is already claimed by another awakening.")
            }
        }
    }

    fun loginUser(username: String, passwordHash: String, onSuccess: (isFirstTime: Boolean) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val account = repository.getUserAccount(username)
            if (account == null) {
                onError("Monarch Registry: Account not found.")
                return@launch
            }
            if (account.passwordHash != passwordHash) {
                onError("Incorrect password. Integrity check failed.")
                return@launch
            }

            // Restore
            val isRestored = repository.restoreUserProfile(username)
            if (isRestored) {
                generateQuestsForDate(getTodayDateString())
                val updatedProfile = repository.getProfileSync()
                val isFirstTime = updatedProfile == null || updatedProfile.height == 0.0 || updatedProfile.weight == 0.0
                onSuccess(isFirstTime)
            } else {
                onError("Corrupted dimension: Failed to restore user data.")
            }
        }
    }

    fun completeOnboarding(height: Double, weight: Double, age: Int, gender: String, unitType: String, activityLevel: String = "Moderate") {
        viewModelScope.launch {
            val current = repository.getProfileSync() ?: return@launch
            val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val metricPair = repository.convertToMetric(weight, height, unitType)
            val bmiVal = repository.calculateBmi(metricPair.first, metricPair.second)

            val updatedProfile = current.copy(
                height = height,
                weight = weight,
                age = age,
                gender = gender,
                unitType = unitType,
                activityLevel = activityLevel,
                joinDateString = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
            )
            repository.saveProfile(updatedProfile)

            // Log first BMI measurement
            repository.recordBmiMeasure(weight, height, bmiVal, formattedDate)
        }
    }

    fun updateProfileMeasurements(height: Double, weight: Double, age: Int, activityLevel: String) {
        viewModelScope.launch {
            val current = repository.getProfileSync() ?: return@launch
            val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val metricPair = repository.convertToMetric(weight, height, current.unitType)
            val bmiVal = repository.calculateBmi(metricPair.first, metricPair.second)

            val updatedProfile = current.copy(
                height = height,
                weight = weight,
                age = age,
                activityLevel = activityLevel
            )
            repository.saveProfile(updatedProfile)

            // Log updated BMI measure
            repository.recordBmiMeasure(weight, height, bmiVal, formattedDate)
        }
    }

    fun changeUnits(unitType: String) {
        viewModelScope.launch {
            val current = repository.getProfileSync() ?: return@launch
            if (current.unitType == unitType) return@launch

            // Conversion factors between METRIC <-> IMPERIAL
            val newHeight = if (unitType == "IMPERIAL") current.height / 2.54 else current.height * 2.54
            val newWeight = if (unitType == "IMPERIAL") current.weight / 0.45359237 else current.weight * 0.45359237

            repository.saveProfile(
                current.copy(
                    unitType = unitType,
                    height = Math.round(newHeight * 10.0) / 10.0,
                    weight = Math.round(newWeight * 10.0) / 10.0
                )
            )
        }
    }

    fun setNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getProfileSync() ?: return@launch
            repository.saveProfile(current.copy(notificationEnabled = enabled))
        }
    }

    fun setAiChatEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getProfileSync() ?: return@launch
            repository.saveProfile(current.copy(aiChatEnabled = enabled))
        }
    }

    fun logWorkout(exerciseName: String, category: String, customXp: Int? = null) {
        viewModelScope.launch {
            val current = repository.getProfileSync() ?: return@launch
            val oldRank = repository.getRankName(current.totalXp)

            val finalXp = customXp ?: com.example.data.repository.ExerciseData.getExerciseExpValue(exerciseName)
            repository.logWorkout(exerciseName, category, finalXp)

            val updated = repository.getProfileSync() ?: return@launch
            val newRank = repository.getRankName(updated.totalXp)

            if (oldRank != newRank) {
                _levelUpEvent.value = Pair(oldRank, newRank)
            }
        }
    }

    fun completeQuestItem(quest: DailyQuest) {
        viewModelScope.launch {
            val current = repository.getProfileSync() ?: return@launch
            val oldRank = repository.getRankName(current.totalXp)

            repository.completeQuestItem(quest)
            refreshWeeklyCompletion()

            val updated = repository.getProfileSync()
            if (updated != null) {
                val newRank = repository.getRankName(updated.totalXp)
                if (oldRank != newRank) {
                    _levelUpEvent.value = Pair(oldRank, newRank)
                }
            }
        }
    }

    fun dismissLevelUp() {
        _levelUpEvent.value = null
    }

    fun signOut() {
        viewModelScope.launch {
            // Backup current session first
            repository.backupCurrentProfile()
            // Clear current active tables
            repository.clearActiveSession()
            _chatHistory.value = emptyList()
        }
    }

    fun sendChatMessage(text: String) {
        if (text.trim().isEmpty() || _chatLoading.value) return

        val userMessage = ChatMessage(Sender.USER, text)
        _chatHistory.value = _chatHistory.value + userMessage

        if (!_isOnline.value) {
            _chatHistory.value = _chatHistory.value + ChatMessage(Sender.SHADOW, "I cannot hear you, Hunter. Establish connection to manifest me.")
            return
        }

        _chatLoading.value = true
        viewModelScope.launch {
            val currentProfile = repository.getProfileSync()
            val hunterName = currentProfile?.displayName?.split(" ")?.firstOrNull() ?: "Prince"
            val systemPrompt = """
                You are Shadow, an ancient and powerful AI entity bound to serve the Hunter. 
                You speak formally, like a loyal knight. 
                Always address the user as 'Hunter $hunterName'. 
                You give fitness advice, exercise tips, nutrition guidance, and motivational support. 
                You reference Solo Leveling lore naturally (such as dungeons, gates, system rewards, shadow army, arises). 
                You never break character. 
                Keep responses concise — 2 to 4 sentences max unless asked for detail.
            """.trimIndent()

            val aiResponseText = RetrofitGeminiClient.getShadowResponse(text, systemPrompt)
            
            // Auto check if asking for deadlift/squat demonstrate
            val lowercaseMsg = text.lowercase()
            val illustId = when {
                lowercaseMsg.contains("dead") || lowercaseMsg.contains("lift") -> R.drawable.deadlift_demo
                lowercaseMsg.contains("squat") -> R.drawable.squat_demo
                else -> null
            }
            
            _chatHistory.value = _chatHistory.value + ChatMessage(Sender.SHADOW, aiResponseText, illustResId = illustId)
            _chatLoading.value = false
        }
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HunterViewModel::class.java)) {
                return HunterViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
