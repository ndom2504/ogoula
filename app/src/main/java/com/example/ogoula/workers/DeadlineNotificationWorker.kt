package com.example.ogoula.workers

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.ogoula.R

class DeadlineNotificationWorker(
    private val context: Context,
    params: WorkerParameters,
) : Worker(context, params) {

    override fun doWork(): Result {
        val postId   = inputData.getString("post_id")   ?: return Result.failure()
        val content  = inputData.getString("content")   ?: ""
        val type     = inputData.getString("type")      ?: "post"
        val isReminder = inputData.getBoolean("is_reminder", false)

        val typeLabel = when (type) {
            "vote"     -> "vote"
            "sondage"  -> "sondage"
            "concours" -> "concours"
            "enquete"  -> "enquête"
            else       -> "post"
        }

        val preview = content.take(60).let { if (content.length > 60) "$it…" else it }
        val title: String
        val body: String
        if (isReminder) {
            title = "⏰ Rappel – J-1"
            body  = "Votre $typeLabel se termine demain : \"$preview\""
        } else {
            title = "🔴 Délai atteint"
            body  = "Votre $typeLabel est maintenant clôturé : \"$preview\""
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(postId.hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "ogoula_deadlines"
    }
}
