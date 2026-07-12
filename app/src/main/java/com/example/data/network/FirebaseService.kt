package com.example.data.network

import android.content.Context
import com.example.data.model.Channel
import com.example.data.model.MediaItem
import com.example.data.model.UserReel
import com.example.data.model.UserNotification
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

// --- Firestore REST API Network Models ---

@JsonClass(generateAdapter = true)
data class FirestoreValue(
    val stringValue: String? = null,
    val integerValue: String? = null,
    val doubleValue: Double? = null,
    val booleanValue: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class FirestoreDocumentDetail(
    val name: String? = null,
    val fields: Map<String, FirestoreValue>? = null
)

@JsonClass(generateAdapter = true)
data class FirestoreDocumentListResponse(
    val documents: List<FirestoreDocumentDetail>? = null
)

@JsonClass(generateAdapter = true)
data class FirestoreWrite(
    val update: FirestoreDocumentDetail
)

@JsonClass(generateAdapter = true)
data class FirestoreCommitRequest(
    val writes: List<FirestoreWrite>
)

// --- Retrofit Service Interface ---

interface FirebaseApi {
    @GET("v1/projects/{projectId}/databases/(default)/documents/channels")
    suspend fun getChannels(
        @Path("projectId") projectId: String
    ): FirestoreDocumentListResponse?

    @GET("v1/projects/{projectId}/databases/(default)/documents/media_items")
    suspend fun getMediaItems(
        @Path("projectId") projectId: String
    ): FirestoreDocumentListResponse?

    @GET("v1/projects/{projectId}/databases/(default)/documents/reels")
    suspend fun getReels(
        @Path("projectId") projectId: String
    ): FirestoreDocumentListResponse?

    @GET("v1/projects/{projectId}/databases/(default)/documents/version")
    suspend fun getVersionCollection(
        @Path("projectId") projectId: String
    ): FirestoreDocumentListResponse?

    @GET("v1/projects/{projectId}/databases/(default)/documents/userdata/{userId}")
    suspend fun getUserDataDocument(
        @Path("projectId") projectId: String,
        @Path("userId") userId: String
    ): FirestoreDocumentDetail?

    @GET("v1/projects/{projectId}/databases/(default)/documents/userdata")
    suspend fun getUserDataCollection(
        @Path("projectId") projectId: String
    ): FirestoreDocumentListResponse?

    @GET("v1/projects/{projectId}/databases/(default)/documents/fcm_history")
    suspend fun getFcmHistory(
        @Path("projectId") projectId: String
    ): FirestoreDocumentListResponse?

    @GET("v1/projects/{projectId}/databases/(default)/documents/categories")
    suspend fun getCategories(
        @Path("projectId") projectId: String
    ): FirestoreDocumentListResponse?

    @GET("v1/projects/{projectId}/databases/(default)/documents/video_categories")
    suspend fun getVideoCategories(
        @Path("projectId") projectId: String
    ): FirestoreDocumentListResponse?

    @GET("v1/projects/{projectId}/databases/(default)/documents/comics")
    suspend fun getComics(
        @Path("projectId") projectId: String
    ): FirestoreDocumentListResponse?

    @GET("v1/projects/{projectId}/databases/(default)/documents/stories")
    suspend fun getStories(
        @Path("projectId") projectId: String
    ): FirestoreDocumentListResponse?

    @GET("v1/projects/{projectId}/databases/(default)/documents/notifications")
    suspend fun getNotifications(
        @Path("projectId") projectId: String
    ): FirestoreDocumentListResponse?

    @GET("v1/projects/{projectId}/databases/(default)/documents/audio_batches")
    suspend fun getAudioBatches(
        @Path("projectId") projectId: String
    ): FirestoreDocumentListResponse?

    @POST("v1/projects/{projectId}/databases/(default)/documents:commit")
    suspend fun commit(
        @Path("projectId") projectId: String,
        @Body request: FirestoreCommitRequest
    )
}

@JsonClass(generateAdapter = true)
data class AppVersionDetails(
    val version: Int = 1,
    val url: String = "",
    val days: Int? = null,
    val instructionsText: String? = null,
    val instructionsUrl: String? = null
)

// --- Firestore Business Service ---

class FirebaseService(val context: Context) {

    var lastError: String? = null
        private set

    fun clearLastError() {
        lastError = null
    }

    private fun handleError(e: Exception) {
        e.printStackTrace()
        val errorMsg = when (e) {
            is retrofit2.HttpException -> {
                val code = e.code()
                val body = e.response()?.errorBody()?.string() ?: ""
                "HTTP $code: ${e.message()}. Response details: $body"
            }
            is java.io.IOException -> {
                "Network Error (Timeout/Unreachable): ${e.message}"
            }
            else -> {
                e.localizedMessage ?: e.message ?: "Unknown local execution error"
            }
        }
        lastError = errorMsg
    }

    companion object {
        // Production Cloud Firestore Project ID of OTT Provider
        const val DEFAULT_PROJECT_ID = "studio-1311116373-71cd3"
        const val BASE_URL = "https://firestore.googleapis.com/"
    }

    private val prefs = context.getSharedPreferences("firebase_prefs", Context.MODE_PRIVATE)

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val mediaListAdapter by lazy {
        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, MediaItem::class.java)
        moshi.adapter<List<MediaItem>>(type)
    }

    private val comicChapterListAdapter by lazy {
        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.data.model.ComicChapter::class.java)
        moshi.adapter<List<com.example.data.model.ComicChapter>>(type)
    }

    private val storyPartListAdapter by lazy {
        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.data.model.StoryPart::class.java)
        moshi.adapter<List<com.example.data.model.StoryPart>>(type)
    }

    private val userReelListAdapter by lazy {
        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, UserReel::class.java)
        moshi.adapter<List<UserReel>>(type)
    }

    /**
     * Gets the Cloud Firestore Project ID.
     */
    fun getFirebaseUrl(): String {
        return prefs.getString("project_id", DEFAULT_PROJECT_ID) ?: DEFAULT_PROJECT_ID
    }

    /**
     * Saves the Cloud Firestore Project ID.
     */
    fun saveFirebaseUrl(url: String) {
        prefs.edit().putString("project_id", url.trim()).apply()
    }

    fun isUsingDefaultUrl(): Boolean {
        return getFirebaseUrl() == DEFAULT_PROJECT_ID
    }

    fun resetToDefault() {
        prefs.edit().remove("project_id").apply()
    }

    private fun getRetrofit(): Retrofit? {
        return try {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()

            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getApi(): FirebaseApi? {
        return getRetrofit()?.create(FirebaseApi::class.java)
    }

    // --- High-Fidelity Converter Utilities ---

    private fun mapToChannel(doc: FirestoreDocumentDetail): Channel? {
        val fields = doc.fields ?: return null
        val id = fields["id"]?.integerValue?.toIntOrNull() 
            ?: fields["id"]?.stringValue?.toIntOrNull() 
            ?: 0
        val name = fields["name"]?.stringValue ?: ""
        val imageUrl = fields["imageUrl"]?.stringValue ?: ""
        return Channel(id = id, name = name, imageUrl = imageUrl)
    }

    private fun mapToMediaItem(doc: FirestoreDocumentDetail): MediaItem? {
        val fields = doc.fields ?: return null
        val id = fields["id"]?.integerValue?.toIntOrNull() 
            ?: fields["id"]?.stringValue?.toIntOrNull() 
            ?: 0
        val title = fields["title"]?.stringValue ?: ""
        val description = fields["description"]?.stringValue ?: ""
        val imageUrl = fields["imageUrl"]?.stringValue ?: ""
        val category = fields["category"]?.stringValue ?: ""
        val isPremium = fields["isPremium"]?.booleanValue 
            ?: fields["isPremium"]?.stringValue?.toBoolean() 
            ?: false
        val rating = fields["rating"]?.doubleValue?.toFloat()
            ?: fields["rating"]?.integerValue?.toFloat()
            ?: fields["rating"]?.stringValue?.toFloatOrNull()
            ?: 4.5f
        val reelsUrl = fields["reelsUrl"]?.stringValue ?: ""
        val likesCount = fields["likesCount"]?.stringValue ?: "1.2k"

        return MediaItem(
            id = id,
            title = title,
            description = description,
            imageUrl = imageUrl,
            category = category,
            isPremium = isPremium,
            rating = rating,
            reelsUrl = reelsUrl,
            likesCount = likesCount
        )
    }

    private fun buildFirestoreChannelDoc(projectId: String, channel: Channel, indexId: Int): FirestoreDocumentDetail {
        val finalId = if (channel.id > 0) channel.id else indexId
        val fields = mapOf(
            "id" to FirestoreValue(integerValue = finalId.toString()),
            "name" to FirestoreValue(stringValue = channel.name),
            "imageUrl" to FirestoreValue(stringValue = channel.imageUrl)
        )
        return FirestoreDocumentDetail(
            name = "projects/$projectId/databases/(default)/documents/channels/$finalId",
            fields = fields
        )
    }

    private fun buildFirestoreMediaDoc(projectId: String, item: MediaItem, indexId: Int): FirestoreDocumentDetail {
        val finalId = if (item.id > 0) item.id else indexId
        val fields = mapOf(
            "id" to FirestoreValue(integerValue = finalId.toString()),
            "title" to FirestoreValue(stringValue = item.title),
            "description" to FirestoreValue(stringValue = item.description),
            "imageUrl" to FirestoreValue(stringValue = item.imageUrl),
            "category" to FirestoreValue(stringValue = item.category),
            "isPremium" to FirestoreValue(booleanValue = item.isPremium),
            "rating" to FirestoreValue(doubleValue = item.rating.toDouble()),
            "reelsUrl" to FirestoreValue(stringValue = item.reelsUrl),
            "likesCount" to FirestoreValue(stringValue = item.likesCount)
        )
        return FirestoreDocumentDetail(
            name = "projects/$projectId/databases/(default)/documents/media_items/$finalId",
            fields = fields
        )
    }

    private fun buildFirestoreComicDoc(
        projectId: String, 
        item: com.example.data.model.Comic, 
        indexId: Int,
        chapters: List<com.example.data.model.ComicChapter> = emptyList()
    ): FirestoreDocumentDetail {
        val finalId = if (item.id > 0) item.id else indexId
        val chaptersJson = try {
            comicChapterListAdapter.toJson(chapters)
        } catch (e: Exception) {
            "[]"
        }
        val fields = mapOf(
            "id" to FirestoreValue(integerValue = finalId.toString()),
            "title" to FirestoreValue(stringValue = item.title),
            "category" to FirestoreValue(stringValue = item.category),
            "imageUrl" to FirestoreValue(stringValue = item.imageUrl),
            "description" to FirestoreValue(stringValue = item.description),
            "likesCount" to FirestoreValue(stringValue = item.likesCount),
            "isPremium" to FirestoreValue(booleanValue = item.isPremium),
            "chaptersJson" to FirestoreValue(stringValue = chaptersJson)
        )
        return FirestoreDocumentDetail(
            name = "projects/$projectId/databases/(default)/documents/comics/$finalId",
            fields = fields
        )
    }

    private fun mapToComicWithChapters(doc: FirestoreDocumentDetail): Pair<com.example.data.model.Comic, List<com.example.data.model.ComicChapter>>? {
        val fields = doc.fields ?: return null
        val id = fields["id"]?.integerValue?.toIntOrNull() 
            ?: fields["id"]?.stringValue?.toIntOrNull() 
            ?: 0
        val title = fields["title"]?.stringValue ?: ""
        val category = fields["category"]?.stringValue ?: ""
        val imageUrl = fields["imageUrl"]?.stringValue ?: ""
        val description = fields["description"]?.stringValue ?: ""
        val likesCount = fields["likesCount"]?.stringValue ?: "12k"
        val isPremium = fields["isPremium"]?.booleanValue ?: false
        val comic = com.example.data.model.Comic(
            id = id,
            title = title,
            category = category,
            imageUrl = imageUrl,
            description = description,
            likesCount = likesCount,
            isPremium = isPremium
        )

        val chaptersJson = fields["chaptersJson"]?.stringValue ?: "[]"
        val chapters = try {
            comicChapterListAdapter.fromJson(chaptersJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        return Pair(comic, chapters)
    }

    private fun buildFirestoreComicChapterDoc(projectId: String, item: com.example.data.model.ComicChapter, indexId: Int): FirestoreDocumentDetail {
        val finalId = if (item.id > 0) item.id else indexId
        val fields = mapOf(
            "id" to FirestoreValue(integerValue = finalId.toString()),
            "comicId" to FirestoreValue(integerValue = item.comicId.toString()),
            "chapterNumber" to FirestoreValue(integerValue = item.chapterNumber.toString()),
            "title" to FirestoreValue(stringValue = item.title),
            "pagesCsv" to FirestoreValue(stringValue = item.pagesCsv)
        )
        return FirestoreDocumentDetail(
            name = "projects/$projectId/databases/(default)/documents/comic_chapters/$finalId",
            fields = fields
        )
    }

    private fun buildFirestoreCategoryDoc(
        projectId: String, 
        item: com.example.data.model.Category, 
        indexId: Int, 
        isVideoCollection: Boolean = false,
        videos: List<MediaItem> = emptyList()
    ): FirestoreDocumentDetail {
        val finalId = if (item.id > 0) item.id else indexId
        val videosJson = try {
            mediaListAdapter.toJson(videos)
        } catch (e: Exception) {
            "[]"
        }
        val fields = mapOf(
            "id" to FirestoreValue(integerValue = finalId.toString()),
            "name" to FirestoreValue(stringValue = item.name),
            "isPortrait" to FirestoreValue(booleanValue = item.isPortrait),
            "type" to FirestoreValue(stringValue = item.type),
            "videosJson" to FirestoreValue(stringValue = videosJson)
        )
        val collection = if (isVideoCollection) "video_categories" else "categories"
        return FirestoreDocumentDetail(
            name = "projects/$projectId/databases/(default)/documents/$collection/$finalId",
            fields = fields
        )
    }

    private fun mapToCategoryWithVideos(doc: FirestoreDocumentDetail, defaultType: String): Pair<com.example.data.model.Category, List<MediaItem>>? {
        val fields = doc.fields ?: return null
        val id = fields["id"]?.integerValue?.toIntOrNull() 
            ?: fields["id"]?.stringValue?.toIntOrNull() 
            ?: 0
        val name = fields["name"]?.stringValue ?: ""
        val isPortrait = fields["isPortrait"]?.booleanValue ?: false
        val type = fields["type"]?.stringValue ?: defaultType
        val category = com.example.data.model.Category(id = id, name = name, isPortrait = isPortrait, type = type)

        val videosJson = fields["videosJson"]?.stringValue ?: "[]"
        val videos = try {
            mediaListAdapter.fromJson(videosJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        return Pair(category, videos)
    }

    private fun buildFirestoreStoryDoc(
        projectId: String, 
        item: com.example.data.model.Story, 
        indexId: Int,
        parts: List<com.example.data.model.StoryPart> = emptyList()
    ): FirestoreDocumentDetail {
        val finalId = if (item.id > 0) item.id else indexId
        val partsJson = try {
            storyPartListAdapter.toJson(parts)
        } catch (e: Exception) {
            "[]"
        }
        val fields = mapOf(
            "id" to FirestoreValue(integerValue = finalId.toString()),
            "title" to FirestoreValue(stringValue = item.title),
            "imageUrl" to FirestoreValue(stringValue = item.imageUrl),
            "content" to FirestoreValue(stringValue = item.content),
            "category" to FirestoreValue(stringValue = item.category),
            "isPremium" to FirestoreValue(booleanValue = item.isPremium),
            "partsJson" to FirestoreValue(stringValue = partsJson),
            "audioUrl" to FirestoreValue(stringValue = item.audioUrl),
            "duration" to FirestoreValue(stringValue = item.duration),
            "releaseDate" to FirestoreValue(integerValue = item.releaseDate.toString())
        )
        return FirestoreDocumentDetail(
            name = "projects/$projectId/databases/(default)/documents/stories/$finalId",
            fields = fields
        )
    }

    private fun mapToStoryWithParts(doc: FirestoreDocumentDetail): Pair<com.example.data.model.Story, List<com.example.data.model.StoryPart>>? {
        val fields = doc.fields ?: return null
        val id = fields["id"]?.integerValue?.toIntOrNull() 
            ?: fields["id"]?.stringValue?.toIntOrNull() 
            ?: 0
        val title = fields["title"]?.stringValue ?: ""
        val imageUrl = fields["imageUrl"]?.stringValue ?: ""
        val content = fields["content"]?.stringValue ?: ""
        val category = fields["category"]?.stringValue ?: "Fantasy"
        val isPremium = fields["isPremium"]?.booleanValue ?: false
        val audioUrl = fields["audioUrl"]?.stringValue ?: ""
        val duration = fields["duration"]?.stringValue ?: ""
        val releaseDate = fields["releaseDate"]?.integerValue?.toLongOrNull()
            ?: fields["releaseDate"]?.stringValue?.toLongOrNull()
            ?: System.currentTimeMillis()

        val story = com.example.data.model.Story(
            id = id,
            title = title,
            imageUrl = imageUrl,
            content = content,
            category = category,
            isPremium = isPremium,
            audioUrl = audioUrl,
            duration = duration,
            releaseDate = releaseDate
        )

        val partsJson = fields["partsJson"]?.stringValue ?: "[]"
        val parts = try {
            storyPartListAdapter.fromJson(partsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        return Pair(story, parts)
    }

    suspend fun uploadComics(comics: List<com.example.data.model.Comic>): Boolean {
        val mapped = comics.map { Pair(it, emptyList<com.example.data.model.ComicChapter>()) }
        return uploadComicsWithChapters(mapped)
    }

    suspend fun uploadComicsWithChapters(comicsWithChapters: List<Pair<com.example.data.model.Comic, List<com.example.data.model.ComicChapter>>>): Boolean {
        clearLastError()
        val api = getApi() ?: return false
        val projectId = getFirebaseUrl()
        if (comicsWithChapters.isEmpty()) return true
        return try {
            val writes = comicsWithChapters.mapIndexed { index, pair ->
                val item = pair.first
                val chapters = pair.second
                FirestoreWrite(update = buildFirestoreComicDoc(projectId, item, index + 1, chapters))
            }
            api.commit(projectId, FirestoreCommitRequest(writes))
            true
        } catch (e: Exception) {
            handleError(e)
            false
        }
    }

    suspend fun uploadComicChapters(chapters: List<com.example.data.model.ComicChapter>): Boolean {
        clearLastError()
        val api = getApi() ?: return false
        val projectId = getFirebaseUrl()
        if (chapters.isEmpty()) return true
        return try {
            val writes = chapters.mapIndexed { index, item ->
                FirestoreWrite(update = buildFirestoreComicChapterDoc(projectId, item, index + 1))
            }
            api.commit(projectId, FirestoreCommitRequest(writes))
            true
        } catch (e: Exception) {
            handleError(e)
            false
        }
    }

    suspend fun uploadCategories(categories: List<com.example.data.model.Category>): Boolean {
        val mapped = categories.map { Pair(it, emptyList<MediaItem>()) }
        return uploadCategoriesWithVideos(mapped)
    }

    suspend fun uploadCategoriesWithVideos(categoriesWithVideos: List<Pair<com.example.data.model.Category, List<MediaItem>>>): Boolean {
        clearLastError()
        val api = getApi() ?: return false
        val projectId = getFirebaseUrl()
        if (categoriesWithVideos.isEmpty()) return true
        return try {
            val videoCats = categoriesWithVideos.filter { it.first.type == "video" }
            val comicCats = categoriesWithVideos.filter { it.first.type == "comic" }

            val writes = mutableListOf<FirestoreWrite>()
            videoCats.forEachIndexed { index, pair ->
                val item = pair.first
                val finalId = if (item.id > 0) item.id else (index + 1)
                writes.add(FirestoreWrite(update = buildFirestoreCategoryDoc(projectId, item, finalId, isVideoCollection = true, videos = pair.second)))
            }
            comicCats.forEachIndexed { index, pair ->
                val item = pair.first
                val finalId = if (item.id > 0) item.id else (index + 10)
                writes.add(FirestoreWrite(update = buildFirestoreCategoryDoc(projectId, item, finalId, isVideoCollection = false, videos = pair.second)))
            }

            if (writes.isNotEmpty()) {
                api.commit(projectId, FirestoreCommitRequest(writes))
            }
            true
        } catch (e: Exception) {
            handleError(e)
            false
        }
    }

    suspend fun fetchCategories(): List<Pair<com.example.data.model.Category, List<MediaItem>>>? {
        clearLastError()
        val api = getApi() ?: return null
        val projectId = getFirebaseUrl()
        return try {
            val response = api.getCategories(projectId)
            response?.documents?.mapNotNull { mapToCategoryWithVideos(it, defaultType = "comic") } ?: emptyList()
        } catch (e: Exception) {
            handleError(e)
            null
        }
    }

    suspend fun fetchVideoCategories(): List<Pair<com.example.data.model.Category, List<MediaItem>>>? {
        clearLastError()
        val api = getApi() ?: return null
        val projectId = getFirebaseUrl()
        return try {
            val response = api.getVideoCategories(projectId)
            response?.documents?.mapNotNull { mapToCategoryWithVideos(it, defaultType = "video") } ?: emptyList()
        } catch (e: Exception) {
            handleError(e)
            null
        }
    }

    suspend fun uploadStories(stories: List<com.example.data.model.Story>): Boolean {
        val mapped = stories.map { Pair(it, emptyList<com.example.data.model.StoryPart>()) }
        return uploadStoriesWithParts(mapped)
    }

    suspend fun uploadStoriesWithParts(storiesWithParts: List<Pair<com.example.data.model.Story, List<com.example.data.model.StoryPart>>>): Boolean {
        clearLastError()
        val api = getApi() ?: return false
        val projectId = getFirebaseUrl()
        if (storiesWithParts.isEmpty()) return true
        return try {
            val writes = storiesWithParts.mapIndexed { index, pair ->
                val item = pair.first
                val parts = pair.second
                FirestoreWrite(update = buildFirestoreStoryDoc(projectId, item, index + 1, parts))
            }
            api.commit(projectId, FirestoreCommitRequest(writes))
            true
        } catch (e: Exception) {
            handleError(e)
            false
        }
    }

    suspend fun fetchComics(): List<Pair<com.example.data.model.Comic, List<com.example.data.model.ComicChapter>>>? {
        clearLastError()
        val api = getApi() ?: return null
        val projectId = getFirebaseUrl()
        return try {
            val response = api.getComics(projectId)
            response?.documents?.mapNotNull { mapToComicWithChapters(it) } ?: emptyList()
        } catch (e: Exception) {
            handleError(e)
            null
        }
    }

    suspend fun fetchStories(): List<Pair<com.example.data.model.Story, List<com.example.data.model.StoryPart>>>? {
        clearLastError()
        val api = getApi() ?: return null
        val projectId = getFirebaseUrl()
        return try {
            val response = api.getStories(projectId)
            response?.documents?.mapNotNull { mapToStoryWithParts(it) } ?: emptyList()
        } catch (e: Exception) {
            handleError(e)
            null
        }
    }

    suspend fun uploadAudioBatches(batches: Map<String, List<com.example.data.model.Story>>): Boolean {
        clearLastError()
        val api = getApi() ?: return false
        val projectId = getFirebaseUrl()
        if (batches.isEmpty()) return true
        return try {
            val writes = batches.map { (batchName, list) ->
                val audiosJson = try {
                    val moshi = com.squareup.moshi.Moshi.Builder()
                        .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                        .build()
                    val adapter = moshi.adapter<List<com.example.data.model.Story>>(
                        com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.data.model.Story::class.java)
                    )
                    adapter.toJson(list)
                } catch (e: Exception) {
                    "[]"
                }

                val fields = mapOf(
                    "batchName" to FirestoreValue(stringValue = batchName),
                    "audiosJson" to FirestoreValue(stringValue = audiosJson)
                )

                FirestoreWrite(
                    update = FirestoreDocumentDetail(
                        name = "projects/$projectId/databases/(default)/documents/audio_batches/$batchName",
                        fields = fields
                    )
                )
            }
            api.commit(projectId, FirestoreCommitRequest(writes))
            true
        } catch (e: Exception) {
            handleError(e)
            false
        }
    }

    suspend fun fetchAudioBatches(): List<com.example.data.model.Story>? {
        clearLastError()
        val api = getApi() ?: return null
        val projectId = getFirebaseUrl()
        return try {
            val response = api.getAudioBatches(projectId)
            val allAudios = mutableListOf<com.example.data.model.Story>()
            response?.documents?.forEach { doc ->
                val fields = doc.fields ?: return@forEach
                val audiosJson = fields["audiosJson"]?.stringValue ?: "[]"
                try {
                    val moshi = com.squareup.moshi.Moshi.Builder()
                        .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                        .build()
                    val adapter = moshi.adapter<List<com.example.data.model.Story>>(
                        com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.data.model.Story::class.java)
                    )
                    val list = adapter.fromJson(audiosJson)
                    if (list != null) {
                        allAudios.addAll(list)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            allAudios
        } catch (e: Exception) {
            handleError(e)
            null
        }
    }

    // --- Service API Implementations ---

    suspend fun fetchChannels(): List<Channel>? {
        clearLastError()
        val api = getApi() ?: return null
        val projectId = getFirebaseUrl()
        return try {
            val response = api.getChannels(projectId)
            response?.documents?.mapNotNull { mapToChannel(it) } ?: emptyList()
        } catch (e: Exception) {
            handleError(e)
            null
        }
    }

    suspend fun fetchMediaItems(): List<MediaItem>? {
        clearLastError()
        val api = getApi() ?: return null
        val projectId = getFirebaseUrl()
        return try {
            val response = api.getMediaItems(projectId)
            response?.documents?.mapNotNull { mapToMediaItem(it) } ?: emptyList()
        } catch (e: Exception) {
            handleError(e)
            null
        }
    }

    suspend fun uploadChannels(channels: List<Channel>): Boolean {
        clearLastError()
        val api = getApi() ?: return false
        val projectId = getFirebaseUrl()
        if (channels.isEmpty()) return true
        return try {
            val writes = channels.mapIndexed { index, channel ->
                FirestoreWrite(update = buildFirestoreChannelDoc(projectId, channel, index + 1))
            }
            api.commit(projectId, FirestoreCommitRequest(writes))
            true
        } catch (e: Exception) {
            handleError(e)
            false
        }
    }

    suspend fun uploadMediaItems(items: List<MediaItem>): Boolean {
        clearLastError()
        val api = getApi() ?: return false
        val projectId = getFirebaseUrl()
        if (items.isEmpty()) return true
        return try {
            val writes = items.mapIndexed { index, item ->
                FirestoreWrite(update = buildFirestoreMediaDoc(projectId, item, index + 1))
            }
            api.commit(projectId, FirestoreCommitRequest(writes))
            true
        } catch (e: Exception) {
            handleError(e)
            false
        }
    }

    private fun mapToUserReel(doc: FirestoreDocumentDetail): UserReel? {
        val fields = doc.fields ?: return null
        val id = fields["id"]?.integerValue?.toIntOrNull() 
            ?: fields["id"]?.stringValue?.toIntOrNull() 
            ?: 0
        val title = fields["title"]?.stringValue ?: ""
        val description = fields["description"]?.stringValue ?: ""
        val imageUrl = fields["imageUrl"]?.stringValue ?: ""
        val videoUrl = fields["videoUrl"]?.stringValue 
            ?: fields["reelsUrl"]?.stringValue 
            ?: ""
        val likesCount = fields["likesCount"]?.stringValue ?: "1.2k"
        val isLiked = fields["isLiked"]?.booleanValue ?: false
        val createdAt = fields["createdAt"]?.integerValue?.toLongOrNull() 
            ?: fields["createdAt"]?.stringValue?.toLongOrNull() 
            ?: System.currentTimeMillis()
        val userStatus = fields["userStatus"]?.stringValue ?: "normal"
        return UserReel(
            id = id,
            title = title,
            description = description,
            imageUrl = imageUrl,
            videoUrl = videoUrl,
            likesCount = likesCount,
            isLiked = isLiked,
            createdAt = createdAt,
            userStatus = userStatus
        )
    }

    private fun buildFirestoreUserReelDoc(projectId: String, reel: UserReel, indexId: Int): FirestoreDocumentDetail {
        val finalId = if (reel.id > 0) reel.id else indexId
        val fields = mapOf(
            "id" to FirestoreValue(integerValue = finalId.toString()),
            "title" to FirestoreValue(stringValue = reel.title),
            "description" to FirestoreValue(stringValue = reel.description),
            "imageUrl" to FirestoreValue(stringValue = reel.imageUrl),
            "videoUrl" to FirestoreValue(stringValue = reel.videoUrl),
            "likesCount" to FirestoreValue(stringValue = reel.likesCount),
            "isLiked" to FirestoreValue(booleanValue = reel.isLiked),
            "createdAt" to FirestoreValue(integerValue = reel.createdAt.toString()),
            "userStatus" to FirestoreValue(stringValue = reel.userStatus)
        )
        return FirestoreDocumentDetail(
            name = "projects/$projectId/databases/(default)/documents/reels/$finalId",
            fields = fields
        )
    }

    private fun buildFirestoreReelsBatchDoc(projectId: String, batchId: String, reels: List<UserReel>): FirestoreDocumentDetail {
        val serialized = try {
            userReelListAdapter.toJson(reels)
        } catch (e: Exception) {
            ""
        }
        val fields = mapOf(
            "batchId" to FirestoreValue(stringValue = batchId),
            "reelsCount" to FirestoreValue(integerValue = reels.size.toString()),
            "reelsData" to FirestoreValue(stringValue = serialized)
        )
        return FirestoreDocumentDetail(
            name = "projects/$projectId/databases/(default)/documents/reels/$batchId",
            fields = fields
        )
    }

    suspend fun fetchReels(): List<UserReel>? {
        clearLastError()
        val api = getApi() ?: return null
        val projectId = getFirebaseUrl()
        return try {
            val response = api.getReels(projectId)
            val documents = response?.documents ?: return emptyList()
            val allReels = mutableListOf<UserReel>()
            for (doc in documents) {
                val fields = doc.fields
                if (fields != null && fields.containsKey("reelsData")) {
                    val reelsJson = fields["reelsData"]?.stringValue
                    if (!reelsJson.isNullOrBlank()) {
                        try {
                            val parsedList = userReelListAdapter.fromJson(reelsJson)
                            if (parsedList != null) {
                                allReels.addAll(parsedList)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    val singleReel = mapToUserReel(doc)
                    if (singleReel != null) {
                        allReels.add(singleReel)
                    }
                }
            }
            allReels
        } catch (e: Exception) {
            handleError(e)
            null
        }
    }

    suspend fun uploadReels(reels: List<UserReel>): Boolean {
        clearLastError()
        val api = getApi() ?: return false
        val projectId = getFirebaseUrl()
        if (reels.isEmpty()) return true
        return try {
            val chunks = reels.chunked(100)
            val writes = chunks.mapIndexed { index, chunk ->
                val batchId = "reels_batch_${index + 1}"
                val batchDoc = buildFirestoreReelsBatchDoc(projectId, batchId, chunk)
                FirestoreWrite(update = batchDoc)
            }
            api.commit(projectId, FirestoreCommitRequest(writes))
            true
        } catch (e: Exception) {
            handleError(e)
            false
        }
    }

    private fun buildFirestoreVersionDoc(
        projectId: String,
        details: AppVersionDetails
    ): FirestoreDocumentDetail {
        val fields = mutableMapOf<String, FirestoreValue>(
            "version" to FirestoreValue(integerValue = details.version.toString()),
            "url" to FirestoreValue(stringValue = details.url)
        )
        details.days?.let { fields["days"] = FirestoreValue(integerValue = it.toString()) }
        details.instructionsText?.let { fields["instructions_text"] = FirestoreValue(stringValue = it) }
        details.instructionsUrl?.let { fields["instructions_url"] = FirestoreValue(stringValue = it) }

        return FirestoreDocumentDetail(
            name = "projects/$projectId/databases/(default)/documents/version/current_config",
            fields = fields
        )
    }

    suspend fun uploadVersionDetails(details: AppVersionDetails): Boolean {
        clearLastError()
        val api = getApi() ?: return false
        val projectId = getFirebaseUrl()
        return try {
            val write = FirestoreWrite(update = buildFirestoreVersionDoc(projectId, details))
            api.commit(projectId, FirestoreCommitRequest(listOf(write)))
            true
        } catch (e: Exception) {
            handleError(e)
            false
        }
    }

    suspend fun fetchAppVersionDetails(): AppVersionDetails? {
        clearLastError()
        val api = getApi() ?: return null
        val projectId = getFirebaseUrl()
        return try {
            val response = api.getVersionCollection(projectId)
            val doc = response?.documents?.firstOrNull() 
            if (doc == null) {
                // Seeding default version details if collection is empty
                val defaultDetails = AppVersionDetails(
                    version = 1,
                    url = "https://example.com/update_app",
                    days = 0, // Triggers instructions notification after 0 days of install
                    instructionsText = "Thank you for installing our OTT application! Enjoy the premium reels, secure movies, and dynamic content. Tap below to join our update telemetry list.",
                    instructionsUrl = "https://example.com/telemetry"
                )
                uploadVersionDetails(defaultDetails)
                return defaultDetails
            }
            val fields = doc.fields ?: return null
            val versionVal = fields["version"]?.integerValue?.toIntOrNull()
                ?: fields["version"]?.stringValue?.toIntOrNull()
                ?: 1
            val urlVal = fields["url"]?.stringValue ?: ""
            val daysVal = fields["days"]?.integerValue?.toIntOrNull()
                ?: fields["days"]?.stringValue?.toIntOrNull()
            val instText = fields["instructions_text"]?.stringValue
            val instUrl = fields["instructions_url"]?.stringValue

            AppVersionDetails(
                version = versionVal,
                url = urlVal,
                days = daysVal,
                instructionsText = instText,
                instructionsUrl = instUrl
            )
        } catch (e: Exception) {
            handleError(e)
            null
        }
    }

    private fun buildFirestoreUserDataDoc(projectId: String, userId: String, isPremium: Boolean): FirestoreDocumentDetail {
        val appPrefs = context.getSharedPreferences("app_user_prefs", Context.MODE_PRIVATE)
        val fcmToken = appPrefs.getString("fcm_token", null)
        val fields = mutableMapOf(
            "userid" to FirestoreValue(stringValue = userId),
            "status" to FirestoreValue(stringValue = if (isPremium) "premium" else "normal")
        )
        if (!fcmToken.isNullOrEmpty()) {
            fields["fcmToken"] = FirestoreValue(stringValue = fcmToken)
        }
        return FirestoreDocumentDetail(
            name = "projects/$projectId/databases/(default)/documents/userdata/$userId",
            fields = fields
        )
    }

    suspend fun registerOrUpdateUserData(userId: String, isPremium: Boolean): Boolean {
        clearLastError()
        val api = getApi() ?: return false
        val projectId = getFirebaseUrl()
        return try {
            val write = FirestoreWrite(update = buildFirestoreUserDataDoc(projectId, userId, isPremium))
            api.commit(projectId, FirestoreCommitRequest(listOf(write)))
            true
        } catch (e: Exception) {
            handleError(e)
            false
        }
    }

    suspend fun fetchUserData(userId: String): String? {
        clearLastError()
        val api = getApi() ?: return null
        val projectId = getFirebaseUrl()
        return try {
            val doc = api.getUserDataDocument(projectId, userId) ?: return null
            val fields = doc.fields ?: return null
            fields["status"]?.stringValue
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 404) {
                null
            } else {
                handleError(e)
                null
            }
        } catch (e: Exception) {
            handleError(e)
            null
        }
    }

    private fun buildFirestoreNotificationDoc(projectId: String, n: UserNotification): FirestoreDocumentDetail {
        val fields = mutableMapOf<String, FirestoreValue>(
            "id" to FirestoreValue(stringValue = n.id),
            "title" to FirestoreValue(stringValue = n.title),
            "body" to FirestoreValue(stringValue = n.body),
            "type" to FirestoreValue(stringValue = n.type),
            "timestamp" to FirestoreValue(integerValue = n.timestamp.toString())
        )
        n.imageUrl?.let { fields["imageUrl"] = FirestoreValue(stringValue = it) }
        n.actionUrl?.let { fields["actionUrl"] = FirestoreValue(stringValue = it) }

        return FirestoreDocumentDetail(
            name = "projects/$projectId/databases/(default)/documents/notifications/${n.id}",
            fields = fields
        )
    }

    suspend fun uploadNotification(n: UserNotification): Boolean {
        clearLastError()
        val api = getApi() ?: return false
        val projectId = getFirebaseUrl()
        return try {
            val write = FirestoreWrite(update = buildFirestoreNotificationDoc(projectId, n))
            api.commit(projectId, FirestoreCommitRequest(listOf(write)))
            true
        } catch (e: Exception) {
            handleError(e)
            false
        }
    }

    suspend fun fetchNotifications(): List<UserNotification>? {
        clearLastError()
        val api = getApi() ?: return null
        val projectId = getFirebaseUrl()
        return try {
            val response = api.getNotifications(projectId)
            val list = response?.documents?.mapNotNull { doc ->
                val fields = doc.fields ?: return@mapNotNull null
                val idVal = fields["id"]?.stringValue ?: ""
                val titleVal = fields["title"]?.stringValue ?: ""
                val bodyVal = fields["body"]?.stringValue ?: ""
                val imageVal = fields["imageUrl"]?.stringValue
                val typeVal = fields["type"]?.stringValue ?: "text"
                val actionVal = fields["actionUrl"]?.stringValue
                val timeVal = fields["timestamp"]?.integerValue?.toLongOrNull()
                    ?: fields["timestamp"]?.stringValue?.toLongOrNull()
                    ?: System.currentTimeMillis()

                UserNotification(
                    id = idVal,
                    title = titleVal,
                    body = bodyVal,
                    imageUrl = imageVal,
                    type = typeVal,
                    actionUrl = actionVal,
                    timestamp = timeVal
                )
            }
            if (list.isNullOrEmpty()) {
                val defaultNotifications = listOf(
                    UserNotification(
                        id = "welcome_text",
                        title = "Welcome aboard!",
                        body = "Unbelievable video streaming preloading latency is now active on your device! Enjoy zero buffering and fluid playback.",
                        type = "text",
                        timestamp = System.currentTimeMillis()
                    ),
                    UserNotification(
                        id = "premium_reels_direct",
                        title = "Explore Hot Reels Now!",
                        body = "Supercharged short video feeds are live. Tap below to navigate straight to the Reels tab.",
                        type = "text_link",
                        actionUrl = "app://tab/reels",
                        timestamp = System.currentTimeMillis() - 60000
                    ),
                    UserNotification(
                        id = "original_premier",
                        title = "Exclusive Originals!",
                        body = "Fresh originals have arrived! Tap to browse movies and original series.",
                        imageUrl = "https://images.unsplash.com/photo-1542204172-e7052809a936?auto=format&fit=crop&w=600&q=80",
                        type = "text_image",
                        actionUrl = "app://tab/explore",
                        timestamp = System.currentTimeMillis() - 360000
                    ),
                    UserNotification(
                        id = "netflix_news",
                        title = "Trending Global Content!",
                        body = "Check out what's trending across top global premium channels.",
                        imageUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?auto=format&fit=crop&w=600&q=80",
                        type = "text_image_link",
                        actionUrl = "https://example.com/trending_global",
                        timestamp = System.currentTimeMillis() - 720000
                    )
                )
                defaultNotifications.forEach { uploadNotification(it) }
                return defaultNotifications
            }
            list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            handleError(e)
            null
        }
    }

    suspend fun fetchUserDataList(): List<Map<String, String>>? {
        clearLastError()
        val api = getApi() ?: return null
        val projectId = getFirebaseUrl()
        return try {
            val response = api.getUserDataCollection(projectId)
            response?.documents?.mapNotNull { doc ->
                val fields = doc.fields ?: return@mapNotNull null
                val userId = fields["userid"]?.stringValue ?: ""
                val status = fields["status"]?.stringValue ?: "normal"
                val fcmToken = fields["fcmToken"]?.stringValue ?: ""
                mapOf(
                    "userid" to userId,
                    "status" to status,
                    "fcmToken" to fcmToken
                )
            }
        } catch (e: Exception) {
            handleError(e)
            null
        }
    }

    suspend fun fetchBroadcastHistory(): List<com.example.data.model.BroadcastNotification>? {
        clearLastError()
        val api = getApi() ?: return null
        val projectId = getFirebaseUrl()
        return try {
            val response = api.getFcmHistory(projectId)
            response?.documents?.mapNotNull { doc ->
                val fields = doc.fields ?: return@mapNotNull null
                val idVal = fields["id"]?.stringValue ?: ""
                val titleVal = fields["title"]?.stringValue ?: ""
                val bodyVal = fields["body"]?.stringValue ?: ""
                val targetAudienceVal = fields["targetAudience"]?.stringValue ?: "All Users"
                val targetTokenVal = fields["targetToken"]?.stringValue
                val targetTopicVal = fields["targetTopic"]?.stringValue
                val deliveredDevicesVal = fields["deliveredDevices"]?.integerValue?.toIntOrNull()
                    ?: fields["deliveredDevices"]?.stringValue?.toIntOrNull()
                    ?: 0
                val statusVal = fields["status"]?.stringValue ?: "Sent"
                val timestampVal = fields["timestamp"]?.integerValue?.toLongOrNull()
                    ?: fields["timestamp"]?.stringValue?.toLongOrNull()
                    ?: System.currentTimeMillis()

                com.example.data.model.BroadcastNotification(
                    id = idVal,
                    title = titleVal,
                    body = bodyVal,
                    targetAudience = targetAudienceVal,
                    targetToken = targetTokenVal,
                    targetTopic = targetTopicVal,
                    deliveredDevices = deliveredDevicesVal,
                    status = statusVal,
                    timestamp = timestampVal
                )
            }?.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            handleError(e)
            null
        }
    }

    private fun buildFirestoreBroadcastDoc(projectId: String, b: com.example.data.model.BroadcastNotification): FirestoreDocumentDetail {
        val fields = mutableMapOf<String, FirestoreValue>(
            "id" to FirestoreValue(stringValue = b.id),
            "title" to FirestoreValue(stringValue = b.title),
            "body" to FirestoreValue(stringValue = b.body),
            "targetAudience" to FirestoreValue(stringValue = b.targetAudience),
            "deliveredDevices" to FirestoreValue(integerValue = b.deliveredDevices.toString()),
            "status" to FirestoreValue(stringValue = b.status),
            "timestamp" to FirestoreValue(integerValue = b.timestamp.toString())
        )
        b.targetToken?.let { fields["targetToken"] = FirestoreValue(stringValue = it) }
        b.targetTopic?.let { fields["targetTopic"] = FirestoreValue(stringValue = it) }

        return FirestoreDocumentDetail(
            name = "projects/$projectId/databases/(default)/documents/fcm_history/${b.id}",
            fields = fields
        )
    }

    suspend fun uploadBroadcastNotification(b: com.example.data.model.BroadcastNotification): Boolean {
        clearLastError()
        val api = getApi() ?: return false
        val projectId = getFirebaseUrl()
        return try {
            val write = FirestoreWrite(update = buildFirestoreBroadcastDoc(projectId, b))
            api.commit(projectId, FirestoreCommitRequest(listOf(write)))
            true
        } catch (e: Exception) {
            handleError(e)
            false
        }
    }
}
