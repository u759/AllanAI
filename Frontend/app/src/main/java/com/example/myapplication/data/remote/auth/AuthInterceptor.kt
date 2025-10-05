package com.example.myapplication.data.remote.auth

import com.example.myapplication.data.local.AuthLocalDataSource
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds the Authorization header when a session token is available.
 */
class AuthInterceptor(
    private val localDataSource: AuthLocalDataSource
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()
        localDataSource.getAuthorizationHeader()?.let { header ->
            builder.addHeader("Authorization", header)
        }
        return chain.proceed(builder.build())
    }
}
