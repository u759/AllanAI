package com.example.myapplication.di

import android.content.Context
import com.example.myapplication.data.local.AuthManager
import com.example.myapplication.data.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for authentication dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    
    @Provides
    @Singleton
    fun provideAuthManager(
        @ApplicationContext context: Context
    ): AuthManager {
        return AuthManager(context)
    }
    
    @Provides
    @Singleton
    fun provideAuthRepository(
        authManager: AuthManager
    ): AuthRepository {
        return AuthRepository(authManager)
    }
}
