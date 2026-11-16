package com.example.data.repository

import com.example.data.dao.AppDao
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AppRepository(
    private val appDao: AppDao,
    val context: android.content.Context? = null
) {
    val firebaseService = context?.let { com.example.data.network.FirebaseService(it) }
    val cloudflareService = context?.let { com.example.data.network.CloudflareService(it) }

    private val syncDataMutex = Mutex()
    private val userSyncMutex = Mutex()

    @Volatile
    private var hasCompletedInitialCloudflareSync = false

    @Volatile
    var hasCheckedRemoteUserStatusThisSession = false
        private set

    suspend fun uploadAdConditionsToFirebase(): Boolean = withContext(Dispatchers.IO) {
        firebaseService?.uploadAdConditionsToFirebase() ?: false
    }

    suspend fun syncDataWithCloudflare(forceRefresh: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        syncDataMutex.withLock {
            if (!forceRefresh && hasCompletedInitialCloudflareSync) {
                return@withLock true
            }

            var hasData = false
            val service = cloudflareService ?: return@withLock false

            // Fetch all 7 Cloudflare endpoints + Firebase config in parallel (1 single request per endpoint)
            coroutineScope {
                val firebaseJob = async {
                    try {
                        firebaseService?.uploadAdConditionsToFirebase()
                        firebaseService?.syncAdConfigFromFirebase()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                val videoCatsJob = async {
                    try { service.fetchVideoCategoriesFromCloudflare() } catch (e: Exception) { e.printStackTrace(); emptyList() }
                }
                val channelsJob = async {
                    try { service.fetchChannelsFromCloudflare() } catch (e: Exception) { e.printStackTrace(); emptyList() }
                }
                val reelsJob = async {
                    try { service.fetchReelsFromCloudflare() } catch (e: Exception) { e.printStackTrace(); emptyList() }
                }
                val comicsJob = async {
                    try { service.fetchComicsFromCloudflare() } catch (e: Exception) { e.printStackTrace(); emptyList() }
                }
                val storiesJob = async {
                    try { service.fetchStoriesFromCloudflareWithParts() } catch (e: Exception) { e.printStackTrace(); Pair(emptyList(), emptyList()) }
                }
                val audioStoriesJob = async {
                    try { service.fetchAudioBatchesFromCloudflare() } catch (e: Exception) { e.printStackTrace(); emptyList() }
                }
                val batchVideosJob = async {
                    try { service.fetchBatchVideosFromCloudflare() } catch (e: Exception) { e.printStackTrace(); emptyList() }
                }

                val videoCats = videoCatsJob.await()
                val channels = channelsJob.await()
                val reels = reelsJob.await()
                val comics = comicsJob.await()
                val (stories, parts) = storiesJob.await()
                val audioStories = audioStoriesJob.await()
                val batchVideos = batchVideosJob.await()
                firebaseJob.await()

                // 2. Replace Video Categories & Videos with fresh live Cloudflare data
                if (videoCats.isNotEmpty()) {
                    val categoriesList = mutableListOf<Category>()
                    val mediaItemsList = mutableListOf<MediaItem>()
                    val joinsList = mutableListOf<MediaCategoryJoin>()

                    videoCats.forEach { (cat, videos) ->
                        categoriesList.add(cat)
                        videos.forEach { video ->
                            mediaItemsList.add(video)
                            if (cat.id > 0 && video.id > 0) {
                                joinsList.add(MediaCategoryJoin(mediaId = video.id, categoryId = cat.id))
                            }
                        }
                    }

                    appDao.deleteAllMediaCategoryJoins()
                    appDao.deleteAllCategories()
                    appDao.deleteAllMediaItems()
                    if (categoriesList.isNotEmpty()) {
                        appDao.insertCategories(categoriesList)
                    }
                    if (mediaItemsList.isNotEmpty()) {
                        appDao.insertMediaItems(mediaItemsList)
                    }
                    if (joinsList.isNotEmpty()) {
                        appDao.insertMediaCategoryJoins(joinsList)
                    }
                    hasData = true
                }

                // 3. Replace Channels with fresh Cloudflare data
                if (channels.isNotEmpty()) {
                    appDao.deleteAllChannels()
                    appDao.insertChannels(channels)
                    hasData = true
                }

                // 4. Replace Reels with fresh Cloudflare data
                if (reels.isNotEmpty()) {
                    appDao.deleteAllUserReels()
                    appDao.insertUserReels(reels)
                    hasData = true
                }

                // 5. Replace Comics with fresh Cloudflare data
                if (comics.isNotEmpty()) {
                    appDao.deleteAllComics()
                    appDao.deleteAllComicChapters()
                    comics.forEach { (comic, chapters) ->
                        appDao.insertComics(listOf(comic))
                        if (chapters.isNotEmpty()) {
                            appDao.insertComicChapters(chapters)
                        }
                    }
                    hasData = true
                }

                // 6 & 7. Replace Stories, Story Parts & Audio Batches with fresh Cloudflare data
                if (stories.isNotEmpty() || audioStories.isNotEmpty()) {
                    appDao.deleteAllStories()
                    if (stories.isNotEmpty()) {
                        appDao.insertStories(stories)
                        if (parts.isNotEmpty()) {
                            appDao.deleteAllStoryParts()
                            appDao.insertStoryParts(parts)
                        }
                    }
                    if (audioStories.isNotEmpty()) {
                        appDao.insertStories(audioStories)
                    }
                    hasData = true
                }

                // 8. Replace Batch Videos with fresh Cloudflare data
                if (batchVideos.isNotEmpty()) {
                    val allBatches = batchVideos.map { it.first }
                    val allItems = batchVideos.flatMap { it.second }
                    appDao.deleteAllBatchVideos()
                    appDao.insertBatchVideos(allBatches)
                    appDao.deleteAllBatchVideoItems()
                    if (allItems.isNotEmpty()) {
                        appDao.insertBatchVideoItems(allItems)
                    }
                    hasData = true
                }
            }

            hasCompletedInitialCloudflareSync = true
            hasData
        }
    }

    suspend fun uploadFirestoreVideoCategoriesToCloudflareWithoutEditingKeys(): Boolean = withContext(Dispatchers.IO) {
        val rawCategories = firebaseService?.fetchRawVideoCategoriesFromFirestore() ?: return@withContext false
        val service = cloudflareService ?: return@withContext false
        var allSuccess = true
        for (raw in rawCategories) {
            val ok = service.uploadRawVideoCategoryToCloudflare(
                id = raw.id,
                name = raw.name,
                isPortrait = raw.isPortrait,
                type = raw.type,
                videosJson = raw.videosJson
            )
            if (!ok) allSuccess = false
        }
        allSuccess
    }

    suspend fun syncDataWithFirebase(): Boolean = withContext(Dispatchers.IO) {
        // Redirect sync to Cloudflare
        syncDataWithCloudflare()
    }

    suspend fun fetchAppVersionDetails(): com.example.data.network.AppVersionDetails? = withContext(Dispatchers.IO) {
        val service = firebaseService ?: return@withContext null
        service.fetchAppVersionDetails()
    }

    suspend fun registerOrUpdateUserData(userId: String, isPremium: Boolean): Boolean = withContext(Dispatchers.IO) {
        val status = if (isPremium) "premium" else "normal"
        val prefs = context?.getSharedPreferences("app_user_prefs", android.content.Context.MODE_PRIVATE)
        val fcmToken = prefs?.getString("fcm_token", null)
        val cloudflareOk = cloudflareService?.saveUserDataToCloudflare(
            userId = userId,
            status = status,
            isPremium = isPremium,
            fcmToken = fcmToken
        ) ?: false
        if (isPremium && cloudflareOk) {
            prefs?.edit()
                ?.putBoolean("confirmed_premium", true)
                ?.putLong("last_premium_check_time", System.currentTimeMillis())
                ?.putBoolean("status_synced_after_install", true)
                ?.apply()
        }
        cloudflareOk
    }

    suspend fun fetchUserData(userId: String): String? = withContext(Dispatchers.IO) {
        val cloudflareList = cloudflareService?.fetchUserDataListFromCloudflare()
        val cfMatch = cloudflareList?.find {
            it["userid"]?.equals(userId, ignoreCase = true) == true ||
            it["userId"]?.equals(userId, ignoreCase = true) == true ||
            it["id"]?.equals(userId, ignoreCase = true) == true ||
            it["_id"]?.equals(userId, ignoreCase = true) == true
        }
        if (cfMatch != null) {
            val statusStr = cfMatch["status"]?.trim().orEmpty()
            val isPremFlag = cfMatch["isPremium"]?.trim().orEmpty()
            if (statusStr.equals("premium", ignoreCase = true) ||
                isPremFlag.equals("true", ignoreCase = true) ||
                isPremFlag == "1"
            ) {
                return@withContext "premium"
            }
            if (statusStr.isNotBlank()) {
                return@withContext statusStr.lowercase()
            }
        }
        null
    }

    suspend fun fetchNotifications(): List<UserNotification>? = withContext(Dispatchers.IO) {
        val cfNotifs = cloudflareService?.fetchNotificationsFromCloudflare()
        if (!cfNotifs.isNullOrEmpty()) {
            return@withContext cfNotifs
        }
        val service = firebaseService ?: return@withContext null
        service.fetchNotifications()
    }

    suspend fun uploadNotification(n: UserNotification): Boolean = withContext(Dispatchers.IO) {
        val service = firebaseService ?: return@withContext false
        service.uploadNotification(n)
    }

    suspend fun fetchUserDataList(): List<Map<String, String>>? = withContext(Dispatchers.IO) {
        cloudflareService?.fetchUserDataListFromCloudflare()
    }

    suspend fun logUserActivity(userId: String, action: String, details: Map<String, Any?> = emptyMap()): Boolean = withContext(Dispatchers.IO) {
        cloudflareService?.saveUserActivityToCloudflare(userId, action, details) ?: false
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
        syncDataWithCloudflare()
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
            Triple("Crimson Nights", "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?auto=format&fit=crop&w=600&q=80", "https://github.com/mediaelement/mediaelement-files/raw/master/big_buck_bunny.mp4"),
            Triple("Late Night Passion", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=600&q=80", "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4"),
            Triple("First Glance Romance", "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=600&q=80", "https://raw.githubusercontent.com/intel-iot-devkit/sample-videos/master/person-bicycle-car-detection.mp4"),
            Triple("Behind-The-Scenes Glamour", "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=600&q=80", "https://test-videos.co.uk/vids/jellyfish/mp4/h264/360/Jellyfish_360_10s_1MB.mp4"),
            Triple("Mystic Eyes & Whispers", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=300&q=80", "https://test-videos.co.uk/vids/sintel/mp4/h264/360/Sintel_360_10s_1MB.mp4")
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

            // Local Room seeding only - do not auto-upload to Firestore so media_items are not automatically created in Cloud Firestore
            true
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
    val allBatchVideos: Flow<List<BatchVideo>> = appDao.getAllBatchVideos()
    val allBatchVideoItems: Flow<List<BatchVideoItem>> = appDao.getAllBatchVideoItems()

    fun getItemsForBatch(batchId: String): Flow<List<BatchVideoItem>> = appDao.getItemsForBatch(batchId)
    suspend fun getItemsForBatchDirect(batchId: String): List<BatchVideoItem> = appDao.getItemsForBatchDirect(batchId)
    
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
        val androidId = try {
            android.provider.Settings.Secure.getString(ctx.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            null
        }
        val prefs = ctx.getSharedPreferences("app_user_prefs", android.content.Context.MODE_PRIVATE)
        if (!androidId.isNullOrEmpty() && androidId != "android_id" && androidId != "9774d56d682e549c") {
            prefs.edit().putString("userid", androidId).apply()
            return androidId
        }
        var userId = prefs.getString("userid", null)
        if (userId.isNullOrEmpty()) {
            userId = "dev_" + java.util.UUID.randomUUID().toString().take(8)
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

    fun isUserConfirmedPremiumLocally(): Boolean {
        val ctx = context ?: return false
        val prefs = ctx.getSharedPreferences("app_user_prefs", android.content.Context.MODE_PRIVATE)
        return prefs.getBoolean("confirmed_premium", false) || prefs.getBoolean("premium_from_referrer", false)
    }

    fun markPremiumFromReferrer() {
        val ctx = context ?: return
        val prefs = ctx.getSharedPreferences("app_user_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("premium_from_referrer", true)
            .putBoolean("confirmed_premium", true)
            .putLong("last_premium_check_time", System.currentTimeMillis())
            .putBoolean("status_synced_after_install", true)
            .apply()
    }

    suspend fun syncUserUniqueIdAndPremiumStatus(forceCheck: Boolean = false) {
        val ctx = context ?: return
        userSyncMutex.withLock {
            val prefs = ctx.getSharedPreferences("app_user_prefs", android.content.Context.MODE_PRIVATE)
            val userId = getOrCreateUserUniqueId()
            val currentProfile = profileState.first() ?: ProfileState(isPremium = false).also { saveProfile(it) }

            val lastPremiumCheck = prefs.getLong("last_premium_check_time", 0L)
            val confirmedPremium = prefs.getBoolean("confirmed_premium", false) || prefs.getBoolean("premium_from_referrer", false)
            val now = System.currentTimeMillis()
            val tenDaysMs = 10L * 24 * 60 * 60 * 1000

            // If user is already verified Premium within last 10 days, ensure local profile matches and skip network call
            if ((currentProfile.isPremium || confirmedPremium) && lastPremiumCheck > 0L && (now - lastPremiumCheck) < tenDaysMs && !forceCheck) {
                if (!currentProfile.isPremium) {
                    saveProfile(currentProfile.copy(isPremium = true))
                }
                hasCheckedRemoteUserStatusThisSession = true
                return@withLock
            }

            val isSynced = prefs.getBoolean("status_synced_after_install", false)
            if (isSynced && !currentProfile.isPremium && !confirmedPremium && !forceCheck) {
                // Non-premium user already synced after install, skip network check
                hasCheckedRemoteUserStatusThisSession = true
                return@withLock
            }

            // Query Cloudflare once by ANDROID_ID
            val remoteStatus = fetchUserData(userId)
            hasCheckedRemoteUserStatusThisSession = true

            if (remoteStatus.equals("premium", ignoreCase = true)) {
                if (!currentProfile.isPremium) {
                    saveProfile(currentProfile.copy(isPremium = true))
                }
                prefs.edit()
                    .putBoolean("confirmed_premium", true)
                    .putLong("last_premium_check_time", now)
                    .putBoolean("status_synced_after_install", true)
                    .apply()
            } else if (confirmedPremium || prefs.getBoolean("premium_from_referrer", false)) {
                if (!currentProfile.isPremium) {
                    saveProfile(currentProfile.copy(isPremium = true))
                }
                registerOrUpdateUserData(userId, true)
                prefs.edit()
                    .putBoolean("confirmed_premium", true)
                    .putLong("last_premium_check_time", now)
                    .putBoolean("status_synced_after_install", true)
                    .apply()
            } else {
                if (currentProfile.isPremium) {
                    saveProfile(currentProfile.copy(isPremium = false))
                }
                if (remoteStatus == null) {
                    registerOrUpdateUserData(userId, false)
                }
                prefs.edit()
                    .putBoolean("confirmed_premium", false)
                    .putBoolean("status_synced_after_install", true)
                    .apply()
            }
        }
    }

    suspend fun restorePremiumStatusByDeviceId(): Boolean {
        val userId = getOrCreateUserUniqueId()
        val remoteStatus = fetchUserData(userId)
        hasCheckedRemoteUserStatusThisSession = true
        val currentProfile = profileState.first() ?: ProfileState(isPremium = false)
        val now = System.currentTimeMillis()
        if (remoteStatus.equals("premium", ignoreCase = true)) {
            if (!currentProfile.isPremium) {
                saveProfile(currentProfile.copy(isPremium = true))
            }
            context?.let { ctx ->
                val prefs = ctx.getSharedPreferences("app_user_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("confirmed_premium", true)
                    .putLong("last_premium_check_time", now)
                    .putBoolean("status_synced_after_install", true)
                    .apply()
            }
            return true
        } else if (isUserConfirmedPremiumLocally()) {
            if (!currentProfile.isPremium) {
                saveProfile(currentProfile.copy(isPremium = true))
            }
            registerOrUpdateUserData(userId, true)
            context?.let { ctx ->
                val prefs = ctx.getSharedPreferences("app_user_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("confirmed_premium", true)
                    .putLong("last_premium_check_time", now)
                    .putBoolean("status_synced_after_install", true)
                    .apply()
            }
            return true
        }
        return false
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

    // Ensure a local ProfileState row exists (without triggering any duplicate network sync)
    suspend fun prepopulateDatabase() = withContext(Dispatchers.IO) {
        val currentProfileCount = appDao.getProfileCount()
        if (currentProfileCount == 0) {
            appDao.insertProfileState(ProfileState(isPremium = false))
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

        val extraComicsWithChapters = try {
            val userComicsJson = context?.assets?.open("user_comics.json")?.bufferedReader()?.use { it.readText() }
            if (!userComicsJson.isNullOrBlank()) {
                parseComicsJson(userComicsJson)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }

        // Save extra comics & chapters to local Room DB
        extraComicsWithChapters.forEach { (comic, chapters) ->
            appDao.insertComics(listOf(comic))
            if (chapters.isNotEmpty()) {
                appDao.insertComicChapters(chapters)
            }
        }

        val comicsWithChapters = (sampleComics.map { comic ->
            val chapters = sampleChapters.filter { it.comicId == comic.id }
            Pair(comic, chapters)
        } + extraComicsWithChapters)

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

    fun parseComicsJson(jsonString: String): List<Pair<com.example.data.model.Comic, List<com.example.data.model.ComicChapter>>> {
        val result = mutableListOf<Pair<com.example.data.model.Comic, List<com.example.data.model.ComicChapter>>>()
        try {
            val rootArray = org.json.JSONArray(jsonString)
            var comicIdCounter = 1000
            var chapterIdCounter = 5000

            for (i in 0 until rootArray.length()) {
                val catObj = rootArray.optJSONObject(i) ?: continue
                val category = catObj.optString("category", "General")
                val comicsArray = catObj.optJSONArray("comics") ?: continue

                for (j in 0 until comicsArray.length()) {
                    val comicObj = comicsArray.optJSONObject(j) ?: continue
                    val rawName = comicObj.optString("name", "Untitled Comic")
                    val displayTitle = rawName.replace("-", " ")
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .joinToString(" ") { word -> word.replaceFirstChar { char -> char.uppercase() } }

                    val partsArray = comicObj.optJSONArray("parts")
                    val chapters = mutableListOf<com.example.data.model.ComicChapter>()
                    var coverImage = ""

                    if (partsArray != null) {
                        for (k in 0 until partsArray.length()) {
                            val partObj = partsArray.optJSONObject(k) ?: continue
                            val partNum = partObj.optInt("part", k + 1)
                            val title = partObj.optString("title", "Part $partNum")
                            val urlArray = partObj.optJSONArray("urlarray")
                            val pagesList = mutableListOf<String>()

                            if (urlArray != null) {
                                for (u in 0 until urlArray.length()) {
                                    val url = urlArray.optString(u, "")
                                    if (url.isNotEmpty()) {
                                        pagesList.add(url)
                                    }
                                }
                            }

                            val pagesCsv = pagesList.joinToString(",")
                            if (coverImage.isEmpty() && pagesList.isNotEmpty()) {
                                coverImage = pagesList.first()
                            }

                            chapterIdCounter++
                            chapters.add(
                                com.example.data.model.ComicChapter(
                                    id = chapterIdCounter,
                                    comicId = 0,
                                    chapterNumber = partNum,
                                    title = title,
                                    pagesCsv = pagesCsv,
                                    pdfUrl = ""
                                )
                            )
                        }
                    }

                    comicIdCounter++
                    if (coverImage.isEmpty()) {
                        coverImage = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=800&q=80"
                    }

                    val comic = com.example.data.model.Comic(
                        id = comicIdCounter,
                        title = displayTitle,
                        category = category,
                        imageUrl = coverImage,
                        description = "$displayTitle ($category) graphic novel series.",
                        likesCount = "${(12..88).random()}k",
                        isPremium = false
                    )

                    val updatedChapters = chapters.map { it.copy(comicId = comic.id) }
                    result.add(Pair(comic, updatedChapters))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Error parsing comics JSON: ${e.message}", e)
        }
        return result
    }

    suspend fun importUserComicsJsonAndUpload(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        val parsed = parseComicsJson(jsonString)
        if (parsed.isEmpty()) return@withContext false

        val comics = parsed.map { it.first }
        val chapters = parsed.flatMap { it.second }

        appDao.insertComics(comics)
        if (chapters.isNotEmpty()) {
            appDao.insertComicChapters(chapters)
        }

        val service = firebaseService ?: return@withContext true
        return@withContext service.uploadComicsWithChapters(parsed)
    }

    suspend fun syncBatchVideos(): Boolean = withContext(Dispatchers.IO) {
        var success = false

        // 1. Try Cloudflare
        try {
            val cfBatches = cloudflareService?.fetchBatchVideosFromCloudflare()
            if (!cfBatches.isNullOrEmpty()) {
                val batches = cfBatches.map { it.first }
                val items = cfBatches.flatMap { it.second }
                appDao.deleteAllBatchVideos()
                appDao.insertBatchVideos(batches)
                appDao.deleteAllBatchVideoItems()
                if (items.isNotEmpty()) {
                    appDao.insertBatchVideoItems(items)
                }
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Try Firestore fallback
        try {
            val fsBatches = firebaseService?.fetchBatchVideosFromFirestore()
            if (!fsBatches.isNullOrEmpty()) {
                val batches = fsBatches.map { it.first }
                val items = fsBatches.flatMap { it.second }
                appDao.deleteAllBatchVideos()
                appDao.insertBatchVideos(batches)
                appDao.deleteAllBatchVideoItems()
                if (items.isNotEmpty()) {
                    appDao.insertBatchVideoItems(items)
                }
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. If local database has no batches yet, seed realistic starter series
        val localCount = appDao.getBatchVideosCount()
        if (localCount == 0) {
            seedSampleBatchVideos()
            success = true
        }

        success
    }

    suspend fun seedSampleBatchVideos() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val sampleBatches = listOf(
            BatchVideo(
                id = 1,
                batchId = "arkestra_dance_batch_01",
                title = "Arkestra dance",
                description = "Exclusive live stage Arkestra dance performances and high-energy musical events.",
                imageUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=600&q=80",
                category = "Stage Dance",
                isPremium = true,
                totalVideos = 45,
                releaseDate = now - 86400000L * 1
            ),
            BatchVideo(
                id = 2,
                batchId = "batch_series_01",
                title = "Midnight Shadows: The Syndicate",
                description = "An undercover agent gets embroiled in a clandestine web of deceit, betrayal, and high-octane confrontations across the city underbelly.",
                imageUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?auto=format&fit=crop&w=800&q=80",
                category = "Crime Thriller",
                isPremium = true,
                totalVideos = 6,
                releaseDate = now - 86400000L * 3
            ),
            BatchVideo(
                id = 3,
                batchId = "batch_series_02",
                title = "Secret Obsession: Pure Desire",
                description = "A forbidden love affair sets off unexpected emotional sparks that test loyalties and unravel long-held family secrets in luxury estates.",
                imageUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=800&q=80",
                category = "Romance Specials",
                isPremium = true,
                totalVideos = 5,
                releaseDate = now - 86400000L * 7
            ),
            BatchVideo(
                id = 4,
                batchId = "batch_series_03",
                title = "Cybernetic Horizon 2099",
                description = "In a futuristic neon metropolis, rogue hackers race against an artificial intelligence supergrid to preserve the last free sanctuary.",
                imageUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=800&q=80",
                category = "Action & Sci-Fi",
                isPremium = true,
                totalVideos = 6,
                releaseDate = now - 86400000L * 12
            ),
            BatchVideo(
                id = 5,
                batchId = "batch_series_04",
                title = "Campus Diaries: Young & Wild",
                description = "Excitement, teenage drama, college rivalry, and spontaneous road trips collide in this heartwarming coming-of-age youth series.",
                imageUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=800&q=80",
                category = "Youth & Drama",
                isPremium = true,
                totalVideos = 5,
                releaseDate = now - 86400000L * 18
            )
        )

        val sampleUrls = com.example.util.VideoUrlSanitizer.RELIABLE_STREAMS

        val samplePosters = listOf(
            "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?auto=format&fit=crop&w=600&q=80"
        )

        val titlesPool = listOf(
            "Arkestra Dance Performance",
            "Arkestra Dance Stage Show",
            "Arkestra Folk Dance Episode",
            "Arkestra Live Stage Drama",
            "Night Stage Celebration Dance",
            "Arkestra Superhit Finale",
            "Bhojpuri Beats Stage Dhamaka",
            "Electrifying Fusion Folk Dance",
            "Midnight Moonlight Stage Performance",
            "Village Fair Special Gala Dance",
            "High Energy Arkestra Celebration",
            "Sensational Stage Showcase",
            "Enthralling Festive Arkestra",
            "Rhythm & Beats Stage Spectacle",
            "Mega Festival Special Night"
        )

        val arkestraItems = (1..45).map { ep ->
            val titlePrefix = titlesPool[(ep - 1) % titlesPool.size]
            val url = sampleUrls[(ep - 1) % sampleUrls.size]
            val poster = samplePosters[(ep - 1) % samplePosters.size]
            val min = (ep * 3 + 2) % 25
            val sec = (ep * 13) % 60
            val durStr = if (min == 0) "0:${String.format(java.util.Locale.US, "%02d", sec)}" else "$min:${String.format(java.util.Locale.US, "%02d", sec)}"
            val sizeMb = Math.round((1.2 + ((ep * 4.7) % 68.0)) * 10.0) / 10.0
            val uploadTime = now - (45 - ep) * 3600000L * 14L
            BatchVideoItem(
                batchId = "arkestra_dance_batch_01",
                videoIndex = ep,
                title = "$titlePrefix Part $ep",
                videoUrl = url,
                imageUrl = poster,
                duration = durStr,
                description = "$sizeMb MB",
                uploadTime = uploadTime,
                durationSec = min * 60 + sec,
                sizeMb = sizeMb
            )
        }

        val sampleItems = mutableListOf<BatchVideoItem>()
        sampleItems.addAll(arkestraItems)

        sampleBatches.filter { it.batchId != "arkestra_dance_batch_01" }.forEach { batch ->
            for (ep in 1..batch.totalVideos) {
                val epUrl = sampleUrls[(ep - 1) % sampleUrls.size]
                val epDurationSec = (12 + ep * 4) * 60
                val epSizeMb = 15.0 + ep * 8.5
                val epUploadTime = batch.releaseDate + (ep * 3600000L * 6)
                sampleItems.add(
                    BatchVideoItem(
                        batchId = batch.batchId,
                        videoIndex = ep,
                        title = "${batch.title} - Episode $ep: ${when (ep) {
                            1 -> "The First Encounter"
                            2 -> "Rising Heat"
                            3 -> "Crossroads"
                            4 -> "Tangled Desires"
                            5 -> "The Climax"
                            else -> "Grand Finale"
                        }}",
                        videoUrl = epUrl,
                        imageUrl = batch.imageUrl,
                        duration = "${12 + ep * 4} min",
                        description = "${String.format(java.util.Locale.US, "%.1f", epSizeMb)} MB",
                        uploadTime = epUploadTime,
                        durationSec = epDurationSec,
                        sizeMb = epSizeMb
                    )
                )
            }
        }

        appDao.insertBatchVideos(sampleBatches)
        appDao.insertBatchVideoItems(sampleItems)
    }

}
