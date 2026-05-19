// LoginScreen.kt
package screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var alertMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val context = androidx.compose.ui.platform.LocalContext.current

    fun handleLogin() {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Please enter both email and password."
            return
        }
        isLoading = true
        errorMessage = null
        com.example.myapplication.network.ApiClient.login(email, password) { response ->
            coroutineScope.launch {
                isLoading = false
                if (response.status == "success") {
                    val sharedPrefs = context.getSharedPreferences("flashlearn_prefs", android.content.Context.MODE_PRIVATE)
                    response.user?.let { u ->
                        sharedPrefs.edit()
                            .putInt("logged_in_user_id", u.id)
                            .putString("logged_in_user_name", u.fullName)
                            .putString("logged_in_user_email", u.email)
                            .apply()
                    }
                    navController.navigate("tabs") {
                        popUpTo("welcome") { inclusive = true }
                    }
                } else {
                    errorMessage = response.message
                }
            }
        }
    }

    fun handleForgotPassword() {
        if (email.isBlank()) {
            errorMessage = "Please enter your email address into the input field above so we know where to send the password reset link!"
            return
        }
        isLoading = true
        errorMessage = null
        com.example.myapplication.network.ApiClient.forgotPassword(email) { success, message ->
            coroutineScope.launch {
                isLoading = false
                if (success) {
                    alertMessage = message
                } else {
                    errorMessage = message
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7FDFC))
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.size(44.dp).background(Color.White, RoundedCornerShape(22.dp))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF1F2937))
            }
            
            Row(
                modifier = Modifier.weight(1f).padding(end = 44.dp), // Perfectly offset the back button to mathematically center the logo
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF006156), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.School, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("FlashLearn", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006156))
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Titles
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("Log in", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Enter your email and password to securely access your account and manage your services.",
                fontSize = 15.sp,
                color = Color(0xFF6B7280),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Error / Alert Displays
        errorMessage?.let {
            Text(it, color = Color(0xFFDC2626), modifier = Modifier.padding(bottom = 16.dp))
        }
        alertMessage?.let {
            Text(it, color = Color(0xFF006156), modifier = Modifier.padding(bottom = 16.dp))
        }

        // Form
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("Email Address") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF9CA3AF)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF1F2937),
                unfocusedTextColor = Color(0xFF1F2937),
                focusedBorderColor = Color(0xFF006156),
                focusedLabelColor = Color(0xFF006156),
                cursorColor = Color(0xFF006156),
                unfocusedBorderColor = Color(0xFFE5E7EB)
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF9CA3AF)) },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF)
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF1F2937),
                unfocusedTextColor = Color(0xFF1F2937),
                focusedBorderColor = Color(0xFF006156),
                focusedLabelColor = Color(0xFF006156),
                cursorColor = Color(0xFF006156),
                unfocusedBorderColor = Color(0xFFE5E7EB)
            ),
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Text(
            "FORGOT PASSWORD?",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF006156),
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 12.dp)
                .clickable { handleForgotPassword() }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { handleLogin() },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006156)),
            shape = RoundedCornerShape(28.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Log In", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("Don't have an account? ", color = Color(0xFF4B5563), fontSize = 14.sp)
            Text(
                "Sign Up here",
                color = Color(0xFF006156),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { navController.navigate("register") }
            )
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}