package com.example.data.repository

import com.example.data.dao.AppDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class AppRepository(
    private val appDao: AppDao,
    val context: android.content.Context? = null
) {
    val firebaseService = context?.let { com.example.data.network.FirebaseService(it) }

    suspend fun syncDataWithFirebase(): Boolean = withContext(Dispatchers.IO) {
        val service = firebaseService ?: return@withContext false
        val firebaseChannels = service.fetchChannels()
        val firebaseMedia = service.fetchMediaItems()
        val firebaseReels = service.fetchReels()

        var hasData = false

        if (firebaseChannels != null && firebaseChannels.isNotEmpty()) {
            appDao.deleteAllChannels()
            appDao.insertChannels(firebaseChannels)
            hasData = true
        }

        if (firebaseMedia != null && firebaseMedia.isNotEmpty()) {
            appDao.deleteAllMediaItems()
            appDao.insertMediaItems(firebaseMedia)
            hasData = true
        }

        if (firebaseReels != null && firebaseReels.isNotEmpty()) {
            appDao.deleteAllUserReels()
            appDao.insertUserReels(firebaseReels)
            hasData = true
        }

        return@withContext hasData
    }

    suspend fun fetchAppVersionDetails(): com.example.data.network.AppVersionDetails? = withContext(Dispatchers.IO) {
        val service = firebaseService ?: return@withContext null
        service.fetchAppVersionDetails()
    }

    suspend fun registerOrUpdateUserData(userId: String, isPremium: Boolean): Boolean = withContext(Dispatchers.IO) {
        val service = firebaseService ?: return@withContext false
        service.registerOrUpdateUserData(userId, isPremium)
    }

    suspend fun fetchUserData(userId: String): String? = withContext(Dispatchers.IO) {
        val service = firebaseService ?: return@withContext null
        service.fetchUserData(userId)
    }

    suspend fun fetchNotifications(): List<UserNotification>? = withContext(Dispatchers.IO) {
        val service = firebaseService ?: return@withContext null
        service.fetchNotifications()
    }

    suspend fun uploadNotification(n: UserNotification): Boolean = withContext(Dispatchers.IO) {
        val service = firebaseService ?: return@withContext false
        service.uploadNotification(n)
    }

    suspend fun fetchUserDataList(): List<Map<String, String>>? = withContext(Dispatchers.IO) {
        val service = firebaseService ?: return@withContext null
        service.fetchUserDataList()
    }

    suspend fun fetchBroadcastHistory(): List<com.example.data.model.BroadcastNotification>? = withContext(Dispatchers.IO) {
        val service = firebaseService ?: return@withContext null
        service.fetchBroadcastHistory()
    }

    suspend fun uploadBroadcastNotification(b: com.example.data.model.BroadcastNotification): Boolean = withContext(Dispatchers.IO) {
        val service = firebaseService ?: return@withContext false
        service.uploadBroadcastNotification(b)
    }

    suspend fun uploadLocalDataToFirebase(forceReset: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val service = firebaseService ?: return@withContext false
        val currentChannelsCount = appDao.getChannelsCount()
        val currentMediaCount = appDao.getMediaItemsCount()
        if (forceReset || currentChannelsCount < 5 || currentMediaCount < 10) {
            appDao.deleteAllChannels()
            appDao.deleteAllMediaItems()
            prepopulateOfflineDatabase()
        }
        val channelsToUpload = appDao.getAllChannelsDirect()
        val mediaToUpload = appDao.getAllMediaItemsDirect()

        val successChannels = service.uploadChannels(channelsToUpload)
        val successMedia = service.uploadMediaItems(mediaToUpload)

        return@withContext successChannels && successMedia
    }

    suspend fun uploadCategoriesToFirebase(categories: List<Category>): Boolean = withContext(Dispatchers.IO) {
        val service = firebaseService ?: return@withContext false
        val categoriesWithVideos = categories.map { category ->
            val videos = appDao.getMediaItemsByCategoryDirect(category.name)
            Pair(category, videos)
        }
        service.uploadCategoriesWithVideos(categoriesWithVideos)
    }

    suspend fun syncCategoriesWithFirebase(): Boolean = withContext(Dispatchers.IO) {
        val service = firebaseService ?: return@withContext false
        val comicCatsWithVideos = service.fetchCategories()
        val videoCatsWithVideos = service.fetchVideoCategories()

        val allCategories = mutableListOf<Category>()
        val allMediaItems = mutableListOf<MediaItem>()

        comicCatsWithVideos?.forEach { (category, videos) ->
            allCategories.add(category)
            allMediaItems.addAll(videos)
        }

        videoCatsWithVideos?.forEach { (category, videos) ->
            allCategories.add(category)
            allMediaItems.addAll(videos)
        }

        if (allCategories.isNotEmpty()) {
            appDao.deleteAllCategories()
            appDao.insertCategories(allCategories)

            if (allMediaItems.isNotEmpty()) {
                appDao.deleteAllMediaItems()
                appDao.insertMediaItems(allMediaItems)
            }
            true
        } else {
            false
        }
    }

    suspend fun createReelsDatabase(): Boolean = withContext(Dispatchers.IO) {
        val service = firebaseService ?: return@withContext false
        
        // 1. Seed Categories for Normalized Schema
        val categoriesToSeed = listOf(
            Category(id = 1, name = "Trending Movies", isPortrait = false),
            Category(id = 2, name = "MPlay Hot Shorts", isPortrait = true),
            Category(id = 3, name = "Reels", isPortrait = true)
        )
        
        // Generating Batched Reels data to reduce document reads and optimize storage
        // We will generate 5 batches, each having 100 high-quality reels data (Total 500 reels)
        val sampleBases = listOf(
            Triple("Crimson Nights", "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?auto=format&fit=crop&w=600&q=80", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"),
            Triple("Late Night Passion", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=600&q=80", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4"),
            Triple("First Glance Romance", "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=600&q=80", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"),
            Triple("Behind-The-Scenes Glamour", "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=600&q=80", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"),
            Triple("Mystic Eyes & Whispers", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=300&q=80", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4")
        )

        val fullReelsList = mutableListOf<UserReel>()
        var globalReelId = 1
        for (batchNum in 1..5) {
            for (i in 1..100) {
                val base = sampleBases[(i + batchNum) % sampleBases.size]
                val likesVal = (50 + (globalReelId * 7) % 450)
                val status = if (globalReelId % 3 == 0) "premium" else "normal"
                fullReelsList.add(
                    UserReel(
                        id = globalReelId,
                        title = "${base.first} #${globalReelId} (Batch $batchNum) [${status.uppercase()}]",
                        description = "Viral video segment demonstrating high-quality stories from series #${globalReelId}. Enjoy immersive streaming and instant community responses.",
                        imageUrl = base.second,
                        videoUrl = base.third,
                        likesCount = "${likesVal}k",
                        isLiked = false,
                        userStatus = status
                    )
                )
                globalReelId++
            }
        }

        // Pick randomly 3 batches out of 5, merge them, randomly shuffle their indices and save locally
        val selectedBatchNums = listOf(1, 2, 3, 4, 5).shuffled().take(3)
        val userReelsToSeed = fullReelsList.filter { reel ->
            val match = """Batch (\d+)""".toRegex().find(reel.title)
            val bNum = match?.groupValues?.get(1)?.toIntOrNull() ?: 1
            bNum in selectedBatchNums
        }.shuffled()

        // Construct matching media items for local navigation consistency
        val reelsToSeed = userReelsToSeed.take(15).map { reel ->
            MediaItem(
                id = 100 + reel.id,
                title = reel.title,
                description = reel.description,
                imageUrl = reel.imageUrl,
                category = "Reels",
                isPremium = true,
                rating = 4.8f,
                reelsUrl = reel.videoUrl,
                likesCount = reel.likesCount
            )
        }

        val joinsToSeed = reelsToSeed.map { media ->
            MediaCategoryJoin(mediaId = media.id, categoryId = 3)
        }

        return@withContext try {
            // Write local Room database transactionally
            appDao.insertCategories(categoriesToSeed)
            appDao.insertMediaItems(reelsToSeed)
            
            // Delete old user reels and insert new shuffled/merged 3 batches locally (300 items)
            appDao.deleteAllUserReels()
            appDao.insertUserReels(userReelsToSeed)
            appDao.insertMediaCategoryJoins(joinsToSeed)

            // Sync/Upload subset of curated reels to Firestore to keep Firestore footprints ultra-low
            val successMedia = service.uploadMediaItems(reelsToSeed)
            val successReels = service.uploadReels(userReelsToSeed.take(20)) // seed first 20 onto firebase to keep doc read fees extremely small!
            
            successMedia && successReels
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun refreshAndShuffleReelsLocal() = withContext(Dispatchers.IO) {
        var currentReels = appDao.getAllUserReelsDirect()
        if (currentReels.isEmpty()) {
            createReelsDatabase()
            currentReels = appDao.getAllUserReelsDirect()
        }
        if (currentReels.isNotEmpty()) {
            val shuffled = currentReels.shuffled()
            appDao.deleteAllUserReels()
            appDao.insertUserReels(shuffled)
        }
    }

    suspend fun syncPendingLocalLikesToCloud(): Boolean = withContext(Dispatchers.IO) {
        try {
            val localReels = appDao.getAllUserReelsDirect()
            val likedReels = localReels.filter { it.isLiked }
            if (likedReels.isNotEmpty()) {
                // Bulk sync of locally liked items to Cloud database
                firebaseService?.uploadReels(likedReels)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Exposition of Data Streams
    val allChannels: Flow<List<Channel>> = appDao.getAllChannels()
    val allMediaItems: Flow<List<MediaItem>> = appDao.getAllMediaItems()
    val allCategories: Flow<List<Category>> = appDao.getAllCategories()
    val allUserReels: Flow<List<UserReel>> = appDao.getAllUserReels()
    val allDownloads: Flow<List<DownloadItem>> = appDao.getAllDownloads()
    val profileState: Flow<ProfileState?> = appDao.getProfileState()
    val allTickets: Flow<List<SupportTicket>> = appDao.getAllTickets()
    val wishlist: Flow<List<WishlistItem>> = appDao.getWishlist()
    val allPlaybackProgress: Flow<List<PlaybackProgress>> = appDao.getAllPlaybackProgress()
    val allComics: Flow<List<Comic>> = appDao.getAllComics()
    val allComicHistory: Flow<List<ComicHistory>> = appDao.getAllComicHistory()
    val allStoryHistory: Flow<List<StoryHistory>> = appDao.getAllStoryHistory()
    
    // Comic & Story History Operations
    suspend fun saveComicHistory(history: ComicHistory) = withContext(Dispatchers.IO) {
        appDao.insertComicHistory(history)
    }

    suspend fun deleteComicHistory(comicId: Int) = withContext(Dispatchers.IO) {
        appDao.deleteComicHistory(comicId)
    }

    suspend fun clearComicHistory() = withContext(Dispatchers.IO) {
        appDao.clearComicHistory()
    }

    suspend fun saveStoryHistory(history: StoryHistory) = withContext(Dispatchers.IO) {
        appDao.insertStoryHistory(history)
    }

    suspend fun deleteStoryHistory(storyId: Int) = withContext(Dispatchers.IO) {
        appDao.deleteStoryHistory(storyId)
    }

    suspend fun clearStoryHistory() = withContext(Dispatchers.IO) {
        appDao.clearStoryHistory()
    }

    fun getChaptersForComic(comicId: Int): Flow<List<ComicChapter>> = appDao.getChaptersForComic(comicId)

    // Playback Progress
    suspend fun savePlaybackProgress(progress: PlaybackProgress) {
        appDao.insertPlaybackProgress(progress)
    }

    suspend fun getPlaybackProgressForMedia(mediaId: Int): PlaybackProgress? {
        return appDao.getPlaybackProgressForMedia(mediaId)
    }

    suspend fun deletePlaybackProgress(mediaId: Int) {
        appDao.deletePlaybackProgress(mediaId)
    }

    suspend fun clearAllPlaybackProgress() {
        appDao.clearAllPlaybackProgress()
    }

    fun getByCategory(category: String): Flow<List<MediaItem>> =
        appDao.getMediaItemsByCategory(category)

    fun isInWishlist(mediaId: Int): Flow<Boolean> =
        appDao.isInWishlist(mediaId)

    // Direct Suspend Operations
    suspend fun insertChannels(channels: List<Channel>) = appDao.insertChannels(channels)
    suspend fun insertMediaItems(items: List<MediaItem>) = appDao.insertMediaItems(items)
    suspend fun deleteMediaItemById(id: Int) = appDao.deleteMediaItemById(id)
    suspend fun insertCategories(categories: List<Category>) = appDao.insertCategories(categories)
    suspend fun deleteCategoryById(id: Int) = appDao.deleteCategoryById(id)
    suspend fun insertMediaCategoryJoins(joins: List<MediaCategoryJoin>) = appDao.insertMediaCategoryJoins(joins)
    suspend fun insertUserReels(reels: List<UserReel>) = appDao.insertUserReels(reels)

    // Comics operations
    suspend fun insertComics(comics: List<Comic>) = appDao.insertComics(comics)
    suspend fun deleteComicById(id: Int) = appDao.deleteComicById(id)
    suspend fun insertComicChapters(chapters: List<ComicChapter>) = appDao.insertComicChapters(chapters)
    suspend fun getComicChapterByIdDirect(id: Int) = appDao.getComicChapterByIdDirect(id)
    suspend fun deleteComicChapterById(id: Int) = appDao.deleteComicChapterById(id)

    // Stories operations
    suspend fun getStoriesCount(): Int = appDao.getStoriesCount()
    suspend fun getStoriesPaged(limit: Int, offset: Int): List<Story> = appDao.getStoriesPaged(limit, offset)
    fun getAllAudioStories(): kotlinx.coroutines.flow.Flow<List<Story>> = appDao.getAllAudioStories()
    suspend fun getStoryByIdDirect(id: Int): Story? = appDao.getStoryByIdDirect(id)
    suspend fun insertStories(stories: List<Story>) = appDao.insertStories(stories)
    suspend fun deleteStoryById(id: Int) = appDao.deleteStoryById(id)
    suspend fun deleteAllStories() = appDao.deleteAllStories()

    // Story Parts operations
    fun getPartsForStory(storyId: Int): kotlinx.coroutines.flow.Flow<List<StoryPart>> = appDao.getPartsForStory(storyId)
    suspend fun getPartsForStoryDirect(storyId: Int): List<StoryPart> = appDao.getPartsForStoryDirect(storyId)
    suspend fun insertStoryParts(parts: List<StoryPart>) = appDao.insertStoryParts(parts)
    suspend fun deleteAllStoryParts() = appDao.deleteAllStoryParts()

    // Similarity & Recommendation operations
    suspend fun getOtherStories(excludeStoryId: Int, limit: Int = 6): List<Story> = appDao.getOtherStories(excludeStoryId, limit)
    suspend fun getAllOtherComicsDirect(excludeComicId: Int): List<Comic> = appDao.getAllOtherComicsDirect(excludeComicId)

    suspend fun addDownload(item: DownloadItem) = appDao.insertDownload(item)
    suspend fun deleteDownload(id: Int) = appDao.deleteDownloadById(id)

    suspend fun saveProfile(state: ProfileState) = appDao.insertProfileState(state)
    suspend fun updateProfile(state: ProfileState) = appDao.updateProfileState(state)

    fun getOrCreateUserUniqueId(): String {
        val ctx = context ?: return "unknown_device"
        val prefs = ctx.getSharedPreferences("app_user_prefs", android.content.Context.MODE_PRIVATE)
        var userId = prefs.getString("userid", null)
        if (userId.isNullOrEmpty()) {
            userId = android.provider.Settings.Secure.getString(ctx.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
            if (userId.isNullOrEmpty() || userId == "android_id") {
                userId = java.util.UUID.randomUUID().toString()
            }
            prefs.edit().putString("userid", userId).apply()
        }
        return userId
    }

    fun getOrCreateInstalledDate(): Long {
        val ctx = context ?: return System.currentTimeMillis()
        val prefs = ctx.getSharedPreferences("app_user_prefs", android.content.Context.MODE_PRIVATE)
        var installedDate = prefs.getLong("installed_date", 0L)
        if (installedDate == 0L) {
            installedDate = System.currentTimeMillis()
            prefs.edit().putLong("installed_date", installedDate).apply()
        }
        return installedDate
    }

    fun setMockInstalledDaysAgo(daysAgo: Int) {
        val ctx = context ?: return
        val prefs = ctx.getSharedPreferences("app_user_prefs", android.content.Context.MODE_PRIVATE)
        val mockDate = System.currentTimeMillis() - (daysAgo.toLong() * 24 * 60 * 60 * 1000)
        prefs.edit().putLong("installed_date", mockDate).apply()
    }

    suspend fun syncUserUniqueIdAndPremiumStatus() {
        val ctx = context ?: return
        val prefs = ctx.getSharedPreferences("app_user_prefs", android.content.Context.MODE_PRIVATE)
        val isSynced = prefs.getBoolean("status_synced_after_install", false)
        val userId = getOrCreateUserUniqueId()
        val currentProfile = profileState.first() ?: ProfileState()
        
        if (isSynced) {
            // Already synced, retrieve from local storage and skip firestore search
            return
        }

        // 1. Fetch from Firestore userdata collection on first app start after install
        val remoteStatus = fetchUserData(userId)
        if (remoteStatus != null) {
            // User exists in remote Firestore
            if (remoteStatus == "premium") {
                // If remote is premium, restore local to premium (and save to local profile)
                if (!currentProfile.isPremium) {
                    val updated = currentProfile.copy(isPremium = true)
                    saveProfile(updated)
                }
            } else {
                // If remote is normal but local is premium, register/update Firestore to premium
                if (currentProfile.isPremium) {
                    registerOrUpdateUserData(userId, true)
                } else {
                    registerOrUpdateUserData(userId, false)
                }
            }
        } else {
            // Under first install, register the user with their current local premium status
            registerOrUpdateUserData(userId, currentProfile.isPremium)
        }
        // Save flag to avoid checking Firestore on subsequent starts
        prefs.edit().putBoolean("status_synced_after_install", true).apply()
    }

    suspend fun submitTicket(message: String) {
        appDao.insertTicket(SupportTicket(message = message))
    }

    suspend fun toggleWishlist(mediaId: Int, title: String, imageUrl: String) {
        val list = wishlist.first()
        val exists = list.any { it.mediaId == mediaId }
        if (exists) {
            appDao.removeFromWishlist(mediaId)
        } else {
            appDao.addToWishlist(WishlistItem(mediaId = mediaId, title = title, imageUrl = imageUrl))
        }
    }

    suspend fun getMediaItemById(id: Int): MediaItem? = appDao.getMediaItemById(id)

    // Seed Initial Database if empty
    suspend fun prepopulateDatabase() = withContext(Dispatchers.IO) {
        try {
            syncDataWithFirebase()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Populate local DB with offline items (channels, media, categories, comics, chapters, stories)
        prepopulateOfflineDatabase()
        
        // Auto-seed/upload the offline data directly to active Cloud Firestore so it's populated
        try {
            uploadLocalDataToFirebase()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun prepopulateOfflineDatabase() = withContext(Dispatchers.IO) {
        val currentChannelsCount = appDao.getChannelsCount()
        if (currentChannelsCount == 0 || currentChannelsCount < 5) {
            appDao.deleteAllChannels()
            val defaultChannels = listOf(
                Channel(
                    id = 1,
                    name = "Ridhima T.",
                    imageUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=300&q=80"
                ),
                Channel(
                    id = 2,
                    name = "Pihu Singh",
                    imageUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=300&q=80"
                ),
                Channel(
                    id = 3,
                    name = "Kataleyagil",
                    imageUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=300&q=80"
                ),
                Channel(
                    id = 4,
                    name = "Priyanka R.",
                    imageUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=300&q=80"
                ),
                Channel(
                    id = 5,
                    name = "Malvika T.",
                    imageUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=300&q=80"
                ),
                Channel(
                    id = 6,
                    name = "Alia Sen",
                    imageUrl = "https://images.unsplash.com/photo-1488426862026-3ee34a7d66df?auto=format&fit=crop&w=300&q=80"
                )
            )
            appDao.insertChannels(defaultChannels)
        }

        val currentMediaCount = appDao.getMediaItemsCount()
        if (currentMediaCount == 0 || currentMediaCount < 10) {
            appDao.deleteAllMediaItems()
            val defaultMedia = listOf(
                // BANNERS (Featured and multi-row indexed)
                MediaItem(
                    id = 1,
                    title = "Walkman Series",
                    description = "A gripping retro romance and mystery saga starring Ridhima T. and Pihu Singh in an elegant vintage setting of secret desires.",
                    imageUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=1200&q=80",
                    category = "Banner, Freshly Added Now!, Ullu Originals – Most Loved!",
                    isPremium = false,
                    rating = 4.8f,
                    reelsUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    likesCount = "25k"
                ),
                MediaItem(
                    id = 2,
                    title = "Kavita Bhabhi",
                    description = "Seductive dramatic tales featuring Priyanka R. and Malvika T. exploring deep secrets of modern domestic intimacy.",
                    imageUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=1200&q=80",
                    category = "Banner, Best Of Voovi, Watch Hunter – Now Streaming!",
                    isPremium = true,
                    rating = 4.9f,
                    reelsUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    likesCount = "45k"
                ),
                MediaItem(
                    id = 3,
                    title = "Charmsukh",
                    description = "An premium anthology starring Alia Sen and Kataleyagil that explores complex human relationships and deep hidden desires.",
                    imageUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=1200&q=80",
                    category = "Banner, PrimePlay Specials – Hot & Wild!, Rabbit Originals",
                    isPremium = true,
                    rating = 4.7f,
                    reelsUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    likesCount = "38k"
                ),

                // FRESHLY ADDED NOW + MULTI-CATEGORY
                MediaItem(
                    id = 4,
                    title = "PURANI HAVELI",
                    description = "A haunting saga of an ancient mansion with supernatural secrets and wild desires starring Kataleyagil.",
                    imageUrl = "https://images.unsplash.com/photo-1507679799987-c73779587ccf?auto=format&fit=crop&w=600&q=80",
                    category = "Freshly Added Now!, PrimePlay Specials – Hot & Wild!",
                    isPremium = true,
                    rating = 4.6f,
                    reelsUrl = "https://playertest.longtailvideo.com/adaptive/bipbop/bipbop.m3u8",
                    likesCount = "18k"
                ),
                MediaItem(
                    id = 5,
                    title = "Online Romance",
                    description = "Two souls connect in a digital web of high-speed passion starring Priyanka R.",
                    imageUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?auto=format&fit=crop&w=600&q=80",
                    category = "Freshly Added Now!, Best Of Voovi",
                    isPremium = true,
                    rating = 4.2f,
                    reelsUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                    likesCount = "9.4k"
                ),
                MediaItem(
                    id = 6,
                    title = "Midnight Shadows",
                    description = "Late night investigations reveal a dangerous underground web of glamorous secrets starring Malvika T. and Alia Sen.",
                    imageUrl = "https://images.unsplash.com/photo-1504701954957-2390f80619b4?auto=format&fit=crop&w=600&q=80",
                    category = "Freshly Added Now!, Watch Hunter – Now Streaming!",
                    isPremium = false,
                    rating = 4.1f,
                    reelsUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                    likesCount = "12k"
                ),

                // ULLU ORIGINALS
                MediaItem(
                    id = 7,
                    title = "DO ANKHEIN",
                    description = "Two locked gazes reveal deep affection, betrayal, and passion that challenges social chains starring Ridhima T.",
                    imageUrl = "https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&fit=crop&w=600&q=80",
                    category = "Ullu Originals – Most Loved!, Freshly Added Now!",
                    isPremium = true,
                    rating = 4.7f,
                    reelsUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
                    likesCount = "45k"
                ),
                MediaItem(
                    id = 8,
                    title = "PEHLA NASHA",
                    description = "A musical journey of discovering first infatuation amidst college melodies starring Pihu Singh.",
                    imageUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=600&q=80",
                    category = "Ullu Originals – Most Loved!, Watch Hunter – Now Streaming!",
                    isPremium = true,
                    rating = 4.5f,
                    reelsUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
                    likesCount = "33k"
                ),
                MediaItem(
                    id = 9,
                    title = "RANGEELA PART 2",
                    description = "The saga of colorful expressions and dynamic characters continues with enhanced drama starring Kataleyagil.",
                    imageUrl = "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?auto=format&fit=crop&w=600&q=80",
                    category = "Ullu Originals – Most Loved!, Best Of Voovi",
                    isPremium = true,
                    rating = 4.9f,
                    reelsUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4",
                    likesCount = "88k"
                ),
                MediaItem(
                    id = 10,
                    title = "CHAAR HAATH",
                    description = "Four hands coming together to guard a familial legacy through tough times starring Priyanka R.",
                    imageUrl = "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=600&q=80",
                    category = "Ullu Originals – Most Loved!, Rabbit Originals",
                    isPremium = false,
                    rating = 4.3f,
                    reelsUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                    likesCount = "21k"
                ),

                // PRIMEPLAY SPECIALS
                MediaItem(
                    id = 11,
                    title = "PAGLET 3",
                    description = "The madness returns with heightened laughter, complex love triangles, and wild encounters starring Malvika T.",
                    imageUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=600&q=80",
                    category = "PrimePlay Specials – Hot & Wild!, Freshly Added Now!",
                    isPremium = true,
                    rating = 4.6f,
                    reelsUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                    likesCount = "39k"
                ),
                MediaItem(
                    id = 12,
                    title = "PAGLET 2",
                    description = "Stream the hit segment where unpredictable characters and modern relationships clash starring Alia Sen.",
                    imageUrl = "https://images.unsplash.com/photo-1478720568477-1510c57f7a0c?auto=format&fit=crop&w=600&q=80",
                    category = "PrimePlay Specials – Hot & Wild!, Rabbit Originals",
                    isPremium = true,
                    rating = 4.4f,
                    reelsUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    likesCount = "29k"
                ),
                MediaItem(
                    id = 13,
                    title = "DESI TADKA",
                    description = "A culinary love drama featuring modern twists on traditional family dynamics starring Ridhima T.",
                    imageUrl = "https://images.unsplash.com/photo-1506157786151-b8491531f063?auto=format&fit=crop&w=600&q=80",
                    category = "PrimePlay Specials – Hot & Wild!, Best Of Voovi",
                    isPremium = false,
                    rating = 4.0f,
                    reelsUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    likesCount = "15k"
                ),

                // WATCH HUNTER
                MediaItem(
                    id = 14,
                    title = "Adhuri Aas",
                    description = "Incomplete desires are rekindled during a foggy winter evening and hidden encounters starring Pihu Singh.",
                    imageUrl = "https://images.unsplash.com/photo-1518173946687-a4c8a383392e?auto=format&fit=crop&w=600&q=80",
                    category = "Watch Hunter – Now Streaming!, Freshly Added Now!",
                    isPremium = true,
                    rating = 4.7f,
                    reelsUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    likesCount = "52k"
                ),
                MediaItem(
                    id = 15,
                    title = "Beema Babu",
                    description = "An insurance agent finds himself caught in an unexpected web of high-stakes visual secrets starring Kataleyagil.",
                    imageUrl = "https://images.unsplash.com/photo-1485686531765-ba63b07845a7?auto=format&fit=crop&w=600&q=80",
                    category = "Watch Hunter – Now Streaming!, PrimePlay Specials – Hot & Wild!",
                    isPremium = true,
                    rating = 4.5f,
                    reelsUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                    likesCount = "41k"
                ),
                MediaItem(
                    id = 16,
                    title = "Buddha Pyaar",
                    description = "An age-gap romance that defies family conventions and tests societal definitions of companionship starring Priyanka R.",
                    imageUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=600&q=80",
                    category = "Watch Hunter – Now Streaming!, Best Of Voovi",
                    isPremium = true,
                    rating = 4.3f,
                    reelsUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                    likesCount = "19k"
                ),

                // BEST OF VOOVI
                MediaItem(
                    id = 17,
                    title = "ANGREZY AKHBAR",
                    description = "A controversial foreign newspaper report is leaked, generating wild gossip across the village starring Malvika T.",
                    imageUrl = "https://images.unsplash.com/photo-1505373877841-8d25f7d46678?auto=format&fit=crop&w=600&q=80",
                    category = "Best Of Voovi, Freshly Added Now!",
                    isPremium = true,
                    rating = 4.8f,
                    reelsUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
                    likesCount = "31k"
                ),
                MediaItem(
                    id = 18,
                    title = "BERANG",
                    description = "When all vibrant colors fade from a relationship, two partners strive to rediscover the initial glow starring Alia Sen.",
                    imageUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=600&q=80",
                    category = "Best Of Voovi, Rabbit Originals",
                    isPremium = true,
                    rating = 4.4f,
                    reelsUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
                    likesCount = "24k"
                ),

                // RABBIT ORIGINALS
                MediaItem(
                    id = 19,
                    title = "BHAIS KI VIDAI",
                    description = "A comical rural drama centered around a prized buffalo's departure that upsets the entire village starring Ridhima T.",
                    imageUrl = "https://images.unsplash.com/photo-1495107334309-fcf20504a5ab?auto=format&fit=crop&w=600&q=80",
                    category = "Rabbit Originals, Freshly Added Now!",
                    isPremium = true,
                    rating = 4.7f,
                    reelsUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4",
                    likesCount = "64k"
                ),
                MediaItem(
                    id = 20,
                    title = "ANGREGI BABA",
                    description = "A fake guru claiming visual spiritual remedies tricks residents until a clever student exposes his secrets starring Pihu Singh.",
                    imageUrl = "https://images.unsplash.com/photo-1519074069444-1ba4e6664104?auto=format&fit=crop&w=600&q=80",
                    category = "Rabbit Originals, PrimePlay Specials – Hot & Wild!",
                    isPremium = true,
                    rating = 4.6f,
                    reelsUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                    likesCount = "58k"
                ),
                MediaItem(
                    id = 21,
                    title = "MICHAL LOVE",
                    description = "A suspenseful romantic drama of a young couple caught in a small-town mystery starring Kataleyagil.",
                    imageUrl = "https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?auto=format&fit=crop&w=600&q=80",
                    category = "Rabbit Originals, Watch Hunter – Now Streaming!",
                    isPremium = true,
                    rating = 4.5f,
                    reelsUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                    likesCount = "27k"
                )
            )
            appDao.insertMediaItems(defaultMedia)
        }

        // Seed default categories if empty
        val currentCategoriesCount = appDao.getCategoriesCount()
        if (currentCategoriesCount == 0) {
            val defaultCategories = listOf(
                // Video Categories
                Category(name = "Freshly Added Now!", isPortrait = false, type = "video"),
                Category(name = "Ullu Originals – Most Loved!", isPortrait = true, type = "video"),
                Category(name = "PrimePlay Specials – Hot & Wild!", isPortrait = true, type = "video"),
                Category(name = "Watch Hunter – Now Streaming!", isPortrait = false, type = "video"),
                Category(name = "Best Of Voovi", isPortrait = true, type = "video"),
                Category(name = "Rabbit Originals", isPortrait = false, type = "video"),

                // Comics & Stories Categories
                Category(name = "Fantasy & Magic", isPortrait = false, type = "comic"),
                Category(name = "Action & Sci-Fi", isPortrait = true, type = "comic"),
                Category(name = "Romance Novels", isPortrait = false, type = "comic"),
                Category(name = "Mystery Thrillers", isPortrait = true, type = "comic")
            )
            appDao.insertCategories(defaultCategories)
        }

        // Seed default comics if empty
        val currentComicsCount = appDao.getComicsCount()
        if (currentComicsCount == 0) {
            val defaultComics = listOf(
                Comic(
                    id = 1,
                    title = "Mistress of Shadows",
                    category = "Action & Romance",
                    imageUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=600&q=80",
                    description = "A mysterious assassin falls in love with the target she was hired to eliminate. High stakes, intense romance, and thrilling action!",
                    likesCount = "48k"
                ),
                Comic(
                    id = 2,
                    title = "Secret Desires",
                    category = "Drama & Passion",
                    imageUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=600&q=80",
                    description = "Whispers of passion behind closed doors. When childhood sweethearts reunite under dangerous circumstances, forbidden sparks fly.",
                    likesCount = "125k"
                ),
                Comic(
                    id = 3,
                    title = "The Royal Treat",
                    category = "Fantasy & Royalty",
                    imageUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=600&q=80",
                    description = "Step into a realm of luxury, magic, and royal hierarchy where a common girl finds herself selected for the palace trials.",
                    likesCount = "89k"
                )
            )
            appDao.insertComics(defaultComics)
        }

        // Seed default comic chapters if empty
        val currentChaptersCount = appDao.getComicChaptersCount()
        if (currentChaptersCount == 0) {
            val defaultChapters = listOf(
                ComicChapter(
                    comicId = 1,
                    chapterNumber = 1,
                    title = "The Target",
                    pagesCsv = "https://images.unsplash.com/photo-1541963463532-d68292c34b19?auto=format&fit=crop&w=600&q=80,https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=600&q=80,https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=600&q=80,https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=600&q=80"
                ),
                ComicChapter(
                    comicId = 1,
                    chapterNumber = 2,
                    title = "First Encounter",
                    pagesCsv = "https://images.unsplash.com/photo-1515260268569-9271009adfdb?auto=format&fit=crop&w=600&q=80,https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?auto=format&fit=crop&w=600&q=80,https://images.unsplash.com/photo-1518156677180-95a2893f3e9f?auto=format&fit=crop&w=600&q=80"
                ),
                ComicChapter(
                    comicId = 2,
                    chapterNumber = 1,
                    title = "Reunion",
                    pagesCsv = "https://images.unsplash.com/photo-1501183007986-d0d080b147f9?auto=format&fit=crop&w=600&q=80,https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=600&q=80,https://images.unsplash.com/photo-1520201163981-8cc95007dd2a?auto=format&fit=crop&w=600&q=80"
                ),
                ComicChapter(
                    comicId = 2,
                    chapterNumber = 2,
                    title = "The Confession",
                    pagesCsv = "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?auto=format&fit=crop&w=600&q=80,https://images.unsplash.com/photo-1500627869374-13cd993b1115?auto=format&fit=crop&w=600&q=80,https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?auto=format&fit=crop&w=600&q=80"
                ),
                ComicChapter(
                    comicId = 3,
                    chapterNumber = 1,
                    title = "The Decree",
                    pagesCsv = "https://images.unsplash.com/photo-1469474968028-56623f02e42e?auto=format&fit=crop&w=600&q=80,https://images.unsplash.com/photo-1472214222541-d510753a4907?auto=format&fit=crop&w=600&q=80,https://images.unsplash.com/photo-1502082553048-f009c37129b9?auto=format&fit=crop&w=600&q=80"
                ),
                ComicChapter(
                    comicId = 3,
                    chapterNumber = 2,
                    title = "Palace Gates",
                    pagesCsv = "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?auto=format&fit=crop&w=600&q=80,https://images.unsplash.com/photo-1501854140801-50d01698950b?auto=format&fit=crop&w=600&q=80,https://images.unsplash.com/photo-1475924156734-496f6cac6ec1?auto=format&fit=crop&w=600&q=80"
                )
            )
            appDao.insertComicChapters(defaultChapters)
        }

        // Seed default stories if empty
        val currentStoriesCount = appDao.getStoriesCount()
        if (currentStoriesCount == 0) {
            val defaultStories = listOf(
                Story(
                    id = 1,
                    title = "The Chronicles of Eldoria",
                    imageUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=600&q=80",
                    category = "Fantasy & Magic",
                    content = "Deep within the ancient whispers of Eldoria, a forgotten relic begins to glow. Elian, a young apprentice, discovers that his lineage traces back to the legendary Sky Wardens. Guided by an enigmatic map, he sets off across the floating cliffs of Aurelia, encountering celestial beasts and unlocking ancient runic magic. His destiny is to seal the Rift of Shadows before the next solar eclipse.",
                    isPremium = true
                ),
                Story(
                    id = 2,
                    title = "Cyber City Echoes",
                    imageUrl = "https://images.unsplash.com/photo-1515621061946-eff1c2a352bd?auto=format&fit=crop&w=600&q=80",
                    category = "Action & Sci-Fi",
                    content = "In the neon-drenched spires of Neo-Tokyo, information is the ultimate currency. Kaelen, a rogue cyber-runner, accidentally intercepts an encrypted memory core belonging to the city's omnipotent governing syndicate. Hunted by cyborg mercenary squads, he must team up with a mysterious artificial intelligence to upload the core to the global resistance network before the grid goes into complete lockdown.",
                    isPremium = true
                ),
                Story(
                    id = 3,
                    title = "Whispers of the Heart",
                    imageUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=600&q=80",
                    category = "Romance Novels",
                    content = "Elena had always lived in the safety of her quiet bookstore, surrounded by paper worlds. That was until Julian, a nomadic travel photographer with a secret burden, walked in one rainy afternoon. As they spend the autumn exploring hidden city parks and sharing forgotten poetry, their lives interweave in ways they never expected, proving that sometimes the quietest love makes the loudest impact.",
                    isPremium = true
                ),
                Story(
                    id = 4,
                    title = "The Haunted Clockwork",
                    imageUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=600&q=80",
                    category = "Mystery Thrillers",
                    content = "When the grand clock tower of Blackwood starts running backward, the townspeople panic. Detective Clara Vance is called to investigate and finds herself trapped in a shifting labyrinth of copper gears and dark alchemy. She must solve a centuries-old murder mystery encoded in the clock's pendulum before time runs out for everyone.",
                    isPremium = true
                ),
                Story(
                    id = 5,
                    title = "Journey through Shadow-glen",
                    imageUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=600&q=80",
                    category = "Fantasy & Magic",
                    content = "Shadow-glen was once a forest of pure light, but a dark curse withered its leaves. Lyra, an elven archer, is chosen to carry the Seed of Hope to the sacred grove at the forest's core. With monsters lurking in every shadow, she learns that her greatest weapon is not her bow, but the kindness she shows to the creatures of the woods.",
                    isPremium = true
                ),
                Story(
                    id = 6,
                    title = "Rider of the Solar Winds",
                    imageUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=600&q=80",
                    category = "Action & Sci-Fi",
                    content = "Clara is a solar-sail racer, navigating the treacherous gravity wells of Jupiter. When her brother is captured by space pirates, she converts her lightweight racing ship into a stealth scout to infiltrate the pirate stronghold in the Asteroid Belt, leading to a thrilling high-speed chase across the rings of Saturn.",
                    isPremium = true
                ),
                Story(
                    id = 7,
                    title = "Starlight and Coffee Cups",
                    imageUrl = "https://images.unsplash.com/photo-1498804103079-a6351b050096?auto=format&fit=crop&w=600&q=80",
                    category = "Romance Novels",
                    content = "Two astrophysicists, Leo and Sarah, compete for the same research grant at the Royal Observatory. Bound by their passion for the stars but separated by professional rivalry, a shared discovery of a strange stellar anomaly forces them to work together under the starlit canopy, sparking an unexpected celestial romance.",
                    isPremium = true
                ),
                Story(
                    id = 8,
                    title = "The Last Train to Nowhere",
                    imageUrl = "https://images.unsplash.com/photo-1474487548417-781cb71495f3?auto=format&fit=crop&w=600&q=80",
                    category = "Mystery Thrillers",
                    content = "A group of strangers boards a midnight train from Paris. As they cross the countryside, they discover the train has no driver, the windows are completely blacked out, and one passenger has vanished from their locked cabin. To survive, they must uncover the secret connection that binds them all to a crime committed ten years ago.",
                    isPremium = true
                )
            )
            appDao.insertStories(defaultStories)

            val defaultStoryParts = listOf(
                StoryPart(id = 1, storyId = 1, partNumber = 1, title = "Part 1: The Call of Aurelia", content = "Elian sat in his workspace, watching the strange starlight crystal pulse softly. He had spent years reading legends, but this was different. The ancient runic text translated perfectly: 'When the sky-island shadows grow long, the rift opens.'"),
                StoryPart(id = 2, storyId = 1, partNumber = 2, title = "Part 2: The Floating Mountains", content = "Stepping off the air-skiff, Elian felt the ground shift. Aurelia's mountains were held by magnetic gravity. Lyra pointed to a glowing spire. 'There lies the Warden's seal. But hurry, the dark clouds are rising from the deep fissure.'"),
                StoryPart(id = 3, storyId = 1, partNumber = 3, title = "Part 3: The Sealed Gateway", content = "With the crystal in place, a wave of pure force swept through the canyon. The shadows hissed and dissolved back into the abyss. Elian turned to Lyra. 'It is done. For now, the rift is quiet.'"),
                StoryPart(id = 4, storyId = 2, partNumber = 1, title = "Episode 1: Memory Infiltration", content = "Neon rain fell upon Neo-Tokyo. Kaelen ran his deck with trembling fingers. The memory core was hot, glowing under the blue mesh. 'Uploading now,' he muttered, while tactical sirens began to echo in the distance."),
                StoryPart(id = 5, storyId = 2, partNumber = 2, title = "Episode 2: Syndicate Retaliation", content = "Shattered glass showered the street. Kaelen rolled behind a neural-charging station. The AI, Iris, beeped. 'Rebellion channels activated. Cyborg strike team approaching.'"),
                StoryPart(id = 6, storyId = 3, partNumber = 1, title = "Chapter 1: The Book of Autumn", content = "Julian walked into Elena's cozy shop on a rainy Wednesday. His travel logs were drenched, but his eyes were bright with a strange story of the lost northern lakes. 'I think we can find them together,' he smiled."),
                StoryPart(id = 7, storyId = 3, partNumber = 2, title = "Chapter 2: Following the Compass", content = "Elena held the map to her face. 'The ink, Julian, look! It only shows up when under candlelight.' Their eyes met in the warm glow of the cozy back room of the bookstore."),
                StoryPart(id = 8, storyId = 4, partNumber = 1, title = "Act I: Copper Gears & Brass Keys", content = "The clock tower struck thirteen. Clara adjusted her lens. 'No mechanism should behave this way,' she murmured, tracing a strange alchemical residue on the central pendulum gear."),
                StoryPart(id = 9, storyId = 4, partNumber = 2, title = "Act II: Labyrinth of Shadows", content = "As she turned the brass key, the floorboards slid open. A hidden staircase wound deep under the town center. Clara stepped inside, her oil lamp casting long, distorted shadows."),
                StoryPart(id = 10, storyId = 5, partNumber = 1, title = "Chapter 1: The Seed of Hope", content = "Lyra packed her satchel with glowing moss. 'The path through Shadow-glen is dangerous,' warned the village elder. 'Do not wander off the luminescent trail.'"),
                StoryPart(id = 11, storyId = 5, partNumber = 2, title = "Chapter 2: Whispers in the Canopy", content = "In the dark branches, tiny eyes blinked. Rather than aiming her bow, Lyra offered a handful of honey-berries. A small, winged sprite emerged, bowing in gratitude and pointing toward the sacred grove."),
                StoryPart(id = 12, storyId = 6, partNumber = 1, title = "Section 1: Solar Wind Navigation", content = "The solar flare was registered at Class X. Clara pulled back the primary sail levers. 'Hold on tight!' she shouted, as the ship rode the superheated plasma wave at relativistic speeds."),
                StoryPart(id = 13, storyId = 7, partNumber = 1, title = "Phase 1: Constellation Rivalry", content = "Sarah's thesis was impeccable. Leo's telescope findings were groundbreaking. They spent the first week of the summit pointing out flaws in each other's research, unaware of the cosmic anomaly currently aligning above them.")
            )
            appDao.insertStoryParts(defaultStoryParts)
        }

        // Seed default profile state if empty
        val currentProfileCount = appDao.getProfileCount()
        if (currentProfileCount == 0) {
            appDao.insertProfileState(ProfileState())
        }
    }

    // --- Notification History & Analytics ---
    val allNotificationHistory: Flow<List<NotificationHistoryItem>> = appDao.getAllNotificationHistory()
    val allNotificationAnalytics: Flow<List<NotificationAnalyticsMetric>> = appDao.getAllNotificationAnalytics()

    suspend fun addNotificationHistory(title: String, body: String, type: String, payloadJson: String, status: String): Long = withContext(Dispatchers.IO) {
        val item = NotificationHistoryItem(
            title = title,
            body = body,
            type = type,
            payloadJson = payloadJson,
            status = status
        )
        val id = appDao.insertNotificationHistory(item)
        trackNotificationAnalytic(status.lowercase().replace(" ", "_"))
        id
    }

    suspend fun updateNotificationHistoryStatus(id: Int, status: String) = withContext(Dispatchers.IO) {
        appDao.updateNotificationHistoryStatus(id, status)
        trackNotificationAnalytic(status.lowercase().replace(" ", "_"))
    }

    suspend fun clearNotificationHistory() = withContext(Dispatchers.IO) {
        appDao.clearNotificationHistory()
    }

    suspend fun trackNotificationAnalytic(metricId: String) = withContext(Dispatchers.IO) {
        val cleanMetric = when(metricId.lowercase().trim()) {
            "delivered" -> "delivered"
            "opened" -> "opened"
            "dismissed" -> "dismissed"
            "action_clicked" -> "action_clicked"
            "failed" -> "failed"
            else -> metricId.lowercase().trim()
        }
        val current = appDao.getNotificationAnalytic(cleanMetric)
        if (current == null) {
            appDao.insertNotificationAnalytic(NotificationAnalyticsMetric(metricId = cleanMetric, count = 1))
        } else {
            appDao.insertNotificationAnalytic(current.copy(count = current.count + 1))
        }
    }

    suspend fun clearNotificationAnalytics() = withContext(Dispatchers.IO) {
        appDao.clearNotificationAnalytics()
    }

    suspend fun seedSampleStoriesComicsAndCategories(): Boolean = withContext(Dispatchers.IO) {
        val service = firebaseService ?: return@withContext false

        // 1. Clear existing local entries to avoid key collision and keep it clean
        appDao.deleteAllStories()
        appDao.deleteAllComics()
        appDao.deleteAllComicChapters()
        appDao.deleteAllCategories()

        // 2. Generate and Insert Categories
        val sampleCategories = listOf(
            // Video Categories
            com.example.data.model.Category(id = 1, name = "Freshly Added Now!", isPortrait = false, type = "video"),
            com.example.data.model.Category(id = 2, name = "Ullu Originals – Most Loved!", isPortrait = true, type = "video"),
            com.example.data.model.Category(id = 3, name = "PrimePlay Specials – Hot & Wild!", isPortrait = true, type = "video"),
            com.example.data.model.Category(id = 4, name = "Watch Hunter – Now Streaming!", isPortrait = false, type = "video"),
            com.example.data.model.Category(id = 5, name = "Best Of Voovi", isPortrait = true, type = "video"),
            com.example.data.model.Category(id = 6, name = "Rabbit Originals", isPortrait = false, type = "video"),

            // Comics & Stories Categories
            com.example.data.model.Category(id = 10, name = "Fantasy & Magic", isPortrait = false, type = "comic"),
            com.example.data.model.Category(id = 11, name = "Action & Sci-Fi", isPortrait = true, type = "comic"),
            com.example.data.model.Category(id = 12, name = "Romance Novels", isPortrait = false, type = "comic"),
            com.example.data.model.Category(id = 13, name = "Mystery Thrillers", isPortrait = true, type = "comic")
        )
        appDao.insertCategories(sampleCategories)

        // 3. Generate and Insert Stories (at least 15 to demonstrate pagination after every 10 stories)
        val sampleStories = listOf(
            com.example.data.model.Story(
                id = 1,
                title = "The Chronicles of Eldoria",
                imageUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=600&q=80",
                category = "Fantasy & Magic",
                content = "Deep within the ancient whispers of Eldoria, a forgotten relic begins to glow. Elian, a young apprentice, discovers that his lineage traces back to the legendary Sky Wardens. Guided by an enigmatic map, he sets off across the floating cliffs of Aurelia, encountering celestial beasts and unlocking ancient runic magic. His destiny is to seal the Rift of Shadows before the next solar eclipse.",
                isPremium = true
            ),
            com.example.data.model.Story(
                id = 2,
                title = "Cyber City Echoes",
                imageUrl = "https://images.unsplash.com/photo-1515621061946-eff1c2a352bd?auto=format&fit=crop&w=600&q=80",
                category = "Action & Sci-Fi",
                content = "In the neon-drenched spires of Neo-Tokyo, information is the ultimate currency. Kaelen, a rogue cyber-runner, accidentally intercepts an encrypted memory core belonging to the city's omnipotent governing syndicate. Hunted by cyborg mercenary squads, he must team up with a mysterious artificial intelligence to upload the core to the global resistance network before the grid goes into complete lockdown.",
                isPremium = true
            ),
            com.example.data.model.Story(
                id = 3,
                title = "Whispers of the Heart",
                imageUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=600&q=80",
                category = "Romance Novels",
                content = "Elena had always lived in the safety of her quiet bookstore, surrounded by paper worlds. That was until Julian, a nomadic travel photographer with a secret burden, walked in one rainy afternoon. As they spend the autumn exploring hidden city parks and sharing forgotten poetry, their lives interweave in ways they never expected, proving that sometimes the quietest love makes the loudest impact.",
                isPremium = true
            ),
            com.example.data.model.Story(
                id = 4,
                title = "The Haunted Clockwork",
                imageUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=600&q=80",
                category = "Mystery Thrillers",
                content = "When the grand clock tower of Blackwood starts running backward, the townspeople panic. Detective Clara Vance is called to investigate and finds herself trapped in a shifting labyrinth of copper gears and dark alchemy. She must solve a centuries-old murder mystery encoded in the clock's pendulum before time runs out for everyone.",
                isPremium = true
            ),
            com.example.data.model.Story(
                id = 5,
                title = "Journey through Shadow-glen",
                imageUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=600&q=80",
                category = "Fantasy & Magic",
                content = "Shadow-glen was once a forest of pure light, but a dark curse withered its leaves. Lyra, an elven archer, is chosen to carry the Seed of Hope to the sacred grove at the forest's core. With monsters lurking in every shadow, she learns that her greatest weapon is not her bow, but the kindness she shows to the creatures of the woods.",
                isPremium = true
            ),
            com.example.data.model.Story(
                id = 6,
                title = "Rider of the Solar Winds",
                imageUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=600&q=80",
                category = "Action & Sci-Fi",
                content = "Clara is a solar-sail racer, navigating the treacherous gravity wells of Jupiter. When her brother is captured by space pirates, she converts her lightweight racing ship into a stealth scout to infiltrate the pirate stronghold in the Asteroid Belt, leading to a thrilling high-speed chase across the rings of Saturn.",
                isPremium = true
            ),
            com.example.data.model.Story(
                id = 7,
                title = "Starlight and Coffee Cups",
                imageUrl = "https://images.unsplash.com/photo-1498804103079-a6351b050096?auto=format&fit=crop&w=600&q=80",
                category = "Romance Novels",
                content = "Two astrophysicists, Leo and Sarah, compete for the same research grant at the Royal Observatory. Bound by their passion for the stars but separated by professional rivalry, a shared discovery of a strange stellar anomaly forces them to work together under the starlit canopy, sparking an unexpected celestial romance.",
                isPremium = true
            ),
            com.example.data.model.Story(
                id = 8,
                title = "The Last Train to Nowhere",
                imageUrl = "https://images.unsplash.com/photo-1474487548417-781cb71495f3?auto=format&fit=crop&w=600&q=80",
                category = "Mystery Thrillers",
                content = "A group of strangers boards a midnight train from Paris. As they cross the countryside, they discover the train has no driver, the windows are completely blacked out, and one passenger has vanished from their locked cabin. To survive, they must uncover the secret connection that binds them all to a crime committed ten years ago.",
                isPremium = true
            ),
            com.example.data.model.Story(
                id = 9,
                title = "The Fire Mage's Daughter",
                imageUrl = "https://images.unsplash.com/photo-1461360370896-922624d12aa1?auto=format&fit=crop&w=600&q=80",
                category = "Fantasy & Magic",
                content = "Ignis was the most powerful fire mage, but he left his staff to his daughter, Chloe, who can only conjure cold blue frost. Rejected by the Fire Academy, she travels to the Northern Ice Spires and discovers her frost is actually the only element capable of pacifying the ancient volcanic dragons sleeping under the earth.",
                isPremium = true
            ),
            com.example.data.model.Story(
                id = 10,
                title = "Neon Samurai Legacy",
                imageUrl = "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=600&q=80",
                category = "Action & Sci-Fi",
                content = "Takahiro wields the Plasma Katana, a relic of his ancestors in a futuristic high-tech Kyoto. When the corrupt Megacorp tries to seize the blade to power their superweapon, Takahiro must lead an underground rebellion to restore honor and protect the spiritual heart of his home.",
                isPremium = true
            ),
            com.example.data.model.Story(
                id = 11,
                title = "A Song for the Autumn",
                imageUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=600&q=80",
                category = "Romance Novels",
                content = "A violinist who lost her hearing in an accident finds solace in painting. She meets a blind cellist who plays beautiful melodies on the street. Together, they create a sensory language of art and music that heals their broken hearts and bridges the gap between their quiet worlds.",
                isPremium = true
            ),
            com.example.data.model.Story(
                id = 12,
                title = "The Crimson Cipher",
                imageUrl = "https://images.unsplash.com/photo-1501535033-a59376afb713?auto=format&fit=crop&w=600&q=80",
                category = "Mystery Thrillers",
                content = "An ancient leather-bound manuscript is found in a Vatican library vault. Professor Marcus spends nights decoding its crimson-ink symbols, only to realize the text predicts major historical events. As mysterious figures start trailing his family, he realizes the final page details an impending global catastrophe set to happen in 48 hours.",
                isPremium = true
            ),
            com.example.data.model.Story(
                id = 13,
                title = "The Dragon of Ironwood",
                imageUrl = "https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=600&q=80",
                category = "Fantasy & Magic",
                content = "Ironwood is a land where trees are as strong as steel. A baby dragon, born with iron scales instead of fire, is found by a local lumberjack. Together, they defend the forest against an encroaching empire's strip-mining machines, showing that nature's strength is unbreakable.",
                isPremium = true
            ),
            com.example.data.model.Story(
                id = 14,
                title = "Infiltration of Sector 7",
                imageUrl = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?auto=format&fit=crop&w=600&q=80",
                category = "Action & Sci-Fi",
                content = "An elite spy, Jax, is sent to infiltrate a submarine base in the arctic Sector 7. Equipped with state-of-the-art optical camouflage, he navigates laser grids and acoustic sensors only to find that the base is a decoy for a rogue missile launch scheduled from an orbiting satellite station.",
                isPremium = true
            ),
            com.example.data.model.Story(
                id = 15,
                title = "Summer Rain and Sunshine",
                imageUrl = "https://images.unsplash.com/photo-1519751138087-5bf79df62d5b?auto=format&fit=crop&w=600&q=80",
                category = "Romance Novels",
                content = "Mia runs an ice cream parlor that is struggling to survive a rainy summer. Liam, a cheerful weather reporter who always predicts sunshine, steps in to help her organize creative 'rainy day' specials, turning the shop into a cozy town favorite while falling deeply in love with her under their shared yellow umbrella.",
                isPremium = true
            )
        )
        appDao.insertStories(sampleStories)

        // 4. Generate and Insert Comics and Comic Chapters
        val sampleComics = listOf(
            com.example.data.model.Comic(
                id = 201,
                title = "Rise of the Frost Monarch",
                category = "Fantasy & Magic",
                imageUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=600&q=80",
                description = "In an age covered in eternal ice, a banished prince wields the legendary Frost Scepter to reclaim his throne from the flame tyrants.",
                likesCount = "45k",
                isPremium = true
            ),
            com.example.data.model.Comic(
                id = 202,
                title = "Galactic Bounty Hunters",
                category = "Action & Sci-Fi",
                imageUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=600&q=80",
                description = "Join the universe's most notorious mercenary crew as they chase high-value targets across uncharted nebulae.",
                likesCount = "38k",
                isPremium = true
            ),
            com.example.data.model.Comic(
                id = 203,
                title = "Legends of Shadow-Realm",
                category = "Mystery Thrillers",
                imageUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=600&q=80",
                description = "Uncover ancient curses and solve dark alchemical riddles alongside Detective Vance in a Victorian city covered in smoke and mystery.",
                likesCount = "29k",
                isPremium = false
            ),
            com.example.data.model.Comic(
                id = 204,
                title = "Love in the Digital Age",
                category = "Romance Novels",
                imageUrl = "https://images.unsplash.com/photo-1519751138087-5bf79df62d5b?auto=format&fit=crop&w=600&q=80",
                description = "Two AI engineers working on conflicting models find themselves falling in love through sweet, randomized chat algorithms.",
                likesCount = "52k",
                isPremium = false
            )
        )
        appDao.insertComics(sampleComics)

        val sampleChapters = listOf(
            com.example.data.model.ComicChapter(
                id = 301,
                comicId = 201,
                chapterNumber = 1,
                title = "The Frozen Exile (Images)",
                pagesCsv = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=800&q=80,https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?auto=format&fit=crop&w=800&q=80",
                pdfUrl = ""
            ),
            com.example.data.model.ComicChapter(
                id = 302,
                comicId = 201,
                chapterNumber = 2,
                title = "Wielding the Scepter (PDF)",
                pagesCsv = "",
                pdfUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf"
            ),
            com.example.data.model.ComicChapter(
                id = 303,
                comicId = 202,
                chapterNumber = 1,
                title = "The Nebula Chase (Combined)",
                pagesCsv = "https://images.unsplash.com/photo-1614850523459-c2f4c699c52e?auto=format&fit=crop&w=800&q=80,https://images.unsplash.com/photo-1541701494587-cb58502866ab?auto=format&fit=crop&w=800&q=80",
                pdfUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf"
            ),
            com.example.data.model.ComicChapter(
                id = 304,
                comicId = 202,
                chapterNumber = 2,
                title = "Infiltrating the Syndicate Fleet",
                pagesCsv = "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=800&q=80",
                pdfUrl = ""
            ),
            com.example.data.model.ComicChapter(
                id = 305,
                comicId = 203,
                chapterNumber = 1,
                title = "The Clockwork Pendulum (PDF)",
                pagesCsv = "",
                pdfUrl = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf"
            ),
            com.example.data.model.ComicChapter(
                id = 306,
                comicId = 204,
                chapterNumber = 1,
                title = "Pixelated Whispers",
                pagesCsv = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=800&q=80",
                pdfUrl = ""
            )
        )
        appDao.insertComicChapters(sampleChapters)

        // Seed Story Parts locally
        appDao.deleteAllStoryParts()
        val sampleStoryParts = listOf(
            com.example.data.model.StoryPart(id = 1, storyId = 1, partNumber = 1, title = "Part 1: The Call of Aurelia", content = "Elian sat in his workspace, watching the strange starlight crystal pulse softly. He had spent years reading legends, but this was different. The ancient runic text translated perfectly: 'When the sky-island shadows grow long, the rift opens.'"),
            com.example.data.model.StoryPart(id = 2, storyId = 1, partNumber = 2, title = "Part 2: The Floating Mountains", content = "Stepping off the air-skiff, Elian felt the ground shift. Aurelia's mountains were held by magnetic gravity. Lyra pointed to a glowing spire. 'There lies the Warden's seal. But hurry, the dark clouds are rising from the deep fissure.'"),
            com.example.data.model.StoryPart(id = 3, storyId = 1, partNumber = 3, title = "Part 3: The Sealed Gateway", content = "With the crystal in place, a wave of pure force swept through the canyon. The shadows hissed and dissolved back into the abyss. Elian turned to Lyra. 'It is done. For now, the rift is quiet.'"),
            com.example.data.model.StoryPart(id = 4, storyId = 2, partNumber = 1, title = "Episode 1: Memory Infiltration", content = "Neon rain fell upon Neo-Tokyo. Kaelen ran his deck with trembling fingers. The memory core was hot, glowing under the blue mesh. 'Uploading now,' he muttered, while tactical sirens began to echo in the distance."),
            com.example.data.model.StoryPart(id = 5, storyId = 2, partNumber = 2, title = "Episode 2: Syndicate Retaliation", content = "Shattered glass showered the street. Kaelen rolled behind a neural-charging station. The AI, Iris, beeped. 'Rebellion channels activated. Cyborg strike team approaching.'"),
            com.example.data.model.StoryPart(id = 6, storyId = 3, partNumber = 1, title = "Chapter 1: The Book of Autumn", content = "Julian walked into Elena's cozy shop on a rainy Wednesday. His travel logs were drenched, but his eyes were bright with a strange story of the lost northern lakes. 'I think we can find them together,' he smiled."),
            com.example.data.model.StoryPart(id = 7, storyId = 3, partNumber = 2, title = "Chapter 2: Following the Compass", content = "Elena held the map to her face. 'The ink, Julian, look! It only shows up when under candlelight.' Their eyes met in the warm glow of the cozy back room of the bookstore."),
            com.example.data.model.StoryPart(id = 8, storyId = 4, partNumber = 1, title = "Act I: Copper Gears & Brass Keys", content = "The clock tower struck thirteen. Clara adjusted her lens. 'No mechanism should behave this way,' she murmured, tracing a strange alchemical residue on the central pendulum gear."),
            com.example.data.model.StoryPart(id = 9, storyId = 4, partNumber = 2, title = "Act II: Labyrinth of Shadows", content = "As she turned the brass key, the floorboards slid open. A hidden staircase wound deep under the town center. Clara stepped inside, her oil lamp casting long, distorted shadows."),
            com.example.data.model.StoryPart(id = 10, storyId = 5, partNumber = 1, title = "Chapter 1: The Seed of Hope", content = "Lyra packed her satchel with glowing moss. 'The path through Shadow-glen is dangerous,' warned the village elder. 'Do not wander off the luminescent trail.'"),
            com.example.data.model.StoryPart(id = 11, storyId = 5, partNumber = 2, title = "Chapter 2: Whispers in the Canopy", content = "In the dark branches, tiny eyes blinked. Rather than aiming her bow, Lyra offered a handful of honey-berries. A small, winged sprite emerged, bowing in gratitude and pointing toward the sacred grove."),
            com.example.data.model.StoryPart(id = 12, storyId = 6, partNumber = 1, title = "Section 1: Solar Wind Navigation", content = "The solar flare was registered at Class X. Clara pulled back the primary sail levers. 'Hold on tight!' she shouted, as the ship rode the superheated plasma wave at relativistic speeds."),
            com.example.data.model.StoryPart(id = 13, storyId = 7, partNumber = 1, title = "Phase 1: Constellation Rivalry", content = "Sarah's thesis was impeccable. Leo's telescope findings were groundbreaking. They spent the first week of the summit pointing out flaws in each other's research, unaware of the cosmic anomaly currently aligning above them.")
        )
        appDao.insertStoryParts(sampleStoryParts)

        // 4b. Generate, insert and upload 100 Audio Stories (Batch 1) and 100 Audio Stories (Batch 2)
        val audioStoriesBatch1 = mutableListOf<com.example.data.model.Story>()
        val audioStoriesBatch2 = mutableListOf<com.example.data.model.Story>()
        
        val categories = listOf("Romance Novels", "Mystery Thrillers", "Fantasy & Magic", "Action & Sci-Fi")
        val images = listOf(
            "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1515621061946-eff1c2a352bd?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=600&q=80"
        )
        val audioUrls = listOf(
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3"
        )
        
        for (i in 1..100) {
            val id = 1000 + i
            val category = categories[i % categories.size]
            val imageUrl = images[i % images.size]
            val audioUrl = audioUrls[i % audioUrls.size]
            val durationMinutes = 5 + (i % 15)
            val releaseTime = System.currentTimeMillis() - java.util.concurrent.TimeUnit.DAYS.toMillis(i.toLong())
            audioStoriesBatch1.add(
                com.example.data.model.Story(
                    id = id,
                    title = "Audio Story #$i (Batch 1)",
                    imageUrl = imageUrl,
                    content = "Premium $category audio story designed for smooth offline playback, rich vocal delivery, and full playback scrubbing. Experience beautiful, professionally recorded voice acting.",
                    category = category,
                    isPremium = i % 10 != 0,
                    audioUrl = audioUrl,
                    duration = "$durationMinutes min",
                    releaseDate = releaseTime
                )
            )
        }
        
        for (i in 1..100) {
            val id = 2000 + i
            val category = categories[i % categories.size]
            val imageUrl = images[i % images.size]
            val audioUrl = audioUrls[i % audioUrls.size]
            val durationMinutes = 8 + (i % 20)
            val releaseTime = System.currentTimeMillis() - java.util.concurrent.TimeUnit.DAYS.toMillis(100L + i.toLong())
            audioStoriesBatch2.add(
                com.example.data.model.Story(
                    id = id,
                    title = "Audio Story #$i (Batch 2)",
                    imageUrl = imageUrl,
                    content = "Premium $category adventure serialized in audio. Featuring immersive background ambient scores and seamless play controls. Download to your phone and listen anytime without internet.",
                    category = category,
                    isPremium = i % 12 != 0,
                    audioUrl = audioUrl,
                    duration = "$durationMinutes min",
                    releaseDate = releaseTime
                )
            )
        }
        
        appDao.insertStories(audioStoriesBatch1)
        appDao.insertStories(audioStoriesBatch2)

        // 5. Upload everything to Cloud Firestore using optimized nested structures
        val categoriesWithVideos = sampleCategories.map { category ->
            val videos = appDao.getMediaItemsByCategoryDirect(category.name)
            Pair(category, videos)
        }
        val successCat = service.uploadCategoriesWithVideos(categoriesWithVideos)

        val comicsWithChapters = sampleComics.map { comic ->
            val chapters = sampleChapters.filter { it.comicId == comic.id }
            Pair(comic, chapters)
        }
        val successComics = service.uploadComicsWithChapters(comicsWithChapters)

        val storiesWithParts = sampleStories.map { story ->
            val parts = sampleStoryParts.filter { it.storyId == story.id }
            Pair(story, parts)
        }
        val successStories = service.uploadStoriesWithParts(storiesWithParts)

        val audioBatches = mapOf(
            "batch_1" to audioStoriesBatch1,
            "batch_2" to audioStoriesBatch2
        )
        val successAudioBatches = service.uploadAudioBatches(audioBatches)

        return@withContext successCat && successComics && successStories && successAudioBatches
    }

    suspend fun syncStoriesAndComicsWithFirebase(): Boolean = withContext(Dispatchers.IO) {
        val service = firebaseService ?: return@withContext false
        val comicsWithChapters = service.fetchComics()
        val storiesWithParts = service.fetchStories()
        val audioStories = service.fetchAudioBatches()

        var success = false

        if (comicsWithChapters != null) {
            val allComics = comicsWithChapters.map { it.first }
            val allChapters = comicsWithChapters.flatMap { it.second }

            appDao.deleteAllComics()
            appDao.insertComics(allComics)

            appDao.deleteAllComicChapters()
            if (allChapters.isNotEmpty()) {
                appDao.insertComicChapters(allChapters)
            }
            success = true
        }

        if (storiesWithParts != null) {
            val allStories = storiesWithParts.map { it.first }
            val allParts = storiesWithParts.flatMap { it.second }

            appDao.deleteAllStories()
            appDao.insertStories(allStories)

            appDao.deleteAllStoryParts()
            if (allParts.isNotEmpty()) {
                appDao.insertStoryParts(allParts)
            }
            success = true
        }

        if (!audioStories.isNullOrEmpty()) {
            appDao.insertStories(audioStories)
            success = true
        }

        return@withContext success
    }
}
