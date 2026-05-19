// QuizScreen.kt
package screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Flashcard(
    val category: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int
)

val fallbackQuizData = listOf(
    Flashcard("FUNDAMENTALS", "What is the powerhouse of the cell?", listOf("Nucleus", "Mitochondria", "Ribosome", "Endoplasmic Reticulum"), 1),
    Flashcard("GEOGRAPHY", "What is the capital of the Byzantine Empire?", listOf("Rome", "Athens", "Constantinople", "Alexandria"), 2),
    Flashcard("ART HISTORY", "Who painted 'The Starry Night'?", listOf("Claude Monet", "Vincent van Gogh", "Leonardo da Vinci", "Pablo Picasso"), 1)
)

@Composable
fun QuizScreen(navController: NavController, moduleId: String? = null) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("flashlearn_prefs", android.content.Context.MODE_PRIVATE) }
    var quizData by remember { mutableStateOf<List<Flashcard>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentIndex by remember { mutableStateOf(0) }
    var typedAnswer by remember { mutableStateOf("") }
    var isAnswerChecked by remember { mutableStateOf(false) }
    var isCorrectAnswer by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(moduleId) {
        if (moduleId != null) {
            com.example.myapplication.network.ApiClient.getQuiz(moduleId) { fetchedCards ->
                coroutineScope.launch {
                    quizData = fetchedCards.map { c ->
                        Flashcard(c.category, c.question, c.options, c.correctIndex)
                    }
                    isLoading = false
                }
            }
        } else {
            quizData = fallbackQuizData
            isLoading = false
        }
    }

    LaunchedEffect(isFinished) {
        if (isFinished && quizData.isNotEmpty()) {
            com.example.myapplication.network.ApiClient.updateStats(score, quizData.size)
        }
    }

    val currentCard = quizData.getOrNull(currentIndex)

    fun handleCheckAnswer() {
        if (isAnswerChecked || currentCard == null) return
        isAnswerChecked = true
        val correctAnswerVal = currentCard.options.getOrNull(currentCard.correctIndex) ?: ""
        val correct = typedAnswer.trim().equals(correctAnswerVal.trim(), ignoreCase = true)
        isCorrectAnswer = correct
        if (correct) {
            score++
        } else {
            // Save to failed questions pool in sharedPrefs for "Needs Review"
            val failedListJson = sharedPrefs.getString("failed_questions", "[]") ?: "[]"
            val failedList = try {
                val type = object : TypeToken<List<Map<String, String>>>() {}.type
                Gson().fromJson<List<Map<String, String>>>(failedListJson, type).toMutableList()
            } catch(e: Exception) {
                mutableListOf()
            }
            
            // Avoid duplicate question entries in the pool
            if (failedList.none { it["question"] == currentCard.question }) {
                failedList.add(mapOf("question" to currentCard.question, "answer" to correctAnswerVal))
                // Max limit of 10 entries in failed pool to avoid clutter
                if (failedList.size > 10) {
                    failedList.removeAt(0)
                }
                sharedPrefs.edit().putString("failed_questions", Gson().toJson(failedList)).apply()
            }
        }
    }

    fun handleNext() {
        typedAnswer = ""
        isAnswerChecked = false
        isCorrectAnswer = false
        if (currentIndex < quizData.size - 1) {
            currentIndex++
        } else {
            isFinished = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7FAF9))
            .systemBarsPadding()
            .padding(20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Header Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Quiz")
            }
            Text(
                text = currentCard?.category ?: "PRACTICE",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF006156),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(44.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF006156))
            }
        } else if (isFinished) {
            // Results State
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Practice Complete!", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                Spacer(modifier = Modifier.height(16.dp))
                Text("You scored $score out of ${quizData.size}", fontSize = 16.sp, color = Color(0xFF6B7280))
                Spacer(modifier = Modifier.height(40.dp))
                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006156)),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Finish Review", color = Color.White)
                }
            }
        } else if (currentCard != null) {
            // Progress Bar
            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / quizData.size },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF006156),
                trackColor = Color(0xFFE5E7EB)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Question Card
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentCard.question,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Answer Entry Area
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!isAnswerChecked) {
                    OutlinedTextField(
                        value = typedAnswer,
                        onValueChange = { typedAnswer = it },
                        placeholder = { Text("Type your answer here...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1F2937),
                            unfocusedTextColor = Color(0xFF1F2937),
                            focusedBorderColor = Color(0xFF006156),
                            unfocusedBorderColor = Color(0xFFD1D5DB),
                            focusedLabelColor = Color(0xFF006156),
                            cursorColor = Color(0xFF006156)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { handleCheckAnswer() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006156)),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = typedAnswer.isNotBlank()
                    ) {
                        Text("Check Answer", color = Color.White)
                    }
                } else {
                    // Answer checked banner feedback
                    val correctAnswerVal = currentCard.options.getOrNull(currentCard.correctIndex) ?: ""
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCorrectAnswer) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isCorrectAnswer) Color(0xFF16A34A) else Color(0xFFDC2626)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isCorrectAnswer) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = if (isCorrectAnswer) Color(0xFF16A34A) else Color(0xFFDC2626)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isCorrectAnswer) "Correct!" else "Incorrect!",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCorrectAnswer) Color(0xFF16A34A) else Color(0xFFDC2626),
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your answer: $typedAnswer",
                                color = Color(0xFF374151),
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Correct answer: $correctAnswerVal",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937),
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { handleNext() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006156)),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Next Question", color = Color.White)
                    }
                }
            }
        }
    }
}