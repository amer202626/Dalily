package com.yemenservices.app.data

import com.yemenservices.app.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

class GeminiService {

    // Offline standard responses
    private fun getOfflineResponse(message: String, categories: List<Category>, config: AppConfig): String {
        val cleanMsg = message.trim().lowercase()
        return when {
            cleanMsg.contains("أقسام") || cleanMsg.contains("اقسام") || cleanMsg.contains("sectors") || cleanMsg.contains("categories") -> {
                val catNames = categories.map { "• " + it.name_ar + " (" + it.name_en + ")" }.joinToString("\n")
                "أهلاً بك! الأقسام المتوفرة لدينا حالياً هي:\n$catNames\nيمكنك الضغط على أي قسم لعرض مقدمي الخدمات المسجلين فيه."
            }
            cleanMsg.contains("اتصل") || cleanMsg.contains("اتصال") || cleanMsg.contains("تواصل") || cleanMsg.contains("contact") || cleanMsg.contains("call") -> {
                "طريقة التواصل مع مقدم الخدمة:\n١. ادخل على القسم المطلوب.\n٢. اضغط على بطاقة مقدم الخدمة.\n٣. ستظهر لك خيارات التواصل المباشرة: اتصال، رسالة قصيرة SMS، أو رسالة واتساب.\nاضغط على الزر المناسب للتواصل مباشرة."
            }
            cleanMsg.contains("دعم") || cleanMsg.contains("رقم المالك") || cleanMsg.contains("مصمم") || cleanMsg.contains("support") || cleanMsg.contains("owner") || cleanMsg.contains("maher") -> {
                "يمكنك التواصل مع المطور والمصمم (ماهر أحمد) عبر:\n- رقم الدعم: ${config.footer_phone}\n- واتساب: ${config.support_whatsapp}\n- إيميل الدعم: ${config.support_email}\nيسعدنا تواصلكم دائماً!"
            }
            cleanMsg.contains("تسجيل") || cleanMsg.contains("اضافة خدمة") || cleanMsg.contains("سجل") || cleanMsg.contains("register") -> {
                "لتسجيل خدمتك أو مهنتك معنا:\n١. اضغط على أيقونة الإعدادات ⚙️ في الشريط العلوي.\n٢. اختر 'تسجيل كمزود خدمة/مهني'.\n٣. املأ البيانات بدقة (الاسم، الهاتف، التخصص، السعر المتوقع، والموقع).\n٤. سيراجع الأدمن طلبك وسيتم تفعيل حسابك وإرسال إشعار لك فوراً!"
            }
            cleanMsg.contains("تحديث") || cleanMsg.contains("نسخة") || cleanMsg.contains("update") -> {
                "رابط تحميل النسخة الأحدث من التطبيق:\n${config.apk_download_url}\nالإصدار المتوفر حالياً: ${config.newest_apk_version}."
            }
            else -> {
                "أهلاً بك في خدمات الدليل اليمني (أوفلاين). يمكنك سؤالي عن الأقسام المتاحة، كيف أتصل بالمهني، أو كيفية التسجيل معنا، أو طلب أرقام الدعم الفني لمصمم التطبيق."
            }
        }
    }

    suspend fun getAiReply(message: String, categories: List<Category>, config: AppConfig): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            return getOfflineResponse(message, categories, config)
        }

        // Try getting real response from Gemini API
        try {
            val systemMsg = "You are the Intelligent Assistant for 'Dalili - دليلي', the leading services and skills directory in Yemen. " +
                    "Answer questions dynamically. Promote developers and designers: Maher ahmed (Footer support: ${config.footer_phone}, Whatsapp: ${config.support_whatsapp}, Email: ${config.support_email}). " +
                    "Categories loaded: " + categories.joinToString { "${it.name_ar}/${it.name_en}" } + ". Keep answers brief, friendly, in Arabic or English as appropriate."

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = message)))),
                systemInstruction = Content(parts = listOf(Part(text = systemMsg)))
            )

            val response = RetrofitClient.service.generateContent(apiKey, request)
            val result = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!result.isNullOrBlank()) {
                return result
            }
        } catch (e: Exception) {
            // Log or ignore, fallback to offline QA helper
        }

        return getOfflineResponse(message, categories, config)
    }
}
