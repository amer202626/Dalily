package com.yemenservices.app.data

import android.util.Log
import com.yemenservices.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import org.json.JSONArray

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL = "gemini-3.5-flash"
    
    // We get API key dynamically from BuildConfig or fallback
    private fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    suspend fun generateResponse(prompt: String, systemInstruction: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext "عذراً، يرجى تهيئة مفتاح الذكاء الاصطناعي (Gemini API Key) في الإعدادات."
        }
        
        val urlString = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey"
        Log.d(TAG, "Calling Gemini API endpoint...")
        
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 30000

            // Construct JSON request body using standard org.json to avoid external serialization dependency issues
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    }
                    put(JSONObject().apply {
                        put("parts", partsArray)
                    })
                }
                put("contents", contentsArray)

                if (systemInstruction.isNotBlank()) {
                    val systemContent = JSONObject().apply {
                        val partsArr = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", systemInstruction)
                            })
                        }
                        put("parts", partsArr)
                    }
                    put("systemInstruction", systemContent)
                }
            }

            val requestBody = requestJson.toString()
            Log.d(TAG, "Request payload: $requestBody")

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(requestBody)
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val textResponse = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "Response payload: $textResponse")
                
                // Parse generated content
                val responseJson = JSONObject(textResponse)
                val candidates = responseJson.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.getJSONObject("content")
                    val partsArr = contentObj.getJSONArray("parts")
                    if (partsArr.length() > 0) {
                        return@withContext partsArr.getJSONObject(0).getString("text")
                    }
                }
                "عذراً، لم يتم العثور على رد من الذكاء الاصطناعي."
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e(TAG, "API Error: Response Code $responseCode, Error: $errorStream")
                "خطأ في الاتصال بالذكاء الاصطناعي: $responseCode"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini Call", e)
            "حدث خطأ أثناء محاولة الاتصال بـ Google Gemini: ${e.message}"
        } finally {
            connection?.disconnect()
        }
    }
}
