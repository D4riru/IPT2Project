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
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import android.net.Uri

data class ModuleData(
    val id: String,
    val title: String,
    val status: String = "Ready",
    val flashcardCount: Int = 10
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController, onNavigateToProfile: () -> Unit = {}) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("flashlearn_prefs", android.content.Context.MODE_PRIVATE) }
    var profileImageUri by remember {
        mutableStateOf<Uri?>(
            sharedPrefs.getString("profile_image_uri_${com.example.myapplication.network.ApiClient.currentUser?.id}", null)?.let { Uri.parse(it) }
        )
    }
    var modules by remember { mutableStateOf<List<ModuleData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showFabOptions by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newModuleTitle by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingModuleId by remember { mutableStateOf("") }
    var editingModuleTitle by remember { mutableStateOf("") }
    var creationStep by remember { mutableStateOf(0) }
    var currentQuestionText by remember { mutableStateOf("") }
    var currentAnswerText by remember { mutableStateOf("") }
    val enteredCards = remember { mutableStateListOf<Map<String, String>>() }

    LaunchedEffect(Unit) {
        com.example.myapplication.network.ApiClient.getModules { fetchedModules ->
            coroutineScope.launch {
                modules = fetchedModules.map { m ->
                    ModuleData(m.id, m.title, m.status, m.flashcardCount)
                }
                isLoading = false
            }
        }
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
                        text = com.example.myapplication.network.ApiClient.currentUser?.fullName ?: "Student",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF006156), CircleShape)
                        .clickable { onNavigateToProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    if (profileImageUri != null) {
                        AsyncImage(
                            model = profileImageUri,
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("YOUR FLASHCARDS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3AF))
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
                    Text("No flashcard decks found", fontWeight = FontWeight.Bold, color = Color(0xFF4B5563))
                    Text("Tap the + button below to create a new flashcard deck.", fontSize = 14.sp, color = Color(0xFF9CA3AF))
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            editingModuleId = module.id
                                            editingModuleTitle = module.title
                                            showEditDialog = true
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Module", tint = Color(0xFF6B7280), modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            com.example.myapplication.network.ApiClient.deleteModule(module.id) { success ->
                                                com.example.myapplication.network.ApiClient.getModules { fetchedModules ->
                                                    coroutineScope.launch {
                                                        modules = fetchedModules.map { m ->
                                                            ModuleData(m.id, m.title, m.status, m.flashcardCount)
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Module", tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Practice", tint = Color(0xFF006156))
                                }
                            }
                        }
                    }
                }
            }
        }

        // FAB Actions Bottom Sheet
        if (showFabOptions) {
            ModalBottomSheet(
                onDismissRequest = { showFabOptions = false },
                containerColor = Color.White
            ) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                    Text("Flashcard Actions", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showFabOptions = false
                                showCreateDialog = true
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF006156)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, tint = Color(0xFF006156), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Create Manually", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                                Text("Add a new empty deck to your study library.", fontSize = 12.sp, color = Color(0xFF6B7280))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showFabOptions = false
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE5E7EB)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Upload Document", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9CA3AF))
                                Text("Parse PDF or TXT into flashcards automatically.", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // Create Module Dialog
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showCreateDialog = false
                    creationStep = 0
                    enteredCards.clear()
                    currentQuestionText = ""
                    currentAnswerText = ""
                },
                containerColor = Color.White,
                title = { 
                    if (creationStep == 0) {
                        Text("Create New Flashcard Deck", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Add Question $creationStep of 10", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column {
                        if (creationStep == 0) {
                            Text("Enter the title for your flashcard deck:")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newModuleTitle,
                                onValueChange = { newModuleTitle = it },
                                placeholder = { Text("e.g. Anatomy & Physiology") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF1F2937),
                                    unfocusedTextColor = Color(0xFF1F2937),
                                    focusedBorderColor = Color(0xFF006156),
                                    unfocusedBorderColor = Color(0xFFD1D5DB),
                                    cursorColor = Color(0xFF006156)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text("Define your custom question and answer:")
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Question:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006156))
                            OutlinedTextField(
                                value = currentQuestionText,
                                onValueChange = { currentQuestionText = it },
                                placeholder = { Text("e.g. What is the powerhouse of the cell?") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF1F2937),
                                    unfocusedTextColor = Color(0xFF1F2937),
                                    focusedBorderColor = Color(0xFF006156),
                                    unfocusedBorderColor = Color(0xFFD1D5DB),
                                    cursorColor = Color(0xFF006156)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Correct Answer:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006156))
                            OutlinedTextField(
                                value = currentAnswerText,
                                onValueChange = { currentAnswerText = it },
                                placeholder = { Text("e.g. Mitochondria") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF1F2937),
                                    unfocusedTextColor = Color(0xFF1F2937),
                                    focusedBorderColor = Color(0xFF006156),
                                    unfocusedBorderColor = Color(0xFFD1D5DB),
                                    cursorColor = Color(0xFF006156)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (creationStep == 0) {
                                if (newModuleTitle.isNotBlank()) {
                                    creationStep = 1
                                }
                            } else {
                                if (currentQuestionText.isNotBlank() && currentAnswerText.isNotBlank()) {
                                    enteredCards.add(mapOf("question" to currentQuestionText, "answer" to currentAnswerText))
                                    currentQuestionText = ""
                                    currentAnswerText = ""
                                    if (creationStep < 10) {
                                        creationStep++
                                    } else {
                                        // Final Step: Submit payload to Flask
                                        isLoading = true
                                        com.example.myapplication.network.ApiClient.createModule(newModuleTitle, enteredCards.toList()) { success ->
                                            com.example.myapplication.network.ApiClient.getModules { fetchedModules ->
                                                coroutineScope.launch {
                                                    modules = fetchedModules.map { m ->
                                                        ModuleData(m.id, m.title, m.status, m.flashcardCount)
                                                    }
                                                    showCreateDialog = false
                                                    creationStep = 0
                                                    newModuleTitle = ""
                                                    enteredCards.clear()
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006156)),
                        enabled = if (creationStep == 0) newModuleTitle.isNotBlank() else (currentQuestionText.isNotBlank() && currentAnswerText.isNotBlank())
                    ) {
                        if (creationStep == 0) {
                            Text("Next", color = Color.White)
                        } else if (creationStep < 10) {
                            Text("Next Question", color = Color.White)
                        } else {
                            Text("Finish Deck", color = Color.White)
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showCreateDialog = false 
                            creationStep = 0
                            enteredCards.clear()
                            currentQuestionText = ""
                            currentAnswerText = ""
                        }
                    ) {
                        Text("Cancel", color = Color(0xFF6B7280))
                    }
                }
            )
        }

        // Edit Module Dialog
        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                containerColor = Color.White,
                title = { Text("Rename Flashcard Deck", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Enter the new title for your study deck:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editingModuleTitle,
                            onValueChange = { editingModuleTitle = it },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF1F2937),
                                unfocusedTextColor = Color(0xFF1F2937),
                                focusedBorderColor = Color(0xFF006156),
                                unfocusedBorderColor = Color(0xFFD1D5DB),
                                cursorColor = Color(0xFF006156)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editingModuleTitle.isNotBlank()) {
                                com.example.myapplication.network.ApiClient.updateModule(editingModuleId, editingModuleTitle) { success ->
                                    com.example.myapplication.network.ApiClient.getModules { fetchedModules ->
                                        coroutineScope.launch {
                                            modules = fetchedModules.map { m ->
                                                ModuleData(m.id, m.title, m.status, m.flashcardCount)
                                            }
                                            showEditDialog = false
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006156))
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("Cancel", color = Color(0xFF6B7280))
                    }
                }
            )
        }
    }
}