package com.example.myapplication.di

import com.example.myapplication.data.repository.ApiMatchRepository
import com.example.myapplication.data.repository.MatchRepository
import com.example.myapplication.data.repository.MockMatchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides repository implementations.
 *
 * This module uses @Binds to map interfaces to their implementations.
 * Hilt will automatically inject the correct implementation wherever
 * MatchRepository is requested.
 *
 * Current implementation: ApiMatchRepository (Production API)
 * - Communicates with Spring Boot backend
 * - Uses Retrofit for HTTP requests
 * - Maps API DTOs to domain models
 *
 * To switch back to mock data for testing:
 * - Replace ApiMatchRepository with MockMatchRepository
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Provides the MatchRepository implementation.
     *
     * Now using ApiMatchRepository for real backend communication.
     * The API service is automatically injected by NetworkModule.
     */
    @Binds
    @Singleton
    abstract fun bindMatchRepository(
        impl: ApiMatchRepository
    ): MatchRepository
}
