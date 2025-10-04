package com.example.myapplication.ui.navigation

/**
 * Screen routes for navigation in AllanAI app
 * 
 * Each sealed class represents a destination in the app
 */
sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Upload : Screen("upload")
    object History : Screen("history")
    object MatchDetail : Screen("match_detail/{matchId}") {
        fun createRoute(matchId: String) = "match_detail/$matchId"
    }
    object Highlights : Screen("highlights")
    object Profile : Screen("profile")
}
