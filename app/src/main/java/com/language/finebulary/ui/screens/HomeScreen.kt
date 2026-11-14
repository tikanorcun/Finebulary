package com.language.finebulary.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import com.language.finebulary.data.pref.FolderViewStyle
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.language.finebulary.R
import com.language.finebulary.data.model.Course
import com.language.finebulary.data.model.CourseWithCount
import com.language.finebulary.data.model.Language
import com.language.finebulary.data.model.supportedLanguages
import com.language.finebulary.ui.components.EmptyStateView
import com.language.finebulary.ui.components.LanguagePicker
import com.language.finebulary.ui.components.getLanguageEmoji
import com.language.finebulary.ui.viewmodel.AiGenerationState
import com.language.finebulary.ui.viewmodel.HomeViewModel
import com.language.finebulary.ui.viewmodel.TranslationState
import com.language.finebulary.util.AiServiceProvider

data class WizardCategory(
    val id: String,
    val nameRes: Int,
    val subTopics: List<String>
)

val wizardPresets = listOf(
    WizardCategory("travel", R.string.cat_travel, listOf("Airport", "Hotel", "Restaurant", "Sightseeing", "Transport", "Emergency")),
    WizardCategory("business", R.string.cat_business, listOf("Meetings", "Email", "Negotiation", "Marketing", "Finance", "Interview")),
    WizardCategory("academic", R.string.cat_academic, listOf("Science", "History", "Literature", "Philosophy", "Grammar", "Exam Prep")),
    WizardCategory("life", R.string.cat_lifestyle, listOf("Kitchen", "Shopping", "Family", "Health", "Home Office", "Weather")),
    WizardCategory("hobbies", R.string.cat_hobbies, listOf("Sports", "Photography", "Gaming", "Music", "Cooking", "Nature"))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    title: String,
    courses: List<CourseWithCount>,
    viewModel: HomeViewModel,
    isPersonalMode: Boolean,
    onCourseClick: (Course) -> Unit,
    onNavigateToApiKeys: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }
    val incomingSharedText by viewModel.incomingSharedText.collectAsState()
    var courseToDelete by remember { mutableStateOf<Course?>(null) }
    var courseToRename by remember { mutableStateOf<Course?>(null) }
    val folderViewStyle by viewModel.folderViewStyle.collectAsState()
    val nativeLanguage by viewModel.nativeLanguage.collectAsState()
    val aiState by viewModel.aiGenerationState.collectAsState()
    
    val activeProvider by viewModel.activeAiProvider.collectAsState()
    val geminiKey by viewModel.geminiApiKey.collectAsState()
    val groqKey by viewModel.groqApiKey.collectAsState()
    val openaiKey by viewModel.openaiApiKey.collectAsState()
    val deepseekKey by viewModel.deepseekApiKey.collectAsState()
    val openrouterKey by viewModel.openrouterApiKey.collectAsState()

    val isAiConfigured = when (activeProvider) {
        AiServiceProvider.GEMINI -> !geminiKey.isNullOrBlank()
        AiServiceProvider.GROQ -> !groqKey.isNullOrBlank()
        AiServiceProvider.OPENAI -> !openaiKey.isNullOrBlank()
        AiServiceProvider.DEEPSEEK -> !deepseekKey.isNullOrBlank()
        AiServiceProvider.OPENROUTER -> !openrouterKey.isNullOrBlank()
    }

    val context = LocalContext.current

    LaunchedEffect(aiState) {
        if (aiState is AiGenerationState.Success) {
            Toast.makeText(context, context.getString(R.string.course_created), Toast.LENGTH_SHORT).show()
            viewModel.clearAiState()
            showAiDialog = false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ai_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val json = inputStream?.bufferedReader().use { it?.readText() }
                if (!json.isNullOrBlank()) {
                    viewModel.importSharedCourse(json) { success ->
                        if (success) {
                            Toast.makeText(context, context.getString(R.string.course_imported_success), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, context.getString(R.string.course_imported_error), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (_: Exception) {
                Toast.makeText(context, context.getString(R.string.course_imported_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { filePickerLauncher.launch("application/json") }) {
                        Icon(Icons.Default.FileOpen, contentDescription = stringResource(R.string.import_course))
                    }
                    IconButton(onClick = {
                        viewModel.setFolderViewStyle(
                            if (folderViewStyle == FolderViewStyle.GRID) FolderViewStyle.LIST else FolderViewStyle.GRID
                        )
                    }) {
                        Icon(
                            imageVector = if (folderViewStyle == FolderViewStyle.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            contentDescription = stringResource(R.string.toggle_view)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            if (isPersonalMode) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_course))
                }
            } else {
                ExtendedFloatingActionButton(
                    onClick = { showAiDialog = true },
                    modifier = Modifier.scale(scale),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.AutoAwesome, null) },
                    text = { Text(stringResource(R.string.generate_with_ai)) }
                )
            }
        }
    ) { padding ->
        if (courses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                EmptyStateView(
                    icon = if (isPersonalMode) Icons.Default.FolderOpen else Icons.Default.AutoAwesome,
                    title = if (isPersonalMode) stringResource(R.string.no_folders) else stringResource(R.string.no_courses),
                    description = if (isPersonalMode) "Your personal vocabulary space is empty." else "Start your journey by generating your first course.",
                    action = {
                        if (!isPersonalMode && !isAiConfigured) {
                            Button(onClick = onNavigateToApiKeys) {
                                Text(stringResource(R.string.go_to_settings))
                            }
                        }
                    }
                )
            }
        } else {
            if (folderViewStyle == FolderViewStyle.GRID) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    gridItems(items = courses) { item ->
                        CourseCard(
                            course = item.course,
                            wordCount = item.wordCount,
                            onClick = { onCourseClick(item.course) },
                            onDelete = { courseToDelete = item.course },
                            onRename = { courseToRename = item.course }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listItems(items = courses) { item ->
                        CourseListItem(
                            course = item.course,
                            wordCount = item.wordCount,
                            onClick = { onCourseClick(item.course) },
                            onDelete = { courseToDelete = item.course },
                            onRename = { courseToRename = item.course }
                        )
                    }
                }
            }
        }
    }

    if (incomingSharedText != null) {
        QuickAddDialog(
            text = incomingSharedText!!,
            viewModel = viewModel,
            onDismiss = { 
                viewModel.clearTranslationState()
                viewModel.clearIncomingText()
            }
        )
    }

    if (courseToRename != null) {
        RenameFolderDialog(
            course = courseToRename!!,
            onDismiss = { courseToRename = null },
            onConfirm = { newName ->
                viewModel.renameCourse(courseToRename!!, newName)
                courseToRename = null
            }
        )
    }

    if (courseToDelete != null) {
        AlertDialog(
            onDismissRequest = { courseToDelete = null },
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.delete_course_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.delete_course_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        courseToDelete?.let { viewModel.deleteCourse(it) }
                        courseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { courseToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAddDialog) {
        AddFolderDialog(
            defaultNativeLang = nativeLanguage,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, targetCode, nativeCode ->
                viewModel.createCourseWithLangs(name, nativeCode, targetCode, true)
                showAddDialog = false
            }
        )
    }

    if (showAiDialog) {
        val availableModels by viewModel.availableModels.collectAsState()
        val selectedModel by viewModel.currentSelectedModel.collectAsState()

        AiGenerateDialog(
            nativeLanguage = nativeLanguage,
            aiState = aiState,
            availableModels = availableModels,
            selectedModel = selectedModel,
            onModelSelect = { viewModel.setSelectedModel(it) },
            onDismiss = { 
                showAiDialog = false
                viewModel.clearAiState()
            },
            onGenerate = { topic, targetCode, nativeCode, count, level ->
                viewModel.generateCourseWithAi(topic, targetCode, nativeCode, count, level)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiGenerateDialog(
    nativeLanguage: Language,
    aiState: AiGenerationState,
    availableModels: List<String>,
    selectedModel: String,
    onModelSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    onGenerate: (String, String, String, Int, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var topic by remember { mutableStateOf("") }
    var wordCountText by remember { mutableStateOf("20") }
    var selectedTargetLang by remember { mutableStateOf(supportedLanguages[0]) } 
    var selectedNativeLang by remember(nativeLanguage) { mutableStateOf(nativeLanguage) }
    
    var selectedLevel by remember { mutableStateOf("B2") }
    val levels = listOf("A1", "A2", "B1", "B2", "C1", "C2")
    var showLevelMenu by remember { mutableStateOf(false) }

    // Wizard Specific State
    var selectedCategory by remember { mutableStateOf<WizardCategory?>(null) }
    var selectedSubTopic by remember { mutableStateOf<String?>(null) }

    var showTargetMenu by remember { mutableStateOf(false) }
    var showNativeMenu by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }

    val isLoading = aiState is AiGenerationState.Loading
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        shape = MaterialTheme.shapes.large,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.ai_generate_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isLoading) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.generating_course))
                    }
                } else {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = { tabPositions: List<TabPosition> ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text(stringResource(R.string.ai_wizard)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text(stringResource(R.string.ai_manual)) }
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    if (selectedTab == 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.select_category), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                wizardPresets.forEach { category ->
                                    FilterChip(
                                        selected = selectedCategory?.id == category.id,
                                        onClick = { 
                                            selectedCategory = category
                                            selectedSubTopic = null 
                                        },
                                        label = { Text(stringResource(category.nameRes), fontSize = 12.sp) }
                                    )
                                }
                            }

                            if (selectedCategory != null) {
                                Text(stringResource(R.string.popular_topics), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    selectedCategory!!.subTopics.forEach { subTopic ->
                                        SuggestionChip(
                                            onClick = { 
                                                selectedSubTopic = subTopic
                                                topic = "${context.getString(selectedCategory!!.nameRes)}: $subTopic"
                                            },
                                            label = { Text(subTopic, fontSize = 11.sp) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = if (selectedSubTopic == subTopic) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = topic,
                                onValueChange = { topic = it },
                                label = { Text(stringResource(R.string.topic_label)) },
                                placeholder = { Text(stringResource(R.string.topic_hint)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.small,
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.difficulty_level), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    Box(modifier = Modifier.padding(top = 4.dp)) {
                                        OutlinedCard(
                                            onClick = { showLevelMenu = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(selectedLevel, style = MaterialTheme.typography.bodyMedium)
                                                Spacer(Modifier.weight(1f))
                                                Icon(Icons.Default.ArrowDropDown, null)
                                            }
                                        }
                                        DropdownMenu(expanded = showLevelMenu, onDismissRequest = { showLevelMenu = false }) {
                                            levels.forEach { level ->
                                                DropdownMenuItem(
                                                    text = { Text(level) },
                                                    onClick = { 
                                                        selectedLevel = level
                                                        showLevelMenu = false 
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.word_count_field), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = wordCountText,
                                        onValueChange = { if (it.all { char -> char.isDigit() }) wordCountText = it },
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        shape = MaterialTheme.shapes.small,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    val hasError = aiState is AiGenerationState.Error
                    if (hasError) {
                        Text(stringResource(R.string.ai_model_label), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Box {
                            OutlinedTextField(
                                value = selectedModel.removePrefix("models/"),
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth().clickable { if (availableModels.isNotEmpty()) showModelMenu = true },
                                enabled = false,
                                readOnly = true,
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                                shape = MaterialTheme.shapes.small
                            )
                            DropdownMenu(expanded = showModelMenu, onDismissRequest = { showModelMenu = false }) {
                                availableModels.forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model.removePrefix("models/")) },
                                        onClick = { onModelSelect(model); showModelMenu = false }
                                    )
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.target_lang_label), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            LanguagePicker(
                                selected = selectedTargetLang,
                                isExpanded = showTargetMenu,
                                onToggle = { showTargetMenu = !showTargetMenu },
                                onSelect = { selectedTargetLang = it; showTargetMenu = false }
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.native_lang_label), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            LanguagePicker(
                                selected = selectedNativeLang,
                                isExpanded = showNativeMenu,
                                onToggle = { showNativeMenu = !showNativeMenu },
                                onSelect = { selectedNativeLang = it; showNativeMenu = false }
                            )
                        }
                    }

                    if (aiState is AiGenerationState.Error) {
                        val errorMsg = when (aiState.code) {
                            "NO_KEY" -> stringResource(R.string.ai_error_no_key)
                            "FAILED" -> stringResource(R.string.ai_error_failed)
                            else -> aiState.code 
                        }
                        Text(errorMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            if (!isLoading) {
                Button(
                    onClick = { 
                        val count = wordCountText.toIntOrNull() ?: 20
                        if (topic.isNotBlank()) {
                            val levelToUse = if (selectedTab == 0) "B2" else selectedLevel
                            onGenerate(topic, selectedTargetLang.code, selectedNativeLang.code, count, levelToUse)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(stringResource(R.string.add), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isLoading) {
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}

@Composable
fun AddFolderDialog(
    defaultNativeLang: Language,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedTargetLang by remember { mutableStateOf(supportedLanguages[1]) }
    var selectedNativeLang by remember(defaultNativeLang) { mutableStateOf(defaultNativeLang) }
    
    var showTargetMenu by remember { mutableStateOf(false) }
    var showNativeMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CreateNewFolder, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.new_folder), 
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                ) 
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.folder_name)) },
                    placeholder = { Text("e.g. TOEFL Prep") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small
                )

                Text(stringResource(R.string.target_lang_label), style = MaterialTheme.typography.labelLarge)
                LanguagePicker(
                    selected = selectedTargetLang,
                    isExpanded = showTargetMenu,
                    onToggle = { showTargetMenu = !showTargetMenu },
                    onSelect = { selectedTargetLang = it; showTargetMenu = false }
                )

                Text(stringResource(R.string.native_lang_label), style = MaterialTheme.typography.labelLarge)
                LanguagePicker(
                    selected = selectedNativeLang,
                    isExpanded = showNativeMenu,
                    onToggle = { showNativeMenu = !showNativeMenu },
                    onSelect = { selectedNativeLang = it; showNativeMenu = false }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedTargetLang.code, selectedNativeLang.code) },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.add), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { 
                Text(stringResource(R.string.cancel)) 
            }
        }
    )
}

@Composable
fun CourseCard(course: Course, wordCount: Int, onClick: () -> Unit, onDelete: () -> Unit, onRename: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(160.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(getLanguageEmoji(course.targetLanguageCode), fontSize = 26.sp)
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📚", fontSize = 12.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = wordCount.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = course.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔥", fontSize = 12.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.streak_days, course.currentStreak),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.rename_folder)) },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = {
                            showMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CourseListItem(course: Course, wordCount: Int, onClick: () -> Unit, onDelete: () -> Unit, onRename: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(getLanguageEmoji(course.targetLanguageCode), fontSize = 24.sp)
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📚 $wordCount kelime",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "🔥 ${course.currentStreak} gün",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.rename_folder)) },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = {
                            showMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RenameFolderDialog(
    course: Course,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(course.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text(stringResource(R.string.rename_folder_title), fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.folder_name)) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                shape = MaterialTheme.shapes.small
            ) {
                Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun QuickAddDialog(text: String, viewModel: HomeViewModel, onDismiss: () -> Unit) {
    LaunchedEffect(text) {
        viewModel.autoTranslate(text, "tr", "en") // Basit bir başlangıç
    }
    val translationState by viewModel.translationState.collectAsState()
    val translationResult by viewModel.translationResult.collectAsState()

    AlertDialog(
        onDismissRequest = { 
            viewModel.clearTranslationState()
            onDismiss() 
        },
        title = { Text(stringResource(R.string.quick_add_title)) },
        text = {
            Column {
                Text(text, style = MaterialTheme.typography.bodyLarge)
                if (translationState is TranslationState.Loading) {
                    CircularProgressIndicator()
                } else {
                    Text(translationResult?.translation ?: stringResource(R.string.translation_not_found))
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                // Ekleme mantığı
                onDismiss()
                viewModel.clearIncomingText()
            }) { Text(stringResource(R.string.save)) }
        }
    )
}
