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
    var currentIndex by remember { mutableStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var isFinished by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableStateOf(15) }
    var score by remember { mutableStateOf(0) }

    val currentCard = fallbackQuizData.getOrNull(currentIndex)

    // Spaced repetition timer loop
    LaunchedEffect(currentIndex, isFinished) {
        if (isFinished) return@LaunchedEffect
        timeLeft = 15
        while (timeLeft > 0 && selectedOption == null) {
            delay(1000)
            timeLeft--
        }
        // Auto-advance if time runs out
        if (timeLeft == 0 && selectedOption == null) {
            if (currentIndex < fallbackQuizData.size - 1) {
                currentIndex++
            } else {
                isFinished = true
            }
        }
    }

    fun handleSelection(index: Int) {
        if (selectedOption != null) return
        selectedOption = index
        if (index == currentCard?.correctIndex) {
            score++
        }
    }

    fun handleNext() {
        selectedOption = null
        if (currentIndex < fallbackQuizData.size - 1) {
            currentIndex++
        } else {
            isFinished = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7FAF9))
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
            Text(
                text = "${timeLeft}s",
                fontWeight = FontWeight.Bold,
                color = if (timeLeft > 5) Color(0xFF1F2937) else Color.Red
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isFinished) {
            // Results State
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Practice Complete!", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                Spacer(modifier = Modifier.height(16.dp))
                Text("You scored $score out of ${fallbackQuizData.size}", fontSize = 16.sp, color = Color(0xFF6B7280))
                Spacer(modifier = Modifier.height(40.dp))
                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006156)),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Finish Review")
                }
            }
        } else if (currentCard != null) {
            // Progress Bar
            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / fallbackQuizData.size },
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

            // Answers Options Grid
            currentCard.options.forEachIndexed { index, option ->
                val isSelected = selectedOption == index
                val isCorrect = index == currentCard.correctIndex

                val containerColor = when {
                    selectedOption == null -> Color.White
                    isCorrect -> Color(0xFFDCFCE7)
                    isSelected && !isCorrect -> Color(0xFFFEE2E2)
                    else -> Color.White
                }

                Button(
                    onClick = { handleSelection(index) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = containerColor,
                        contentColor = Color(0xFF1F2937)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(option, fontSize = 15.sp)
                        if (selectedOption != null) {
                            if (isCorrect) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16A34A))
                            } else if (isSelected) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFDC2626))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedOption != null) {
                Button(
                    onClick = { handleNext() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006156)),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Next Question")
                }
            }
        }
    }
}