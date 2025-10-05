package com.example.myapplication.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    onNavigateBack: () -> Unit = {},
    onUpdatePassword: (String, String, String) -> com.example.myapplication.data.local.PasswordChangeResult = { _, _, _ -> com.example.myapplication.data.local.PasswordChangeResult.Success }
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ChangePasswordTopBar(onNavigateBack = onNavigateBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        ChangePasswordContent(
            currentPassword = currentPassword,
            newPassword = newPassword,
            confirmPassword = confirmPassword,
            errorMessage = errorMessage,
            currentPasswordVisible = currentPasswordVisible,
            newPasswordVisible = newPasswordVisible,
            confirmPasswordVisible = confirmPasswordVisible,
            onCurrentPasswordChange = { currentPassword = it },
            onNewPasswordChange = { newPassword = it },
            onConfirmPasswordChange = { confirmPassword = it },
            onCurrentPasswordVisibilityToggle = { currentPasswordVisible = !currentPasswordVisible },
            onNewPasswordVisibilityToggle = { newPasswordVisible = !newPasswordVisible },
            onConfirmPasswordVisibilityToggle = { confirmPasswordVisible = !confirmPasswordVisible },
            onUpdateClick = {
                // Validate inputs
                when {
                    currentPassword.isBlank() -> {
                        errorMessage = "Current password cannot be empty"
                    }
                    newPassword.isBlank() -> {
                        errorMessage = "New password cannot be empty"
                    }
                    newPassword != confirmPassword -> {
                        errorMessage = "Passwords do not match"
                    }
                    else -> {
                        val result = onUpdatePassword(currentPassword, newPassword, confirmPassword)
                        when (result) {
                            is com.example.myapplication.data.local.PasswordChangeResult.Success -> {
                                errorMessage = null
                            }
                            is com.example.myapplication.data.local.PasswordChangeResult.WrongCurrentPassword -> {
                                errorMessage = "Current password is incorrect"
                            }
                            is com.example.myapplication.data.local.PasswordChangeResult.NotLoggedIn -> {
                                errorMessage = "You must be logged in"
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePasswordTopBar(
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Change Password",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Back",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        actions = {
            Spacer(modifier = Modifier.width(40.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        )
    )
}

@Composable
private fun ChangePasswordContent(
    currentPassword: String,
    newPassword: String,
    confirmPassword: String,
    errorMessage: String?,
    currentPasswordVisible: Boolean,
    newPasswordVisible: Boolean,
    confirmPasswordVisible: Boolean,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onCurrentPasswordVisibilityToggle: () -> Unit,
    onNewPasswordVisibilityToggle: () -> Unit,
    onConfirmPasswordVisibilityToggle: () -> Unit,
    onUpdateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Current Password
            PasswordField(
                label = "Current Password",
                value = currentPassword,
                onValueChange = onCurrentPasswordChange,
                isPasswordVisible = currentPasswordVisible,
                onVisibilityToggle = onCurrentPasswordVisibilityToggle,
                placeholder = "Enter current password"
            )
            
            // New Password
            PasswordField(
                label = "New Password",
                value = newPassword,
                onValueChange = onNewPasswordChange,
                isPasswordVisible = newPasswordVisible,
                onVisibilityToggle = onNewPasswordVisibilityToggle,
                placeholder = "Enter new password"
            )
            
            // Confirm New Password
            PasswordField(
                label = "Confirm New Password",
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                isPasswordVisible = confirmPasswordVisible,
                onVisibilityToggle = onConfirmPasswordVisibilityToggle,
                placeholder = "Confirm new password"
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Error message
        if (errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
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
        
        // Update button
        Button(
            onClick = onUpdateClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Update Password",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onVisibilityToggle: () -> Unit,
    placeholder: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color(0xFF9CA3AF)
        )
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            placeholder = {
                Text(
                    placeholder,
                    color = Color(0xFF9CA3AF)
                )
            },
            visualTransformation = if (isPasswordVisible) 
                VisualTransformation.None 
            else 
                PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onVisibilityToggle) {
                    Icon(
                        imageVector = if (isPasswordVisible)
                            Icons.Default.Visibility
                        else
                            Icons.Default.VisibilityOff,
                        contentDescription = if (isPasswordVisible) 
                            "Hide password" 
                        else 
                            "Show password",
                        tint = Color(0xFF9CA3AF)
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1A2831),
                unfocusedContainerColor = Color(0xFF1A2831),
                focusedBorderColor = Color(0xFF334155),
                unfocusedBorderColor = Color(0xFF334155),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ChangePasswordScreenPreview() {
    MyApplicationTheme {
        ChangePasswordScreen()
    }
}
