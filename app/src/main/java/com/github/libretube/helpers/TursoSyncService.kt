package com.github.libretube.helpers

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object TursoSyncService {
    private const val PREFS_NAME = "smart_chapters_turso_prefs"
    const val KEY_TURSO_URL = "turso_url"
    const val KEY_TURSO_TOKEN = "turso_token"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun loadTimestamps(context: Context, videoId: String): String? = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val url = prefs.getString(KEY_TURSO_URL, "")?.trim()?.replace(Regex("^libsql://", RegexOption.IGNORE_CASE), "https://")?.trimEnd('/') ?: ""
        val token = prefs.getString(KEY_TURSO_TOKEN, "")?.trim() ?: ""

        if (url.isEmpty() || token.isEmpty()) return@withContext null

        try {
            val payload = JSONObject().apply {
                put("requests", JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "execute")
                        put("stmt", JSONObject().apply {
                            put("sql", "CREATE TABLE IF NOT EXISTS video_timestamps (video_id TEXT PRIMARY KEY, timestamps TEXT);")
                        })
                    })
                    put(JSONObject().apply {
                        put("type", "execute")
                        put("stmt", JSONObject().apply {
                            put("sql", "SELECT timestamps FROM video_timestamps WHERE video_id = ?;")
                            put("args", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("type", "text")
                                    put("value", videoId)
                                })
                            })
                        })
                    })
                    put(JSONObject().apply { put("type", "close") })
                })
            }

            val request = Request.Builder()
                .url("$url/v2/pipeline")
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val respStr = response.body?.string() ?: return@withContext null
            val json = JSONObject(respStr)
            val results = json.optJSONArray("results") ?: return@withContext null

            if (results.length() >= 2) {
                val selectResult = results.getJSONObject(1).optJSONObject("response")?.optJSONObject("result")
                val rows = selectResult?.optJSONArray("rows")
                if (rows != null && rows.length() > 0) {
                    val firstRow = rows.getJSONArray(0)
                    val value = firstRow.getJSONObject(0).optString("value")
                    if (value.isNotBlank()) return@withContext value
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun saveTimestamps(context: Context, videoId: String, timestampsText: String): Boolean = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val url = prefs.getString(KEY_TURSO_URL, "")?.trim()?.replace(Regex("^libsql://", RegexOption.IGNORE_CASE), "https://")?.trimEnd('/') ?: ""
        val token = prefs.getString(KEY_TURSO_TOKEN, "")?.trim() ?: ""

        if (url.isEmpty() || token.isEmpty()) return@withContext false

        try {
            val payload = JSONObject().apply {
                put("requests", JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "execute")
                        put("stmt", JSONObject().apply {
                            put("sql", "CREATE TABLE IF NOT EXISTS video_timestamps (video_id TEXT PRIMARY KEY, timestamps TEXT);")
                        })
                    })
                    put(JSONObject().apply {
                        put("type", "execute")
                        put("stmt", JSONObject().apply {
                            put("sql", "INSERT INTO video_timestamps (video_id, timestamps) VALUES (?, ?) ON CONFLICT(video_id) DO UPDATE SET timestamps = excluded.timestamps;")
                            put("args", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("type", "text")
                                    put("value", videoId)
                                })
                                put(JSONObject().apply {
                                    put("type", "text")
                                    put("value", timestampsText)
                                })
                            })
                        })
                    })
                    put(JSONObject().apply { put("type", "close") })
                })
            }

            val request = Request.Builder()
                .url("$url/v2/pipeline")
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            return@withContext response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}