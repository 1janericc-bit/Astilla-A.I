package com.example.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiCloudService {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    fun getEffectiveApiKey(customKey: String): String {
        return when {
            customKey.isNotBlank() -> customKey
            try { BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" } catch (e: Throwable) { false } -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }
    }

    suspend fun generateResponse(
        apiKey: String,
        prompt: String,
        systemInstruction: String,
        chatHistory: List<Pair<String, String>> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("No valid API Key provided"))
        }

        try {
            val root = JSONObject()

            // System instruction
            if (systemInstruction.isNotBlank()) {
                val sysInstObj = JSONObject()
                val sysPartsArray = JSONArray().apply {
                    put(JSONObject().apply { put("text", systemInstruction) })
                }
                sysInstObj.put("parts", sysPartsArray)
                root.put("systemInstruction", sysInstObj)
            }

            // Contents array
            val contentsArray = JSONArray()

            // Recent history
            chatHistory.takeLast(6).forEach { (role, text) ->
                val geminiRole = if (role == "user") "user" else "model"
                val contentObj = JSONObject().apply {
                    put("role", geminiRole)
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", text) })
                    })
                }
                contentsArray.put(contentObj)
            }

            // Current prompt
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", prompt) })
                })
            })

            root.put("contents", contentsArray)

            val url = "$BASE_URL?key=$apiKey"
            val body = root.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Gemini API error ${response.code}: $responseBody"))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")

            if (text != null && text.isNotBlank()) {
                Result.success(text)
            } else {
                Result.failure(Exception("Empty response from Gemini"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
