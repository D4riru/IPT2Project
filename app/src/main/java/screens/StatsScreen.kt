// StatsScreen.kt
package screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Composable
fun StatsScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("flashlearn_prefs", android.content.Context.MODE_PRIVATE) }
    var masteredPercentage by remember { mutableStateOf("0%") }
    var needsReviewCount by remember { mutableStateOf("0 Cards") }
    var failedQuestionsList by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Load failed questions from sharedPrefs
        val failedListJson = sharedPrefs.getString("failed_questions", "[]") ?: "[]"
        val loadedList = try {
            val type = object : TypeToken<List<Map<String, String>>>() {}.type
            Gson().fromJson<List<Map<String, String>>>(failedListJson, type) ?: emptyList()
        } catch(e: Exception) {
            emptyList<Map<String, String>>()
        }
        failedQuestionsList = loadedList

        com.example.myapplication.network.ApiClient.getStats { stats ->
            coroutineScope.launch {
                masteredPercentage = stats.masteredPercentage
                needsReviewCount = "${loadedList.size} Cards"
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7FAF9))
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text("RETENTION METRICS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3AF))
        Spacer(modifier = Modifier.height(8.dp))
        Text("Flashcards Overview", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF006156))
            }
        } else {
            // Fast Stats Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatBox(modifier = Modifier.weight(1f), title = "Mastered", value = masteredPercentage, color = Color(0xFF16A34A))
                StatBox(modifier = Modifier.weight(1f), title = "Needs Review", value = needsReviewCount, color = Color(0xFFDC2626))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("NEEDS REVIEW", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            Spacer(modifier = Modifier.height(16.dp))

            if (failedQuestionsList.isEmpty()) {
                // Empty state for needs review
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "No items requiring review.",
                            fontSize = 15.sp,
                            color = Color(0xFF1F2937),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF7FAF9), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF006156), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Finish quizzes with mistakes to see reviews here.", fontSize = 12.sp, color = Color(0xFF4B5563))
                        }
                    }
                }
            } else {
                // Show maximum of 2 failed questions in reverse order (newest first)
                val displayList = failedQuestionsList.takeLast(2).reversed()
                displayList.forEach { card ->
                    val q = card["question"] ?: ""
                    val a = card["answer"] ?: ""
                    val reversedAnswer = a.reversed()
                    
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = q,
                                fontSize = 15.sp,
                                color = Color(0xFF1F2937),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF7FAF9), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF006156), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Answer (Reversed): $reversedAnswer", fontSize = 12.sp, color = Color(0xFF4B5563), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun StatBox(modifier: Modifier = Modifier, title: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, fontSize = 12.sp, color = Color(0xFF6B7280))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}