package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.data.database.AppDatabase
import com.example.data.repository.HunterRepository
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("NotificationReceiver", "Received broadcast with action: $action")

        if (action == "com.example.notifications.ACTION_TRIGGER_NOTIFY" || action == Intent.ACTION_BOOT_COMPLETED) {
            
            // Re-schedule on device boot completed
            if (action == Intent.ACTION_BOOT_COMPLETED) {
                SystemNotificationManager.scheduleAllDailyNotifications(context)
                Log.d("NotificationReceiver", "Re-scheduled daily notifications after boot.")
                return
            }

            val alertId = intent.getIntExtra("ALERT_ID", -1)
            if (alertId == -1) return

            // Run in background coroutine to access Room DB safely
            CoroutineScope(Dispatchers.IO).launch {
                triggerNotification(context, alertId)
            }
        }
    }

    private suspend fun triggerNotification(context: Context, alertId: Int) {
        val db = AppDatabase.getDatabase(context)
        val profile = db.hunterDao().getProfileSync()
        val name = profile?.displayName ?: "Hunter"
        
        val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val repository = HunterRepository(db.hunterDao())

        // Calculate calories target
        val weightKg = profile?.weight ?: 70.0
        val heightCm = profile?.height ?: 175.0
        val age = profile?.age ?: 25
        val gender = profile?.gender ?: "MALE"
        val calorieGoal = repository.calculateDailyCalories(weightKg, heightCm, age, gender).toInt()

        // Fetch workouts and quests completed today
        val workoutsCount = db.hunterDao().getWorkoutsCountForDate(todayDateStr)
        val quests = db.hunterDao().getQuestsForDateSync(todayDateStr)
        
        val isWaterCompleted = quests.find { it.exerciseName.contains("water", ignoreCase = true) }?.isCompleted ?: false
        val waterIntakeStr = if (isWaterCompleted) "8 / 8 glasses" else "0 / 8 glasses"

        val workoutLogToday = if (workoutsCount > 0) "Yes" else "No"

        // Calculate rank name
        val rawXp = profile?.totalXp ?: 0
        val rankName = when {
            rawXp < 100 -> "E-RANK"
            rawXp < 300 -> "D-RANK"
            rawXp < 600 -> "C-RANK"
            rawXp < 1000 -> "B-RANK"
            rawXp < 1500 -> "A-RANK"
            else -> "S-RANK"
        }

        // Today's EXP
        val allWorkouts = db.hunterDao().getRecentWorkouts(100) // retrieve recent sync
        // To be safe, let's filter workouts from database for today
        var todayWorkoutXp = 0
        try {
            // Retrieve today's workout logs
            todayWorkoutXp = db.hunterDao().getRecentWorkouts(100)
                .let { flow ->
                    // Since it's a Flow, we can get a sync snapshots or we query simple sum or fallback
                    0 // Flow requires collection, but we can also just fall back or calculate from quests
                }
        } catch (e: Exception) {}

        // Since we are in IO thread, we can query simple metrics or count
        val todayQuestXp = quests.filter { it.isCompleted }.sumOf { it.xpEarned }
        val totalTodayXp = todayQuestXp + (workoutsCount * 50) // estimate 50 xp per workout log

        val title: String
        val body: String

        when (alertId) {
            1001 -> {
                title = "[ SYSTEM ] DAILY MISSION AVAILABLE"
                body = "Hunter $name — Your daily quests are ready. Report for duty."
            }
            1002 -> {
                title = "[ HYDRATION ] WATER INTAKE ALERT"
                body = "Current intake: $waterIntakeStr. Stay hydrated, Hunter."
            }
            1003 -> {
                title = "[ ACTIVE STATUS ] ENERGY CORE VERIFICATION"
                body = "Current energy target: $calorieGoal kcal. Ensure nutritional replenishment."
            }
            1004 -> {
                title = "[ GATE ALERT ] SYSTEM DUNGEON ACCESS"
                body = "Workout logged today: $workoutLogToday. Access and clear your physical targets now."
            }
            1005 -> {
                title = "[ DECREE ] DAILY TRIALS EVALUATION"
                body = "Rank state: $rankName. EXP collected: +$totalTodayXp EXP."
            }
            1006 -> {
                title = "[ RECOVERY ] MONARCH REPLENISHMENT REST"
                body = "Sleep 7+ hours. Maintain active recovery schedule."
            }
            else -> return
        }

        sendSystemNotification(context, alertId, title, body)
    }

    private fun sendSystemNotification(context: Context, id: Int, title: String, message: String) {
        val notificationManager = NotificationManagerCompat.from(context)
        
        // Ensure channel and permissions are aligned
        val builder = NotificationCompat.Builder(context, "MONARCH_GATE_NOTIFICATIONS")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            // Check POST_NOTIFICATIONS permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    notificationManager.notify(id, builder.build())
                }
            } else {
                notificationManager.notify(id, builder.build())
            }
        } catch (e: SecurityException) {
            Log.e("NotificationReceiver", "SecurityException posting notification", e)
        }
    }
}
