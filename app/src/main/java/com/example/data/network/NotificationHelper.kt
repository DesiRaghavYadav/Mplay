package com.example.data.network

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.example.MainActivity
import com.example.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object NotificationHelper {
    const val CHANNEL_GENERAL = "channel_general"
    const val CHANNEL_PROMOTIONS = "channel_promotions"
    const val CHANNEL_DOWNLOADS = "channel_downloads"
    const val CHANNEL_UPLOADS = "channel_uploads"
    const val CHANNEL_MESSAGES = "channel_messages"
    const val CHANNEL_MEDIA = "channel_media"
    const val CHANNEL_IMPORTANT = "channel_important"

    const val GROUP_KEY_ALERTS = "com.example.alerts.GROUP_KEY"
    const val SUMMARY_ID = 9999

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channels = listOf(
                NotificationChannel(CHANNEL_GENERAL, "General Alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Standard system updates and status messages"
                    enableLights(true)
                },
                NotificationChannel(CHANNEL_PROMOTIONS, "Promotions & Offers", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Deals, premium releases, and trending content"
                    enableLights(true)
                },
                NotificationChannel(CHANNEL_DOWNLOADS, "Downloads", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Offline media download progress"
                    setShowBadge(false)
                },
                NotificationChannel(CHANNEL_UPLOADS, "Uploads", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Creator file uploads and assets progress"
                    setShowBadge(false)
                },
                NotificationChannel(CHANNEL_MESSAGES, "Chat Messages", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Direct chat requests and replies"
                    enableVibration(true)
                },
                NotificationChannel(CHANNEL_MEDIA, "Now Playing", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Active media control notifications"
                },
                NotificationChannel(CHANNEL_IMPORTANT, "Important Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Immediate account alerts, critical updates"
                    enableLights(true)
                    enableVibration(true)
                }
            )

            channels.forEach { channel ->
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    // Helper to download image on background thread safely
    fun getBitmapFromUrl(urlString: String?): Bitmap? {
        if (urlString.isNullOrEmpty()) return null
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.connect()
            val input: InputStream = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Builder helpers for Intent Actions
    private fun getPendingIntentFlags(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    }

    private fun getMutablePendingIntentFlags(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    }

    // Build standard main intent with parameters
    fun createMainPendingIntent(context: Context, action: String?, url: String?, historyId: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_action", action)
            putExtra("notification_url", url)
            putExtra("notification_history_id", historyId)
        }
        return PendingIntent.getActivity(context, historyId, intent, getPendingIntentFlags())
    }

    // Build broadcast action intent
    fun createBroadcastPendingIntent(context: Context, recordId: Int, btnAction: String, targetUrl: String? = null): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = btnAction
            putExtra("notification_history_id", recordId)
            putExtra("notification_url", targetUrl)
        }
        return PendingIntent.getBroadcast(context, recordId + btnAction.hashCode(), intent, getPendingIntentFlags())
    }

    // Complete dispatch matching requested 20 notification types
    fun showNotification(
        context: Context,
        historyId: Int, 
        type: String, 
        title: String, 
        body: String, 
        imageUrl: String? = null, 
        action: String? = null, 
        url: String? = null
    ) {
        val m = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val mainPi = createMainPendingIntent(context, action, url, historyId)
        val deletePi = createBroadcastPendingIntent(context, historyId, "DISMISS_NOTIFICATION")

        val builder = NotificationCompat.Builder(context, CHANNEL_GENERAL)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(mainPi)
            .setDeleteIntent(deletePi)
            .setAutoCancel(true)

        when (type) {
            "basic" -> {
                builder.setChannelId(CHANNEL_GENERAL)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            }

            "big_text" -> {
                builder.setChannelId(CHANNEL_IMPORTANT)
                    .setStyle(NotificationCompat.BigTextStyle().bigText("$body\n\nThis notification demonstrates production-grade BigText styling. By expanding this banner, you can comfortably consume long articles, service releases, and highly detailed notification alerts that require maximum text real estate, bypassing standard single-line layout constraints."))
            }

            "large_image", "image" -> {
                builder.setChannelId(CHANNEL_PROMOTIONS)
                val bitmap = getBitmapFromUrl(imageUrl ?: "https://images.unsplash.com/photo-1536440136628-849c177e76a1?auto=format&fit=crop&w=600&q=80")
                if (bitmap != null) {
                    builder.setLargeIcon(bitmap)
                    builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap).bigLargeIcon(null as Bitmap?))
                }
            }

            "image_text" -> {
                builder.setChannelId(CHANNEL_PROMOTIONS)
                val bitmap = getBitmapFromUrl(imageUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=300&q=80")
                if (bitmap != null) {
                    builder.setLargeIcon(bitmap)
                    builder.setStyle(NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .setSummaryText(body))
                }
            }

            "multiple_images" -> {
                builder.setChannelId(CHANNEL_PROMOTIONS)
                // Fallback style and carousel mock description
                val bitmap = getBitmapFromUrl(imageUrl ?: "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=600&q=80")
                if (bitmap != null) {
                    builder.setLargeIcon(bitmap)
                    builder.setStyle(NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .setSummaryText("$body (1 of 3 Trending Highlights Available)"))
                }
            }

            "inbox" -> {
                builder.setChannelId(CHANNEL_IMPORTANT)
                    .setStyle(NotificationCompat.InboxStyle()
                        .addLine("1. Hot Release: Kavita Bhabhi Episode 2 is live!")
                        .addLine("2. Pehla Nasha likes peaked 100k today!")
                        .addLine("3. New User registered from Support desk")
                        .addLine("4. System sync task successfully executed")
                        .setSummaryText("+4 more status messages"))
            }

            "chat", "messaging" -> {
                builder.setChannelId(CHANNEL_MESSAGES)
                val sender1 = Person.Builder().setName("Pihu Singh").setBot(false).build()
                val sender2 = Person.Builder().setName("Ridhima T.").setBot(false).build()
                
                // Add quick reply interaction
                val replyLabel = "Reply to Chat"
                val remoteInput = RemoteInput.Builder("key_text_reply").setLabel(replyLabel).build()
                val replyPi = createBroadcastPendingIntent(context, historyId, "NOTIF_REPLY")
                val replyAction = NotificationCompat.Action.Builder(
                    android.R.drawable.ic_menu_send,
                    "Reply",
                    replyPi
                ).addRemoteInput(remoteInput).build()

                builder.setStyle(NotificationCompat.MessagingStyle(sender1)
                    .setConversationTitle("Support & Fans Group Chat")
                    .addMessage("Hey, did you catch Walkman Series Ep 5? Outstanding!", System.currentTimeMillis() - 60000, sender1)
                    .addMessage("Yes! Absolutely loved that retro soundtrack!", System.currentTimeMillis() - 30000, sender2)
                    .addMessage(body, System.currentTimeMillis(), sender1))
                    .addAction(replyAction)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
            }

            "progress" -> {
                builder.setChannelId(CHANNEL_DOWNLOADS)
                    .setProgress(100, 65, false)
                    .setContentText("$body - Download in progress (65%)")
                    .setOngoing(false)
            }

            "media" -> {
                builder.setChannelId(CHANNEL_MEDIA)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .addAction(android.R.drawable.ic_media_previous, "Previous", createBroadcastPendingIntent(context, historyId, "MEDIA_PREV"))
                    .addAction(android.R.drawable.ic_media_pause, "Pause", createBroadcastPendingIntent(context, historyId, "MEDIA_PAUSE"))
                    .addAction(android.R.drawable.ic_media_next, "Next", createBroadcastPendingIntent(context, historyId, "MEDIA_NEXT"))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            }

            "persistent", "ongoing" -> {
                builder.setChannelId(CHANNEL_GENERAL)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentText("$body (Persistent Indicator Active)")
            }

            "foreground_service" -> {
                builder.setChannelId(CHANNEL_GENERAL)
                    .setContentText("$body - Premium streaming core is active")
                    .setPriority(NotificationCompat.PRIORITY_LOW)
            }

            "heads_up", "high" -> {
                builder.setChannelId(CHANNEL_IMPORTANT)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setVibrate(longArrayOf(0, 300, 200, 300))
            }

            "silent" -> {
                builder.setChannelId(CHANNEL_GENERAL)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setNotificationSilent()
            }

            "full_screen" -> {
                builder.setChannelId(CHANNEL_IMPORTANT)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setFullScreenIntent(mainPi, true)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
            }

            "deep_link" -> {
                builder.setChannelId(CHANNEL_IMPORTANT)
                    .setContentText("$body (Deep-link to Content)")
                    .addAction(android.R.drawable.ic_menu_view, "Open in App", mainPi)
            }

            "action_button" -> {
                builder.setChannelId(CHANNEL_GENERAL)
                    .addAction(android.R.drawable.ic_menu_share, "Share Now", createBroadcastPendingIntent(context, historyId, "ACTION_SHARE", url))
                    .addAction(android.R.drawable.ic_dialog_info, "Check Info", mainPi)
            }

            "grouped" -> {
                builder.setChannelId(CHANNEL_GENERAL)
                    .setGroup(GROUP_KEY_ALERTS)
                
                // Also issue summary
                val summaryNotification = NotificationCompat.Builder(context, CHANNEL_GENERAL)
                    .setSmallIcon(android.R.drawable.stat_notify_chat)
                    .setContentTitle("Grouped Notifications Summary")
                    .setContentText("You have multiple notifications from ReelsViral")
                    .setGroup(GROUP_KEY_ALERTS)
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .build()
                m.notify(SUMMARY_ID, summaryNotification)
            }

            "summary" -> {
                builder.setChannelId(CHANNEL_GENERAL)
                    .setGroup(GROUP_KEY_ALERTS)
                    .setGroupSummary(true)
            }

            "rich" -> {
                builder.setChannelId(CHANNEL_IMPORTANT)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .addAction(android.R.drawable.ic_menu_share, "Share", createBroadcastPendingIntent(context, historyId, "ACTION_SHARE", url))
                    .addAction(android.R.drawable.ic_menu_save, "Mark Read", createBroadcastPendingIntent(context, historyId, "ACTION_MARK_READ"))
                    .addAction(android.R.drawable.ic_menu_view, "Details", mainPi)
                val bitmap = getBitmapFromUrl(imageUrl ?: "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=600&q=80")
                if (bitmap != null) {
                    builder.setLargeIcon(bitmap)
                    builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))
                }
            }

            else -> {
                builder.setChannelId(CHANNEL_GENERAL)
            }
        }

        m.notify(historyId, builder.build())
    }

    // AlarmManager standard showcase local scheduler
    fun scheduleLocalNotification(context: Context, historyId: Int, delaySeconds: Int, title: String, body: String, url: String?) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "SCHEDULED_LOCAL_NOTIF"
            putExtra("notification_history_id", historyId)
            putExtra("notif_title", title)
            putExtra("notif_body", body)
            putExtra("notification_url", url)
        }
        val pi = PendingIntent.getBroadcast(
            context, 
            historyId, 
            intent, 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
        )
        val triggerTime = SystemClock.elapsedRealtime() + (delaySeconds * 1000)
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pi)
    }
}
