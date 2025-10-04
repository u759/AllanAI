package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.myapplication.ui.screens.matches.MatchListScreen
import com.example.myapplication.ui.screens.matches.MatchDetailScreen
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Change this to UploadScreen() or MatchDetailScreen() to view other screens
                //MatchListScreen()
                MatchDetailScreen()
            }
        }
    }
}