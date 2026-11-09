package com.language.finebulary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.language.finebulary.R
import com.language.finebulary.data.model.Course
import com.language.finebulary.data.model.IeltsPassageEntity
import com.language.finebulary.data.model.Language
import com.language.finebulary.data.pref.UserPreferences
import com.language.finebulary.ui.components.EmptyStateView
import com.language.finebulary.ui.components.LanguagePicker
import com.language.finebulary.ui.components.getLanguageEmoji
import com.language.finebulary.ui.viewmodel.HomeViewModel
import com.language.finebulary.ui.viewmodel.IeltsReadingState
import com.language.finebulary.ui.viewmodel.TranslationState
import com.language.finebulary.util.CloudAudioPlayer
import com.language.finebulary.util.TtsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IeltsReadingScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.ieltsState.collectAsState()
    val scrollState = rememberScrollState()
    val personalFolders by viewModel.personalFolders.collectAsState()
    val nativeLanguage by viewModel.nativeLanguage.collectAsState()
    val scope = rememberCoroutineScope()
    
    var showTranslation by remember { mutableStateOf(false) }
    var wordToLookup by remember { mutableStateOf<String?>(null) }
    var wordToAdd by remember { mutableStateOf<com.language.finebulary.data.api.AiGeneratedWord?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    
    var selectedLevel by remember { mutableStateOf("B2") }
    val levels = listOf("A1", "A2", "B1", "B2", "C1", "C2")
    
    val contentLanguages = listOf(
        Language(R.string.lang_english, "en"),
        Language(R.string.lang_spanish, "es"),
        Language(R.string.lang_french, "fr"),
        Language(R.string.lang_german, "de"),
        Language(R.string.lang_italian, "it"),
        Language(R.string.lang_russian, "ru"),
        Language(R.string.lang_chinese, "zh"),
        Language(R.string.lang_japanese, "ja"),
        Language(R.string.lang_korean, "ko"),
        Language(R.string.lang_portuguese, "pt")
    )
    var selectedTargetLang by remember { mutableStateOf(contentLanguages[0]) }
    var showTargetMenu by remember { mutableStateOf(false) }

    val categories = listOf(
        Pair(stringResource(R.string.cat_story), "Story"),
        Pair(stringResource(R.string.cat_current_article), "Current Article"),
        Pair(stringResource(R.string.cat_history), "History"),
        Pair(stringResource(R.string.cat_science_tech), "Science and Technology"),
        Pair(stringResource(R.string.cat_academic_exam), "Academic / Exam")
    )
    var selectedCategoryPair by remember { mutableStateOf(categories[0]) }

    var customTopic by remember { mutableStateOf("") }
    var selectedNativeLang by remember(nativeLanguage) { mutableStateOf(nativeLanguage) }
    var showNativeMenu by remember { mutableStateOf(false) }

    var isSentenceMode by remember { mutableStateOf(false) }
    var selectedSentence by remember { mutableStateOf<String?>(null) }

    val translationResult by viewModel.translationResult.collectAsState()
    val translationState by viewModel.translationState.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            TtsManager.stop()
            CloudAudioPlayer.stop()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.academy_title), fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                navigationIcon = {
                    IconButton(onClick = {
                        TtsManager.stop()
                        CloudAudioPlayer.stop()
                        viewModel.exitIelts()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    IconButton(onClick = { showHistory = !showHistory }) {
                        Icon(if (showHistory) Icons.Default.HistoryToggleOff else Icons.Default.History, "History", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    if (state is IeltsReadingState.Success && !showHistory) {
                        IconButton(onClick = { isSentenceMode = !isSentenceMode }) {
                            Icon(if (isSentenceMode) Icons.AutoMirrored.Filled.Subject else Icons.Default.TextFields, "Sentence/Word Mode", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        IconButton(onClick = { showTranslation = !showTranslation }) {
                            Icon(if (showTranslation) Icons.Default.GTranslate else Icons.Default.Translate, "Toggle Translation", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                    IconButton(onClick = { 
                        showHistory = false
                        TtsManager.stop()
                        CloudAudioPlayer.stop()
                        viewModel.exitIelts() 
                    }) {
                        Icon(Icons.Default.AutoAwesome, "New Passage", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (showHistory) {
                val history by viewModel.ieltsHistory.collectAsState()
                IeltsHistoryView(
                    history = history,
                    onSelect = { 
                        viewModel.loadIeltsPassage(it)
                        showHistory = false 
                    },
                    onDelete = { viewModel.deleteIeltsPassage(it) }
                )
            } else {
                when (val s = state) {
                    is IeltsReadingState.Idle -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Kompakt Başlık Alanı
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.School, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.academy_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text(stringResource(R.string.academy_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // 1. Dil Seçimleri (Yan Yana)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.target_lang_label), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    LanguagePicker(
                                        selected = selectedTargetLang,
                                        isExpanded = showTargetMenu,
                                        onToggle = { showTargetMenu = !showTargetMenu },
                                        onSelect = { selectedTargetLang = it; showTargetMenu = false }
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.explanation_lang_label), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    LanguagePicker(
                                        selected = selectedNativeLang,
                                        isExpanded = showNativeMenu,
                                        onToggle = { showNativeMenu = !showNativeMenu },
                                        onSelect = { selectedNativeLang = it; showNativeMenu = false }
                                    )
                                }
                            }

                            // 2. Kategori Seçimi
                            Text(stringResource(R.string.content_category_label), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                categories.forEach { catPair ->
                                    FilterChip(
                                        selected = selectedCategoryPair == catPair,
                                        onClick = { selectedCategoryPair = catPair },
                                        label = { Text(catPair.first, fontSize = 13.sp) }
                                    )
                                }
                            }

                            // 3. Zorluk Seviyesi
                            Text(stringResource(R.string.difficulty_level), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                levels.forEach { level ->
                                    FilterChip(
                                        selected = selectedLevel == level,
                                        onClick = { selectedLevel = level },
                                        label = { Text(level, fontSize = 13.sp) }
                                    )
                                }
                            }

                            // 4. Özel Konu
                            Text(stringResource(R.string.custom_topic_label), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = customTopic,
                                onValueChange = { customTopic = it },
                                placeholder = { Text(stringResource(R.string.custom_topic_placeholder)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.small,
                                singleLine = true
                            )

                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = { 
                                    val fullTopic = "Category: ${selectedCategoryPair.second}. ${customTopic.ifBlank { "" }}"
                                    viewModel.generateIeltsReading(selectedLevel, selectedNativeLang.code, selectedTargetLang.code, fullTopic) 
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(stringResource(R.string.start_reading), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    is IeltsReadingState.Loading -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(R.string.curating_content), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    is IeltsReadingState.Success -> {
                        // LOCAL ANSWERS STATE
                        var currentAnswers by remember(s.passageId) { 
                            mutableStateOf(s.userAnswers ?: emptyMap()) 
                        }
                        var isChecked by remember(s.passageId) { 
                            mutableStateOf(s.userAnswers != null) 
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = s.passage.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                val audioProvider by viewModel.audioProvider.collectAsState()
                                val openaiKey by viewModel.openaiApiKey.collectAsState()
                                val elevenlabsKey by viewModel.elevenlabsApiKey.collectAsState()
                                val azureKey by viewModel.azureSpeechApiKey.collectAsState()
                                val deepgramKey by viewModel.deepgramApiKey.collectAsState()

                                IconButton(onClick = {
                                    scope.launch {
                                        val targetLangCode = selectedTargetLang.code
                                        if (audioProvider == "STANDARD") {
                                            TtsManager.speak(s.passage.passage, targetLangCode)
                                        } else {
                                            val activeKey = when (audioProvider) {
                                                "ELEVENLABS" -> elevenlabsKey
                                                "AZURE" -> azureKey
                                                "DEEPGRAM" -> deepgramKey
                                                else -> openaiKey
                                            }

                                            if (!activeKey.isNullOrBlank()) {
                                                val success = CloudAudioPlayer.playSpeech(context, s.passage.passage, audioProvider, activeKey, targetLangCode)
                                                if (!success) {
                                                    TtsManager.speak(s.passage.passage, targetLangCode)
                                                }
                                            } else {
                                                TtsManager.speak(s.passage.passage, targetLangCode)
                                            }
                                        }
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Metni Dinle", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = MaterialTheme.shapes.large
                            ) {
                                if (showTranslation) {
                                    val translation = s.passage.translatedPassage
                                    if (translation.isNullOrBlank()) {
                                        Box(Modifier.padding(20.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            Text(stringResource(R.string.translation_not_available), color = MaterialTheme.colorScheme.error)
                                        }
                                    } else {
                                        Text(
                                            text = translation,
                                            style = MaterialTheme.typography.bodyLarge,
                                            lineHeight = 28.sp,
                                            modifier = Modifier.padding(20.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        val sentenceRegex = Regex("(?<=[.!?])\\s+")
                                        val sentences = s.passage.passage.split(sentenceRegex)
                                        
                                        sentences.forEach { sentence ->
                                            if (isSentenceMode) {
                                                Text(
                                                    text = sentence,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    lineHeight = 28.sp,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            selectedSentence = sentence
                                                            viewModel.translateSentence(sentence, "en", selectedNativeLang.code)
                                                        }
                                                        .padding(vertical = 4.dp)
                                                )
                                            } else {
                                                val isAsianLang = selectedTargetLang.code in listOf("ja", "zh", "ko")
                                                if (isAsianLang) {
                                                    // Asya dillerinde (Japonca, Çince, Korece) boşluk olmayabilir, karakter karakter veya kelime bazlı tıklama
                                                    FlowRow(
                                                        modifier = Modifier.padding(vertical = 2.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        sentence.forEach { char ->
                                                            val cleanChar = char.toString()
                                                            if (cleanChar.isNotBlank()) {
                                                                Text(
                                                                    text = cleanChar,
                                                                    style = MaterialTheme.typography.bodyLarge,
                                                                    lineHeight = 28.sp,
                                                                    modifier = Modifier.clickable {
                                                                        wordToLookup = cleanChar
                                                                        viewModel.autoTranslate(cleanChar, selectedTargetLang.code, selectedNativeLang.code)
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    FlowRow(
                                                        modifier = Modifier.padding(vertical = 2.dp),
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        sentence.split(Regex("\\s+")).forEach { word ->
                                                            val cleanWord = word.trim().replace(Regex("[^\\p{L}\\p{Nd}]"), "")
                                                            Text(
                                                                text = word,
                                                                style = MaterialTheme.typography.bodyLarge,
                                                                lineHeight = 28.sp,
                                                                modifier = Modifier.clickable { 
                                                                    if (cleanWord.isNotEmpty()) {
                                                                        wordToLookup = cleanWord
                                                                        viewModel.autoTranslate(cleanWord, selectedTargetLang.code, selectedNativeLang.code)
                                                                    }
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(32.dp))
                            
                            Text(stringResource(R.string.reading_comp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))

                            s.passage.questions.forEachIndexed { index, q ->
                                IeltsQuestionCard(
                                    index = index + 1,
                                    q = q,
                                    currentAnswer = currentAnswers[index],
                                    isResultVisible = isChecked,
                                    onAnswerChange = { currentAnswers = currentAnswers + (index to it) }
                                )
                                Spacer(Modifier.height(16.dp))
                            }
                            
                            if (!isChecked) {
                                Button(
                                    onClick = { 
                                        isChecked = true
                                        viewModel.saveIeltsAnswers(s.passageId, currentAnswers)
                                        scope.launch { scrollState.animateScrollTo(0) }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = currentAnswers.size == s.passage.questions.size
                                ) {
                                    Text(stringResource(R.string.check_answers))
                                }
                            } else {
                                val score = s.passage.questions.indices.count { i ->
                                    val q = s.passage.questions[i]
                                    val ans = currentAnswers[i]
                                    when (q.type) {
                                        "MULTIPLE_CHOICE", "TFNG", "HEADING_MATCHING" -> {
                                            val correct = q.options?.getOrNull(q.correctIndex ?: -1)
                                            ans == correct
                                        }
                                        "GAP_FILL" -> ans?.trim().equals(q.correctAnswer?.trim(), ignoreCase = true)
                                        else -> false
                                    }
                                }
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text(
                                        text = stringResource(R.string.your_score, score, s.passage.questions.size),
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Spacer(Modifier.height(32.dp))
                            Text(stringResource(R.string.academic_vocab), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            
                            s.passage.academicKeywords.forEach { word ->
                                IeltsWordCard(word) {
                                    wordToAdd = word
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                            
                            Spacer(Modifier.height(48.dp))
                        }
                    }
                    is IeltsReadingState.Error -> {
                        EmptyStateView(
                            icon = Icons.Default.CloudOff,
                            title = "Generation Failed",
                            description = s.message,
                            action = {
                                Button(onClick = { viewModel.generateIeltsReading(selectedLevel, selectedNativeLang.code) }) {
                                    Text("Retry")
                                }
                            }
                        )
                    }
                }
            }

            // Word Lookup Dialog
            if (wordToLookup != null) {
                val dummyCourse = personalFolders.firstOrNull()?.course ?: Course(
                    id = 0,
                    name = "Academy",
                    targetLanguageCode = selectedTargetLang.code,
                    nativeLanguageCode = selectedNativeLang.code
                )
                val prefs = remember { UserPreferences(context) }
                val exampleSentenceSource by prefs.exampleSentenceSource.collectAsState(initial = "WIKTIONARY")

                AddWordDialog(
                    course = dummyCourse,
                    initialFrontText = wordToLookup,
                    translationState = translationState,
                    exampleSentenceSource = exampleSentenceSource,
                    onDismiss = { wordToLookup = null; viewModel.clearTranslationState() },
                    onConfirm = { front, back, definition, synonyms, antonyms, example, imageUrl ->
                        wordToAdd = com.language.finebulary.data.api.AiGeneratedWord(
                            front = front,
                            back = back,
                            definition = definition,
                            synonyms = synonyms,
                            antonyms = antonyms,
                            example = example,
                            imageUrl = imageUrl
                        )
                        wordToLookup = null
                        viewModel.clearTranslationState()
                    },
                    onTranslate = { text, target, native ->
                        viewModel.autoTranslate(text, target, native)
                    },
                    translationResult = translationResult
                )
            }

            // Sentence Translation Dialog
            if (selectedSentence != null) {
                AlertDialog(
                    onDismissRequest = { selectedSentence = null; viewModel.clearTranslationState() },
                    title = { Text("Sentence Translation", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text(selectedSentence!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(16.dp))
                            if (translationState is TranslationState.Loading) {
                                CircularProgressIndicator()
                            } else if (viewModel.sentenceTranslation.collectAsState().value != null) {
                                Text(viewModel.sentenceTranslation.collectAsState().value!!, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { selectedSentence = null }) {
                            Text("Got it")
                        }
                    }
                )
            }

            // Folder Picker Dialog
            if (wordToAdd != null) {
                val matchingFolders = personalFolders.filter { it.course.targetLanguageCode.equals(selectedTargetLang.code, ignoreCase = true) }
                val foldersToList = if (matchingFolders.isNotEmpty()) matchingFolders else personalFolders

                AlertDialog(
                    onDismissRequest = { wordToAdd = null },
                    title = { Text(stringResource(R.string.select_folder), fontWeight = FontWeight.Bold) },
                    text = {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(foldersToList) { item ->
                                ListItem(
                                    headlineContent = { Text(item.course.name) },
                                    leadingContent = { Text(getLanguageEmoji(item.course.targetLanguageCode)) },
                                    modifier = Modifier.clickable {
                                        viewModel.addManualWord(
                                            courseId = item.course.id,
                                            front = wordToAdd!!.front,
                                            back = wordToAdd!!.back,
                                            definition = wordToAdd!!.definition,
                                            synonyms = wordToAdd!!.synonyms,
                                            antonyms = wordToAdd!!.antonyms,
                                            example = wordToAdd!!.example,
                                            imageUrl = wordToAdd!!.imageUrl
                                        ) { success ->
                                            if (!success) {
                                                android.widget.Toast.makeText(context, "Bu kelime zaten bu klasörde mevcut!", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                android.widget.Toast.makeText(context, "Word added to ${item.course.name}", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        wordToAdd = null
                                    }
                                )
                            }
                            if (foldersToList.isEmpty()) {
                                item {
                                    Text(stringResource(R.string.no_personal_folders), color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { wordToAdd = null }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun IeltsHistoryView(
    history: List<IeltsPassageEntity>,
    onSelect: (IeltsPassageEntity) -> Unit,
    onDelete: (IeltsPassageEntity) -> Unit
) {
    if (history.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.History,
            title = "No History Yet",
            description = "Your generated IELTS passages will appear here."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(history) { item ->
                OutlinedCard(
                    onClick = { onSelect(item) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Level: ${item.level} • ${item.nativeLangCode.uppercase()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                if (item.userAnswersJson != null) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(color = Color(0xFF4CAF50), shape = MaterialTheme.shapes.extraSmall) {
                                        Text("Completed", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp))
                                    }
                                }
                            }
                        }
                        IconButton(onClick = { onDelete(item) }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IeltsQuestionCard(
    index: Int, 
    q: com.language.finebulary.data.api.IeltsQuestion,
    currentAnswer: String?,
    isResultVisible: Boolean,
    onAnswerChange: (String) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "$index. ${q.question}", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            when (q.type) {
                "MULTIPLE_CHOICE", "TFNG", "HEADING_MATCHING" -> {
                    q.options?.forEachIndexed { i, option ->
                        val isCorrect = i == q.correctIndex
                        val isSelected = currentAnswer == option
                        
                        Surface(
                            onClick = { if (!isResultVisible) onAnswerChange(option) },
                            shape = MaterialTheme.shapes.small,
                            color = when {
                                isResultVisible && isCorrect -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                isResultVisible && isSelected -> Color(0xFFF44336).copy(alpha = 0.2f)
                                isSelected -> MaterialTheme.colorScheme.primaryContainer
                                else -> Color.Transparent
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = isSelected, onClick = { if (!isResultVisible) onAnswerChange(option) })
                                Text(text = option, style = MaterialTheme.typography.bodyMedium)
                                if (isResultVisible && isCorrect) {
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
                                }
                            }
                        }
                    }
                }
                "GAP_FILL" -> {
                    val isCorrect = currentAnswer?.trim().equals(q.correctAnswer?.trim(), ignoreCase = true)
                    OutlinedTextField(
                        value = currentAnswer ?: "",
                        onValueChange = { if (!isResultVisible) onAnswerChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Type your answer") },
                        enabled = !isResultVisible,
                        trailingIcon = {
                            if (isResultVisible) {
                                Icon(
                                    imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)
                                )
                            }
                        }
                    )
                }
            }
            
            if (isResultVisible) {
                Spacer(Modifier.height(8.dp))
                val feedback = if (q.type == "GAP_FILL") "Correct Answer: ${q.correctAnswer}. ${q.explanation}" else q.explanation
                Text(
                    text = feedback,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun IeltsWordCard(word: com.language.finebulary.data.api.AiGeneratedWord, onAdd: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(word.front, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(word.back, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.AutoAwesome, "Add to Vocabulary", tint = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
