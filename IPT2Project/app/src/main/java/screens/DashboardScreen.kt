// DashboardScreen.kt
package screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase

data class ModuleData(
    val id: String,
    val title: String,
    val status: String = "Ready",
    val flashcardCount: Int = 10
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    val auth = remember {
        try {
            com.google.firebase.Firebase.auth
        } catch (e: Exception) {
            null
        }
    }
    val user = auth?.currentUser
    
    var modules by remember { mutableStateOf<List<ModuleData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showFabOptions by remember { mutableStateOf(false) }

    val db = remember {
        try {
            com.google.firebase.Firebase.firestore
        } catch (e: Exception) {
            null
        }
    }

    // Listen to modules collection in Firestore
    DisposableEffect(user, db) {
        if (user == null || db == null) {
            modules = emptyList()
            isLoading = false
            return@DisposableEffect onDispose {}
        }

        val listener = db.collection("users").document(user.uid)
            .collection("modules")
            .addSnapshotListener { snapshot, error ->
                isLoading = false
                if (error != null || snapshot == null) return@addSnapshotListener

                modules = snapshot.documents.map { doc ->
                    ModuleData(
                        id = doc.id,
                        title = doc.getString("title") ?: "Untitled Module",
                        status = doc.getString("status") ?: "Ready",
                        flashcardCount = doc.getLong("itemCount")?.toInt() ?: 0
                    )
                }
            }

        onDispose { listener.remove() }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showFabOptions = true },
                containerColor = Color(0xFF006156),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Module", tint = Color.White)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7FAF9))
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Greeting Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Welcome back,", fontSize = 14.sp, color = Color(0xFF6B7280))
                    Text(
                        text = user?.displayName ?: "Student",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF006156), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("YOUR MODULES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3AF))
            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF006156))
                }
            } else if (modules.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFFD1D5DB))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No modules found", fontWeight = FontWeight.Bold, color = Color(0xFF4B5563))
                    Text("Tap the + button below to parse a new document.", fontSize = 14.sp, color = Color(0xFF9CA3AF))
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(modules) { module ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Navigate directly to quiz practice passing module ID
                                    navController.navigate("quiz/${module.id}")
                                },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(module.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${module.flashcardCount} Flashcards • ${module.status}", fontSize = 12.sp, color = Color(0xFF6B7280))
                                }
                                Icon(Icons.Default.PlayArrow, contentDescription = "Practice", tint = Color(0xFF006156))
                            }
                        }
                    }
                }
            }
        }

        // FAB Actions Bottom Sheet
        if (showFabOptions) {
            ModalBottomSheet(onDismissRequest = { showFabOptions = false }) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                    Text("Create New Module", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                    Spacer(modifier = Modifier.height(24.dp))

                    ListItem(
                        headlineContent = { Text("Upload Document", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Parse PDF or TXT into flashcards automatically.") },
                        leadingContent = { Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color(0xFF006156)) },
                        modifier = Modifier.clickable {
                            showFabOptions = false
                            // Trigger your file picker logic here
                        }
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}