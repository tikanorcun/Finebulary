package com.language.finebulary.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.language.finebulary.data.model.Course
import com.language.finebulary.data.model.Language
import com.language.finebulary.data.model.ReviewState
import com.language.finebulary.data.model.Word
import com.language.finebulary.data.model.supportedLanguages
import com.language.finebulary.domain.usecase.GetHomeDataUseCase
import com.language.finebulary.domain.usecase.UpdateWordStatusUseCase
import com.language.finebulary.domain.usecase.DetectLanguageUseCase
import com.language.finebulary.domain.usecase.GetRecommendedCourseUseCase
import com.language.finebulary.data.pref.ThemeMode
import com.language.finebulary.data.pref.UserPreferences
import com.language.finebulary.data.pref.FolderViewStyle
import com.language.finebulary.repository.CourseRepository
import com.language.finebulary.util.BackupManager
import com.language.finebulary.util.ImageSearchManager
import com.language.finebulary.util.RichWordDetails
import com.language.finebulary.util.TranslationManager
import com.language.finebulary.util.AiServiceProvider
import com.language.finebulary.util.ProviderRegistry
import com.language.finebulary.BuildConfig
import com.language.finebulary.util.UpdateManager
import com.language.finebulary.data.api.GitHubRelease
import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: CourseRepository,
    private val userPreferences: UserPreferences,
    private val imageSearchManager: ImageSearchManager,
    private val providerRegistry: ProviderRegistry,
    private val getHomeDataUseCase: GetHomeDataUseCase,
    private val updateWordStatusUseCase: UpdateWordStatusUseCase,
    private val detectLanguageUseCase: DetectLanguageUseCase,
    private val getRecommendedCourseUseCase: GetRecommendedCourseUseCase,
    private val updateManager: UpdateManager,
) : ViewModel() {

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _availableUpdate = MutableStateFlow<GitHubRelease?>(null)
    val availableUpdate: StateFlow<GitHubRelease?> = _availableUpdate.asStateFlow()

    fun checkForUpdates(context: Context, onUpToDate: () -> Unit = {}) {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val release = updateManager.checkForUpdate()
            _isCheckingUpdate.value = false
            if (release != null) {
                _availableUpdate.value = release
            } else {
                onUpToDate()
            }
        }
    }

    fun clearAvailableUpdate() {
        _availableUpdate.value = null
    }

    fun downloadAndInstallUpdate(context: Context, release: GitHubRelease) {
        val apkAsset = release.assets?.find { it.name.endsWith(".apk", ignoreCase = true) }
            ?: release.assets?.firstOrNull()

        if (apkAsset != null) {
            updateManager.downloadAndInstallApk(context, apkAsset.browserDownloadUrl, release.tagName)
        } else if (!release.htmlUrl.isNullOrEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl))
            context.startActivity(intent)
        }
    }

    val themeMode: StateFlow<ThemeMode> = userPreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.AUTO)
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { userPreferences.setThemeMode(mode) }

    val appThemeColor: StateFlow<com.language.finebulary.data.pref.AppThemeColor> = userPreferences.appThemeColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.language.finebulary.data.pref.AppThemeColor.WARM)
    fun setAppThemeColor(color: com.language.finebulary.data.pref.AppThemeColor) = viewModelScope.launch { userPreferences.setAppThemeColor(color) }

    val pexelsApiKey: StateFlow<String?> = userPreferences.pexelsApiKey.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    fun setPexelsApiKey(key: String) = viewModelScope.launch { userPreferences.setPexelsApiKey(key) }

    // Multi-Provider Keys
    val geminiApiKey: StateFlow<String?> = userPreferences.geminiApiKey.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val groqApiKey: StateFlow<String?> = userPreferences.groqApiKey.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val openaiApiKey: StateFlow<String?> = userPreferences.openaiApiKey.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val deepseekApiKey: StateFlow<String?> = userPreferences.deepseekApiKey.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val openrouterApiKey: StateFlow<String?> = userPreferences.openrouterApiKey.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val elevenlabsApiKey: StateFlow<String?> = userPreferences.elevenlabsApiKey.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val azureSpeechApiKey: StateFlow<String?> = userPreferences.azureSpeechApiKey.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val deepgramApiKey: StateFlow<String?> = userPreferences.deepgramApiKey.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val audioProvider: StateFlow<String> = userPreferences.audioProvider.stateIn(viewModelScope, SharingStarted.Eagerly, "OPENAI")
    val deviceTtsEngine: StateFlow<String?> = userPreferences.deviceTtsEngine.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setGeminiApiKey(key: String) = viewModelScope.launch { userPreferences.setGeminiApiKey(key); if (activeAiProvider.value == AiServiceProvider.GEMINI) refreshAvailableModels() }
    fun setGroqApiKey(key: String) = viewModelScope.launch { userPreferences.setGroqApiKey(key); if (activeAiProvider.value == AiServiceProvider.GROQ) refreshAvailableModels() }
    fun setOpenAiApiKey(key: String) = viewModelScope.launch { userPreferences.setOpenAiApiKey(key); if (activeAiProvider.value == AiServiceProvider.OPENAI) refreshAvailableModels() }
    fun setDeepseekApiKey(key: String) = viewModelScope.launch { userPreferences.setDeepseekApiKey(key); if (activeAiProvider.value == AiServiceProvider.DEEPSEEK) refreshAvailableModels() }
    fun setOpenrouterApiKey(key: String) = viewModelScope.launch { userPreferences.setOpenrouterApiKey(key); if (activeAiProvider.value == AiServiceProvider.OPENROUTER) refreshAvailableModels() }
    fun setElevenlabsApiKey(key: String) = viewModelScope.launch { userPreferences.setElevenlabsApiKey(key) }
    fun setAzureSpeechApiKey(key: String) = viewModelScope.launch { userPreferences.setAzureSpeechApiKey(key) }
    fun setDeepgramApiKey(key: String) = viewModelScope.launch { userPreferences.setDeepgramApiKey(key) }
    fun setAudioProvider(provider: String) = viewModelScope.launch { userPreferences.setAudioProvider(provider) }
    fun setDeviceTtsEngine(packageName: String?) = viewModelScope.launch { userPreferences.setDeviceTtsEngine(packageName) }

    val activeAiProvider: StateFlow<AiServiceProvider> = userPreferences.activeAiProvider
        .map { try { AiServiceProvider.valueOf(it) } catch (_: Exception) { AiServiceProvider.GEMINI } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AiServiceProvider.GEMINI)

    fun setActiveAiProvider(provider: AiServiceProvider) = viewModelScope.launch { 
        userPreferences.setActiveAiProvider(provider.name)
        refreshAvailableModels()
    }

    // Dynamic Model Selection per provider
    val selectedGeminiModel = userPreferences.geminiModel.stateIn(viewModelScope, SharingStarted.Eagerly, "models/gemini-3-flash")
    val selectedGroqModel = userPreferences.groqModel.stateIn(viewModelScope, SharingStarted.Eagerly, "llama-3.1-70b-versatile")
    val selectedOpenAiModel = userPreferences.openaiModel.stateIn(viewModelScope, SharingStarted.Eagerly, "gpt-4o-mini")
    val selectedDeepseekModel = userPreferences.deepseekModel.stateIn(viewModelScope, SharingStarted.Eagerly, "deepseek-chat")
    val selectedOpenrouterModel = userPreferences.openrouterModel.stateIn(viewModelScope, SharingStarted.Eagerly, "google/gemini-flash-1.5")

    fun setSelectedModel(model: String) = viewModelScope.launch {
        when (activeAiProvider.value) {
            AiServiceProvider.GEMINI -> userPreferences.setGeminiModel(model)
            AiServiceProvider.GROQ -> userPreferences.setGroqModel(model)
            AiServiceProvider.OPENAI -> userPreferences.setOpenAiModel(model)
            AiServiceProvider.DEEPSEEK -> userPreferences.setDeepseekModel(model)
            AiServiceProvider.OPENROUTER -> userPreferences.setOpenrouterModel(model)
        }
    }

    val currentSelectedModel: StateFlow<String> = combine(
        activeAiProvider,
        selectedGeminiModel,
        selectedGroqModel,
        selectedOpenAiModel,
        combine(selectedDeepseekModel, selectedOpenrouterModel) { d, r -> d to r }
    ) { active, gem, groq, openai, extra ->
        val (deep, router) = extra
        when (active) {
            AiServiceProvider.GEMINI -> gem
            AiServiceProvider.GROQ -> groq
            AiServiceProvider.OPENAI -> openai
            AiServiceProvider.DEEPSEEK -> deep
            AiServiceProvider.OPENROUTER -> router
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "models/gemini-3-flash")

    val showLegacyModels: StateFlow<Boolean> = userPreferences.showLegacyModels.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = false)
    fun setShowLegacyModels(show: Boolean) = viewModelScope.launch { 
        userPreferences.setShowLegacyModels(show)
        refreshAvailableModels()
    }

    val autoCloudBackup: StateFlow<Boolean> = userPreferences.autoCloudBackup.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = false)
    fun setAutoCloudBackup(enabled: Boolean) = viewModelScope.launch { userPreferences.setAutoCloudBackup(enabled) }

    val folderViewStyle: StateFlow<FolderViewStyle> = userPreferences.folderViewStyle.stateIn(viewModelScope, SharingStarted.Eagerly, FolderViewStyle.GRID)
    fun setFolderViewStyle(style: FolderViewStyle) = viewModelScope.launch { userPreferences.setFolderViewStyle(style) }

    private val _showChangelog = MutableStateFlow(value = false)
    val showChangelog: StateFlow<Boolean> = _showChangelog
    fun dismissChangelog() = viewModelScope.launch { 
        userPreferences.setLastAppVersion(BuildConfig.VERSION_CODE)
        _showChangelog.value = false 
    }
    fun forceShowChangelog() { _showChangelog.value = true }

    val isOnboardingCompleted: StateFlow<Boolean> = userPreferences.isOnboardingCompleted.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = false)
    fun completeOnboarding() = viewModelScope.launch { userPreferences.setOnboardingCompleted(true) }

    val hasSeenAppTutorial: StateFlow<Boolean> = userPreferences.hasSeenAppTutorial.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    fun setHasSeenAppTutorial(seen: Boolean) = viewModelScope.launch { userPreferences.setHasSeenAppTutorial(seen) }

    val remindersEnabled: StateFlow<Boolean> = userPreferences.remindersEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = true)
    val reminderHour: StateFlow<Int> = userPreferences.reminderHour.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = 9)
    val reminderMinute: StateFlow<Int> = userPreferences.reminderMinute.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = 0)
    val reminderStartHour: StateFlow<Int> = userPreferences.reminderStartHour.stateIn(viewModelScope, SharingStarted.Eagerly, 9)
    val reminderStartMinute: StateFlow<Int> = userPreferences.reminderStartMinute.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val reminderEndHour: StateFlow<Int> = userPreferences.reminderEndHour.stateIn(viewModelScope, SharingStarted.Eagerly, 21)
    val reminderEndMinute: StateFlow<Int> = userPreferences.reminderEndMinute.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val dailyNotificationCount: StateFlow<Int> = userPreferences.dailyNotificationCount.stateIn(viewModelScope, SharingStarted.Eagerly, 5)
    val reminderCourseIds: StateFlow<Set<String>> = userPreferences.reminderCourseIds.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun setRemindersEnabled(enabled: Boolean) = viewModelScope.launch { userPreferences.setRemindersEnabled(enabled) }
    fun setReminderTime(hour: Int, minute: Int) = viewModelScope.launch { userPreferences.setReminderTime(hour, minute) }
    fun setReminderTimeRange(startH: Int, startM: Int, endH: Int, endM: Int) = viewModelScope.launch { userPreferences.setReminderTimeRange(startH, startM, endH, endM) }
    fun setDailyNotificationCount(count: Int) = viewModelScope.launch { userPreferences.setDailyNotificationCount(count) }
    fun setReminderCourseIds(courseIds: Set<String>) = viewModelScope.launch { userPreferences.setReminderCourseIds(courseIds) }

    private val _selectedWordForOverlay = MutableStateFlow<Word?>(null)
    val selectedWordForOverlay: StateFlow<Word?> = _selectedWordForOverlay

    fun loadWordForOverlay(wordId: Long) = viewModelScope.launch {
        val words = repository.getAllWordsRaw()
        val word = words.find { it.id == wordId }
        _selectedWordForOverlay.value = word
    }

    fun clearWordForOverlay() {
        _selectedWordForOverlay.value = null
    }

    val dailyGoal: StateFlow<Int> = userPreferences.dailyGoal.stateIn(viewModelScope, SharingStarted.Eagerly, 10)
    fun setDailyGoal(goal: Int) = viewModelScope.launch { userPreferences.setDailyGoal(goal) }

    val todayProgress: StateFlow<Int> = getHomeDataUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val masteryTarget: StateFlow<Int> = userPreferences.masteryTarget.stateIn(viewModelScope, SharingStarted.Eagerly, 3)
    val srsIntensity: StateFlow<Float> = userPreferences.srsIntensity.stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)
    fun setMasteryTarget(target: Int) = viewModelScope.launch { userPreferences.setMasteryTarget(target) }
    fun setSrsIntensity(intensity: Float) = viewModelScope.launch { userPreferences.setSrsIntensity(intensity) }

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized

    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels: StateFlow<List<String>> = _availableModels

    private val _isRefreshingModels = MutableStateFlow(false)
    val isRefreshingModels: StateFlow<Boolean> = _isRefreshingModels

    fun refreshAvailableModels() {
        val provider = activeAiProvider.value
        val apiKey = when (provider) {
            AiServiceProvider.GEMINI -> geminiApiKey.value
            AiServiceProvider.GROQ -> groqApiKey.value
            AiServiceProvider.OPENAI -> openaiApiKey.value
            AiServiceProvider.DEEPSEEK -> deepseekApiKey.value
            AiServiceProvider.OPENROUTER -> openrouterApiKey.value
        }

        if (apiKey.isNullOrBlank()) {
            _availableModels.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isRefreshingModels.value = true
            try {
                val providerImpl = providerRegistry.getProvider(provider, showLegacyModels.value)
                val filtered = providerImpl.fetchModels(apiKey)
                _availableModels.value = filtered
                
                if (filtered.isNotEmpty() && !filtered.contains(currentSelectedModel.value)) {
                    setSelectedModel(filtered.first())
                }
            } catch (_: Exception) {
                if (_availableModels.value.isEmpty()) {
                    _availableModels.value = listOf(currentSelectedModel.value)
                }
            } finally {
                _isRefreshingModels.value = false
            }
        }
    }

    val soundEnabled: StateFlow<Boolean> = userPreferences.soundEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val hapticEnabled: StateFlow<Boolean> = userPreferences.hapticEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val exampleSentenceSource: StateFlow<String> = userPreferences.exampleSentenceSource.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "WIKTIONARY")
    fun setSoundEnabled(enabled: Boolean) = viewModelScope.launch { userPreferences.setSoundEnabled(enabled) }
    fun setHapticEnabled(enabled: Boolean) = viewModelScope.launch { userPreferences.setHapticEnabled(enabled) }
    fun setExampleSentenceSource(source: String) = viewModelScope.launch { userPreferences.setExampleSentenceSource(source) }

    val appLanguage: StateFlow<String> = userPreferences.appLanguage.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")
    fun setAppLanguage(langCode: String) = viewModelScope.launch { userPreferences.setAppLanguage(langCode) }

    // Native Language derived from App Language
    val nativeLanguage: StateFlow<Language> = appLanguage
        .map { code -> supportedLanguages.find { it.code == code } ?: supportedLanguages.first() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), supportedLanguages.first())

    suspend fun exportData(includeVocabulary: Boolean, includeSettings: Boolean): String {
        var courses: List<Course>? = null
        var words: List<Word>? = null
        var states: List<ReviewState>? = null
        var settings: Map<String, String>? = null

        if (includeVocabulary) {
            courses = repository.allCourses.first()
            words = repository.getAllWordsRaw()
            states = repository.getAllReviewStatesRaw()
        }

        if (includeSettings) {
            settings = mapOf(
                "themeMode" to themeMode.value.name,
                "pexelsKey" to (pexelsApiKey.value ?: ""),
                "geminiKey" to (geminiApiKey.value ?: ""),
                "groqKey" to (groqApiKey.value ?: ""),
                "openaiKey" to (openaiApiKey.value ?: ""),
                "deepseekKey" to (deepseekApiKey.value ?: ""),
                "openrouterKey" to (openrouterApiKey.value ?: ""),
                "activeProvider" to activeAiProvider.value.name,
                "geminiModel" to selectedGeminiModel.value,
                "groqModel" to selectedGroqModel.value,
                "openaiModel" to selectedOpenAiModel.value,
                "deepseekModel" to selectedDeepseekModel.value,
                "openrouterModel" to selectedOpenrouterModel.value,
                "showLegacyModels" to showLegacyModels.value.toString(),
                "remindersEnabled" to remindersEnabled.value.toString(),
                "reminderHour" to reminderHour.value.toString(),
                "reminderMinute" to reminderMinute.value.toString(),
                "dailyNotificationCount" to dailyNotificationCount.value.toString(),
                "soundEnabled" to soundEnabled.value.toString(),
                "hapticEnabled" to hapticEnabled.value.toString(),
                "appLanguage" to appLanguage.value,
                "masteryTarget" to masteryTarget.value.toString(),
                "srsIntensity" to srsIntensity.value.toString(),
                "isOnboardingCompleted" to isOnboardingCompleted.value.toString(),
                "autoCloudBackup" to autoCloudBackup.value.toString(),
            )
        }
        return BackupManager.createBackupJson(courses, words, states, settings)
    }

    fun importData(json: String) {
        viewModelScope.launch {
            val backup = BackupManager.parseBackupJson(json) ?: return@launch
            backup.courses?.let { c ->
                backup.words?.let { w ->
                    backup.reviewStates?.let { s -> repository.importFullData(c, w, s) }
                }
            }
            backup.settings?.let { s ->
                s["themeMode"]?.let { try { setThemeMode(ThemeMode.valueOf(it)) } catch (_: Exception) {} }
                s["pexelsKey"]?.let { setPexelsApiKey(it) }
                s["geminiKey"]?.let { setGeminiApiKey(it) }
                s["groqKey"]?.let { setGroqApiKey(it) }
                s["openaiKey"]?.let { setOpenAiApiKey(it) }
                s["deepseekKey"]?.let { setDeepseekApiKey(it) }
                s["openrouterKey"]?.let { setOpenrouterApiKey(it) }
                s["activeProvider"]?.let { try { setActiveAiProvider(AiServiceProvider.valueOf(it)) } catch (_: Exception) {} }
                s["geminiModel"]?.let { userPreferences.setGeminiModel(it) }
                s["groqModel"]?.let { userPreferences.setGroqModel(it) }
                s["openaiModel"]?.let { userPreferences.setOpenAiModel(it) }
                s["deepseekModel"]?.let { userPreferences.setDeepseekModel(it) }
                s["openrouterModel"]?.let { userPreferences.setOpenrouterModel(it) }
                s["showLegacyModels"]?.let { setShowLegacyModels(it.toBoolean()) }
                s["remindersEnabled"]?.let { setRemindersEnabled(it.toBoolean()) }
                s["reminderHour"]?.let { h -> s["reminderMinute"]?.let { m -> setReminderTime(h.toInt(), m.toInt()) } }
                s["dailyNotificationCount"]?.let { setDailyNotificationCount(it.toInt()) }
                s["soundEnabled"]?.let { setSoundEnabled(it.toBoolean()) }
                s["hapticEnabled"]?.let { setHapticEnabled(it.toBoolean()) }
                s["appLanguage"]?.let { setAppLanguage(it) }
                s["masteryTarget"]?.let { setMasteryTarget(it.toInt()) }
                s["srsIntensity"]?.let { setSrsIntensity(it.toFloat()) }
                s["isOnboardingCompleted"]?.let { userPreferences.setOnboardingCompleted(it.toBoolean()) }
                s["autoCloudBackup"]?.let { setAutoCloudBackup(it.toBoolean()) }
            }
        }
    }

    fun resetAllProgress() = viewModelScope.launch { repository.resetAllProgress() }
    fun wipeAllData() = viewModelScope.launch { repository.wipeAllData(); userPreferences.clearAll() }

    private val _translationResult = MutableStateFlow<RichWordDetails?>(null)
    val translationResult: StateFlow<RichWordDetails?> = _translationResult
    private val _translationState = MutableStateFlow<TranslationState>(TranslationState.Idle)
    val translationState: StateFlow<TranslationState> = _translationState
    
    private val _sentenceTranslation = MutableStateFlow<String?>(null)
    val sentenceTranslation: StateFlow<String?> = _sentenceTranslation

    private val _selectedWordReviewState = MutableStateFlow<ReviewState?>(null)
    val selectedWordReviewState: StateFlow<ReviewState?> = _selectedWordReviewState
    private val _incomingSharedText = MutableStateFlow<String?>(null)
    val incomingSharedText: StateFlow<String?> = _incomingSharedText

    val importedCourses: StateFlow<List<com.language.finebulary.data.model.CourseWithCount>> = repository.allCoursesWithCount
        .map { courses -> courses.filter { !it.course.isPersonal } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personalFolders: StateFlow<List<com.language.finebulary.data.model.CourseWithCount>> = repository.allCoursesWithCount
        .map { courses -> courses.filter { it.course.isPersonal } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun processIncomingWord(text: String) = viewModelScope.launch {
        val detectedLang = detectLanguageUseCase(text) ?: "en"
        val recommendedCourse = getRecommendedCourseUseCase()
        
        // Klasör önerisi varsa doğrudan çeviriye başla ve sonucu bekle
        if (recommendedCourse != null) {
            _incomingSharedText.value = text
            // AI kotası yerine hızlı çeviriyi tetikleyelim
            autoTranslate(text, "en", "tr") // Varsayılan hedef dil
        } else {
            // Klasör yoksa manuel ekleme için diyalog tetiklenebilir
            _incomingSharedText.value = text
        }
    }

    fun handleIncomingText(text: String?) { if (text != null) processIncomingWord(text) }
    fun clearIncomingText() { _incomingSharedText.value = null }

    init {
        viewModelScope.launch {
            // Wait for essential DataStore values to load
            combine(
                userPreferences.themeMode,
                userPreferences.appThemeColor,
                userPreferences.isOnboardingCompleted,
                userPreferences.appLanguage,
                userPreferences.lastAppVersion,
            ) { _, _, onboarding, _, lastVersion -> 
                // Show changelog if app was updated (and not first run)
                if (lastVersion != 0 && (lastVersion < BuildConfig.VERSION_CODE)) {
                    _showChangelog.value = true
                } else if (lastVersion == 0 && onboarding) {
                    // Initialize last version for existing users who just got this update
                    userPreferences.setLastAppVersion(BuildConfig.VERSION_CODE)
                }
                Unit 
            }.first()
            
            // Artificial delay to ensure system settle down
            kotlinx.coroutines.delay(100)
            _isInitialized.value = true
        }

        // Automatically refresh models when API key becomes available
        viewModelScope.launch {
            combine(
                geminiApiKey,
                groqApiKey,
                openaiApiKey,
                deepseekApiKey,
                combine(openrouterApiKey, activeAiProvider) { r, a -> r to a }
            ) { gem, groq, openai, deep, extra ->
                val (router, active) = extra
                when (active) {
                    AiServiceProvider.GEMINI -> gem
                    AiServiceProvider.GROQ -> groq
                    AiServiceProvider.OPENAI -> openai
                    AiServiceProvider.DEEPSEEK -> deep
                    AiServiceProvider.OPENROUTER -> router
                }
            }.collect { key ->
                if (!key.isNullOrBlank()) {
                    refreshAvailableModels()
                }
            }
        }
    }

    fun createCourseWithLangs(name: String, nativeLangCode: String, targetLanguageCode: String, isPersonal: Boolean) {
        viewModelScope.launch {
            repository.addCourseObject(Course(name = name, isPersonal = isPersonal, nativeLanguageCode = nativeLangCode, targetLanguageCode = targetLanguageCode))
        }
    }

    fun getWordsForCourse(courseId: Long) = repository.getWordsForCourse(courseId)
    fun deleteCourse(course: Course) = viewModelScope.launch { repository.deleteCourse(course) }
    fun restartCourse(courseId: Long) = viewModelScope.launch { repository.restartCourse(courseId) }
    fun fetchReviewState(wordId: Long) = viewModelScope.launch { _selectedWordReviewState.value = repository.getReviewState(wordId) }
    fun clearSelectedWord() { _selectedWordReviewState.value = null }
    fun deleteWord(word: Word) = viewModelScope.launch { repository.deleteWord(word); if (_selectedWordReviewState.value?.wordId == word.id) clearSelectedWord() }
    fun deleteWordsBatch(words: List<Word>) = viewModelScope.launch { repository.deleteWords(words) }
    fun addManualWord(courseId: Long, front: String, back: String, definition: String? = null, synonyms: String? = null, antonyms: String? = null, example: String? = null, imageUrl: String? = null, onResult: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val success = repository.addSingleWord(courseId, front, back, definition, synonyms, antonyms, example, imageUrl)
        clearTranslationState()
        onResult(success)
    }

    fun clearTranslationState() {
        _translationResult.value = null
        _translationState.value = TranslationState.Idle
    }

    fun updateWord(word: Word) = viewModelScope.launch { updateWordStatusUseCase(word) }
    fun renameCourse(course: Course, newName: String) = viewModelScope.launch {
        if (newName.isNotBlank()) {
            repository.updateCourse(course.copy(name = newName.trim()))
        }
    }

    private val _aiGenerationState = MutableStateFlow<AiGenerationState>(AiGenerationState.Idle)
    val aiGenerationState: StateFlow<AiGenerationState> = _aiGenerationState

    fun generateCourseWithAi(topic: String, targetLangCode: String, nativeLangCode: String, wordCount: Int = 20, level: String = "B2") {
        val provider = activeAiProvider.value
        val apiKey = when (provider) {
            AiServiceProvider.GEMINI -> geminiApiKey.value
            AiServiceProvider.GROQ -> groqApiKey.value
            AiServiceProvider.OPENAI -> openaiApiKey.value
            AiServiceProvider.DEEPSEEK -> deepseekApiKey.value
            AiServiceProvider.OPENROUTER -> openrouterApiKey.value
        }
        val model = currentSelectedModel.value
        
        if (apiKey.isNullOrBlank()) {
            _aiGenerationState.value = AiGenerationState.Error("NO_KEY")
            return
        }

        viewModelScope.launch {
            _aiGenerationState.value = AiGenerationState.Loading
            try {
                val providerImpl = providerRegistry.getProvider(provider, showLegacyModels.value)
                val result = providerImpl.generateCourse(apiKey, model, topic, targetLangCode, nativeLangCode, wordCount, level)
                
                val (course, words) = result
                // Ensure we don't exceed the requested count if the AI gets over-enthusiastic
                val limitedWords = words.take(wordCount)
                
                val courseId = repository.addCourseObject(course)
                
                // Fetch images for all words in parallel if Pexels key is available
                val pKey = pexelsApiKey.value
                val finalWords = if (!pKey.isNullOrBlank()) {
                    limitedWords.map { word ->
                        async {
                            val imageUrl = imageSearchManager.searchImage(word.front, pKey)
                            word.copy(courseId = courseId, imageUrl = imageUrl)
                        }
                    }.awaitAll()
                } else {
                    limitedWords.map { it.copy(courseId = courseId) }
                }
                
                repository.addWords(finalWords)
                _aiGenerationState.value = AiGenerationState.Success
            } catch (e: Exception) {
                _aiGenerationState.value = AiGenerationState.Error(e.localizedMessage ?: "Unknown Error")
            }
        }
    }

    fun clearAiState() { _aiGenerationState.value = AiGenerationState.Idle }

    private val _ieltsState = MutableStateFlow<IeltsReadingState>(IeltsReadingState.Idle)
    val ieltsState: StateFlow<IeltsReadingState> = _ieltsState
    
    val ieltsHistory: StateFlow<List<com.language.finebulary.data.model.IeltsPassageEntity>> = repository.getAllIeltsPassages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun generateIeltsReading(level: String = "B2", native: String? = null, targetLang: String = "en", topic: String? = null) {
        val provider = activeAiProvider.value
        val apiKey = when (provider) {
            AiServiceProvider.GEMINI -> geminiApiKey.value
            AiServiceProvider.GROQ -> groqApiKey.value
            AiServiceProvider.OPENAI -> openaiApiKey.value
            AiServiceProvider.DEEPSEEK -> deepseekApiKey.value
            AiServiceProvider.OPENROUTER -> openrouterApiKey.value
        }
        val model = currentSelectedModel.value
        val nativeLang = native ?: appLanguage.value

        if (apiKey.isNullOrBlank()) {
            _ieltsState.value = IeltsReadingState.Error("NO_KEY")
            return
        }

        viewModelScope.launch {
            _ieltsState.value = IeltsReadingState.Loading
            try {
                val providerImpl = providerRegistry.getProvider(provider, showLegacyModels.value)
                val topicPrompt = topic ?: "Choose a random topic."
                val passage = providerImpl.generateIeltsReading(apiKey, model, targetLang, nativeLang, level, topicPrompt)
                
                // Save to history
                val gson = com.google.gson.Gson()
                val entity = com.language.finebulary.data.model.IeltsPassageEntity(
                    title = passage.title,
                    passage = passage.passage,
                    translatedPassage = passage.translatedPassage,
                    questionsJson = gson.toJson(passage.questions),
                    keywordsJson = gson.toJson(passage.academicKeywords),
                    level = level,
                    nativeLangCode = nativeLang
                )
                val insertedId = repository.addIeltsPassage(entity)
                
                _ieltsState.value = IeltsReadingState.Success(passage, insertedId)
            } catch (e: Exception) {
                _ieltsState.value = IeltsReadingState.Error(e.message ?: "Unknown Error")
            }
        }
    }

    fun exitIelts() { 
        _ieltsState.value = IeltsReadingState.Idle 
        _sentenceTranslation.value = null
    }
    
    fun loadIeltsPassage(entity: com.language.finebulary.data.model.IeltsPassageEntity) {
        val gson = GsonBuilder().setLenient().create()
        val passage = com.language.finebulary.data.api.IeltsPassage(
            title = entity.title,
            passage = entity.passage,
            translatedPassage = entity.translatedPassage,
            questions = gson.fromJson(entity.questionsJson, object : com.google.gson.reflect.TypeToken<List<com.language.finebulary.data.api.IeltsQuestion>>() {}.type),
            academicKeywords = gson.fromJson(entity.keywordsJson, object : com.google.gson.reflect.TypeToken<List<com.language.finebulary.data.api.AiGeneratedWord>>() {}.type)
        )
        val userAnswers: Map<Int, String>? = entity.userAnswersJson?.let {
            gson.fromJson(it, object : com.google.gson.reflect.TypeToken<Map<Int, String>>() {}.type)
        }
        _ieltsState.value = IeltsReadingState.Success(passage, entity.id, userAnswers)
    }
    
    fun saveIeltsAnswers(passageId: Long, answers: Map<Int, String>) = viewModelScope.launch {
        val entity = repository.getIeltsPassageRaw(passageId)
        if (entity != null) {
            val updated = entity.copy(userAnswersJson = com.google.gson.Gson().toJson(answers))
            repository.updateIeltsPassage(updated)
            
            // Update UI state if current
            val current = _ieltsState.value
            if (current is IeltsReadingState.Success && current.passageId == passageId) {
                _ieltsState.value = current.copy(userAnswers = answers)
            }
        }
    }
    
    fun deleteIeltsPassage(entity: com.language.finebulary.data.model.IeltsPassageEntity) = viewModelScope.launch {
        repository.deleteIeltsPassage(entity)
    }

    fun translateSentence(text: String, targetLangCode: String, nativeLangCode: String) {
        viewModelScope.launch {
            _sentenceTranslation.value = null
            _translationState.value = TranslationState.Loading()
            try {
                val result = TranslationManager.translate(text, targetLangCode, nativeLangCode)
                if (result == null) {
                    _translationState.value = TranslationState.Error("Sentence translation failed.")
                } else {
                    _sentenceTranslation.value = result
                    _translationState.value = TranslationState.Idle
                }
            } catch (e: Exception) {
                _translationState.value = TranslationState.Error("Error: ${e.localizedMessage}")
            }
        }
    }

    fun autoTranslate(text: String, targetLangCode: String, nativeLangCode: String) {
        viewModelScope.launch {
            _translationState.value = TranslationState.Loading(isTranslating = true, isDictionaryLoading = true, isImageLoading = true)
            // Initialize with the original text to prevent UI jumps, other fields null
            _translationResult.value = RichWordDetails(front = text, translation = null, definition = null, synonyms = null, example = null, imageUrl = null)
            
            val pKey = pexelsApiKey.value
            val fetchExSource = userPreferences.exampleSentenceSource.first()
            
            // 1. Translation Job
            launch {
                try {
                    val translation = TranslationManager.translate(text, targetLangCode, nativeLangCode)
                    if (translation == null) {
                        _translationState.value = TranslationState.Error("Translation failed.")
                    } else {
                        _translationResult.value = _translationResult.value?.copy(translation = translation)
                    }
                } catch (e: Exception) {
                    // Ignore for single job
                } finally {
                    val current = _translationState.value
                    if (current is TranslationState.Loading) {
                        _translationState.value = current.copy(isTranslating = false)
                    }
                    checkIfIdle()
                }
            }

            // 2. Dictionary Job (Definition, Example, Synonyms, Base Form)
            val slowWarningJob = launch {
                kotlinx.coroutines.delay(3500)
                val current = _translationState.value
                if (current is TranslationState.Loading && current.isDictionaryLoading) {
                    _translationState.value = current.copy(isSlowWarning = true)
                }
            }

            launch {
                try {
                    val details = if (fetchExSource == "AI") {
                        val provider = activeAiProvider.value
                        val apiKey = when (provider) {
                            AiServiceProvider.GEMINI -> geminiApiKey.value
                            AiServiceProvider.GROQ -> groqApiKey.value
                            AiServiceProvider.OPENAI -> openaiApiKey.value
                            AiServiceProvider.DEEPSEEK -> deepseekApiKey.value
                            AiServiceProvider.OPENROUTER -> openrouterApiKey.value
                        }
                        val model = currentSelectedModel.value
                        if (!apiKey.isNullOrBlank()) {
                            try {
                                providerRegistry.getProvider(provider, showLegacyModels.value).generateWordDetails(apiKey, model, text, targetLangCode, nativeLangCode)
                            } catch (_: Exception) {
                                TranslationManager.getDictionaryDetails(text, targetLangCode, nativeLangCode, "NONE")
                            }
                        } else {
                            TranslationManager.getDictionaryDetails(text, targetLangCode, nativeLangCode, "NONE")
                        }
                    } else {
                        TranslationManager.getDictionaryDetails(text, targetLangCode, nativeLangCode, fetchExSource)
                    }
                    
                    slowWarningJob.cancel()

                    var newResult = _translationResult.value?.copy(
                        definition = details.definition,
                        synonyms = details.synonyms,
                        antonyms = details.antonyms,
                        example = details.example,
                        baseForm = details.baseForm,
                        baseFormTranslation = details.baseFormTranslation
                    )
                    
                    // If dictionary fails, check for spelling suggestions
                    if (details.definition == null && targetLangCode == "en") {
                        TranslationManager.getSpellingSuggestion(text)?.let { suggestion ->
                            newResult = newResult?.copy(suggestedCorrection = suggestion)
                        }
                    }
                    _translationResult.value = newResult
                } catch (e: Exception) {
                    slowWarningJob.cancel()
                } finally {
                    val current = _translationState.value
                    if (current is TranslationState.Loading) {
                        _translationState.value = current.copy(isDictionaryLoading = false)
                    }
                    checkIfIdle()
                }
            }

            // 3. Image Job
            launch {
                try {
                    if (!pKey.isNullOrBlank()) {
                        val imageUrl = imageSearchManager.searchImage(text, pKey)
                        _translationResult.value = _translationResult.value?.copy(imageUrl = imageUrl)
                    }
                } catch (e: Exception) {
                    // Ignore
                } finally {
                    val current = _translationState.value
                    if (current is TranslationState.Loading) {
                        _translationState.value = current.copy(isImageLoading = false)
                    }
                    checkIfIdle()
                }
            }
        }
    }
    
    private fun checkIfIdle() {
        val current = _translationState.value
        if (current is TranslationState.Loading && !current.isTranslating && !current.isDictionaryLoading && !current.isImageLoading) {
            _translationState.value = TranslationState.Idle
        }
    }
}

sealed class TranslationState {
    object Idle : TranslationState()
    data class Loading(
        val isTranslating: Boolean = true,
        val isDictionaryLoading: Boolean = true,
        val isImageLoading: Boolean = true,
        val isSlowWarning: Boolean = false
    ) : TranslationState()
    data class Error(val message: String) : TranslationState()
}

sealed class AiGenerationState {
    object Idle : AiGenerationState()
    object Loading : AiGenerationState()
    object Success : AiGenerationState()
    data class Error(val code: String) : AiGenerationState()
}

sealed class IeltsReadingState {
    object Idle : IeltsReadingState()
    object Loading : IeltsReadingState()
    data class Success(
        val passage: com.language.finebulary.data.api.IeltsPassage,
        val passageId: Long,
        val userAnswers: Map<Int, String>? = null
    ) : IeltsReadingState()
    data class Error(val message: String) : IeltsReadingState()
}
