package com.language.finebulary.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.widget.Toast
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.language.finebulary.R
import com.language.finebulary.data.db.AppDatabase
import com.language.finebulary.data.model.Course
import com.language.finebulary.data.model.CourseWithCount
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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class QuickAddOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sharedText = intent?.getStringExtra(Intent.EXTRA_PROCESS_TEXT) 
            ?: intent?.getStringExtra(Intent.EXTRA_TEXT) 
            ?: ""

        if (sharedText.isNotBlank()) {
            showOverlay(sharedText)
        } else {
            stopSelf()
        }

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        return START_NOT_STICKY
    }

    private fun showOverlay(sharedText: String) {
        if (composeView != null) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@QuickAddOverlayService)
            setViewTreeSavedStateRegistryOwner(this@QuickAddOverlayService)

            setContent {
                FinebularyTheme {
                    var personalFolders by remember { mutableStateOf<List<CourseWithCount>>(emptyList()) }
                    var selectedCourse by remember { mutableStateOf<Course?>(null) }
                    var showFolderPicker by remember { mutableStateOf(true) }
                    var showAddWordDialog by remember { mutableStateOf(false) }

                    var translationResult by remember { mutableStateOf<RichWordDetails?>(null) }
                    var translationState by remember { mutableStateOf<TranslationState>(TranslationState.Idle) }

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
                        serviceScope.launch(Dispatchers.IO) {
                            val folders = repo.allCoursesWithCount.first().filter { it.course.isPersonal }
                            withContext(Dispatchers.Main) {
                                personalFolders = folders
                                if (folders.size == 1) {
                                    selectedCourse = folders[0].course
                                    showFolderPicker = false
                                    showAddWordDialog = true
                                    translateWord(sharedText, folders[0].course, prefs, imageSearchManager, { res -> translationResult = res }, { state -> translationState = state })
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (showFolderPicker) {
                                AlertDialog(
                                    onDismissRequest = { stopSelf() },
                                    shape = MaterialTheme.shapes.large,
                                    title = { Text(stringResource(R.string.select_folder), fontWeight = FontWeight.Bold) },
                                    text = {
                                        Column {
                                            Text(stringResource(R.string.choose_where_add, sharedText), style = MaterialTheme.typography.bodyMedium)
                                            Spacer(Modifier.height(16.dp))
                                            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                                                items(personalFolders) { item ->
                                                    ListItem(
                                                        headlineContent = { Text(item.course.name) },
                                                        leadingContent = { Text(getLanguageEmoji(item.course.targetLanguageCode)) },
                                                        modifier = Modifier.clickable {
                                                            selectedCourse = item.course
                                                            showFolderPicker = false
                                                            showAddWordDialog = true
                                                            serviceScope.launch {
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
                                        TextButton(onClick = { stopSelf() }) {
                                            Text(stringResource(R.string.cancel))
                                        }
                                    }
                                )
                            }

                            if (showAddWordDialog && selectedCourse != null) {
                                AddWordDialog(
                                    course = selectedCourse!!,
                                    initialFrontText = sharedText,
                                    onDismiss = { stopSelf() },
                                    onConfirm = { front, back, definition, synonyms, antonyms, example, imageUrl ->
                                        serviceScope.launch(Dispatchers.IO) {
                                            val success = repo.addSingleWord(selectedCourse!!.id, front, back, definition, synonyms, antonyms, example, imageUrl)
                                            withContext(Dispatchers.Main) {
                                                if (!success) {
                                                    Toast.makeText(context, "Bu kelime zaten bu klasörde mevcut!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Kelime eklendi", Toast.LENGTH_SHORT).show()
                                                    stopSelf()
                                                }
                                            }
                                        }
                                    },
                                    onTranslate = { text, _, _ ->
                                        serviceScope.launch {
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

        try {
            windowManager?.addView(composeView, params)
        } catch (e: Exception) {
            e.printStackTrace()
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

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        serviceJob.cancel()
        if (composeView != null) {
            try {
                windowManager?.removeView(composeView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            composeView = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
