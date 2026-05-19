// DashboardScreen.kt
package screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

data class ModuleData(
    val id: String,
    val title: String,
    val status: String = "Ready",
    val flashcardCount: Int = 10,
    val shareCode: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController, onNavigateToProfile: () -> Unit = {}) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("flashlearn_prefs", android.content.Context.MODE_PRIVATE) }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(com.example.myapplication.network.ApiClient.currentUser?.id) {
        val currentUserId = com.example.myapplication.network.ApiClient.currentUser?.id ?: 999
        if (currentUserId == 999) return@LaunchedEffect
        val localFile = java.io.File(context.filesDir, "profile_pic_$currentUserId.jpg")
        if (localFile.exists()) {
            profileImageUri = Uri.fromFile(localFile)
        } else {
            val savedUriStr = sharedPrefs.getString("profile_image_uri_$currentUserId", null)
            if (!savedUriStr.isNullOrBlank()) {
                val tempUri = Uri.parse(savedUriStr)
                if (tempUri.scheme == "file") {
                    val tempFile = java.io.File(tempUri.path ?: "")
                    if (tempFile.exists()) {
                        profileImageUri = tempUri
                    } else {
                        com.example.myapplication.network.ApiClient.getAvatar(currentUserId) { base64Str ->
                            if (!base64Str.isNullOrBlank()) {
                                try {
                                    val bytes = android.util.Base64.decode(base64Str, android.util.Base64.NO_WRAP)
                                    localFile.writeBytes(bytes)
                                    val localUri = Uri.fromFile(localFile)
                                    profileImageUri = localUri
                                    sharedPrefs.edit().putString("profile_image_uri_$currentUserId", localUri.toString()).apply()
                                } catch(e: Exception) {
                                    android.util.Log.e("DashboardScreen", "Failed to save downloaded avatar", e)
                                }
                            }
                        }
                    }
                } else {
                    profileImageUri = tempUri
                }
            } else {
                com.example.myapplication.network.ApiClient.getAvatar(currentUserId) { base64Str ->
                    if (!base64Str.isNullOrBlank()) {
                        try {
                            val bytes = android.util.Base64.decode(base64Str, android.util.Base64.NO_WRAP)
                            localFile.writeBytes(bytes)
                            val localUri = Uri.fromFile(localFile)
                            profileImageUri = localUri
                            sharedPrefs.edit().putString("profile_image_uri_$currentUserId", localUri.toString()).apply()
                        } catch(e: Exception) {
                            android.util.Log.e("DashboardScreen", "Failed to save downloaded avatar", e)
                        }
                    }
                }
            }
        }
    }

    var modules by remember { mutableStateOf<List<ModuleData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showFabOptions by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    @OptIn(ExperimentalMaterial3Api::class)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    @OptIn(ExperimentalMaterial3Api::class)
    val pullState = rememberPullToRefreshState()

    fun refreshDecks() {
        isRefreshing = true
        com.example.myapplication.network.ApiClient.getModules { fetchedModules ->
            coroutineScope.launch {
                if (fetchedModules != null) {
                    if (fetchedModules != null) modules = fetchedModules.map { m ->
                        ModuleData(m.id, m.title, m.status, m.flashcardCount, m.shareCode)
                    }
                }
                isRefreshing = false
            }
        }
    }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newModuleTitle by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingModuleId by remember { mutableStateOf("") }
    var editingModuleTitle by remember { mutableStateOf("") }
    var creationStep by remember { mutableStateOf(0) }
    var currentQuestionText by remember { mutableStateOf("") }
    var currentAnswerText by remember { mutableStateOf("") }
    val enteredCards = remember { mutableStateListOf<Map<String, String>>() }

    var showShareDialog by remember { mutableStateOf(false) }
    var sharingModuleCode by remember { mutableStateOf("") }
    var sharingModuleTitle by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importShareCode by remember { mutableStateOf("") }
    var showTopicGenDialog by remember { mutableStateOf(false) }
    var topicGenInput by remember { mutableStateOf("") }
    val editingCards = remember { mutableStateListOf<Map<String, String>>() }

    LaunchedEffect(showEditDialog, editingModuleId) {
        if (showEditDialog && editingModuleId.isNotBlank()) {
            com.example.myapplication.network.ApiClient.getQuiz(editingModuleId) { fetchedCards ->
                coroutineScope.launch {
                    editingCards.clear()
                    fetchedCards.forEach { card ->
                        editingCards.add(mapOf("question" to card.question, "answer" to card.options.getOrNull(card.correctIndex).orEmpty()))
                    }
                }
            }
        }
    }


    var showUploadErrorDialog by remember { mutableStateOf(false) }
    var uploadErrorMessage by remember { mutableStateOf("") }
    var pendingFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingFileName by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                coroutineScope.launch {
                    try {
                        isLoading = true
                        val contentResolver = context.contentResolver
                        var displayName = "document.pdf"
                        
                        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1 && cursor.moveToFirst()) {
                                displayName = cursor.getString(nameIndex)
                            }
                        }
                        
                        val isSupported = displayName.endsWith(".pdf", ignoreCase = true) || displayName.endsWith(".txt", ignoreCase = true)
                        if (!isSupported) {
                            isLoading = false
                            android.widget.Toast.makeText(context, "Only .pdf and .txt files are supported", android.widget.Toast.LENGTH_LONG).show()
                            return@launch
                        }

                        val inputStream = contentResolver.openInputStream(uri)
                        val bytes = inputStream?.readBytes()
                        inputStream?.close()
                        
                        if (bytes != null) {
                            com.example.myapplication.network.ApiClient.uploadDocument(displayName, bytes) { success, moduleId ->
                                coroutineScope.launch {
                                    isLoading = false
                                    if (success) {
                                        android.widget.Toast.makeText(context, "Quiz generated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                        com.example.myapplication.network.ApiClient.getModules { fetchedModules ->
                                            coroutineScope.launch {
                                                if (fetchedModules != null) {
                                                    if (fetchedModules != null) modules = fetchedModules.map { m ->
                                                        ModuleData(m.id, m.title, m.status, m.flashcardCount, m.shareCode)
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        pendingFileBytes = bytes
                                        pendingFileName = displayName
                                        uploadErrorMessage = "The Gemini AI model is currently experiencing high demand or is unavailable. Please try again in a few moments."
                                        showUploadErrorDialog = true
                                    }
                                }
                            }
                        } else {
                            isLoading = false
                            android.widget.Toast.makeText(context, "Failed to read file", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        isLoading = false
                        android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )


    LaunchedEffect(com.example.myapplication.network.ApiClient.currentUser?.id) {
        isLoading = true
        com.example.myapplication.network.ApiClient.getModules { fetchedModules ->
            coroutineScope.launch {
                if (fetchedModules != null) {
                    if (fetchedModules != null) modules = fetchedModules.map { m ->
                        ModuleData(m.id, m.title, m.status, m.flashcardCount, m.shareCode)
                    }
                }
                isLoading = false
            }
        }
    }

    Scaffold(
        containerColor = Color.White,
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
        @OptIn(ExperimentalMaterial3Api::class)
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { refreshDecks() },
            state = pullState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    modifier = Modifier.align(Alignment.TopCenter),
                    isRefreshing = isRefreshing,
                    state = pullState,
                    containerColor = Color.White,
                    color = Color(0xFF006156)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFFD1D5DB))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No flashcard decks found", fontWeight = FontWeight.Bold, color = Color(0xFF4B5563))
                        Text("Tap the + button below to create a new flashcard deck.", fontSize = 14.sp, color = Color(0xFF9CA3AF))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(module.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937), maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("${module.flashcardCount} Flashcards • ${module.status}", fontSize = 12.sp, color = Color(0xFF6B7280))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                sharingModuleCode = module.shareCode
                                                sharingModuleTitle = module.title
                                                showShareDialog = true
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = "Share Module", tint = Color(0xFF006156), modifier = Modifier.size(20.dp))
                                        }
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
                                                            if (fetchedModules != null) modules = fetchedModules.map { m ->
                                                                ModuleData(m.id, m.title, m.status, m.flashcardCount, m.shareCode)
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
                sheetState = sheetState,
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
                                filePickerLauncher.launch("*/*")
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF006156)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color(0xFF006156), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Upload Document", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                                Text("Parse PDF or TXT into flashcards automatically.", fontSize = 12.sp, color = Color(0xFF6B7280))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showFabOptions = false
                                showImportDialog = true
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF006156)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF006156), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Import via Code", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                                Text("Use a shared code to download a friend's deck.", fontSize = 12.sp, color = Color(0xFF6B7280))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showFabOptions = false
                                showTopicGenDialog = true
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF006156)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFF006156), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Generate via AI Topic", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                                Text("Type any topic and get 10 AI-generated flashcards.", fontSize = 12.sp, color = Color(0xFF6B7280))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // Gemini AI Error / Retry Dialog
        if (showUploadErrorDialog) {
            AlertDialog(
                onDismissRequest = { showUploadErrorDialog = false },
                title = { Text("Gemini AI Busy", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1F2937)) },
                text = { Text(uploadErrorMessage, fontSize = 14.sp, color = Color(0xFF4B5563)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showUploadErrorDialog = false
                            val bytes = pendingFileBytes
                            val fileName = pendingFileName
                            if (bytes != null) {
                                coroutineScope.launch {
                                    isLoading = true
                                    com.example.myapplication.network.ApiClient.uploadDocument(fileName, bytes) { success, moduleId ->
                                        coroutineScope.launch {
                                            isLoading = false
                                            if (success) {
                                                android.widget.Toast.makeText(context, "Quiz generated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                                com.example.myapplication.network.ApiClient.getModules { fetchedModules ->
                                                    coroutineScope.launch {
                                                        if (fetchedModules != null) modules = fetchedModules.map { m ->
                                                            ModuleData(m.id, m.title, m.status, m.flashcardCount, m.shareCode)
                                                        }
                                                    }
                                                }
                                            } else {
                                                showUploadErrorDialog = true
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Retry", color = Color(0xFF006156), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUploadErrorDialog = false }) {
                        Text("Cancel", color = Color(0xFF6B7280))
                    }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = Color.White
            )
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
                        Text("Create New Flashcard Deck", fontWeight = FontWeight.Bold, color = Color(0xFF006156))
                    } else {
                        Text("Add Question $creationStep of 10", fontWeight = FontWeight.Bold, color = Color(0xFF006156))
                    }
                },
                text = {
                    Column {
                        if (creationStep == 0) {
                            Text("Enter the title for your flashcard deck:", color = Color(0xFF006156), fontWeight = FontWeight.Medium)
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
                            Text("Define your custom question and answer:", color = Color(0xFF006156), fontWeight = FontWeight.Medium)
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
                                        showCreateDialog = false
                                        isLoading = true
                                        com.example.myapplication.network.ApiClient.createModule(newModuleTitle, enteredCards.toList()) { success ->
                                            com.example.myapplication.network.ApiClient.getModules { fetchedModules ->
                                                coroutineScope.launch {
                                                    if (fetchedModules != null) modules = fetchedModules.map { m ->
                                                        ModuleData(m.id, m.title, m.status, m.flashcardCount, m.shareCode)
                                                    }
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
                title = { Text("Edit Flashcard Deck", fontWeight = FontWeight.Bold, color = Color(0xFF006156)) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("Deck Title:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006156))
                        Spacer(modifier = Modifier.height(4.dp))
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
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Flashcards:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006156))
                        Spacer(modifier = Modifier.height(8.dp))

                        editingCards.forEachIndexed { index, card ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                modifier = Modifier.padding(bottom = 12.dp).fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Card #${index + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4B5563))
                                        IconButton(
                                            onClick = { editingCards.removeAt(index) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Card", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Question:", fontSize = 10.sp, color = Color(0xFF6B7280))
                                    OutlinedTextField(
                                        value = card["question"] ?: "",
                                        onValueChange = { newQ ->
                                            editingCards[index] = mapOf("question" to newQ, "answer" to (card["answer"] ?: ""))
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color(0xFF1F2937),
                                            unfocusedTextColor = Color(0xFF1F2937),
                                            focusedBorderColor = Color(0xFF006156),
                                            unfocusedBorderColor = Color(0xFFD1D5DB),
                                            cursorColor = Color(0xFF006156)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Correct Answer:", fontSize = 10.sp, color = Color(0xFF6B7280))
                                    OutlinedTextField(
                                        value = card["answer"] ?: "",
                                        onValueChange = { newA ->
                                            editingCards[index] = mapOf("question" to (card["question"] ?: ""), "answer" to newA)
                                        },
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
                        }

                        Button(
                            onClick = { editingCards.add(mapOf("question" to "", "answer" to "")) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF006156)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF006156)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("+ Add New Card")
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editingModuleTitle.isNotBlank()) {
                                isLoading = true
                                com.example.myapplication.network.ApiClient.updateModule(
                                    editingModuleId,
                                    editingModuleTitle,
                                    cards = editingCards.toList()
                                ) { success ->
                                    com.example.myapplication.network.ApiClient.getModules { fetchedModules ->
                                        coroutineScope.launch {
                                            if (fetchedModules != null) modules = fetchedModules.map { m ->
                                                ModuleData(m.id, m.title, m.status, m.flashcardCount, m.shareCode)
                                            }
                                            showEditDialog = false
                                            isLoading = false
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006156))
                    ) {
                        Text("Save Changes", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("Cancel", color = Color(0xFF6B7280))
                    }
                }
            )
        }

        // Share Module Dialog
        if (showShareDialog) {
            AlertDialog(
                onDismissRequest = { showShareDialog = false },
                containerColor = Color.White,
                title = { Text("Share Flashcard Deck", fontWeight = FontWeight.Bold, color = Color(0xFF006156)) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Share this deck with others using this code:", color = Color(0xFF4B5563), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = sharingModuleCode,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF006156),
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                letterSpacing = 2.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clipData = android.content.ClipData.newPlainText("Share Code", sharingModuleCode)
                                clipboardManager.setPrimaryClip(clipData)
                                android.widget.Toast.makeText(context, "Code copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006156))
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy Code", color = Color.White)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showShareDialog = false }) {
                        Text("Close", color = Color(0xFF006156), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Import Module Dialog
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                containerColor = Color.White,
                title = { Text("Import Deck via Code", fontWeight = FontWeight.Bold, color = Color(0xFF006156)) },
                text = {
                    Column {
                        Text("Enter the 6-character share code of the deck you want to import:", color = Color(0xFF4B5563), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = importShareCode,
                            onValueChange = { importShareCode = it.take(6).uppercase() },
                            placeholder = { Text("e.g. ABC123") },
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
                            if (importShareCode.length == 6) {
                                isLoading = true
                                com.example.myapplication.network.ApiClient.importModule(importShareCode) { success, newId ->
                                    com.example.myapplication.network.ApiClient.getModules { fetchedModules ->
                                        coroutineScope.launch {
                                            if (fetchedModules != null) modules = fetchedModules.map { m ->
                                                ModuleData(m.id, m.title, m.status, m.flashcardCount, m.shareCode)
                                            }
                                            isLoading = false
                                            showImportDialog = false
                                            importShareCode = ""
                                            if (success) {
                                                android.widget.Toast.makeText(context, "Deck imported successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                android.widget.Toast.makeText(context, "Failed to import. Check the code and try again.", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006156)),
                        enabled = importShareCode.length == 6
                    ) {
                        Text("Import", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("Cancel", color = Color(0xFF6B7280))
                    }
                }
            )
        }

        // Generate via AI Topic Dialog
        if (showTopicGenDialog) {
            AlertDialog(
                onDismissRequest = { showTopicGenDialog = false },
                containerColor = Color.White,
                title = { Text("Generate via AI Topic", fontWeight = FontWeight.Bold, color = Color(0xFF006156)) },
                text = {
                    Column {
                        Text("Enter a study topic to generate 10 flashcards using Gemini AI:", color = Color(0xFF4B5563), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = topicGenInput,
                            onValueChange = { topicGenInput = it },
                            placeholder = { Text("e.g. Ancient Greek Philosophy") },
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
                            if (topicGenInput.isNotBlank()) {
                                showTopicGenDialog = false
                                isLoading = true
                                com.example.myapplication.network.ApiClient.generateTopicDeck(topicGenInput) { success, newId ->
                                    com.example.myapplication.network.ApiClient.getModules { fetchedModules ->
                                        coroutineScope.launch {
                                            if (fetchedModules != null) modules = fetchedModules.map { m ->
                                                ModuleData(m.id, m.title, m.status, m.flashcardCount, m.shareCode)
                                            }
                                            isLoading = false
                                            topicGenInput = ""
                                            if (success) {
                                                android.widget.Toast.makeText(context, "AI flashcards generated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                android.widget.Toast.makeText(context, "The AI model is busy or unavailable. Please try again.", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006156)),
                        enabled = topicGenInput.isNotBlank()
                    ) {
                        Text("Generate", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTopicGenDialog = false }) {
                        Text("Cancel", color = Color(0xFF6B7280))
                    }
                }
            )
        }
    }
}
}
