package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AppDao
import com.example.data.model.*

@Database(
    entities = [
        Channel::class,
        MediaItem::class,
        Category::class,
        MediaCategoryJoin::class,
        UserReel::class,
        DownloadItem::class,
        ProfileState::class,
        SupportTicket::class,
        WishlistItem::class,
        PlaybackProgress::class,
        NotificationHistoryItem::class,
        NotificationAnalyticsMetric::class,
        Comic::class,
        ComicChapter::class,
        Story::class,
        StoryPart::class,
        ComicHistory::class,
        StoryHistory::class,
        BatchVideo::class,
        BatchVideoItem::class
    ],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val appDao: AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mplay_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
