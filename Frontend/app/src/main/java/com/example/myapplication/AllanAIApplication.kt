package com.example.myapplication

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * AllanAI Application class.
 *
 * This class is annotated with @HiltAndroidApp to trigger Hilt's code generation,
 * including a base class for the application that serves as the application-level
 * dependency container.
 *
 * Make sure to reference this class in AndroidManifest.xml:
 * <application android:name=".AllanAIApplication" ...>
 */
@HiltAndroidApp
class AllanAIApplication : Application()
