package com.example.myapplication.di

import android.content.Context
import com.example.myapplication.data.local.AuthLocalDataSource
import com.example.myapplication.data.remote.auth.AuthApi
import com.example.myapplication.data.remote.auth.AuthRemoteDataSource
import com.example.myapplication.data.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Hilt module for authentication dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideAuthLocalDataSource(
        @ApplicationContext context: Context
    ): AuthLocalDataSource {
        return AuthLocalDataSource(context)
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthRemoteDataSource(
        authApi: AuthApi
    ): AuthRemoteDataSource {
        return AuthRemoteDataSource(authApi)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        authLocalDataSource: AuthLocalDataSource,
        authRemoteDataSource: AuthRemoteDataSource
    ): AuthRepository {
        return AuthRepository(authLocalDataSource, authRemoteDataSource)
    }
}
