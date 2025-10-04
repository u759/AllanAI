package com.example.myapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.myapplication.ui.screens.highlights.HighlightsListScreen
import com.example.myapplication.ui.screens.history.MatchDetailScreen
import com.example.myapplication.ui.screens.history.MatchListScreen
import com.example.myapplication.ui.screens.profile.ProfileScreen
import com.example.myapplication.ui.screens.upload.UploadScreen
import com.example.myapplication.ui.screens.upload.WelcomeUpload

/**
 * Main navigation graph for AllanAI app
 * 
 * This sets up all navigation routes and handles navigation between screens
 * following the MVVM architecture pattern
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Welcome.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Welcome/Home Screen
        composable(route = Screen.Welcome.route) {
            WelcomeUpload(
                onUploadClick = {
                    navController.navigate(Screen.Upload.route)
                },
                onRecordClick = {
                    // TODO: Navigate to record screen when implemented
                    navController.navigate(Screen.Upload.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToHighlights = {
                    navController.navigate(Screen.Highlights.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }
        
        // Upload Screen
        composable(route = Screen.Upload.route) {
            UploadScreen(
                onNavigateBack = {
                    navController.navigateUp()
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToHighlights = {
                    navController.navigate(Screen.Highlights.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }
        
        // History Screen (Match List)
        composable(route = Screen.History.route) {
            MatchListScreen(
                onNavigateBack = {
                    navController.navigateUp()
                },
                onNavigateToUpload = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToHighlights = {
                    navController.navigate(Screen.Highlights.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onMatchClick = { matchId ->
                    navController.navigate(Screen.MatchDetail.createRoute(matchId))
                }
            )
        }
        
        // Match Detail Screen
        composable(
            route = Screen.MatchDetail.route,
            arguments = listOf(
                navArgument("matchId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
            MatchDetailScreen(
                matchId = matchId,
                onNavigateBack = {
                    navController.navigateUp()
                },
                onNavigateToUpload = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route) {
                        popUpTo(Screen.History.route) { inclusive = true }
                    }
                },
                onNavigateToHighlights = {
                    navController.navigate(Screen.Highlights.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }
        
        // Highlights Screen
        composable(route = Screen.Highlights.route) {
            HighlightsListScreen(
                onNavigateBack = {
                    navController.navigateUp()
                },
                onNavigateToUpload = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onHighlightClick = { highlightId ->
                    // TODO: Navigate to highlight detail screen when implemented
                    // For now, you could navigate to match detail or a video player
                }
            )
        }
        
        // Profile Screen
        composable(route = Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = {
                    navController.navigateUp()
                },
                onEditProfile = {
                    // TODO: Navigate to edit profile screen when implemented
                },
                onChangePassword = {
                    // TODO: Navigate to change password screen when implemented
                },
                onNotifications = {
                    // TODO: Navigate to notifications settings when implemented
                },
                onLogout = {
                    // TODO: Handle logout and navigate to login/welcome
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
