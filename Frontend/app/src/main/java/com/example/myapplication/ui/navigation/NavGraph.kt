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
import com.example.myapplication.ui.screens.profile.ChangePasswordScreen
import com.example.myapplication.ui.screens.profile.EditProfileScreen
import com.example.myapplication.ui.screens.profile.ProfileScreen
import com.example.myapplication.ui.screens.profile.SignInScreen
import com.example.myapplication.ui.screens.profile.SignUpScreen
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
        // Sign In Screen
        composable(route = Screen.SignIn.route) {
            SignInScreen(
                onSignInClick = { username, password ->
                    // TODO: Implement authentication logic with ViewModel
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.SignIn.route) { inclusive = true }
                    }
                },
                onSignUpClick = {
                    navController.navigate(Screen.SignUp.route)
                },
                onForgotPasswordClick = {
                    // TODO: Navigate to forgot password screen when implemented
                },
                onNavigateToUpload = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.SignIn.route) { inclusive = true }
                    }
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
        
        // Sign Up Screen
        composable(route = Screen.SignUp.route) {
            SignUpScreen(
                onSignUpClick = { username, email, password, confirmPassword ->
                    // TODO: Implement registration logic with ViewModel
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                },
                onSignInClick = {
                    navController.navigateUp()
                },
                onNavigateToUpload = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
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
                    navController.navigate(Screen.EditProfile.route)
                },
                onChangePassword = {
                    navController.navigate(Screen.ChangePassword.route)
                },
                onLogout = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToUpload = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToHighlights = {
                    navController.navigate(Screen.Highlights.route)
                }
            )
        }
        
        // Edit Profile Screen
        composable(route = Screen.EditProfile.route) {
            EditProfileScreen(
                onNavigateBack = {
                    navController.navigateUp()
                },
                onSaveChanges = { fullName, email ->
                    // TODO: Implement save changes logic with ViewModel
                    navController.navigateUp()
                }
            )
        }
        
        // Change Password Screen
        composable(route = Screen.ChangePassword.route) {
            ChangePasswordScreen(
                onNavigateBack = {
                    navController.navigateUp()
                },
                onUpdatePassword = { currentPassword, newPassword, confirmPassword ->
                    // TODO: Implement password update logic with ViewModel
                    navController.navigateUp()
                }
            )
        }
    }
}
