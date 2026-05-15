// RegisterScreen.kt
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
import com.google.firebase.auth.auth
import com.google.firebase.Firebase

@Composable
fun RegisterScreen(navController: NavController) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val auth = remember {
        try {
            com.google.firebase.Firebase.auth
        } catch (e: Exception) {
            null
        }
    }

    fun handleRegister() {
        if (auth == null) {
            errorMessage = "Firebase is not initialized. Please add google-services.json."
            return
        }
        if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            errorMessage = "Please fill in all required fields."
            return
        }
        if (password != confirmPassword) {
            errorMessage = "The passwords you entered do not match."
            return
        }
        isLoading = true
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    navController.navigate("tabs") {
                        popUpTo("welcome") { inclusive = true }
                    }
                } else {
                    errorMessage = task.exception?.message ?: "Registration Failed"
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

        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.size(44.dp).background(Color.White, RoundedCornerShape(22.dp))
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF1F2937))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("FlashLearn", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006156))
            Box(modifier = Modifier.width(48.dp).height(3.dp).background(Color(0xFF5EEAD4), RoundedCornerShape(2.dp)))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Create Account", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
        Spacer(modifier = Modifier.height(12.dp))
        Text("Create a new account to get started and enjoy seamless access to our features.", fontSize = 15.sp, color = Color(0xFF6B7280))

        Spacer(modifier = Modifier.height(24.dp))

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 16.dp))
        }

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            placeholder = { Text("Full Name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF9CA3AF)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("Email Address") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF9CA3AF)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF9CA3AF)) },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = Color(0xFF9CA3AF))
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            placeholder = { Text("Confirm Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF9CA3AF)) },
            trailingIcon = {
                IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                    Icon(if (showConfirmPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = Color(0xFF9CA3AF))
                }
            },
            visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { handleRegister() },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006156)),
            shape = RoundedCornerShape(28.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Create Account", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("Already have an account? ", color = Color(0xFF4B5563), fontSize = 14.sp)
            Text(
                "Sign In here",
                color = Color(0xFF006156),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { navController.navigate("login") }
            )
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}