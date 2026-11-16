package com.example.data.network

import android.content.Context
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

data class CloudflarePostResponse(
    val success: Boolean = false,
    val message: String? = null,
    val id: String? = null
)

interface CloudflareApi {
    @GET("api/comics")
    suspend fun getComics(): ResponseBody

    @GET("api/stories")
    suspend fun getStories(): ResponseBody

    @GET("api/categories")
    suspend fun getCategories(): ResponseBody

    @GET("api/video_categories")
    suspend fun getVideoCategories(): ResponseBody

    @GET("api/channels")
    suspend fun getChannels(): ResponseBody

    @GET("api/reels")
    suspend fun getReels(): ResponseBody

    @GET("api/notifications")
    suspend fun getNotifications(): ResponseBody

    @GET("api/userdata")
    suspend fun getUserdata(): ResponseBody

    @GET("api/audio_batches")
    suspend fun getAudioBatches(): ResponseBody

    @GET("api/batchvideo")
    suspend fun getBatchVideos(): ResponseBody

    @GET("api/batch_video")
    suspend fun getBatchVideoAlt(): ResponseBody

    @POST("api/batchvideo")
    suspend fun postBatchVideo(@Body data: Map<String, Any?>): ResponseBody

    @POST("api/userdata")
    suspend fun postUserData(@Body data: Map<String, Any?>): ResponseBody

    @POST("api/user_activities")
    suspend fun postUserActivity(@Body data: Map<String, Any?>): ResponseBody

    @POST("api/video_categories")
    suspend fun postVideoCategory(@Body data: Map<String, Any?>): ResponseBody
}

class CloudflareService(private val context: Context) {

    companion object {
        const val DEFAULT_WORKER_BASE_URL = "https://ott-api.fussionbhakti3.workers.dev/"
        const val R2_BUCKET_NAME = "batchh"
        const val R2_PUBLIC_URL_PREFIX = "https://pub-422ce85136f34f11ac739fd77c4591fd.r2.dev"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Android; OTTApp)")
                .build()
            chain.proceed(request)
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(DEFAULT_WORKER_BASE_URL)
        .client(okHttpClient)
        .build()

    private val api = retrofit.create(CloudflareApi::class.java)

    suspend fun fetchComicsFromCloudflare(): List<Pair<Comic, List<ComicChapter>>> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Pair<Comic, List<ComicChapter>>>()
        try {
            val responseBody = api.getComics()
            val jsonStr = responseBody.string()
            if (jsonStr.isNotBlank()) {
                val jsonArray = JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(i) ?: continue
                    val cId = item.optInt("id", item.optInt("_id", 0))
                    val title = item.optString("title", "")
                    val category = item.optString("category", "3D Comics")
                    val imageUrl = item.optString("imageUrl", "")
                    val description = item.optString("description", "")
                    val likesCount = item.optString("likesCount", "12k")
                    val isPremium = item.optBoolean("isPremium", false)

                    val comic = Comic(
                        id = cId,
                        title = title,
                        category = category,
                        imageUrl = imageUrl,
                        description = description,
                        likesCount = likesCount,
                        isPremium = isPremium
                    )

                    var chaptersArr: JSONArray? = item.optJSONArray("chapters")
                    if (chaptersArr == null && item.has("chaptersJson")) {
                        val chJsonStr = item.optString("chaptersJson", "")
                        if (chJsonStr.isNotBlank()) {
                            try {
                                chaptersArr = JSONArray(chJsonStr)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    val chaptersList = mutableListOf<ComicChapter>()
                    if (chaptersArr != null) {
                        for (j in 0 until chaptersArr.length()) {
                            val chObj = chaptersArr.optJSONObject(j) ?: continue
                            val chId = chObj.optInt("id", 0)
                            val comicId = chObj.optInt("comicId", cId)
                            val chapterNumber = chObj.optInt("chapterNumber", j + 1)
                            val chTitle = chObj.optString("title", "Chapter $chapterNumber")
                            val pagesCsv = chObj.optString("pagesCsv", "")
                            val pdfUrl = chObj.optString("pdfUrl", "")

                            chaptersList.add(
                                ComicChapter(
                                    id = chId,
                                    comicId = comicId,
                                    chapterNumber = chapterNumber,
                                    title = chTitle,
                                    pagesCsv = pagesCsv,
                                    pdfUrl = pdfUrl
                                )
                            )
                        }
                    }
                    result.add(Pair(comic, chaptersList))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }

    suspend fun fetchStoriesFromCloudflareWithParts(): Pair<List<Story>, List<StoryPart>> = withContext(Dispatchers.IO) {
        val storiesList = mutableListOf<Story>()
        val partsList = mutableListOf<StoryPart>()
        try {
            val responseBody = api.getStories()
            val jsonStr = responseBody.string()
            if (jsonStr.isNotBlank()) {
                val jsonArray = JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(i) ?: continue
                    val sId = item.optInt("id", item.optInt("_id", 0))
                    val title = item.optString("title", "")
                    val imageUrl = item.optString("imageUrl", "")
                    val content = item.optString("content", "")
                    val category = item.optString("category", "Fantasy")
                    val isPremium = item.optBoolean("isPremium", true)
                    val audioUrl = item.optString("audioUrl", "")
                    val duration = item.optString("duration", "")
                    val releaseDate = item.optLong("releaseDate", System.currentTimeMillis())

                    val story = Story(
                        id = sId,
                        title = title,
                        imageUrl = imageUrl,
                        content = content,
                        category = category,
                        isPremium = isPremium,
                        audioUrl = audioUrl,
                        duration = duration,
                        releaseDate = releaseDate
                    )
                    storiesList.add(story)

                    var partsArr: JSONArray? = item.optJSONArray("parts")
                    if (partsArr == null && item.has("partsJson")) {
                        val partsJsonStr = item.optString("partsJson", "")
                        if (partsJsonStr.isNotBlank()) {
                            try {
                                partsArr = JSONArray(partsJsonStr)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    if (partsArr != null) {
                        for (j in 0 until partsArr.length()) {
                            val ptObj = partsArr.optJSONObject(j) ?: continue
                            val ptId = ptObj.optInt("id", 0)
                            val storyId = ptObj.optInt("storyId", sId)
                            val partNumber = ptObj.optInt("partNumber", j + 1)
                            val ptTitle = ptObj.optString("title", "Part $partNumber")
                            val ptContent = ptObj.optString("content", "")

                            partsList.add(
                                StoryPart(
                                    id = ptId,
                                    storyId = storyId,
                                    partNumber = partNumber,
                                    title = ptTitle,
                                    content = ptContent
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Pair(storiesList, partsList)
    }

    suspend fun fetchStoriesFromCloudflare(): List<Story> = withContext(Dispatchers.IO) {
        fetchStoriesFromCloudflareWithParts().first
    }

    suspend fun fetchVideoCategoriesFromCloudflare(): List<Pair<Category, List<MediaItem>>> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Pair<Category, List<MediaItem>>>()
        try {
            val responseBody = api.getVideoCategories()
            val jsonStr = responseBody.string()
            if (jsonStr.isNotBlank()) {
                val jsonArray = JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val catObj = jsonArray.optJSONObject(i) ?: continue
                    val catId = i + 1
                    val catName = catObj.optString("name", "")
                    val isPortrait = catObj.optBoolean("isPortrait", false)
                    val catType = catObj.optString("type", "video")

                    val category = Category(
                        id = catId,
                        name = catName,
                        isPortrait = isPortrait,
                        type = catType
                    )

                    var vidsArr: JSONArray? = catObj.optJSONArray("videos")
                    if (vidsArr == null || vidsArr.length() == 0) {
                        if (catObj.has("videosJson")) {
                            val vidsJsonStr = catObj.optString("videosJson", "")
                            if (vidsJsonStr.isNotBlank()) {
                                try {
                                    vidsArr = JSONArray(vidsJsonStr)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }

                    val mediaList = mutableListOf<MediaItem>()
                    if (vidsArr != null) {
                        for (j in 0 until vidsArr.length()) {
                            val vObj = vidsArr.optJSONObject(j) ?: continue
                            val rawId = vObj.optInt("id", 0)
                            val vTitle = vObj.optString("title", "")
                            val vDesc = vObj.optString("description", "")
                            val rawCategory = vObj.optString("category", "").trim()
                            val vCat = if (rawCategory.isBlank() || rawCategory.equals("None", ignoreCase = true) || rawCategory.equals("null", ignoreCase = true)) {
                                catName
                            } else if (!rawCategory.split(",").map { it.trim().lowercase() }.contains(catName.trim().lowercase())) {
                                "$rawCategory, $catName"
                            } else {
                                rawCategory
                            }
                            val vPrem = vObj.optBoolean("isPremium", false)
                            val vRating = vObj.optDouble("rating", 4.5).toFloat()
                            val vLikes = vObj.optString("likesCount", "1k")
                            val rawImg = vObj.optString("imageUrl", "")
                                .ifBlank { vObj.optString("thumburl", "") }
                                .ifBlank { vObj.optString("thumbUrl", "") }
                                .ifBlank { vObj.optString("url", "") }
                                .ifBlank { vObj.optString("href", "") }
                                .trim()
                            val rawReels = vObj.optString("reelsUrl", "")
                                .ifBlank { vObj.optString("cloudflareurl", "") }
                                .ifBlank { vObj.optString("cloudflareUrl", "") }
                                .ifBlank { vObj.optString("videourl", "") }
                                .ifBlank { vObj.optString("videoUrl", "") }
                                .trim()

                            val safeReels = com.example.util.VideoUrlSanitizer.sanitize(rawReels)
                            // Order convention: top of category JSON array (j = 0) is older, bottom (j = N - 1) is latest.
                            // Each newly appended video at the bottom receives a higher ID so it appears first under Latest (NEW) sort.
                            val vId = ((catId % 2000) * 1_000_000) + (j + 1)

                            mediaList.add(
                                MediaItem(
                                    id = vId,
                                    title = vTitle,
                                    description = vDesc,
                                    imageUrl = rawImg,
                                    category = vCat,
                                    isPremium = vPrem,
                                    rating = vRating,
                                    reelsUrl = safeReels,
                                    likesCount = vLikes
                                )
                            )
                        }
                    }
                    result.add(Pair(category, mediaList))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }

    suspend fun fetchCategoriesFromCloudflare(): List<Category> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Category>()
        try {
            val responseBody = api.getCategories()
            val jsonStr = responseBody.string()
            if (jsonStr.isNotBlank()) {
                val jsonArray = JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(i) ?: continue
                    val cId = item.optInt("id", item.optInt("_id", i + 1))
                    val name = item.optString("name", "")
                    val isPortrait = item.optBoolean("isPortrait", false)
                    val type = item.optString("type", "video")

                    result.add(
                        Category(
                            id = cId,
                            name = name,
                            isPortrait = isPortrait,
                            type = type
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }

    suspend fun fetchChannelsFromCloudflare(): List<Channel> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Channel>()
        try {
            val responseBody = api.getChannels()
            val jsonStr = responseBody.string()
            if (jsonStr.isNotBlank()) {
                val jsonArray = JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(i) ?: continue
                    val chId = item.optInt("id", item.optInt("_id", i + 1))
                    val name = item.optString("name", "")
                    val imageUrl = item.optString("imageUrl", "")

                    result.add(
                        Channel(
                            id = chId,
                            name = name,
                            imageUrl = imageUrl
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }

    suspend fun fetchReelsFromCloudflare(): List<UserReel> = withContext(Dispatchers.IO) {
        val result = mutableListOf<UserReel>()
        try {
            val responseBody = api.getReels()
            val jsonStr = responseBody.string()
            if (jsonStr.isNotBlank()) {
                val jsonArray = JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(i) ?: continue

                    fun parseReelObject(obj: JSONObject, index: Int = 0): UserReel? {
                        val rawVideoUrl = obj.optString("videoUrl", "")
                            .ifBlank { obj.optString("cloudflareurl", "") }
                            .ifBlank { obj.optString("cloudflareUrl", "") }
                            .ifBlank { obj.optString("reelsUrl", "") }
                            .ifBlank { obj.optString("videourl", "") }
                            .ifBlank { obj.optString("url", "") }
                            .trim()
                        val rId = obj.optInt("id", 0)
                        val title = obj.optString("title", "").trim()
                        val desc = obj.optString("description", "").trim()
                        val rawImg = obj.optString("imageUrl", "")
                            .ifBlank { obj.optString("thumburl", "") }
                            .ifBlank { obj.optString("thumbUrl", "") }
                            .ifBlank { obj.optString("url", "") }
                            .ifBlank { obj.optString("href", "") }
                            .trim()
                        val likes = obj.optString("likesCount", "1.2k")
                        val isLiked = obj.optBoolean("isLiked", false)
                        val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        val userStatus = obj.optString("userStatus", "normal")

                        return UserReel(
                            id = rId,
                            title = title,
                            description = desc,
                            imageUrl = rawImg,
                            videoUrl = com.example.util.VideoUrlSanitizer.sanitize(rawVideoUrl),
                            likesCount = likes,
                            isLiked = isLiked,
                            createdAt = createdAt,
                            userStatus = userStatus
                        )
                    }

                    val directReel = parseReelObject(item, i)
                    if (directReel != null) {
                        result.add(directReel)
                    } else if (item.has("reelsData")) {
                        val reelsDataStr = item.optString("reelsData", "")
                        if (reelsDataStr.isNotBlank()) {
                            try {
                                val batchArr = JSONArray(reelsDataStr)
                                for (j in 0 until batchArr.length()) {
                                    val bObj = batchArr.optJSONObject(j) ?: continue
                                    val bReel = parseReelObject(bObj, j)
                                    if (bReel != null) {
                                        result.add(bReel)
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }

    suspend fun fetchNotificationsFromCloudflare(): List<UserNotification> = withContext(Dispatchers.IO) {
        val result = mutableListOf<UserNotification>()
        try {
            val responseBody = api.getNotifications()
            val jsonStr = responseBody.string()
            if (jsonStr.isNotBlank()) {
                val jsonArray = JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(i) ?: continue
                    val nId = item.optString("id", item.optString("_id", ""))
                    val title = item.optString("title", "")
                    val body = item.optString("body", "")
                    val imageUrl = if (item.has("imageUrl") && !item.isNull("imageUrl")) item.optString("imageUrl") else null
                    val type = item.optString("type", "text")
                    val actionUrl = if (item.has("actionUrl") && !item.isNull("actionUrl")) item.optString("actionUrl") else null
                    val timestamp = item.optLong("timestamp", System.currentTimeMillis())

                    result.add(
                        UserNotification(
                            id = nId,
                            title = title,
                            body = body,
                            imageUrl = imageUrl,
                            type = type,
                            actionUrl = actionUrl,
                            timestamp = timestamp
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }

    suspend fun fetchUserDataListFromCloudflare(): List<Map<String, String>> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Map<String, String>>()
        try {
            val responseBody = api.getUserdata()
            val jsonStr = responseBody.string()
            if (jsonStr.isNotBlank()) {
                val jsonArray = JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(i) ?: continue
                    val map = mutableMapOf<String, String>()
                    val keys = item.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        map[key] = item.optString(key, "")
                    }
                    result.add(map)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }

    suspend fun fetchAudioBatchesFromCloudflare(): List<Story> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Story>()
        try {
            val responseBody = api.getAudioBatches()
            val jsonStr = responseBody.string()
            if (jsonStr.isNotBlank()) {
                val jsonArray = JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val batchObj = jsonArray.optJSONObject(i) ?: continue

                    var audiosArr: JSONArray? = batchObj.optJSONArray("audios")
                    if (audiosArr == null || audiosArr.length() == 0) {
                        if (batchObj.has("audiosJson")) {
                            val audsJsonStr = batchObj.optString("audiosJson", "")
                            if (audsJsonStr.isNotBlank()) {
                                try {
                                    audiosArr = JSONArray(audsJsonStr)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }

                    if (audiosArr != null) {
                        for (j in 0 until audiosArr.length()) {
                            val aObj = audiosArr.optJSONObject(j) ?: continue
                            val aId = aObj.optInt("id", 0)
                            val aTitle = aObj.optString("title", "")
                            val aImg = aObj.optString("imageUrl", "")
                            val aCnt = aObj.optString("content", "")
                            val aCat = aObj.optString("category", "Audio")
                            val aPrem = aObj.optBoolean("isPremium", true)
                            val aAudioUrl = aObj.optString("audioUrl", "")
                            val aDur = aObj.optString("duration", "")
                            val aRel = aObj.optLong("releaseDate", System.currentTimeMillis())

                            result.add(
                                Story(
                                    id = aId,
                                    title = aTitle,
                                    imageUrl = aImg,
                                    content = aCnt,
                                    category = aCat,
                                    isPremium = aPrem,
                                    audioUrl = aAudioUrl,
                                    duration = aDur,
                                    releaseDate = aRel
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }

    suspend fun saveUserDataToCloudflare(
        userId: String,
        email: String? = null,
        status: String = "normal",
        isPremium: Boolean = false,
        fcmToken: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("id", userId)
                put("_id", userId)
                put("userid", userId)
                put("email", email ?: "")
                put("status", status)
                put("isPremium", isPremium)
                put("fcmToken", fcmToken ?: "")
                put("updatedAt", System.currentTimeMillis())
            }
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = okhttp3.Request.Builder()
                .url(DEFAULT_WORKER_BASE_URL + "api/userdata")
                .post(body)
                .build()

            val resp = okHttpClient.newCall(request).execute()
            return@withContext resp.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun saveUserActivityToCloudflare(
        userId: String,
        action: String,
        details: Map<String, Any?> = emptyMap()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("userId", userId)
                put("action", action)
                put("timestamp", System.currentTimeMillis())
                details.forEach { (k, v) -> put(k, v ?: "") }
            }
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = okhttp3.Request.Builder()
                .url(DEFAULT_WORKER_BASE_URL + "api/user_activities")
                .post(body)
                .build()

            val resp = okHttpClient.newCall(request).execute()
            return@withContext resp.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun uploadRawVideoCategoryToCloudflare(
        id: Int,
        name: String,
        isPortrait: Boolean,
        type: String,
        videosJson: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("id", id)
                put("name", name)
                put("isPortrait", isPortrait)
                put("type", type)
                put("videosJson", videosJson)
            }
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = okhttp3.Request.Builder()
                .url(DEFAULT_WORKER_BASE_URL + "api/video_categories")
                .post(body)
                .build()

            val resp = okHttpClient.newCall(request).execute()
            return@withContext resp.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun parseDurationSeconds(durationStr: String): Int {
        if (durationStr.isBlank()) return 0
        val trimmed = durationStr.trim().lowercase()
        return try {
            if (trimmed.contains("min")) {
                val num = trimmed.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
                num * 60
            } else if (trimmed.contains("sec") || trimmed.contains("s")) {
                trimmed.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
            } else {
                val parts = trimmed.split(":")
                when (parts.size) {
                    1 -> parts[0].toIntOrNull() ?: 0
                    2 -> (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
                    3 -> (parts[0].toIntOrNull() ?: 0) * 3600 + (parts[1].toIntOrNull() ?: 0) * 60 + (parts[2].toIntOrNull() ?: 0)
                    else -> 0
                }
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun parseFileSizeMb(sizeStr: String): Double {
        if (sizeStr.isBlank()) return 0.0
        val trimmed = sizeStr.trim().uppercase()
        return try {
            val numStr = trimmed.replace(Regex("[^0-9.]"), "")
            val num = numStr.toDoubleOrNull() ?: return 0.0
            when {
                trimmed.contains("GB") -> num * 1024.0
                trimmed.contains("KB") -> num / 1024.0
                else -> num
            }
        } catch (e: Exception) {
            0.0
        }
    }

    private fun extractBatchObjects(jsonAny: Any?): List<JSONObject> {
        val list = mutableListOf<JSONObject>()
        when (jsonAny) {
            is JSONObject -> {
                if (jsonAny.has("batches") || jsonAny.has("data") || jsonAny.has("items")) {
                    val sub = jsonAny.optJSONArray("batches")
                        ?: jsonAny.optJSONArray("data")
                        ?: jsonAny.optJSONArray("items")
                    if (sub != null) {
                        list.addAll(extractBatchObjects(sub))
                    } else {
                        list.add(jsonAny)
                    }
                } else {
                    list.add(jsonAny)
                }
            }
            is JSONArray -> {
                for (i in 0 until jsonAny.length()) {
                    list.addAll(extractBatchObjects(jsonAny.opt(i)))
                }
            }
            is String -> {
                val str = jsonAny.trim()
                if (str.startsWith("[")) {
                    try { list.addAll(extractBatchObjects(JSONArray(str))) } catch (_: Exception) {}
                } else if (str.startsWith("{")) {
                    try { list.addAll(extractBatchObjects(JSONObject(str))) } catch (_: Exception) {}
                }
            }
        }
        return list
    }

    suspend fun fetchBatchVideosFromCloudflare(): List<Pair<BatchVideo, List<BatchVideoItem>>> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Pair<BatchVideo, List<BatchVideoItem>>>()
        try {
            var rawJson = ""
            try {
                val resp = api.getBatchVideos()
                rawJson = resp.string().trim()
            } catch (e: Exception) {
                // Fallback to alternate route if /api/batchvideo fails
                try {
                    val respAlt = api.getBatchVideoAlt()
                    rawJson = respAlt.string().trim()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }

            if (rawJson.isBlank() || rawJson.contains("Endpoint ready") || rawJson.startsWith("{\"error\"")) {
                return@withContext result
            }

            val docObjects = mutableListOf<JSONObject>()
            if (rawJson.startsWith("[")) {
                try {
                    val rootArray = JSONArray(rawJson)
                    docObjects.addAll(extractBatchObjects(rootArray))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (rawJson.startsWith("{")) {
                try {
                    val rootObj = JSONObject(rawJson)
                    docObjects.addAll(extractBatchObjects(rootObj))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val batchMap = mutableMapOf<String, Pair<BatchVideo, List<BatchVideoItem>>>()

            for (i in docObjects.indices) {
                val doc = docObjects[i]

                val bId = doc.optString("batchId", doc.optString("id", doc.optString("_id", "batch_${i + 1}"))).ifBlank { "batch_${i + 1}" }
                val bTitle = doc.optString("title", doc.optString("name", doc.optString("batchName", "Batch Series ${i + 1}"))).ifBlank { "Batch Series ${i + 1}" }
                val bDesc = doc.optString("description", doc.optString("content", doc.optString("summary", "Complete web series with all episodes in high definition.")))
                val bImg = doc.optString("imageUrl", doc.optString("thumbnailUrl", doc.optString("posterUrl", doc.optString("bannerUrl", ""))))
                val bCat = doc.optString("category", doc.optString("genre", "Stage Dance")).ifBlank { "Stage Dance" }
                val bPrem = doc.optBoolean("isPremium", true)
                val bRel = doc.optLong("releaseDate", doc.optLong("createdAt", System.currentTimeMillis()))

                // Extract videos from doc
                val itemsList = mutableListOf<BatchVideoItem>()

                var videosArr: JSONArray? = doc.optJSONArray("videos")
                if (videosArr == null || videosArr.length() == 0) {
                    videosArr = doc.optJSONArray("videoUrls")
                }
                if (videosArr == null || videosArr.length() == 0) {
                    videosArr = doc.optJSONArray("urls")
                }
                if (videosArr == null || videosArr.length() == 0) {
                    videosArr = doc.optJSONArray("episodes")
                }
                if (videosArr == null || videosArr.length() == 0) {
                    videosArr = doc.optJSONArray("items")
                }

                // Check JSON string representation (e.g. videosJson)
                if (videosArr == null || videosArr.length() == 0) {
                    val vJsonStr = doc.optString("videosJson", doc.optString("urlsJson", ""))
                    if (vJsonStr.isNotBlank() && vJsonStr.startsWith("[")) {
                        try {
                            videosArr = JSONArray(vJsonStr)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                // Check raw text lines of URLs (e.g. URLs separated by newlines)
                if (videosArr == null || videosArr.length() == 0) {
                    val urlsText = doc.optString("batchVideoUrls", doc.optString("urls", ""))
                    if (urlsText.isNotBlank() && (urlsText.contains("http://") || urlsText.contains("https://"))) {
                        val lines = urlsText.split("\n", ",").map { it.trim() }.filter { it.startsWith("http") }
                        lines.forEachIndexed { vIdx, u ->
                            val defaultTime = bRel - (lines.size - vIdx) * 3600000L * 2
                            itemsList.add(
                                BatchVideoItem(
                                    batchId = bId,
                                    videoIndex = vIdx + 1,
                                    title = "Episode ${vIdx + 1}",
                                    videoUrl = u,
                                    imageUrl = bImg,
                                    duration = "22 min",
                                    description = "Episode ${vIdx + 1} of $bTitle",
                                    uploadTime = defaultTime,
                                    durationSec = 22 * 60,
                                    sizeMb = 5.0
                                )
                            )
                        }
                    }
                }

                if (videosArr != null && videosArr.length() > 0) {
                    for (vIdx in 0 until videosArr.length()) {
                        val vObj = videosArr.optJSONObject(vIdx)
                        if (vObj != null) {
                            val vTitle = vObj.optString("title", vObj.optString("name", "Episode ${vIdx + 1}")).ifBlank { "Episode ${vIdx + 1}" }
                            val vUrl = vObj.optString("videoUrl", vObj.optString("url", vObj.optString("streamUrl", vObj.optString("mp4Url", ""))))
                            val vImg = vObj.optString("imageUrl", vObj.optString("thumbnailUrl", bImg)).ifBlank { bImg }
                            val vDur = vObj.optString("duration", vObj.optString("length", "0:30")).ifBlank { "0:30" }
                            val vDesc = vObj.optString("description", "Episode ${vIdx + 1} of $bTitle")

                            var vUploadTime = vObj.optLong("uploadTime", 0L)
                            if (vUploadTime <= 0L) vUploadTime = vObj.optLong("uploadedAt", 0L)
                            if (vUploadTime <= 0L) vUploadTime = vObj.optLong("createdAt", 0L)
                            if (vUploadTime <= 0L) vUploadTime = vObj.optLong("timestamp", 0L)
                            if (vUploadTime in 1..9999999999L) {
                                vUploadTime *= 1000L
                            }
                            if (vUploadTime <= 0L) {
                                // Default staggered upload timestamp: older index uploaded earlier, higher index uploaded more recently
                                val baseTime = if (bRel > 0L) bRel else System.currentTimeMillis() - 86400000L * 3
                                vUploadTime = baseTime + (vIdx * 3600000L * 4)
                            }

                            val dSec = parseDurationSeconds(vDur)
                            val sMb = parseFileSizeMb(vDesc.ifBlank { vObj.optString("size", "") })

                            val safeUrl = com.example.util.VideoUrlSanitizer.sanitize(vUrl, vIdx)
                            if (safeUrl.isNotBlank()) {
                                itemsList.add(
                                    BatchVideoItem(
                                        batchId = bId,
                                        videoIndex = vObj.optInt("videoIndex", vIdx + 1),
                                        title = vTitle,
                                        videoUrl = safeUrl,
                                        imageUrl = vImg,
                                        duration = vDur,
                                        description = vDesc,
                                        uploadTime = vUploadTime,
                                        durationSec = dSec,
                                        sizeMb = sMb
                                    )
                                )
                            }
                        } else {
                            // Element is directly a string URL
                            val directUrl = videosArr.optString(vIdx, "").trim()
                            val safeDirectUrl = com.example.util.VideoUrlSanitizer.sanitize(directUrl, vIdx)
                            if (safeDirectUrl.isNotBlank()) {
                                val defaultTime = bRel + (vIdx * 3600000L * 4)
                                itemsList.add(
                                    BatchVideoItem(
                                        batchId = bId,
                                        videoIndex = vIdx + 1,
                                        title = "Episode ${vIdx + 1}",
                                        videoUrl = safeDirectUrl,
                                        imageUrl = bImg,
                                        duration = "22 min",
                                        description = "Episode ${vIdx + 1} of $bTitle",
                                        uploadTime = defaultTime,
                                        durationSec = 22 * 60,
                                        sizeMb = 5.0
                                    )
                                )
                            }
                        }
                    }
                }

                val batchVideo = BatchVideo(
                    batchId = bId,
                    title = bTitle,
                    description = bDesc,
                    imageUrl = bImg,
                    category = bCat,
                    isPremium = bPrem,
                    totalVideos = itemsList.size,
                    releaseDate = bRel
                )

                if (itemsList.isNotEmpty()) {
                    val existing = batchMap[bId]
                    if (existing == null || itemsList.size > existing.second.size) {
                        batchMap[bId] = (batchVideo to itemsList)
                    }
                }
            }
            result.addAll(batchMap.values)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }

    suspend fun uploadBatchVideoToCloudflare(
        batch: BatchVideo,
        items: List<BatchVideoItem>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("id", batch.batchId.ifBlank { "batch_${batch.id}" })
                put("batchId", batch.batchId.ifBlank { "batch_${batch.id}" })
                put("title", batch.title)
                put("name", batch.title)
                put("description", batch.description)
                put("imageUrl", batch.imageUrl)
                put("category", batch.category)
                put("isPremium", batch.isPremium)
                put("totalVideos", items.size)
                put("releaseDate", batch.releaseDate)

                val vArr = JSONArray()
                items.forEach { itm ->
                    vArr.put(
                        JSONObject().apply {
                            put("id", itm.id)
                            put("videoIndex", itm.videoIndex)
                            put("title", itm.title)
                            put("videoUrl", itm.videoUrl)
                            put("imageUrl", itm.imageUrl)
                            put("duration", itm.duration)
                            put("description", itm.description)
                        }
                    )
                }
                put("videos", vArr)
                put("videosJson", vArr.toString())
            }

            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = okhttp3.Request.Builder()
                .url(DEFAULT_WORKER_BASE_URL + "api/batchvideo")
                .post(body)
                .build()

            val resp = okHttpClient.newCall(request).execute()
            return@withContext resp.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

