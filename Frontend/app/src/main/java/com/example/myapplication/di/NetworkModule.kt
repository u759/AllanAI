package com.example.myapplication.di

import com.example.myapplication.data.api.AllanAIApiService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module that provides networking dependencies.
 * 
 * This module configures Retrofit, OkHttp, and related networking
 * components following the architecture guidelines.
 * 
 * Key features:
 * - HTTP logging for debugging
 * - Increased timeouts for video uploads
 * - Gson JSON converter
 * - Singleton instances for efficiency
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    /**
     * Base URL for the AllanAI backend API.
     * 
     * - For Android Emulator: Use 10.0.2.2 (special alias for host machine's localhost)
     * - For Physical Device: Replace with actual IP address (e.g., "http://192.168.1.100:8080/")
     */
    private const val BASE_URL = "http://10.0.2.2:8080/"
    
    /**
     * Provides Gson instance for JSON serialization/deserialization.
     */
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }
    
    /**
     * Provides HTTP logging interceptor for debugging network requests.
     * 
     * This logs all HTTP requests and responses, which is helpful for
     * debugging API communication issues.
     */
    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }
    
    /**
     * Provides configured OkHttpClient.
     * 
     * Configuration:
     * - 60 second timeouts for video uploads
     * - HTTP request/response logging
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * Provides Retrofit instance.
     * 
     * Retrofit is the HTTP client that handles API communication.
     * It uses OkHttp under the hood and converts JSON to Kotlin objects.
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    /**
     * Provides AllanAIApiService instance.
     * 
     * This is the main API service interface that defines all endpoints.
     * Retrofit automatically implements this interface at runtime.
     */
    @Provides
    @Singleton
    fun provideAllanAIApiService(retrofit: Retrofit): AllanAIApiService {
        return retrofit.create(AllanAIApiService::class.java)
    }
}
