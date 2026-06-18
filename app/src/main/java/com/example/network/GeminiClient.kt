package com.example.network

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getCaptionsForImage(bitmap: Bitmap): List<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Provide witty fallbacks typical of a sticker keyboard
            return@withContext listOf("BEST BOY", "SASSY", "ICONIC", "CURRENT MOOD", "WILDIN'")
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        // Downscale image drastically for lightning-fast payload uploads over internet
        val targetSize = 256
        val ratio = targetSize.toFloat() / Math.max(bitmap.width, bitmap.height)
        val scaledWidth = (bitmap.width * ratio).toInt()
        val scaledHeight = (bitmap.height * ratio).toInt()
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val imageBytes = outputStream.toByteArray()
        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        val prompt = "Check out this isolated foreground object for a sticker pack. Recommend 5 short, humorous, or punchy message bubbles or caption tags suitable for this sticker. Max 2 words per caption. All caps. Return strictly as a JSON array of strings, e.g. [\"CUTE BUT WILD\", \"NO EXCUSE\", \"MOOD\", \"BOSS ENERGY\", \"ICONIC\"]."

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext listOf("DRAMATIC", "SASSY", "BEST BOY", "WILDIN'", "ICONIC")
            }

            val responseBody = response.body?.string() ?: return@withContext listOf("DRAMATIC", "SASSY", "BEST BOY", "WILDIN'", "ICONIC")
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates") ?: return@withContext listOf("DRAMATIC", "SASSY", "BEST BOY", "WILDIN'", "ICONIC")
            val firstCandidate = candidates.optJSONObject(0) ?: return@withContext listOf("DRAMATIC", "SASSY", "BEST BOY", "WILDIN'", "ICONIC")
            val content = firstCandidate.optJSONObject("content") ?: return@withContext listOf("DRAMATIC", "SASSY", "BEST BOY", "WILDIN'", "ICONIC")
            val parts = content.optJSONArray("parts") ?: return@withContext listOf("DRAMATIC", "SASSY", "BEST BOY", "WILDIN'", "ICONIC")
            val firstPart = parts.optJSONObject(0) ?: return@withContext listOf("DRAMATIC", "SASSY", "BEST BOY", "WILDIN'", "ICONIC")
            val responseText = firstPart.optString("text", "")

            val cleanText = responseText.trim()
            val array = if (cleanText.startsWith("[")) {
                JSONArray(cleanText)
            } else {
                JSONArray().apply {
                    put("CURRENT MOOD")
                    put("NO CAP")
                    put("BEST BOY")
                    put("SASSY")
                    put("ICONIC")
                }
            }

            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i).uppercase())
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            listOf("CUTE", "BIG MOOD", "LEGENDARY", "SQUAD GOALS", "ICONIC")
        }
    }
}
