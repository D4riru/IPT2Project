// WelcomeScreen.kt
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage

@Composable
fun WelcomeScreen(navController: NavController) {
    // No Firebase check needed

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
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
            Text("FlashLearn", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF006156))
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Hero Section
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text("UNLOCK YOUR POTENTIAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006156))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Master Any\nSubject with Focus", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937), lineHeight = 44.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Step into a cognitive sanctuary designed for deep learning. Build lasting study habits through editorial-grade flashcards and smart tracking.",
                fontSize = 14.sp,
                color = Color(0xFF4B5563)
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { navController.navigate("register") },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006156)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Get Started", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            TextButton(
                onClick = { navController.navigate("login") },
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 12.dp)
            ) {
                Text("Log In", color = Color(0xFF006156), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            AsyncImage(
                model = "https://images.unsplash.com/photo-1517842645767-c639042777db?auto=format&fit=crop&q=80&w=800",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(24.dp))
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Features Section
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text("Designed for Retention", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            Spacer(modifier = Modifier.height(8.dp))
            Text("The tools you need to build mastery, without the noise.", fontSize = 14.sp, color = Color(0xFF4B5563))
            Spacer(modifier = Modifier.height(32.dp))

            FeatureCard("Create Decks", "Build beautiful, focused flashcard sets in seconds. Add images, hints, and rich text to make your study material memorable.", Color(0xFF006156), Color.White)
            FeatureCard("Practice Daily", "Our spaced repetition algorithm adapts to your pace, ensuring you review exactly what you need at the perfect time.", Color(0xFF5EEAD4), Color(0xFF0D9488))
            FeatureCard("Track Progress", "Visualize your growth with insightful analytics. Monitor your retention rates and celebrate your study streaks.", Color(0xFFBBF7D0), Color(0xFF16A34A))
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Philosophy Section
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text("OUR PHILOSOPHY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006156))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Focus is the cornerstone of modern learning.", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937), lineHeight = 34.sp)
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp).background(Color(0xFFA7F3D0).copy(alpha = 0.2f))
            ) {
                Text(
                    "\"The best tool is the one that disappears. FlashLearn helps you get into the 'flow state' and stay there, making complex subjects feel manageable.\"",
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFF4B5563),
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            AsyncImage(
                model = "https://images.unsplash.com/photo-1543269865-cbf427effbad?auto=format&fit=crop&q=80&w=800",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(24.dp))
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // CTA Container
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .background(Color(0xFF006156), RoundedCornerShape(32.dp))
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Ready to reach your goals?", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Join thousands of students who have transformed their learning habits with FlashLearn.", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { navController.navigate("register") },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Get Started for Free", color = Color(0xFF006156), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Footer
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("FlashLearn", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006156))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Study anywhere. Offline access included.", fontSize = 12.sp, color = Color(0xFF6B7280))
            Spacer(modifier = Modifier.height(16.dp))
            Text("© 2024 FlashLearn. Built for Focus.", fontSize = 10.sp, color = Color(0xFF9CA3AF))
        }
    }
}

@Composable
fun FeatureCard(title: String, desc: String, iconBgColor: Color, iconTintColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Box(
                modifier = Modifier.size(40.dp).background(iconBgColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Build, contentDescription = null, tint = iconTintColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
            Spacer(modifier = Modifier.height(8.dp))
            Text(desc, fontSize = 13.sp, color = Color(0xFF6B7280), lineHeight = 20.sp)
        }
    }
}