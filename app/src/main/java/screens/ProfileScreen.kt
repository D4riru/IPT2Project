// ProfileScreen.kt
package screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("flashlearn_prefs", android.content.Context.MODE_PRIVATE) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(com.example.myapplication.network.ApiClient.currentUser?.id) {
        val currentUserId = com.example.myapplication.network.ApiClient.currentUser?.id ?: 999
        if (currentUserId == 999) return@LaunchedEffect
        val localFile = java.io.File(context.filesDir, "profile_pic_$currentUserId.jpg")
        if (localFile.exists()) {
            photoUri = Uri.fromFile(localFile)
        } else {
            val savedUriStr = sharedPrefs.getString("profile_image_uri_$currentUserId", null)
            if (!savedUriStr.isNullOrBlank()) {
                val tempUri = Uri.parse(savedUriStr)
                if (tempUri.scheme == "file") {
                    val tempFile = java.io.File(tempUri.path ?: "")
                    if (tempFile.exists()) {
                        photoUri = tempUri
                    } else {
                        com.example.myapplication.network.ApiClient.getAvatar(currentUserId) { base64Str ->
                            if (!base64Str.isNullOrBlank()) {
                                try {
                                    val bytes = android.util.Base64.decode(base64Str, android.util.Base64.NO_WRAP)
                                    localFile.writeBytes(bytes)
                                    val localUri = Uri.fromFile(localFile)
                                    photoUri = localUri
                                    sharedPrefs.edit().putString("profile_image_uri_$currentUserId", localUri.toString()).apply()
                                } catch(e: Exception) {
                                    android.util.Log.e("ProfileScreen", "Failed to save downloaded avatar", e)
                                }
                            }
                        }
                    }
                } else {
                    photoUri = tempUri
                }
            } else {
                com.example.myapplication.network.ApiClient.getAvatar(currentUserId) { base64Str ->
                    if (!base64Str.isNullOrBlank()) {
                        try {
                            val bytes = android.util.Base64.decode(base64Str, android.util.Base64.NO_WRAP)
                            localFile.writeBytes(bytes)
                            val localUri = Uri.fromFile(localFile)
                            photoUri = localUri
                            sharedPrefs.edit().putString("profile_image_uri_$currentUserId", localUri.toString()).apply()
                        } catch(e: Exception) {
                            android.util.Log.e("ProfileScreen", "Failed to save downloaded avatar", e)
                        }
                    }
                }
            }
        }
    }

    // Native image picker configuration replacing expo-image-picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val currentUserId = com.example.myapplication.network.ApiClient.currentUser?.id ?: 999
                val localFile = java.io.File(context.filesDir, "profile_pic_$currentUserId.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    localFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val localUri = Uri.fromFile(localFile)
                photoUri = localUri
                sharedPrefs.edit().putString("profile_image_uri_$currentUserId", localUri.toString()).apply()
                
                val bytes = localFile.readBytes()
                val base64Str = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                com.example.myapplication.network.ApiClient.uploadAvatar(currentUserId, base64Str) { success ->
                    if (success) {
                        android.util.Log.i("ProfileScreen", "Avatar uploaded successfully to server")
                    } else {
                        android.util.Log.e("ProfileScreen", "Avatar upload failed")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileScreen", "Error saving profile pic", e)
            }
        }
    }

    var stats by remember { mutableStateOf<com.example.myapplication.network.StatsResponse?>(null) }

    var isRefreshing by remember { mutableStateOf(false) }
    @OptIn(ExperimentalMaterial3Api::class)
    val pullState = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()

    fun refreshProfile() {
        isRefreshing = true
        val currentUserId = com.example.myapplication.network.ApiClient.currentUser?.id ?: 999
        if (currentUserId != 999) {
            val localFile = java.io.File(context.filesDir, "profile_pic_$currentUserId.jpg")
            com.example.myapplication.network.ApiClient.getAvatar(currentUserId) { base64Str ->
                if (!base64Str.isNullOrBlank()) {
                    try {
                        val bytes = android.util.Base64.decode(base64Str, android.util.Base64.NO_WRAP)
                        localFile.writeBytes(bytes)
                        val localUri = Uri.fromFile(localFile)
                        photoUri = localUri
                        sharedPrefs.edit().putString("profile_image_uri_$currentUserId", localUri.toString()).apply()
                    } catch(e: Exception) {
                        android.util.Log.e("ProfileScreen", "Failed to save downloaded avatar", e)
                    }
                }
            }
            com.example.myapplication.network.ApiClient.getStats { fetchedStats ->
                stats = fetchedStats
                isRefreshing = false
            }
        } else {
            isRefreshing = false
        }
    }

    LaunchedEffect(com.example.myapplication.network.ApiClient.currentUser?.id) {
        com.example.myapplication.network.ApiClient.getStats { fetchedStats ->
            stats = fetchedStats
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { refreshProfile() },
        state = pullState,
        modifier = Modifier.fillMaxSize().background(Color.White),
        indicator = {
            PullToRefreshDefaults.Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
                state = pullState,
                color = Color(0xFF006156)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Avatar Layout
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Color(0xFF006156), CircleShape)
                .clickable {
                    imagePickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (photoUri != null) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = com.example.myapplication.network.ApiClient.currentUser?.fullName ?: "Student Explorer",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937)
        )
        Text(
            text = com.example.myapplication.network.ApiClient.currentUser?.email ?: "student@example.com",
            fontSize = 14.sp,
            color = Color(0xFF6B7280)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Achievements Block
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("YOUR ACHIEVEMENTS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3AF))
                Spacer(modifier = Modifier.height(20.dp))

                val isStreakUnlocked = (stats?.streakCount ?: 1) >= 3
                val isMasteryUnlocked = (stats?.perfectScoresCount ?: 0) >= 3
                val isCreatorUnlocked = (stats?.createdDecksCount ?: 0) >= 3


                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    AchievementBadge("Streak", "3 review days", Icons.Default.Star, isStreakUnlocked)
                    AchievementBadge("Mastery", "3 perfect scores", Icons.Default.Star, isMasteryUnlocked)
                    AchievementBadge("Creator", "3 decks created", Icons.Default.Star, isCreatorUnlocked)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Sign Out Controller
        Button(
            onClick = {
                val sharedPrefs = context.getSharedPreferences("flashlearn_prefs", android.content.Context.MODE_PRIVATE)
                sharedPrefs.edit()
                    .remove("logged_in_user_id")
                    .remove("logged_in_user_name")
                    .remove("logged_in_user_email")
                    .apply()
                com.example.myapplication.network.ApiClient.currentUser = null
                navController.navigate("welcome") { popUpTo(0) }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2), contentColor = Color(0xFFDC2626)),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Out", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun AchievementBadge(label: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, unlocked: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(if (unlocked) Color(0xFFDCFCE7) else Color(0xFFF3F4F6), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (unlocked) icon else Icons.Default.Lock,
                contentDescription = label,
                tint = if (unlocked) Color(0xFF16A34A) else Color(0xFF9CA3AF)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (unlocked) Color(0xFF16A34A) else Color(0xFF4B5563)
        )
        Text(
            text = description,
            fontSize = 10.sp,
            color = Color(0xFF9CA3AF)
        )
    }
}