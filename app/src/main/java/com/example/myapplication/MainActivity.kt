package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import screens.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Safely initialize Firebase to prevent crashes if google-services.json is missing
        try {
            // Try default initialization first
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (t: Throwable) {
            // If default fails, try with manual options
            try {
                if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                    val options = com.google.firebase.FirebaseOptions.Builder()
                        .setApiKey("AIzaSyDummyKey")
                        .setApplicationId("1:1234567890:android:dummy")
                        .setProjectId("dummy-project")
                        .build()
                    com.google.firebase.FirebaseApp.initializeApp(this, options)
                }
            } catch (inner: Throwable) {
                // Completely failed to initialize Firebase
            }
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "welcome") {
                    composable("welcome") { WelcomeScreen(navController) }
                    composable("login") { LoginScreen(navController) }
                    composable("register") { RegisterScreen(navController) }
                    composable("tabs") { TabLayout(navController) }
                    composable("quiz/{moduleId}") { backStackEntry ->
                        val moduleId = backStackEntry.arguments?.getString("moduleId")
                        QuizScreen(navController, moduleId)
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Greeting("Android")
    }
}
