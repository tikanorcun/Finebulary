package com.language.finebulary.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.language.finebulary.MainActivity
import com.language.finebulary.R
import com.language.finebulary.data.model.Word
import com.language.finebulary.data.pref.UserPreferences
import com.language.finebulary.repository.CourseRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: CourseRepository,
    private val userPreferences: UserPreferences
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val enabled = userPreferences.remindersEnabled.first()
        if (!enabled) return Result.success()

        // Check time range
        val startHour = userPreferences.reminderStartHour.first()
        val endHour = userPreferences.reminderEndHour.first()
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        if (currentHour !in startHour..endHour) {
            return Result.success()
        }

        val courseIdsStr = userPreferences.reminderCourseIds.first()
        val words: List<Word> = if (courseIdsStr.isNotEmpty()) {
            val courseIds = courseIdsStr.mapNotNull { it.toLongOrNull() }
            repository.getWordsForCourseIds(courseIds)
        } else {
            repository.getAllWordsRaw()
        }

        if (words.isNotEmpty()) {
            val randomWord = words.random()
            sendWordNotification(randomWord)
        } else {
            val dueCount = repository.getTotalDueWordsCount()
            if (dueCount > 0) {
                sendFallbackNotification(dueCount)
            }
        }

        return Result.success()
    }

    private fun sendWordNotification(word: Word) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "vocabulary_daily_reminder"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Vocabulary Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily vocabulary word reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("WORD_ID", word.id)
            putExtra("SHOW_WORD_OVERLAY", true)
        }

        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(applicationContext, word.id.toInt(), intent, pendingIntentFlag)

        val meaning = word.backsPacked.split("|").first().trim()
        val contentText = buildString {
            append("Anlamı: $meaning")
            if (!word.example.isNullOrEmpty()) {
                append("\nÖrnek: ${word.example}")
            }
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("💡 Kelime Zamanı: ${word.front}")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(word.id.toInt(), notification)
    }

    private fun sendFallbackNotification(count: Int) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "vocabulary_daily_reminder"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Vocabulary Reminders", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 
            0, 
            intent, 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Finebulary Review Time!")
            .setContentText("You have $count words waiting to be mastered today.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(1, notification)
    }
}
