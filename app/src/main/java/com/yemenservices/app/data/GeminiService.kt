package com.yemenservices.app.data

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiService {
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"
    
    // Fallback key or empty
    private const val API_KEY = "AIzaSyCpxwXZZKrN4h2AmyuEkzyat0K4LOUAXD8" // User's API key can be used as fallback

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    suspend fun chatWithAi(prompt: String, isAr: Boolean): String = withContext(Dispatchers.IO) {
        try {
            val endpoint = "$BASE_URL?key=$API_KEY"
            
            val systemDoc = if (isAr) {
                "أنت المساعد الذكي المدمج في تطبيق (دليل خدمات اليمن). أجب باختصار ولطف، وعرّف المستخدم بالخدمات المتاحة وكيف يمكنه العثور على حلاقين، كهربائيين، ميكانيكيين، أطباء إلخ في صنعاء، عدن، تعز ومحافظات اليمن الأخرى. لا تذكر تفاصيل تقنية داخلية."
            } else {
                "You are the intelligent assistant in the Yemen Services Directory app. Respond briefly and politely. Help users find repair services, plumbers, doctors, and other providers in Sana'a, Aden, Taiz, and other cities. Keep details simple."
            }

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemDoc) })
                    })
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    val jsonResponse = JSONObject(responseBody)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Gemini request error: ${e.message}")
        }

        // Resilient Offline/Fallback AI generation in Arabic and English
        return@withContext getOfflineResponse(prompt, isAr)
    }

    private fun getOfflineResponse(prompt: String, isAr: Boolean): String {
        return if (isAr) {
            when {
                prompt.contains("كهربائي") || prompt.contains("صيانة") -> {
                    "أهلاً بك! يمكنك البحث عن فنيي صيانة وكهرباء متميزين في قسم 'الهندسة والصيانة' بالرئيسية. ستجد في الدليل فنيين موثوقين وموقع عملهم وسكنهم."
                }
                prompt.contains("طبيب") || prompt.contains("صحة") || prompt.contains("مستشفى") -> {
                    "مرحباً بك. يتوفر أطباء وممرضون متميزون في قسم 'الطب والتمريض والرعاية الصحة'. ابحث في الرئيسية وتواصل معهم مباشرة."
                }
                prompt.contains("كيف") || prompt.contains("طريقة") -> {
                    "استخدام التطبيق سهل للغاية! اختر القسم المناسب من الشاشة الرئيسية، واقرأ تفاصيل وموقع مقدم الخدمة، أو تواصل معه هاتفياً بنقرة زر."
                }
                else -> {
                    "مرحباً! أنا مساعد دليل الخدمات اليمني الذكي. يمكنني مساعدتك في العثور على أفضل الأطباء والمهندسين والكهربائيين والميكانيكيين في مختلف محافظات اليمن. ما الذي تبحث عنه اليوم؟"
                }
            }
        } else {
            when {
                prompt.contains("repair") || prompt.contains("maintenance") -> {
                    "Hello! You can search for excellent maintenance technicians under 'Engineering & Maintenance' category on the homescreen."
                }
                prompt.contains("doctor") || prompt.contains("health") -> {
                    "Hi! Qualified doctors & nurses are listed under 'Healthcare & Nursing' category. Browse their profiles and call them easily."
                }
                else -> {
                    "Welcome! I am the Smart Assistant for Yemen Services Directory. I can guide you to find doctors, electricians, mechanics, and other services across Yemen. What are you looking for today?"
                }
            }
        }
    }
}
