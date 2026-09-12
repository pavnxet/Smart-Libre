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
import java.util.regex.Pattern

object AiChaptersService {
    private const val PREFS_NAME = "smart_chapters_ai_prefs"
    const val KEY_AI_PROVIDER = "ai_provider" // "aikit" or "openrouter"
    const val KEY_AI_TOKEN = "ai_token"
    const val KEY_AI_BASE_URL = "ai_base_url"
    const val KEY_AI_MODEL = "ai_model"
    const val KEY_SOUND_ENABLED = "sound_enabled"

    const val DEFAULT_AIKIT_URL = "https://claude.aikit.club/qwen.aikit.club/v1"
    const val DEFAULT_AIKIT_MODEL = "qwen3.8-max"
    const val DEFAULT_OPENROUTER_MODEL = "google/gemma-4-31b-it:free"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val TS_REGEX = Pattern.compile("\\b(\\d{1,2}(?::\\d{2}){1,2})\\b")

    suspend fun generateChapters(context: Context, transcript: String): Result<String> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val provider = prefs.getString(KEY_AI_PROVIDER, "aikit") ?: "aikit"
        val token = prefs.getString(KEY_AI_TOKEN, "")?.trim().orEmpty()
        val model = prefs.getString(KEY_AI_MODEL, if (provider == "openrouter") DEFAULT_OPENROUTER_MODEL else DEFAULT_AIKIT_MODEL)?.trim().orEmpty()
        val baseUrl = prefs.getString(KEY_AI_BASE_URL, DEFAULT_AIKIT_URL)?.trim().orEmpty()

        if (token.isEmpty()) {
            return@withContext Result.failure(Exception("AI Token / API Key is missing. Configure it in AI Settings (⚙️)."))
        }

        val lines = transcript.lines()
        var maxSec = 0L
        for (line in lines) {
            val matcher = TS_REGEX.matcher(line)
            if (matcher.find()) {
                val sec = parseSec(matcher.group(1) ?: "")
                if (sec > maxSec) maxSec = sec
            }
        }

        val chunkDuration = 1800L // 30 mins
        if (maxSec > chunkDuration + 180) {
            val totalChunks = Math.ceil(maxSec.toDouble() / chunkDuration).toInt()
            val timeChunks = Array(totalChunks) { mutableListOf<String>() }
            for (line in lines) {
                val matcher = TS_REGEX.matcher(line)
                if (matcher.find()) {
                    val sec = parseSec(matcher.group(1) ?: "")
                    val bucket = Math.min(totalChunks - 1, (sec / chunkDuration).toInt())
                    timeChunks[bucket].add(line)
                }
            }

            val validChunks = timeChunks.map { it.joinToString("\n") }.filter { it.isNotBlank() }
            val tableRows = mutableListOf<String>()

            for (chunk in validChunks) {
                val chunkResult = callAi(chunk, provider, token, model, baseUrl)
                if (chunkResult.isSuccess) {
                    val output = chunkResult.getOrNull().orEmpty()
                    for (l in output.lines()) {
                        val trimmed = l.trim()
                        if (trimmed.startsWith('|') && !trimmed.lowercase().contains("question start") && !trimmed.matches(Regex("^\\|?[\\s\\-:\\|]+\\|?$"))) {
                            tableRows.add(trimmed)
                        }
                    }
                }
            }

            if (tableRows.isNotEmpty()) {
                val header = "| Q# | Question Start | Correct Option Timestamp | Answer Start | Correct Option |\n| -- | -------------- | ------------------------ | ------------ | -------------- |"
                var counter = 1
                val renumbered = tableRows.map { row ->
                    val parts = row.split('|').toMutableList()
                    if (parts.size >= 6) {
                        parts[1] = " Q${counter++} "
                        parts.joinToString("|")
                    } else row
                }
                return@withContext Result.success("$header\n${renumbered.joinToString("\n")}")
            }
        }

        return@withContext callAi(transcript, provider, token, model, baseUrl)
    }

    private fun callAi(chunk: String, provider: String, token: String, model: String, baseUrl: String): Result<String> {
        val prompt = """
You are an expert educational content analyzer specialized in exam preparation and live MCQ video lectures.
Analyze the following timestamped video transcript. Identify every distinct multiple-choice question (MCQ), question practice session, or problem solving item.

For each question, find:
1. Question Start: Earliest timestamp where the question begins.
2. Correct Option Timestamp: Exact timestamp where the instructor reveals/confirms the correct option (or N/A).
3. Answer Start: Timestamp where explanation begins (or N/A).
4. Correct Option: A, B, C, D, or Unclear.

OUTPUT FORMAT:
Return ONLY the markdown table below:
| Q# | Question Start | Correct Option Timestamp | Answer Start | Correct Option |
| -- | -------------- | ------------------------ | ------------ | -------------- |
| Q1 | HH:MM:SS       | HH:MM:SS                 | HH:MM:SS     | B              |

Rules:
- Strictly use HH:MM:SS (or MM:SS) format for all timestamps.
- Do NOT output any conversational text or notes outside the table.

Transcript:
$chunk
""".trimIndent()

        return try {
            if (provider == "openrouter") {
                val url = "https://openrouter.ai/api/v1/chat/completions"
                val bodyJson = JSONObject().apply {
                    put("model", model.ifEmpty { DEFAULT_OPENROUTER_MODEL })
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                }
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Content-Type", "application/json")
                    .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val respStr = response.body?.string() ?: ""
                if (!response.isSuccessful) return Result.failure(Exception("OpenRouter Error: $respStr"))

                val json = JSONObject(respStr)
                val content = json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                Result.success(content.trim())
            } else {
                // AIKit (Anthropic / Qwen proxy format)
                val cleanBase = baseUrl.ifEmpty { DEFAULT_AIKIT_URL }.trimEnd('/')
                val endpoint = if (cleanBase.endsWith("/messages")) cleanBase else "$cleanBase/messages"

                val bodyJson = JSONObject().apply {
                    put("model", model.ifEmpty { DEFAULT_AIKIT_MODEL })
                    put("max_tokens", 4000)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                }

                val request = Request.Builder()
                    .url(endpoint)
                    .addHeader("x-api-key", token)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("Content-Type", "application/json")
                    .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val respStr = response.body?.string() ?: ""
                if (!response.isSuccessful) return Result.failure(Exception("AIKit Error: $respStr"))

                val json = JSONObject(respStr)
                val contentArray = json.getJSONArray("content")
                val textBuilder = StringBuilder()
                for (i in 0 until contentArray.length()) {
                    val part = contentArray.getJSONObject(i)
                    if (part.optString("type") == "text") {
                        textBuilder.append(part.optString("text"))
                    }
                }
                Result.success(textBuilder.toString().trim())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun analyzeRemovableSegments(
        context: Context,
        transcript: String,
        videoDurationSec: Double
    ): Result<List<com.github.libretube.api.obj.Segment>> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val provider = prefs.getString(KEY_AI_PROVIDER, "aikit") ?: "aikit"
        val token = prefs.getString(KEY_AI_TOKEN, "")?.trim().orEmpty()
        val model = prefs.getString(KEY_AI_MODEL, if (provider == "openrouter") DEFAULT_OPENROUTER_MODEL else DEFAULT_AIKIT_MODEL)?.trim().orEmpty()
        val baseUrl = prefs.getString(KEY_AI_BASE_URL, DEFAULT_AIKIT_URL)?.trim().orEmpty()

        if (token.isEmpty()) {
            return@withContext Result.failure(Exception("AI Token / API Key is missing. Configure it in AI Settings (⚙️)."))
        }

        val prompt = """
You are an expert educational video transcript analyst and timestamp extraction specialist.
Analyze the following timestamped video transcript. Identify portions that should be REMOVED from the educational content because they are:
1. SPONSOR / ADVERTISEMENT
2. NON-EDUCATIONAL CONTENT / SELFPROMO
3. JOKES / HUMOR / CASUAL BANTER / FILLER
4. TANGENTS / OFF-TOPIC DISCUSSION
5. INTRO / OUTRO

CATEGORIES (Strictly use only one of these):
- SPONSOR
- SELFPROMO
- INTERACTION
- INTRO
- OUTRO
- PREVIEW
- FILLER

OUTPUT FORMAT:
Return ONLY the table below:
| # | Start | End | Category | Reason |
| - | ----- | --- | -------- | ------ |
| 1 | 00:03:15 | 00:04:42 | SPONSOR | Product promotion |

AUTOMATION OUTPUT:
After the table, output ONLY the timestamp intervals and category, one per line:
[HH:MM:SS]-[HH:MM:SS] CATEGORY

If NO removable segments are found, return:
NO REMOVABLE SEGMENTS FOUND

Transcript:
$transcript
""".trimIndent()

        val aiResult = executePrompt(prompt, provider, token, model, baseUrl)
        if (aiResult.isFailure) {
            return@withContext Result.failure(aiResult.exceptionOrNull() ?: Exception("AI scan failed"))
        }

        val output = aiResult.getOrNull().orEmpty()
        if (output.contains("NO REMOVABLE SEGMENTS FOUND", ignoreCase = true)) {
            return@withContext Result.success(emptyList())
        }

        val sbUserId = com.github.libretube.helpers.PreferenceHelper.getSponsorBlockUserID()
        val segments = parseRemovableSegments(output, videoDurationSec, sbUserId)
        Result.success(segments)
    }

    private fun parseRemovableSegments(text: String, videoDurationSec: Double, userId: String): List<com.github.libretube.api.obj.Segment> {
        val result = mutableListOf<com.github.libretube.api.obj.Segment>()
        val intervalRegex = Regex("\\[?(\\d{1,2}(?::\\d{2}){1,2})\\]?\\s*-\\s*\\[?(\\d{1,2}(?::\\d{2}){1,2})\\]?\\s*(\\w*)")

        for (line in text.lines()) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            val match = intervalRegex.find(trimmed)
            if (match != null) {
                val startSec = parseSec(match.groupValues[1]).toFloat()
                val endSec = parseSec(match.groupValues[2]).toFloat()
                val catRaw = match.groupValues[3].lowercase()
                val cat = when (catRaw) {
                    "sponsor" -> "sponsor"
                    "selfpromo" -> "selfpromo"
                    "interaction" -> "interaction"
                    "intro" -> "intro"
                    "outro" -> "outro"
                    "preview" -> "preview"
                    "filler" -> "filler"
                    else -> "sponsor"
                }

                if (endSec > startSec) {
                    result.add(
                        com.github.libretube.api.obj.Segment(
                            uuid = java.util.UUID.randomUUID().toString(),
                            actionType = "skip",
                            category = cat,
                            description = "AI Detected $cat",
                            locked = 0,
                            segment = listOf(startSec, endSec),
                            userID = userId,
                            videoDuration = videoDurationSec,
                            votes = 1
                        )
                    )
                }
            }
        }
        return result
    }

    private fun executePrompt(prompt: String, provider: String, token: String, model: String, baseUrl: String): Result<String> {
        return try {
            if (provider == "openrouter") {
                val url = "https://openrouter.ai/api/v1/chat/completions"
                val bodyJson = JSONObject().apply {
                    put("model", model.ifEmpty { DEFAULT_OPENROUTER_MODEL })
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                }
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("HTTP-Referer", "https://github.com/libre-tube/LibreTube")
                    .addHeader("X-Title", "LibreTube Smart Chapters")
                    .addHeader("Content-Type", "application/json")
                    .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val respStr = response.body?.string() ?: ""
                if (!response.isSuccessful) return Result.failure(Exception("OpenRouter Error: $respStr"))

                val json = JSONObject(respStr)
                val content = json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                Result.success(content.trim())
            } else {
                // AIKit (Anthropic / Qwen proxy format)
                val cleanBase = baseUrl.ifEmpty { DEFAULT_AIKIT_URL }.trimEnd('/')
                val endpoint = if (cleanBase.endsWith("/messages")) cleanBase else "$cleanBase/messages"

                val bodyJson = JSONObject().apply {
                    put("model", model.ifEmpty { DEFAULT_AIKIT_MODEL })
                    put("max_tokens", 4000)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                }

                val request = Request.Builder()
                    .url(endpoint)
                    .addHeader("x-api-key", token)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("Content-Type", "application/json")
                    .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val respStr = response.body?.string() ?: ""
                if (!response.isSuccessful) return Result.failure(Exception("AIKit Error: $respStr"))

                val json = JSONObject(respStr)
                val contentArray = json.getJSONArray("content")
                val textBuilder = StringBuilder()
                for (i in 0 until contentArray.length()) {
                    val part = contentArray.getJSONObject(i)
                    if (part.optString("type") == "text") {
                        textBuilder.append(part.optString("text"))
                    }
                }
                Result.success(textBuilder.toString().trim())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun parseSec(tsStr: String): Long {
        val parts = tsStr.trim().split(':').mapNotNull { it.toLongOrNull() }
        return when (parts.size) {
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            2 -> parts[0] * 60 + parts[1]
            1 -> parts[0]
            else -> 0L
        }
    }
}