package com.example.myapplication.di

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
 * To switch between Mock and production API implementations:
 * - Development: Bind MockMatchRepository (current)
 * - Production: Bind ApiMatchRepository (future)
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Provides the MatchRepository implementation.
     *
     * Currently binds to MockMatchRepository for development.
     * When backend API is ready, change this to:
     * @Binds
     * @Singleton
     * abstract fun bindMatchRepository(impl: ApiMatchRepository): MatchRepository
     */
    @Binds
    @Singleton
    abstract fun bindMatchRepository(
        impl: MockMatchRepository
    ): MatchRepository
}
