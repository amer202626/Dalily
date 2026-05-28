package com.example.data

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

interface SupabaseService {
    @GET("rest/v1/categories")
    suspend fun getCategories(): List<Category>

    @POST("rest/v1/categories")
    suspend fun createCategory(@Body category: Category): Response<Unit>

    @PATCH("rest/v1/categories")
    suspend fun updateCategory(
        @Query("id") idFilter: String,
        @Body updates: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    @DELETE("rest/v1/categories")
    suspend fun deleteCategory(
        @Query("id") idFilter: String
    ): Response<Unit>

    @GET("rest/v1/service_providers")
    suspend fun getServiceProviders(): List<ServiceProvider>

    @POST("rest/v1/service_providers")
    suspend fun createServiceProvider(@Body provider: ServiceProvider): Response<Unit>

    @PATCH("rest/v1/service_providers")
    suspend fun updateServiceProvider(
        @Query("id") idFilter: String,
        @Body updates: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    @DELETE("rest/v1/service_providers")
    suspend fun deleteServiceProvider(
        @Query("id") idFilter: String
    ): Response<Unit>

    @GET("rest/v1/reviews")
    suspend fun getReviews(): List<Review>

    @POST("rest/v1/reviews")
    suspend fun createReview(@Body review: Review): Response<Unit>

    @DELETE("rest/v1/reviews")
    suspend fun deleteReview(
        @Query("id") idFilter: String
    ): Response<Unit>

    @GET("rest/v1/admins")
    suspend fun getAdmins(): List<Admin>

    @POST("rest/v1/admins")
    suspend fun createAdmin(@Body admin: Admin): Response<Unit>

    @PATCH("rest/v1/admins")
    suspend fun updateAdmin(
        @Query("username") usernameFilter: String,
        @Body updates: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    @DELETE("rest/v1/admins")
    suspend fun deleteAdmin(
        @Query("username") usernameFilter: String
    ): Response<Unit>

    // Sync Events API
    @GET("rest/v1/sync_events")
    suspend fun getSyncEvents(
        @Query("created_at") createdAtFilter: String,
        @Query("order") order: String = "created_at.asc"
    ): List<SyncEvent>

    @POST("rest/v1/sync_events")
    suspend fun createSyncEvent(
        @Body event: SyncEvent
    ): Response<Unit>

    @GET("rest/v1/categories")
    suspend fun getCategoryById(
        @Query("id") idFilter: String,
        @Query("select") select: String = "*"
    ): List<Category>

    @GET("rest/v1/service_providers")
    suspend fun getServiceProviderById(
        @Query("id") idFilter: String,
        @Query("select") select: String = "*"
    ): List<ServiceProvider>

    @GET("rest/v1/admins")
    suspend fun getAdminByUsername(
        @Query("username") usernameFilter: String,
        @Query("select") select: String = "*"
    ): List<Admin>
}

object SupabaseClient {
    // Provide fallback values if NOT found in BuildConfig
    const val DEFAULT_URL = "https://dalili-yemen-placeholder.supabase.co"
    const val DEFAULT_ANON_KEY = "placeholder-anon-key"

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun getService(): SupabaseService {
        val baseUrl = try {
            val url = com.example.BuildConfig::class.java.getField("SUPABASE_URL").get(null) as String
            if (url.isNotBlank()) url else DEFAULT_URL
        } catch (e: Exception) {
            DEFAULT_URL
        }

        val anonKey = try {
            val key = com.example.BuildConfig::class.java.getField("SUPABASE_ANON_KEY").get(null) as String
            if (key.isNotBlank()) key else DEFAULT_ANON_KEY
        } catch (e: Exception) {
            DEFAULT_ANON_KEY
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val requestBuilder = originalRequest.newBuilder()
                    .header("apikey", anonKey)
                    .header("Authorization", "Bearer $anonKey")
                chain.proceed(requestBuilder.build())
            }
            .addInterceptor(loggingInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(SupabaseService::class.java)
    }
}
