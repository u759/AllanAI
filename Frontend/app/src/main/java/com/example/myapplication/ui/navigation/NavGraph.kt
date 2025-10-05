package com.example.myapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.myapplication.ui.screens.highlights.HighlightsListScreen
import com.example.myapplication.ui.screens.highlights.HighlightsViewModel
import com.example.myapplication.ui.screens.history.MatchDetailScreen
import com.example.myapplication.ui.screens.history.MatchListScreen
import com.example.myapplication.ui.screens.history.MatchesViewModel
import com.example.myapplication.ui.screens.profile.AuthViewModel
import com.example.myapplication.ui.screens.profile.ChangePasswordScreen
import com.example.myapplication.ui.screens.profile.ChangePasswordViewModel
import com.example.myapplication.ui.screens.profile.EditProfileScreen
import com.example.myapplication.ui.screens.profile.EditProfileViewModel
import com.example.myapplication.ui.screens.profile.ProfileScreen
import com.example.myapplication.ui.screens.profile.ProfileViewModel
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
    navController: NavHostController
) {
    // Get AuthViewModel to check login state
    val authViewModel: AuthViewModel = hiltViewModel()
    val authRepository = authViewModel.authRepository
    
    // Determine start destination based on auth state
    val startDestination = if (authRepository.isLoggedIn()) {
        Screen.Welcome.route
    } else {
        Screen.SignIn.route
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Sign In Screen
        composable(route = Screen.SignIn.route) {
            val viewModel: AuthViewModel = hiltViewModel()
            val state by viewModel.signInState.collectAsState()
            
            // Handle successful login
            LaunchedEffect(state.isSuccess) {
                if (state.isSuccess) {
                    viewModel.resetSignInState()
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.SignIn.route) { inclusive = true }
                    }
                }
            }
            
            SignInScreen(
                state = state,
                onSignInClick = { username, password ->
                    viewModel.signIn(username, password)
                },
                onSignUpClick = {
                    navController.navigate(Screen.SignUp.route)
                },
                onForgotPasswordClick = {
                    // TODO: Navigate to forgot password screen when implemented
                }
            )
        }
        
        // Sign Up Screen
        composable(route = Screen.SignUp.route) {
            val viewModel: AuthViewModel = hiltViewModel()
            val state by viewModel.signUpState.collectAsState()
            
            // Handle successful signup
            LaunchedEffect(state.isSuccess) {
                if (state.isSuccess) {
                    viewModel.resetSignUpState()
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                }
            }
            
            SignUpScreen(
                state = state,
                onSignUpClick = { username, email, password, confirmPassword ->
                    viewModel.signUp(username, email, password, confirmPassword)
                },
                onSignInClick = {
                    navController.navigateUp()
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
            val viewModel: MatchesViewModel = hiltViewModel()
            MatchListScreen(
                viewModel = viewModel,
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
            val viewModel: HighlightsViewModel = hiltViewModel()
            HighlightsListScreen(
                viewModel = viewModel,
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
            val viewModel: ProfileViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            
            // Handle logout
            LaunchedEffect(state.isLoggedOut) {
                if (state.isLoggedOut) {
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            
            ProfileScreen(
                state = state,
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
                    viewModel.logout()
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
            val viewModel: EditProfileViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            
            // Handle successful update
            LaunchedEffect(state.isSuccess) {
                if (state.isSuccess) {
                    navController.navigateUp()
                }
            }
            
            EditProfileScreen(
                state = state,
                onUsernameChange = { viewModel.updateUsername(it) },
                onEmailChange = { viewModel.updateEmail(it) },
                onNavigateBack = {
                    navController.navigateUp()
                },
                onSaveChanges = {
                    viewModel.saveChanges()
                }
            )
        }
        
        // Change Password Screen
        composable(route = Screen.ChangePassword.route) {
            val viewModel: ChangePasswordViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            
            // Handle successful password change
            LaunchedEffect(state.isSuccess) {
                if (state.isSuccess) {
                    navController.navigateUp()
                }
            }
            
            ChangePasswordScreen(
                state = state,
                onCurrentPasswordChange = { viewModel.updateCurrentPassword(it) },
                onNewPasswordChange = { viewModel.updateNewPassword(it) },
                onConfirmPasswordChange = { viewModel.updateConfirmPassword(it) },
                onNavigateBack = {
                    navController.navigateUp()
                },
                onUpdatePassword = {
                    viewModel.changePassword()
                }
            )
        }
    }
}