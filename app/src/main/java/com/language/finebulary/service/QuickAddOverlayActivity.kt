package com.language.finebulary.service

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.language.finebulary.R
import com.language.finebulary.data.db.AppDatabase
import com.language.finebulary.data.model.Course
import com.language.finebulary.data.model.CourseWithCount
import com.language.finebulary.data.model.supportedLanguages
import com.language.finebulary.repository.CourseRepository
import com.language.finebulary.ui.components.getLanguageEmoji
import com.language.finebulary.ui.screens.AddWordDialog
import com.language.finebulary.ui.theme.FinebularyTheme
import com.language.finebulary.ui.viewmodel.TranslationState
import com.language.finebulary.util.TranslationManager
import com.language.finebulary.util.ImageSearchManager
import com.language.finebulary.data.pref.UserPreferences
import com.language.finebulary.data.api.PexelsApiService
import com.language.finebulary.util.RichWordDetails
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.coroutines.resume

class QuickAddOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedText = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT)
            ?: intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: ""

        setContent {
            FinebularyTheme {
                var personalFolders by remember { mutableStateOf<List<CourseWithCount>>(emptyList()) }
                var selectedCourse by remember { mutableStateOf<Course?>(null) }
                var showFolderPicker by remember { mutableStateOf(false) }
                var showAddWordDialog by remember { mutableStateOf(false) }
                var showMissingLanguageDialog by remember { mutableStateOf<String?>(null) }
                var detectedLang by remember { mutableStateOf("en") }

                var translationResult by remember { mutableStateOf<RichWordDetails?>(null) }
                var translationState by remember { mutableStateOf<TranslationState>(TranslationState.Idle) }

                val coroutineScope = rememberCoroutineScope()
                val context = LocalContext.current
                val db = remember { AppDatabase.getDatabase(context) }
                val repo = remember { CourseRepository(db.vocabularyDao()) }
                val prefs = remember { UserPreferences(context) }
                val exampleSentenceSource by prefs.exampleSentenceSource.collectAsState(initial = "WIKTIONARY")

                val imageSearchManager = remember {
                    val client = OkHttpClient.Builder().build()
                    val pexelsApi = Retrofit.Builder()
                        .baseUrl("https://api.pexels.com/")
                        .client(client)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(PexelsApiService::class.java)
                    ImageSearchManager(pexelsApi)
                }

                LaunchedEffect(Unit) {
                    coroutineScope.launch(Dispatchers.IO) {
                        val folders = repo.allCoursesWithCount.first().filter { it.course.isPersonal }

                        // 1. Detect language of shared text using ML Kit
                        val detectedLangCode = detectLanguage(sharedText) ?: "en"

                        // 2. Find matching folders for this target language
                        val matchingFolders = folders.filter { it.course.targetLanguageCode.equals(detectedLangCode, ignoreCase = true) }

                        withContext(Dispatchers.Main) {
                            personalFolders = folders
                            detectedLang = detectedLangCode
                            if (matchingFolders.size == 1) {
                                // Exactly 1 folder exists, open dialog directly
                                selectedCourse = matchingFolders[0].course
                                showFolderPicker = false
                                showAddWordDialog = true
                                coroutineScope.launch {
                                    translateWord(sharedText, matchingFolders[0].course, prefs, imageSearchManager, { res -> translationResult = res }, { state -> translationState = state })
                                }
                            } else if (matchingFolders.size > 1) {
                                // Multiple matching folders exist! Let user choose among them.
                                showFolderPicker = true
                            } else {
                                // Folder does not exist for this language! Ask user to create one.
                                showMissingLanguageDialog = detectedLangCode
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (showMissingLanguageDialog != null) {
                            val missingLangCode = showMissingLanguageDialog!!
                            val langName = supportedLanguages.find { it.code.equals(missingLangCode, ignoreCase = true) }?.let { stringResource(it.nameRes) } ?: missingLangCode.uppercase()

                            AlertDialog(
                                onDismissRequest = { finish() },
                                shape = MaterialTheme.shapes.large,
                                title = { Text("Yeni Dil Tespit Edildi", fontWeight = FontWeight.Bold) },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("\"$sharedText\" metninin dili **$langName ($missingLangCode)** olarak tespit edildi.")
                                        Text("Bu dilde henüz bir klasörünüz yok. Otomatik olarak yeni bir klasör oluşturulsun mu?")
                                    }
                                },
                                confirmButton = {
                                    Button(onClick = {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            val nativeLangCode = prefs.appLanguage.first()
                                            val newCourseId = repo.addCourseObject(
                                                Course(
                                                    name = "$langName Öğrenme",
                                                    isPersonal = true,
                                                    nativeLanguageCode = nativeLangCode,
                                                    targetLanguageCode = missingLangCode
                                                )
                                            )
                                            val newCourse = repo.getCourseById(newCourseId)
                                            withContext(Dispatchers.Main) {
                                                showMissingLanguageDialog = null
                                                if (newCourse != null) {
                                                    selectedCourse = newCourse
                                                    showAddWordDialog = true
                                                    coroutineScope.launch {
                                                        translateWord(sharedText, newCourse, prefs, imageSearchManager, { res -> translationResult = res }, { state -> translationState = state })
                                                    }
                                                } else {
                                                    finish()
                                                }
                                            }
                                        }
                                    }) {
                                        Text(stringResource(R.string.create_folder_and_save))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { 
                                        showMissingLanguageDialog = null
                                        showFolderPicker = true 
                                    }) {
                                        Text(stringResource(R.string.select_other_folder))
                                    }
                                }
                            )
                        }

                        if (showFolderPicker) {
                            AlertDialog(
                                onDismissRequest = { finish() },
                                shape = MaterialTheme.shapes.large,
                                title = { Text(stringResource(R.string.select_folder), fontWeight = FontWeight.Bold) },
                                text = {
                                    Column {
                                        Text(stringResource(R.string.choose_where_add, sharedText), style = MaterialTheme.typography.bodyMedium)
                                        Spacer(Modifier.height(16.dp))
                                        val foldersToList = personalFolders.filter { it.course.targetLanguageCode.equals(detectedLang, ignoreCase = true) }.let { if (it.isEmpty()) personalFolders else it }
                                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                                            items(foldersToList) { item ->
                                                ListItem(
                                                    headlineContent = { Text(item.course.name) },
                                                    leadingContent = { Text(getLanguageEmoji(item.course.targetLanguageCode)) },
                                                    modifier = Modifier.clickable {
                                                        selectedCourse = item.course
                                                        showFolderPicker = false
                                                        showAddWordDialog = true
                                                        coroutineScope.launch {
                                                            translateWord(sharedText, item.course, prefs, imageSearchManager, { res -> translationResult = res }, { state -> translationState = state })
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                        if (personalFolders.isEmpty()) {
                                            Text(stringResource(R.string.no_personal_folders), color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                },
                                confirmButton = {},
                                dismissButton = {
                                    TextButton(onClick = { finish() }) {
                                        Text(stringResource(R.string.cancel))
                                    }
                                }
                            )
                        }

                        if (showAddWordDialog && selectedCourse != null) {
                            AddWordDialog(
                                course = selectedCourse!!,
                                initialFrontText = sharedText,
                                onDismiss = { finish() },
                                onConfirm = { front, back, definition, synonyms, antonyms, example, imageUrl ->
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val success = repo.addSingleWord(selectedCourse!!.id, front, back, definition, synonyms, antonyms, example, imageUrl)
                                        withContext(Dispatchers.Main) {
                                            if (!success) {
                                                Toast.makeText(context, "Bu kelime zaten bu klasörde mevcut!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Kelime eklendi", Toast.LENGTH_SHORT).show()
                                                finish()
                                            }
                                        }
                                    }
                                },
                                onTranslate = { text, _, _ ->
                                    coroutineScope.launch {
                                        translateWord(text, selectedCourse!!, prefs, imageSearchManager, { res -> translationResult = res }, { state -> translationState = state })
                                    }
                                },
                                translationResult = translationResult,
                                translationState = translationState,
                                exampleSentenceSource = exampleSentenceSource
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun detectLanguage(text: String): String? = suspendCancellableCoroutine { continuation ->
        val identifier = LanguageIdentification.getClient()
        identifier.identifyLanguage(text)
            .addOnSuccessListener { languageCode ->
                if (languageCode == "und") {
                    continuation.resume(null)
                } else {
                    continuation.resume(languageCode)
                }
            }
            .addOnFailureListener {
                continuation.resume(null)
            }
    }

    private suspend fun translateWord(
        text: String,
        course: Course,
        prefs: UserPreferences,
        imageSearchManager: ImageSearchManager,
        onResult: (RichWordDetails?) -> Unit,
        onState: (TranslationState) -> Unit
    ) = coroutineScope {
        onState(TranslationState.Loading())
        try {
            val pKey = prefs.pexelsApiKey.first()
            val fetchExSource = prefs.exampleSentenceSource.first()
            val activeProviderStr = prefs.activeAiProvider.first()
            val aiProviderEnum = try { com.language.finebulary.util.AiServiceProvider.valueOf(activeProviderStr) } catch (_: Exception) { com.language.finebulary.util.AiServiceProvider.GEMINI }

            val translationDeferred = async { TranslationManager.translate(text, course.targetLanguageCode, course.nativeLanguageCode) }
            
            val detailsDeferred = async {
                if (fetchExSource == "AI") {
                    val apiKey = when (aiProviderEnum) {
                        com.language.finebulary.util.AiServiceProvider.GEMINI -> prefs.geminiApiKey.first()
                        com.language.finebulary.util.AiServiceProvider.GROQ -> prefs.groqApiKey.first()
                        com.language.finebulary.util.AiServiceProvider.OPENAI -> prefs.openaiApiKey.first()
                        com.language.finebulary.util.AiServiceProvider.DEEPSEEK -> prefs.deepseekApiKey.first()
                        com.language.finebulary.util.AiServiceProvider.OPENROUTER -> prefs.openrouterApiKey.first()
                    }
                    val model = when (aiProviderEnum) {
                        com.language.finebulary.util.AiServiceProvider.GEMINI -> prefs.geminiModel.first()
                        com.language.finebulary.util.AiServiceProvider.GROQ -> prefs.groqModel.first()
                        com.language.finebulary.util.AiServiceProvider.OPENAI -> prefs.openaiModel.first()
                        com.language.finebulary.util.AiServiceProvider.DEEPSEEK -> prefs.deepseekModel.first()
                        com.language.finebulary.util.AiServiceProvider.OPENROUTER -> prefs.openrouterModel.first()
                    }
                    if (!apiKey.isNullOrBlank()) {
                        try {
                            val client = OkHttpClient.Builder().build()
                            val geminiApi = Retrofit.Builder()
                                .baseUrl("https://generativelanguage.googleapis.com/")
                                .client(client)
                                .addConverterFactory(GsonConverterFactory.create())
                                .build()
                                .create(com.language.finebulary.data.api.GeminiApiService::class.java)
                            val openAiApi = Retrofit.Builder()
                                .baseUrl("https://api.openai.com/")
                                .client(client)
                                .addConverterFactory(GsonConverterFactory.create())
                                .build()
                                .create(com.language.finebulary.data.api.OpenAiApiService::class.java)
                            val providerRegistry = com.language.finebulary.util.ProviderRegistry(geminiApi, openAiApi)
                            val providerImpl = providerRegistry.getProvider(aiProviderEnum, prefs.showLegacyModels.first())
                            providerImpl.generateWordDetails(apiKey, model, text, course.targetLanguageCode, course.nativeLanguageCode)
                        } catch (_: Exception) {
                            TranslationManager.getDictionaryDetails(text, course.targetLanguageCode, course.nativeLanguageCode, "NONE")
                        }
                    } else {
                        TranslationManager.getDictionaryDetails(text, course.targetLanguageCode, course.nativeLanguageCode, "NONE")
                    }
                } else {
                    TranslationManager.getDictionaryDetails(text, course.targetLanguageCode, course.nativeLanguageCode, fetchExSource)
                }
            }

            val imageUrlDeferred = async { if (!pKey.isNullOrBlank()) imageSearchManager.searchImage(text, pKey) else null }
            
            val translation = translationDeferred.await()
            val details = detailsDeferred.await()
            val imageUrl = imageUrlDeferred.await()
            
            var result = details.copy(translation = translation)
            if (imageUrl != null) {
                result = result.copy(imageUrl = imageUrl)
            }
            if (result.translation == null) {
                onState(TranslationState.Error("Translation failed."))
            } else {
                onResult(result)
                onState(TranslationState.Idle)
            }
        } catch (e: Exception) {
            onState(TranslationState.Error("Error: ${e.localizedMessage}"))
        }
    }
}
