package com.example.data.network

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.example.data.database.AppDatabase
import com.example.data.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val actionString = intent.action ?: return
        val historyId = intent.getIntExtra("notification_history_id", -1)
        val url = intent.getStringExtra("notification_url")

        val database = AppDatabase.getInstance(context)
        val repository = AppRepository(database.appDao, context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        CoroutineScope(Dispatchers.IO).launch {
            when (actionString) {
                "DISMISS_NOTIFICATION" -> {
                    if (historyId != -1) {
                        repository.updateNotificationHistoryStatus(historyId, "Dismissed")
                    }
                }

                "ACTION_MARK_READ" -> {
                    if (historyId != -1) {
                        repository.updateNotificationHistoryStatus(historyId, "Opened")
                        notificationManager.cancel(historyId)
                    }
                }

                "NOTIF_REPLY" -> {
                    val replyBundle = RemoteInput.getResultsFromIntent(intent)
                    if (replyBundle != null) {
                        val replyText = replyBundle.getCharSequence("key_text_reply")?.toString() ?: ""
                        if (replyText.isNotEmpty()) {
                            // Save reply to database or status field
                            if (historyId != -1) {
                                repository.updateNotificationHistoryStatus(historyId, "Replied: $replyText")
                            }
                            
                            // Re-display notification with replied state to confirm
                            val builder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_MESSAGES)
                                .setSmallIcon(android.R.drawable.stat_notify_chat)
                                .setContentTitle("Message Sent")
                                .setContentText("You replied: \"$replyText\"")
                                .setTimeoutAfter(3000)
                                .setAutoCancel(true)
                            
                            notificationManager.notify(historyId, builder.build())
                        }
                    }
                }

                "ACTION_SHARE" -> {
                    if (historyId != -1) {
                        repository.updateNotificationHistoryStatus(historyId, "Action Clicked")
                    }
                    val shareUrl = url ?: "https://reelsviral.example.com"
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Check out this content from ReelsViral: $shareUrl")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    val chooser = Intent.createChooser(shareIntent, "Share Content").apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(chooser)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                "MEDIA_PREV", "MEDIA_PAUSE", "MEDIA_NEXT" -> {
                    if (historyId != -1) {
                        repository.updateNotificationHistoryStatus(historyId, "Action Clicked ($actionString)")
                    }
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Media Control Triggered: $actionString", Toast.LENGTH_SHORT).show()
                    }
                }

                "SCHEDULED_LOCAL_NOTIF" -> {
                    val title = intent.getStringExtra("notif_title") ?: "Scheduled Notification"
                    val body = intent.getStringExtra("notif_body") ?: "Here is your scheduled prompt reminder!"
                    
                    // Show actual notification
                    NotificationHelper.showNotification(
                        context = context,
                        historyId = historyId,
                        type = "basic",
                        title = title,
                        body = body,
                        url = url
                    )
                }
            }
        }
    }
}
