package com.language.finebulary.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.language.finebulary.data.db.AppDatabase
import com.language.finebulary.repository.CourseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    private val receiverJob = SupervisorJob()
    private val receiverScope = CoroutineScope(Dispatchers.IO + receiverJob)

    override fun onReceive(context: Context, intent: Intent) {
        val wordId = intent.getLongExtra("EXTRA_WORD_ID", -1L)
        val courseId = intent.getLongExtra("EXTRA_COURSE_ID", -1L)
        val actionType = intent.action

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (wordId != -1L) {
            notificationManager.cancel(wordId.toInt())
        }

        if (wordId == -1L || courseId == -1L) return

        val db = AppDatabase.getDatabase(context)
        val repo = CourseRepository(db.vocabularyDao())

        receiverScope.launch {
            when (actionType) {
                "ACTION_MASTER_WORD" -> {
                    repo.updateReview(courseId, wordId, "EASY")
                }
                "ACTION_KEEP_REMINDING" -> {
                    repo.updateReview(courseId, wordId, "HARD")
                }
            }
        }
    }
}
