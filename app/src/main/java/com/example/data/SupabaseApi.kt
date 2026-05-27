package com.example.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

interface SupabaseService {
    @GET("rest/v1/categories")
    suspend fun getCategories(
        @Query("select") select: String = "*",
        @Query("order") order: String = "order_index.asc"
    ): List<Category>

    @POST("rest/v1/categories")
    suspend fun createCategory(
        @Body category: Category
    ): Response<Unit>

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
    suspend fun getServiceProviders(
        @Query("select") select: String = "*"
    ): List<ServiceProvider>

    @POST("rest/v1/service_providers")
    suspend fun createServiceProvider(
        @Body provider: ServiceProvider
    ): Response<Unit>

    @PATCH("rest/v1/service_providers")
    suspend fun updateServiceProvider(
        @Query("id") idFilter: String,
        @Body updates: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    @DELETE("rest/v1/service_providers")
    suspend fun deleteServiceProvider(
        @Query("id") idFilter: String
    ): Response<Unit>

    @GET("rest/v1/admins")
    suspend fun getAdmins(
        @Query("select") select: String = "*"
    ): List<Admin>

    @POST("rest/v1/admins")
    suspend fun createAdmin(
        @Body admin: Admin
    ): Response<Unit>

    @PATCH("rest/v1/admins")
    suspend fun updateAdmin(
        @Query("username") usernameFilter: String,
        @Body updates: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    @DELETE("rest/v1/admins")
    suspend fun deleteAdmin(
        @Query("username") usernameFilter: String
    ): Response<Unit>

    @POST("rest/v1/login_attempts")
    suspend fun logLoginAttempt(
        @Body attempt: LoginAttempt
    ): Response<Unit>
}

object SupabaseClient {
    private const val BASE_URL = "https://sazbudkuzxbvmuztaxeg.supabase.co/"
    private const val API_KEY = "sb_publishable_vvR8V-Y4Ge4-PMZa1AuFnQ_t9TJrwnx"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("apikey", API_KEY)
                .header("Authorization", "Bearer $API_KEY")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val service: SupabaseService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(SupabaseService::class.java)
    }
}
