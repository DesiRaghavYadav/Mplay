package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass


@Entity(
    tableName = "channels",
    indices = [Index(value = ["id"]), Index(value = ["name"])]
)
data class Channel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val imageUrl: String
)

@Entity(
    tableName = "media_items",
    indices = [
        Index(value = ["id"]),
        Index(value = ["title"]),
        Index(value = ["category"])
    ]
)
data class MediaItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val imageUrl: String,
    val category: String,
    val isPremium: Boolean,
    val rating: Float = 4.5f,
    val reelsUrl: String = "",
    val likesCount: String = "1k"
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["id"]), Index(value = ["name"], unique = true)]
)
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isPortrait: Boolean = false,
    val type: String = "video" // "video" or "comic"
)

@Entity(
    tableName = "media_category_join",
    primaryKeys = ["mediaId", "categoryId"],
    indices = [
        Index(value = ["mediaId"]),
        Index(value = ["categoryId"])
    ]
)
data class MediaCategoryJoin(
    val mediaId: Int,
    val categoryId: Int
)

@Entity(
    tableName = "user_reels",
    indices = [
        Index(value = ["id"]),
        Index(value = ["title"]),
        Index(value = ["videoUrl"])
    ]
)
data class UserReel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val imageUrl: String,
    val videoUrl: String,
    val likesCount: String = "1.2k",
    val isLiked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val userStatus: String = "normal"
)

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val imageUrl: String,
    val progress: Int = 100, // 0 to 100
    val downloadedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "profile_states")
data class ProfileState(
    @PrimaryKey val id: Int = 1, // Single state entry
    val name: String = "hhg",
    val phone: String = "8888880588",
    val imageUrl: String = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=300&q=80",
    val isPremium: Boolean = true
)

@Entity(tableName = "support_tickets")
data class SupportTicket(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val message: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "wishlist")
data class WishlistItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mediaId: Int,
    val title: String,
    val imageUrl: String
)

@Entity(tableName = "playback_progress")
data class PlaybackProgress(
    @PrimaryKey val mediaId: Int,
    val title: String,
    val imageUrl: String,
    val positionMs: Long,
    val durationMs: Long,
    val lastWatchedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class UserNotification(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val imageUrl: String? = null,
    val type: String = "text", // "text", "text_link", "text_image", "text_image_link"
    val actionUrl: String? = null, // "https://..." or "app://tab/..."
    val timestamp: Long = System.currentTimeMillis()
)

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class BroadcastNotification(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val targetAudience: String = "All Users", // "All Users", "Premium Users", "Specific FCM Token", "Custom Topic"
    val targetToken: String? = null,
    val targetTopic: String? = null,
    val deliveredDevices: Int = 0,
    val status: String = "Sent",
    val timestamp: Long = System.currentTimeMillis()
)

@androidx.room.Entity(tableName = "notification_history")
@com.squareup.moshi.JsonClass(generateAdapter = true)
data class NotificationHistoryItem(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val body: String,
    val type: String,
    val payloadJson: String,
    val status: String, // "Delivered", "Opened", "Dismissed", "Action Clicked"
    val timestamp: Long = System.currentTimeMillis()
)

@androidx.room.Entity(tableName = "notification_analytics")
@com.squareup.moshi.JsonClass(generateAdapter = true)
data class NotificationAnalyticsMetric(
    @androidx.room.PrimaryKey val metricId: String, // "delivered", "opened", "dismissed", "action_clicked", "failed"
    val count: Int = 0
)

@Entity(tableName = "comics")
@com.squareup.moshi.JsonClass(generateAdapter = true)
data class Comic(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String,
    val imageUrl: String,
    val description: String,
    val likesCount: String = "12k",
    val isPremium: Boolean = false
)

@Entity(tableName = "comic_chapters")
@com.squareup.moshi.JsonClass(generateAdapter = true)
data class ComicChapter(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val comicId: Int,
    val chapterNumber: Int,
    val title: String,
    val pagesCsv: String, // Comma-separated list of page image URLs
    val pdfUrl: String = "" // Optional PDF link
)

@Entity(tableName = "stories")
@com.squareup.moshi.JsonClass(generateAdapter = true)
data class Story(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val imageUrl: String,
    val content: String,
    val category: String = "Fantasy",
    val isPremium: Boolean = true,
    val audioUrl: String = "",
    val duration: String = "",
    val releaseDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "story_parts")
@com.squareup.moshi.JsonClass(generateAdapter = true)
data class StoryPart(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val storyId: Int,
    val partNumber: Int,
    val title: String,
    val content: String
)

@Entity(tableName = "comic_history")
@com.squareup.moshi.JsonClass(generateAdapter = true)
data class ComicHistory(
    @PrimaryKey val comicId: Int,
    val title: String,
    val imageUrl: String,
    val chapterNumber: Int,
    val lastReadAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "story_history")
@com.squareup.moshi.JsonClass(generateAdapter = true)
data class StoryHistory(
    @PrimaryKey val storyId: Int,
    val title: String,
    val imageUrl: String,
    val partNumber: Int,
    val lastReadAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "batch_videos",
    indices = [
        Index(value = ["id"]),
        Index(value = ["batchId"], unique = true),
        Index(value = ["title"]),
        Index(value = ["category"])
    ]
)
@com.squareup.moshi.JsonClass(generateAdapter = true)
data class BatchVideo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val batchId: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val category: String = "Web Series",
    val isPremium: Boolean = true,
    val totalVideos: Int = 0,
    val releaseDate: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "batch_video_items",
    indices = [
        Index(value = ["id"]),
        Index(value = ["batchId"]),
        Index(value = ["videoIndex"])
    ]
)
@com.squareup.moshi.JsonClass(generateAdapter = true)
data class BatchVideoItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val batchId: String = "",
    val videoIndex: Int = 1,
    val title: String = "",
    val videoUrl: String = "",
    val imageUrl: String = "",
    val duration: String = "20 min",
    val description: String = "",
    val uploadTime: Long = System.currentTimeMillis(),
    val durationSec: Int = 0,
    val sizeMb: Double = 0.0
)

fun BatchVideoItem.toMediaItem(batch: BatchVideo? = null): MediaItem {
    val bTitle = batch?.title ?: "Batch Series"
    val bCat = batch?.category ?: "Web Series"
    val bImg = if (imageUrl.isNotBlank()) imageUrl else (batch?.imageUrl ?: "")
    val fullTitle = if (title.isNotBlank()) {
        if (title.contains(bTitle, ignoreCase = true)) title else "$bTitle - $title"
    } else {
        "$bTitle - Episode $videoIndex"
    }
    val safeVideoUrl = com.example.util.VideoUrlSanitizer.sanitize(videoUrl)
    return MediaItem(
        id = if (id > 0) 100000 + id else 100000 + (videoUrl.hashCode() and 0x7FFFFFFF % 900000),
        title = fullTitle,
        description = if (description.isNotBlank()) description else (batch?.description ?: "Episode $videoIndex of $bTitle"),
        imageUrl = bImg,
        category = bCat,
        isPremium = true,
        reelsUrl = safeVideoUrl,
        rating = 4.8f,
        likesCount = "${videoIndex * 150 + 600} likes"
    )
}




