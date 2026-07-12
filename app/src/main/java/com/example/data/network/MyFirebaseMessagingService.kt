package com.example.data.network

import android.content.Context
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.data.database.AppDatabase
import com.example.data.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Store token in SharedPreferences so dashboard can retrieve it
        val prefs = getSharedPreferences("app_user_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()

        // Sync with repository / firebase cloud database if available
        val db = AppDatabase.getInstance(this)
        val repository = AppRepository(db.appDao, this)
        CoroutineScope(Dispatchers.IO).launch {
            repository.trackNotificationAnalytic("token_refreshed")
        }
    }

    override fun onMessageReceived(remoteMessage: com.google.firebase.messaging.RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Extract values from data payload primarily, fallback to notification payload
        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val title = data["title"] ?: notification?.title ?: "ReelsViral Alert"
        val body = data["body"] ?: notification?.body ?: "Open the app to explore new premium releases!"
        val type = data["type"] ?: "basic"
        val imageUrl = data["image"] ?: data["imageUrl"] ?: notification?.imageUrl?.toString()
        val action = data["action"] ?: "open_app"
        val url = data["url"]

        val db = AppDatabase.getInstance(this)
        val repository = AppRepository(db.appDao, this)

        CoroutineScope(Dispatchers.IO).launch {
            // Write notification JSON payload directly to persistent history database
            val payloadJson = data.toString()
            val historyId = repository.addNotificationHistory(
                title = title,
                body = body,
                type = type,
                payloadJson = payloadJson,
                status = "Delivered"
            )

            // Trigger the native notification view on background threads
            NotificationHelper.showNotification(
                context = this@MyFirebaseMessagingService,
                historyId = historyId.toInt(),
                type = type,
                title = title,
                body = body,
                imageUrl = imageUrl,
                action = action,
                url = url
            )
        }
    }
}
