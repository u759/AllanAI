package com.example.myapplication.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    state: SignInState,
    onSignInClick: (String, String) -> Unit = { _, _ -> },
    onSignUpClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {}
) {
    // Local state for form fields only
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        SignInContent(
            username = username,
            password = password,
            isLoading = state.isLoading,
            errorMessage = state.error,
            onUsernameChange = { username = it },
            onPasswordChange = { password = it },
            onSignInClick = {
                onSignInClick(username, password)
            },
            onSignUpClick = onSignUpClick,
            onForgotPasswordClick = onForgotPasswordClick,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

@Composable
private fun SignInContent(
    username: String,
    password: String,
    isLoading: Boolean,
    errorMessage: String?,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignInClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 48.dp)
        ) {
            Text(
                text = "Allan AI",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp
                ),
                color = Color.White
            )
            Text(
                text = "Welcome back!",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp
                ),
                color = Color(0xFF9CA3AF)
            )
        }
        
        // Form fields
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp)
        ) {
            // Username field
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Username",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFF9CA3AF)
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    placeholder = {
                        Text(
                            "Enter your username",
                            color = Color(0xFF6B7280)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1A2831),
                        unfocusedContainerColor = Color(0xFF1A2831),
                        focusedBorderColor = Color(0xFF374151),
                        unfocusedBorderColor = Color(0xFF374151),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            }
            
            // Password field
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Password",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFF9CA3AF)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    placeholder = {
                        Text(
                            "Enter your password",
                            color = Color(0xFF6B7280)
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1A2831),
                        unfocusedContainerColor = Color(0xFF1A2831),
                        focusedBorderColor = Color(0xFF374151),
                        unfocusedBorderColor = Color(0xFF374151),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                
                // Forgot password link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onForgotPasswordClick,
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Forgot password?",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Error message
        if (errorMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFEF4444).copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFEF4444),
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Sign In button
        Button(
            onClick = onSignInClick,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Sign In",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Sign Up link
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Don't have an account? ",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9CA3AF)
            )
            TextButton(
                onClick = onSignUpClick
            ) {
                Text(
                    text = "Sign Up",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SignInScreenPreview() {
    MyApplicationTheme {
        SignInScreen(state = SignInState())
    }
}
