package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object SystemNotificationManager {

    private const val CHANNEL_ID = "MONARCH_GATE_NOTIFICATIONS"
    private const val CHANNEL_NAME = "System Gate Reminders"

    fun initNotifications(context: Context) {
        createNotificationChannel(context)
        scheduleAllDailyNotifications(context)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Monarch Gate reminder and quest notification systems."
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleAllDailyNotifications(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val times = listOf(
            Pair(1001, Pair(8, 0)),   // 08:00 AM
            Pair(1002, Pair(10, 0)),  // 10:00 AM
            Pair(1003, Pair(13, 0)),  // 01:00 PM
            Pair(1004, Pair(16, 0)),  // 04:00 PM
            Pair(1005, Pair(20, 0)),  // 08:00 PM
            Pair(1006, Pair(23, 0))   // 11:00 PM
        )

        for ((id, hourMinute) in times) {
            val hour = hourMinute.first
            val minute = hourMinute.second

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = "com.example.notifications.ACTION_TRIGGER_NOTIFY"
                putExtra("ALERT_ID", id)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                // Inexact alarm works wonderfully, is compliant with Android restrictions, and repeating ensures battery efficiency
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
                Log.d("SystemNotificationManager", "Scheduled alarm $id for $hour:$minute")
            } catch (e: Exception) {
                Log.e("SystemNotificationManager", "Failed to schedule alarm $id", e)
            }
        }
    }
}
