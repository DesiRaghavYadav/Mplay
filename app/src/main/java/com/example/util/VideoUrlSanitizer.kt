package com.example.util

/**
 * Utility to validate and sanitize video stream URLs across the app.
 * Automatically replaces obsolete, 403-forbidden, or inaccessible video sources
 * (e.g. deprecated Google Cloud Storage sample buckets, hotlink-blocked domains)
 * with verified, ultra-reliable 200-OK public CDN progressive MP4 streams.
 */
object VideoUrlSanitizer {

    val RELIABLE_STREAMS = listOf(
        "https://github.com/mediaelement/mediaelement-files/raw/master/big_buck_bunny.mp4",
        "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
        "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/360/Big_Buck_Bunny_360_10s_1MB.mp4",
        "https://test-videos.co.uk/vids/jellyfish/mp4/h264/360/Jellyfish_360_10s_1MB.mp4",
        "https://test-videos.co.uk/vids/sintel/mp4/h264/360/Sintel_360_10s_1MB.mp4",
        "https://raw.githubusercontent.com/intel-iot-devkit/sample-videos/master/person-bicycle-car-detection.mp4"
    )

    val DEFAULT_FALLBACK: String = RELIABLE_STREAMS[0]

    fun sanitize(url: String?, fallbackIndex: Int = 0): String {
        if (url.isNullOrBlank()) {
            return RELIABLE_STREAMS[Math.abs(fallbackIndex) % RELIABLE_STREAMS.size]
        }
        val trimmed = url.trim()

        // Detect forbidden/broken Google storage bucket or hotlink-restricted test domains
        val isBrokenGtv = trimmed.contains("commondatastorage.googleapis.com/gtv-videos-bucket", ignoreCase = true) ||
                trimmed.contains("storage.googleapis.com/gtv-videos-bucket", ignoreCase = true) ||
                trimmed.contains("w3schools.com/html/mov_bbb.mp4", ignoreCase = true)

        if (isBrokenGtv) {
            val idx = Math.abs(trimmed.hashCode()) % RELIABLE_STREAMS.size
            return RELIABLE_STREAMS[idx]
        }

        // Return trimmed URL if it has a valid protocol
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("file://")) {
            return trimmed
        }

        return RELIABLE_STREAMS[Math.abs(fallbackIndex) % RELIABLE_STREAMS.size]
    }
}
