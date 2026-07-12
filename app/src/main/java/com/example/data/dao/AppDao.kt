package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Counts for prepopulation check
    @Query("SELECT COUNT(*) FROM channels")
    suspend fun getChannelsCount(): Int

    @Query("SELECT COUNT(*) FROM media_items")
    suspend fun getMediaItemsCount(): Int

    @Query("SELECT COUNT(*) FROM profile_states")
    suspend fun getProfileCount(): Int

    // Channels
    @Query("SELECT * FROM channels ORDER BY id DESC")
    fun getAllChannels(): Flow<List<Channel>>

    @Query("SELECT * FROM channels ORDER BY id DESC")
    suspend fun getAllChannelsDirect(): List<Channel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<Channel>)

    @Query("DELETE FROM channels")
    suspend fun deleteAllChannels()

    // Media Items
    @Query("SELECT * FROM media_items ORDER BY id DESC")
    fun getAllMediaItems(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items ORDER BY id DESC")
    suspend fun getAllMediaItemsDirect(): List<MediaItem>

    @Query("SELECT * FROM media_items WHERE category = :category ORDER BY id DESC")
    fun getMediaItemsByCategory(category: String): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE category = :category ORDER BY id DESC")
    suspend fun getMediaItemsByCategoryDirect(category: String): List<MediaItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItems(items: List<MediaItem>)

    @Query("DELETE FROM media_items")
    suspend fun deleteAllMediaItems()

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteMediaItemById(id: Int)

    @Query("SELECT * FROM media_items WHERE id = :id LIMIT 1")
    suspend fun getMediaItemById(id: Int): MediaItem?

    // Downloads
    @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC")
    fun getAllDownloads(): Flow<List<DownloadItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(item: DownloadItem)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownloadById(id: Int)

    // Profile State
    @Query("SELECT * FROM profile_states WHERE id = 1 LIMIT 1")
    fun getProfileState(): Flow<ProfileState?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfileState(state: ProfileState)

    @Update
    suspend fun updateProfileState(state: ProfileState)

    // Support Tickets
    @Query("SELECT * FROM support_tickets ORDER BY createdAt DESC")
    fun getAllTickets(): Flow<List<SupportTicket>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: SupportTicket)

    // Wishlist
    @Query("SELECT * FROM wishlist ORDER BY id DESC")
    fun getWishlist(): Flow<List<WishlistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWishlist(item: WishlistItem)

    @Query("DELETE FROM wishlist WHERE mediaId = :mediaId")
    suspend fun removeFromWishlist(mediaId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM wishlist WHERE mediaId = :mediaId)")
    fun isInWishlist(mediaId: Int): Flow<Boolean>

    // Playback Progress for Continue Watching
    @Query("SELECT * FROM playback_progress ORDER BY lastWatchedAt DESC")
    fun getAllPlaybackProgress(): Flow<List<PlaybackProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaybackProgress(progress: PlaybackProgress)

    @Query("SELECT * FROM playback_progress WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getPlaybackProgressForMedia(mediaId: Int): PlaybackProgress?

    @Query("DELETE FROM playback_progress WHERE mediaId = :mediaId")
    suspend fun deletePlaybackProgress(mediaId: Int)

    @Query("DELETE FROM playback_progress")
    suspend fun clearAllPlaybackProgress()

    // Normalized Categories
    @Query("SELECT * FROM categories ORDER BY id DESC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoriesCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<Category>)

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Int)

    // Normalized Media-Category Relationships
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaCategoryJoins(joins: List<MediaCategoryJoin>)

    @Query("DELETE FROM media_category_join")
    suspend fun deleteAllMediaCategoryJoins()

    // Normalized Advanced User Reels
    @Query("SELECT * FROM user_reels ORDER BY createdAt DESC")
    fun getAllUserReels(): Flow<List<UserReel>>

    @Query("SELECT * FROM user_reels ORDER BY id DESC")
    suspend fun getAllUserReelsDirect(): List<UserReel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserReels(reels: List<UserReel>)

    @Query("DELETE FROM user_reels")
    suspend fun deleteAllUserReels()

    // Notification History
    @Query("SELECT * FROM notification_history ORDER BY timestamp DESC")
    fun getAllNotificationHistory(): Flow<List<NotificationHistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationHistory(item: NotificationHistoryItem): Long

    @Query("UPDATE notification_history SET status = :status WHERE id = :id")
    suspend fun updateNotificationHistoryStatus(id: Int, status: String)

    @Query("DELETE FROM notification_history")
    suspend fun clearNotificationHistory()

    // Notification Analytics
    @Query("SELECT * FROM notification_analytics")
    fun getAllNotificationAnalytics(): Flow<List<NotificationAnalyticsMetric>>

    @Query("SELECT * FROM notification_analytics WHERE metricId = :metricId LIMIT 1")
    suspend fun getNotificationAnalytic(metricId: String): NotificationAnalyticsMetric?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationAnalytic(metric: NotificationAnalyticsMetric)

    @Query("DELETE FROM notification_analytics")
    suspend fun clearNotificationAnalytics()

    // Comics
    @Query("SELECT * FROM comics ORDER BY id DESC")
    fun getAllComics(): Flow<List<Comic>>

    @Query("SELECT COUNT(*) FROM comics")
    suspend fun getComicsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComics(comics: List<Comic>)

    @Query("DELETE FROM comics WHERE id = :id")
    suspend fun deleteComicById(id: Int)

    @Query("DELETE FROM comics")
    suspend fun deleteAllComics()

    // Comic Chapters
    @Query("SELECT * FROM comic_chapters WHERE comicId = :comicId ORDER BY chapterNumber ASC")
    fun getChaptersForComic(comicId: Int): Flow<List<ComicChapter>>

    @Query("SELECT COUNT(*) FROM comic_chapters")
    suspend fun getComicChaptersCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComicChapters(chapters: List<ComicChapter>)

    @Query("SELECT * FROM comic_chapters WHERE id = :id LIMIT 1")
    suspend fun getComicChapterByIdDirect(id: Int): ComicChapter?

    @Query("DELETE FROM comic_chapters WHERE id = :id")
    suspend fun deleteComicChapterById(id: Int)

    @Query("DELETE FROM comic_chapters")
    suspend fun deleteAllComicChapters()

    // Stories
    @Query("SELECT COUNT(*) FROM stories")
    suspend fun getStoriesCount(): Int

    @Query("SELECT * FROM stories ORDER BY id DESC LIMIT :limit OFFSET :offset")
    suspend fun getStoriesPaged(limit: Int, offset: Int): List<Story>

    @Query("SELECT * FROM stories WHERE audioUrl != ''")
    fun getAllAudioStories(): kotlinx.coroutines.flow.Flow<List<Story>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStories(stories: List<Story>)

    @Query("SELECT * FROM stories WHERE id = :id")
    suspend fun getStoryByIdDirect(id: Int): Story?

    @Query("DELETE FROM stories WHERE id = :id")
    suspend fun deleteStoryById(id: Int)

    @Query("DELETE FROM stories")
    suspend fun deleteAllStories()

    // Story Parts
    @Query("SELECT * FROM story_parts WHERE storyId = :storyId ORDER BY partNumber ASC")
    fun getPartsForStory(storyId: Int): Flow<List<StoryPart>>

    @Query("SELECT * FROM story_parts WHERE storyId = :storyId ORDER BY partNumber ASC")
    suspend fun getPartsForStoryDirect(storyId: Int): List<StoryPart>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoryParts(parts: List<StoryPart>)

    @Query("DELETE FROM story_parts")
    suspend fun deleteAllStoryParts()

    // Recommendations & Similarity Support
    @Query("SELECT * FROM stories WHERE id != :excludeStoryId LIMIT :limit")
    suspend fun getOtherStories(excludeStoryId: Int, limit: Int = 6): List<Story>

    @Query("SELECT * FROM comics WHERE id != :excludeComicId")
    suspend fun getAllOtherComicsDirect(excludeComicId: Int): List<Comic>

    // Comic History
    @Query("SELECT * FROM comic_history ORDER BY lastReadAt DESC")
    fun getAllComicHistory(): Flow<List<ComicHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComicHistory(history: ComicHistory)

    @Query("DELETE FROM comic_history WHERE comicId = :comicId")
    suspend fun deleteComicHistory(comicId: Int)

    @Query("DELETE FROM comic_history")
    suspend fun clearComicHistory()

    // Story History
    @Query("SELECT * FROM story_history ORDER BY lastReadAt DESC")
    fun getAllStoryHistory(): Flow<List<StoryHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoryHistory(history: StoryHistory)

    @Query("DELETE FROM story_history WHERE storyId = :storyId")
    suspend fun deleteStoryHistory(storyId: Int)

    @Query("DELETE FROM story_history")
    suspend fun clearStoryHistory()
}
