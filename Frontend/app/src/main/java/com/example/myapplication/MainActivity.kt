package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.myapplication.ui.screens.history.MatchListScreen
import com.example.myapplication.ui.screens.history.MatchDetailScreen
import com.example.myapplication.ui.screens.upload.WelcomeUpload
import com.example.myapplication.ui.screens.upload.UploadScreen
import com.example.myapplication.ui.screens.highlights.HighlightsListScreen
import com.example.myapplication.ui.screens.profile.ProfileScreen
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // View different screens by changing which one is called:
                WelcomeUpload()          // Home/Landing screen
                // UploadScreen()        // Upload with progress
                // MatchListScreen()     // History list
                // MatchDetailScreen()   // Match analysis
                // HighlightsListScreen() // Highlights grid
                // ProfileScreen()       // Profile & settings
            }
        }
    }
}