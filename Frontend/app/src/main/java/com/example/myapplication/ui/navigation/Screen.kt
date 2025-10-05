package com.example.myapplication.ui.navigation

/**
 * Screen routes for navigation in AllanAI app
 * 
 * Each sealed class represents a destination in the app
 */
sealed class Screen(val route: String) {
    object SignIn : Screen("sign_in")
    object SignUp : Screen("sign_up")
    object Welcome : Screen("welcome")
    object Upload : Screen("upload")
    object Record : Screen("record")
    object History : Screen("history")
    object MatchDetail : Screen("match_detail/{matchId}") {
        fun createRoute(matchId: String) = "match_detail/$matchId"
    }
    object Highlights : Screen("highlights")
    object HighlightDetail : Screen("highlight_detail/{matchId}") {
        fun createRoute(matchId: String) = "highlight_detail/$matchId"
    }
    object Profile : Screen("profile")
    object EditProfile : Screen("edit_profile")
    object ChangePassword : Screen("change_password")
}
