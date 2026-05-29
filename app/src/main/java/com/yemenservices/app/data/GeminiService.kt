package com.yemenservices.app.data

import android.util.Log
import com.yemenservices.app.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

object GeminiRetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val json = Json { ignoreUnknownKeys = true }
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApiService::class.java)
    }
}

class GeminiService {
    suspend fun getAssistantResponse(prompt: String, systemPrompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "mock_key_val") {
            return@withContext getMockResponse(prompt)
        }
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )
        try {
            val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: getMockResponse(prompt)
        } catch (e: Exception) {
            Log.e("GeminiService", "API Call failed. Using fallback response. Error: ${e.message}")
            getMockResponse(prompt)
        }
    }

    private fun getMockResponse(prompt: String): String {
        return when {
            prompt.contains("كهرباء") || prompt.contains("كهربائي") || prompt.contains("انارة") -> 
                "يمكنك تصفح قسم 'كهرباء منازل' للعثور على أقرب الفنيين المؤهلين لخدمتك في منطقتك باليمن صيانة وتركيب شبكات وكابلات وساعات كهربائية."
            prompt.contains("سباك") || prompt.contains("سباكة") || prompt.contains("مجاري") -> 
                "فنيو السباكة في اليمن بخدمتك! افتح قسم 'سباكة وصحي' للتواصل مع أفضل المعلمين لحل مشاكل التسريب وتركيب شبكات المياه والصرف وسخانات الطاقة الشمسية."
            prompt.contains("هاتف") || prompt.contains("جوال") || prompt.contains("تلفون") || prompt.contains("شاشة") -> 
                "لإصلاح هاتفك الذكي وتغيير الشاشات وبرمجة الرومات، تصفح قسم 'صيانة هواتف' للعثور على أمهر محلات الصيانة القريبة منك في اليمن."
            prompt.contains("تكييف") || prompt.contains("مكيف") || prompt.contains("تبريد") -> 
                "اضبط برودتك معنا! قسم 'صيانة تكييف' يوفر لك أفضل الفنيين لغسيل وشحن فريون وصيانة المكيفات المنزلية والمركزية والاسبليت."
            prompt.contains("نقل") || prompt.contains("أجرة") || prompt.contains("دينات") || prompt.contains("دينة") -> 
                "أنت بحاجة لنقل أثاث أو بحاجة مشوار تاكسي؟ افتح قسم 'نقل وأجرة' للاستعلام والاتصال المباشر مع مكاتب وسائقي الأجرة وسيارات الشحن في منطقتك."
            prompt.contains("خياط") || prompt.contains("خياطة") || prompt.contains("تطريز") || prompt.contains("فستان") -> 
                "لأرقى التصاميم وتفصيل الملابس والأثواب الرجالية والفساتين النسائية، تفضلوا بزيارة قسم 'خياطة وتطريز' لعرض المحلات الراقية."
            prompt.contains("دهان") || prompt.contains("دهانات") || prompt.contains("جبس") -> 
                "لتشطيب وجبس منزلك وترميم الجدران بأحدث ديكورات الطلاء الحديثة، تفضل بزيارة قسم 'دهانات وأعمال جبس' للتواصل مع معلم يمني محترف."
            prompt.contains("نجار") || prompt.contains("نجارة") || prompt.contains("أثاث") || prompt.contains("خشب") -> 
                "لتفصيل غرف النوم الفاخرة، المطابخ، الأبواب وصيانة الأثاث الخشبي، قسم 'نجارة وأثاث' يوفر لك أمهر النجارين الفنيين في مدينتك."
            prompt.contains("حديقة") || prompt.contains("مسبح") || prompt.contains("سباحة") -> 
                "لتنسيق وري وتزيين حدائق المنازل، وبناء أو تعقيم وصيانة الفلاتر للمسابح، تصفح الاختصاصيين في قسم 'حدائق ومسابح'."
            prompt.contains("برمجة") || prompt.contains("تطبيق") || prompt.contains("موقع") || prompt.contains("تطبيقات") -> 
                "لتطوير تطبيقات الموبايل الأندرويد والآيفون، المواقع الإلكترونية والأنظمة المحاسبية، زر الفنيين والمكاتب في قسم 'تطبيقات وبرمجة'."
            prompt.contains("تسويق") || prompt.contains("ترويج") || prompt.contains("اعلان") -> 
                "لتسويق منتجاتك وتكبير مبيعاتك في السوق اليمني، قسم 'تسويق إلكتروني' يضم أفضل وكالات الإعلان والمسوقين المحترفين على السوشيال ميديا."
            prompt.contains("محام") || prompt.contains("محامي") || prompt.contains("إستشارة") || prompt.contains("قانون") -> 
                "للاستشارات الشرعية والقانونية، كتابة العقود والمرافعات القضائية، تصفح المحامين المعتمدين في قسم 'محاماة واستشارات قانونية'."
            prompt.contains("دكتور") || prompt.contains("طبيب") || prompt.contains("صحة") || prompt.contains("عيادة") -> 
                "نهتم بعافيتكم! تجدون في قسم 'طب وصحة' نخبة من الأطباء، والمستوصفات، ومقدمي الرعاية الطبية المناسبة في مختلف التخصصات باليمن."
            prompt.contains("مدرسة") || prompt.contains("تعليم") || prompt.contains("تدريب") || prompt.contains("كورس") -> 
                "لمستقبل مشرق لتعليم الأبناء أو لتعلم اللغات والتنمية البشرية، قسم 'تعليم وتدريب' يضم كوكبة معلمين متميزين ومعاهد تدريب معتمدة."
            prompt.contains("سفر") || prompt.contains("طيران") || prompt.contains("سياحة") || prompt.contains("فيزا") -> 
                "لحجز تذاكر الطيران، السفر البري الداخلي والخارجي، ومعاملات الفيزا والترجمة، تصفح الوكالات في قسم 'سياحة وسفر'."
            prompt.contains("حشرات") || prompt.contains("تنظيف") || prompt.contains("رش") -> 
                "لغسيل الفلل والبيوت وإبادة الحشرات والآفات ورش المبيدات الفعالة والآمنة، تفضل بالتواصل مع شركات ومكاتب قسم 'تنظيف ومكافحة حشرات'."
            else -> 
                "أهلاً بكم في تطبيق دليل الخدمات اليمني الموحد! من فضلك اسألني عن أي خدمة تريدها (مثل: صيانة مكثفة، كهرباء، برمجة وتطبيقات، تسويق، أو رش حشرات) وسأرشدك لأفضل فني قريب منك."
        }
    }
}
