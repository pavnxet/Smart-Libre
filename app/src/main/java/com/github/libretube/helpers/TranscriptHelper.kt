package com.github.libretube.helpers

import android.content.Context
import com.github.libretube.api.obj.Streams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object TranscriptHelper {
    private const val PREFS_NAME = "smart_chapters_ai_prefs"
    const val KEY_TRANSCRIPT_API_KEY = "transcript_api_key"
    const val DEFAULT_KEY = ""

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val VTT_TIMESTAMP_PATTERN = Pattern.compile("(\\d{1,2}:\\d{2}:\\d{2}\\.\\d{3}|\\d{2}:\\d{2}\\.\\d{3})\\s*-->")

    suspend fun extractTranscript(context: Context, videoId: String, streams: Streams): String = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val apiKey = prefs.getString(KEY_TRANSCRIPT_API_KEY, DEFAULT_KEY)?.trim().orEmpty().ifEmpty { DEFAULT_KEY }

        // Primary: TranscriptAPI.com
        if (apiKey.isNotBlank() && videoId.isNotBlank()) {
            val transcriptFromApi = fetchFromTranscriptApi(videoId, apiKey)
            if (transcriptFromApi.isNotBlank()) {
                return@withContext transcriptFromApi
            }
        }

        // Fallback: Local Subtitle / VTT from streams
        return@withContext extractFromLocalSubtitles(streams)
    }

    private fun fetchFromTranscriptApi(videoId: String, apiKey: String): String {
        return try {
            val url = "https://transcriptapi.com/api/v2/youtube/transcript?video_url=https://www.youtube.com/watch?v=$videoId&format=json"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return ""
            if (!response.isSuccessful) return ""

            val json = JSONObject(body)
            val transcriptArray = json.optJSONArray("transcript") ?: return ""
            val result = StringBuilder()

            for (i in 0 until transcriptArray.length()) {
                val item = transcriptArray.getJSONObject(i)
                val text = item.optString("text").replace("\n", " ").trim()
                val startSeconds = item.optDouble("start", 0.0)

                if (text.isNotEmpty()) {
                    val formattedTime = formatSeconds(startSeconds.toLong())
                    result.append("$formattedTime $text\n")
                }
            }
            result.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun extractFromLocalSubtitles(streams: Streams): String {
        val subtitle = streams.subtitles.firstOrNull { it.code == "en" || it.code?.startsWith("en") == true }
            ?: streams.subtitles.firstOrNull { it.code == "hi" || it.code?.startsWith("hi") == true }
            ?: streams.subtitles.firstOrNull()
            ?: return ""

        val url = subtitle.url ?: return ""

        return try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return ""
            parseVttToTranscript(body)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun parseVttToTranscript(vtt: String): String {
        val lines = vtt.lines()
        val result = StringBuilder()
        var currentTimestamp = ""

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("WEBVTT") || trimmed.startsWith("NOTE")) continue

            val matcher = VTT_TIMESTAMP_PATTERN.matcher(trimmed)
            if (matcher.find()) {
                val fullTs = matcher.group(1) ?: ""
                currentTimestamp = fullTs.substringBefore('.')
                continue
            }

            if (currentTimestamp.isNotEmpty()) {
                val cleanText = trimmed.replace(Regex("<[^>]*>"), "").trim()
                if (cleanText.isNotEmpty()) {
                    result.append("$currentTimestamp $cleanText\n")
                }
            }
        }
        return result.toString()
    }

    private fun formatSeconds(sec: Long): String {
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return if (h > 0) {
            String.format("%02d:%02d:%02d", h, m, s)
        } else {
            String.format("%02d:%02d", m, s)
        }
    }
}