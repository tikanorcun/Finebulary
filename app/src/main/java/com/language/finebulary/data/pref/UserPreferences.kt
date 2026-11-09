package com.language.finebulary.data.pref

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_settings")

enum class ThemeMode { AUTO, LIGHT, NIGHT }
enum class AppThemeColor { WARM, OCEAN, FOREST, NIGHT_PLUS, SYSTEM }
enum class FolderViewStyle { GRID, LIST }

class UserPreferences(private val context: Context) {
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val APP_THEME_COLOR_KEY = stringPreferencesKey("app_theme_color")
    private val PEXELS_API_KEY = stringPreferencesKey("pexels_api_key")
    private val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
    private val GROQ_API_KEY = stringPreferencesKey("groq_api_key")
    private val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
    private val DEEPSEEK_API_KEY = stringPreferencesKey("deepseek_api_key")
    private val OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key")
    private val ELEVENLABS_API_KEY = stringPreferencesKey("elevenlabs_api_key")
    private val AZURE_SPEECH_API_KEY = stringPreferencesKey("azure_speech_api_key")
    private val DEEPGRAM_API_KEY = stringPreferencesKey("deepgram_api_key")
    private val AUDIO_PROVIDER_KEY = stringPreferencesKey("audio_provider") // OPENAI, ELEVENLABS, AZURE, DEEPGRAM, DEVICE_TTS
    private val DEVICE_TTS_ENGINE_KEY = stringPreferencesKey("device_tts_engine")
    
    private val ACTIVE_AI_PROVIDER = stringPreferencesKey("active_ai_provider")
    
    private val GEMINI_MODEL_KEY = stringPreferencesKey("gemini_model")
    private val GROQ_MODEL_KEY = stringPreferencesKey("groq_model")
    private val OPENAI_MODEL_KEY = stringPreferencesKey("openai_model")
    private val DEEPSEEK_MODEL_KEY = stringPreferencesKey("deepseek_model")
    private val OPENROUTER_MODEL_KEY = stringPreferencesKey("openrouter_model")
    
    private val SHOW_LEGACY_MODELS_KEY = booleanPreferencesKey("show_legacy_models")
    private val IS_ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("is_onboarding_completed")
    private val AUTO_CLOUD_BACKUP_KEY = booleanPreferencesKey("auto_cloud_backup")
    private val LAST_APP_VERSION_KEY = intPreferencesKey("last_app_version")
    private val REMINDERS_ENABLED_KEY = booleanPreferencesKey("reminders_enabled")
    private val REMINDER_HOUR_KEY = intPreferencesKey("reminder_hour")
    private val REMINDER_MINUTE_KEY = intPreferencesKey("reminder_minute")
    private val REMINDER_START_HOUR_KEY = intPreferencesKey("reminder_start_hour")
    private val REMINDER_START_MINUTE_KEY = intPreferencesKey("reminder_start_minute")
    private val REMINDER_END_HOUR_KEY = intPreferencesKey("reminder_end_hour")
    private val REMINDER_END_MINUTE_KEY = intPreferencesKey("reminder_end_minute")
    private val DAILY_NOTIFICATION_COUNT_KEY = intPreferencesKey("daily_notification_count")
    private val REMINDER_COURSE_IDS_KEY = stringSetPreferencesKey("reminder_course_ids")
    private val APP_LANGUAGE_KEY = stringPreferencesKey("app_language")
    private val FOLDER_VIEW_STYLE_KEY = stringPreferencesKey("folder_view_style")
    private val HAS_SEEN_APP_TUTORIAL_KEY = booleanPreferencesKey("has_seen_app_tutorial")
    
    private val SOUND_ENABLED_KEY = booleanPreferencesKey("sound_enabled")
    private val HAPTIC_ENABLED_KEY = booleanPreferencesKey("haptic_enabled")
    private val EXAMPLE_SENTENCE_SOURCE_KEY = stringPreferencesKey("example_sentence_source")
    private val DAILY_GOAL_KEY = intPreferencesKey("daily_goal")
    private val MASTERY_TARGET_KEY = intPreferencesKey("mastery_target")
    private val SRS_INTENSITY_KEY = floatPreferencesKey("srs_intensity")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { p ->
        try { ThemeMode.valueOf(p[THEME_MODE_KEY] ?: ThemeMode.AUTO.name) } catch (e: Exception) { ThemeMode.AUTO }
    }

    val appThemeColor: Flow<AppThemeColor> = context.dataStore.data.map { p ->
        try { AppThemeColor.valueOf(p[APP_THEME_COLOR_KEY] ?: AppThemeColor.WARM.name) } catch (e: Exception) { AppThemeColor.WARM }
    }

    val pexelsApiKey: Flow<String?> = context.dataStore.data.map { it[PEXELS_API_KEY] }
    val geminiApiKey: Flow<String?> = context.dataStore.data.map { it[GEMINI_API_KEY] }
    val groqApiKey: Flow<String?> = context.dataStore.data.map { it[GROQ_API_KEY] }
    val openaiApiKey: Flow<String?> = context.dataStore.data.map { it[OPENAI_API_KEY] }
    val deepseekApiKey: Flow<String?> = context.dataStore.data.map { it[DEEPSEEK_API_KEY] }
    val openrouterApiKey: Flow<String?> = context.dataStore.data.map { it[OPENROUTER_API_KEY] }
    val elevenlabsApiKey: Flow<String?> = context.dataStore.data.map { it[ELEVENLABS_API_KEY] }
    val azureSpeechApiKey: Flow<String?> = context.dataStore.data.map { it[AZURE_SPEECH_API_KEY] }
    val deepgramApiKey: Flow<String?> = context.dataStore.data.map { it[DEEPGRAM_API_KEY] }
    val audioProvider: Flow<String> = context.dataStore.data.map { it[AUDIO_PROVIDER_KEY] ?: "OPENAI" }
    val deviceTtsEngine: Flow<String?> = context.dataStore.data.map { it[DEVICE_TTS_ENGINE_KEY] }

    val activeAiProvider: Flow<String> = context.dataStore.data.map { it[ACTIVE_AI_PROVIDER] ?: "GEMINI" }

    val geminiModel: Flow<String> = context.dataStore.data.map { it[GEMINI_MODEL_KEY] ?: "models/gemini-3-flash" }
    val groqModel: Flow<String> = context.dataStore.data.map { it[GROQ_MODEL_KEY] ?: "llama-3.1-70b-versatile" }
    val openaiModel: Flow<String> = context.dataStore.data.map { it[OPENAI_MODEL_KEY] ?: "gpt-4o-mini" }
    val deepseekModel: Flow<String> = context.dataStore.data.map { it[DEEPSEEK_MODEL_KEY] ?: "deepseek-chat" }
    val openrouterModel: Flow<String> = context.dataStore.data.map { it[OPENROUTER_MODEL_KEY] ?: "google/gemini-flash-1.5" }

    val showLegacyModels: Flow<Boolean> = context.dataStore.data.map { it[SHOW_LEGACY_MODELS_KEY] ?: false }
    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { it[IS_ONBOARDING_COMPLETED_KEY] ?: false }
    val autoCloudBackup: Flow<Boolean> = context.dataStore.data.map { it[AUTO_CLOUD_BACKUP_KEY] ?: false }
    val lastAppVersion: Flow<Int> = context.dataStore.data.map { it[LAST_APP_VERSION_KEY] ?: 0 }
    val remindersEnabled: Flow<Boolean> = context.dataStore.data.map { it[REMINDERS_ENABLED_KEY] ?: true }
    val reminderHour: Flow<Int> = context.dataStore.data.map { it[REMINDER_HOUR_KEY] ?: 9 }
    val reminderMinute: Flow<Int> = context.dataStore.data.map { it[REMINDER_MINUTE_KEY] ?: 0 }
    val reminderStartHour: Flow<Int> = context.dataStore.data.map { it[REMINDER_START_HOUR_KEY] ?: 9 }
    val reminderStartMinute: Flow<Int> = context.dataStore.data.map { it[REMINDER_START_MINUTE_KEY] ?: 0 }
    val reminderEndHour: Flow<Int> = context.dataStore.data.map { it[REMINDER_END_HOUR_KEY] ?: 21 }
    val reminderEndMinute: Flow<Int> = context.dataStore.data.map { it[REMINDER_END_MINUTE_KEY] ?: 0 }
    val dailyNotificationCount: Flow<Int> = context.dataStore.data.map { it[DAILY_NOTIFICATION_COUNT_KEY] ?: 5 }
    val reminderCourseIds: Flow<Set<String>> = context.dataStore.data.map { it[REMINDER_COURSE_IDS_KEY] ?: emptySet() }

    val appLanguage: Flow<String> = context.dataStore.data.map { it[APP_LANGUAGE_KEY] ?: "en" }
    val folderViewStyle: Flow<FolderViewStyle> = context.dataStore.data.map { p ->
        try { FolderViewStyle.valueOf(p[FOLDER_VIEW_STYLE_KEY] ?: FolderViewStyle.GRID.name) } catch (e: Exception) { FolderViewStyle.GRID }
    }
    val hasSeenAppTutorial: Flow<Boolean> = context.dataStore.data.map { it[HAS_SEEN_APP_TUTORIAL_KEY] ?: false }
    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { it[SOUND_ENABLED_KEY] ?: true }
    val hapticEnabled: Flow<Boolean> = context.dataStore.data.map { it[HAPTIC_ENABLED_KEY] ?: true }
    val exampleSentenceSource: Flow<String> = context.dataStore.data.map { it[EXAMPLE_SENTENCE_SOURCE_KEY] ?: "WIKTIONARY" }
    val dailyGoal: Flow<Int> = context.dataStore.data.map { it[DAILY_GOAL_KEY] ?: 10 }
    val masteryTarget: Flow<Int> = context.dataStore.data.map { it[MASTERY_TARGET_KEY] ?: 3 }
    val srsIntensity: Flow<Float> = context.dataStore.data.map { it[SRS_INTENSITY_KEY] ?: 1.0f }

    suspend fun setThemeMode(mode: ThemeMode) { context.dataStore.edit { it[THEME_MODE_KEY] = mode.name } }
    suspend fun setAppThemeColor(color: AppThemeColor) { context.dataStore.edit { it[APP_THEME_COLOR_KEY] = color.name } }
    suspend fun setPexelsApiKey(key: String) { context.dataStore.edit { it[PEXELS_API_KEY] = key } }
    suspend fun setGeminiApiKey(key: String) { context.dataStore.edit { it[GEMINI_API_KEY] = key } }
    suspend fun setGroqApiKey(key: String) { context.dataStore.edit { it[GROQ_API_KEY] = key } }
    suspend fun setOpenAiApiKey(key: String) { context.dataStore.edit { it[OPENAI_API_KEY] = key } }
    suspend fun setDeepseekApiKey(key: String) { context.dataStore.edit { it[DEEPSEEK_API_KEY] = key } }
    suspend fun setOpenrouterApiKey(key: String) { context.dataStore.edit { it[OPENROUTER_API_KEY] = key } }
    suspend fun setElevenlabsApiKey(key: String) { context.dataStore.edit { it[ELEVENLABS_API_KEY] = key } }
    suspend fun setAzureSpeechApiKey(key: String) { context.dataStore.edit { it[AZURE_SPEECH_API_KEY] = key } }
    suspend fun setDeepgramApiKey(key: String) { context.dataStore.edit { it[DEEPGRAM_API_KEY] = key } }
    suspend fun setAudioProvider(provider: String) { context.dataStore.edit { it[AUDIO_PROVIDER_KEY] = provider } }
    suspend fun setDeviceTtsEngine(packageName: String?) {
        if (packageName == null) context.dataStore.edit { it.remove(DEVICE_TTS_ENGINE_KEY) }
        else context.dataStore.edit { it[DEVICE_TTS_ENGINE_KEY] = packageName }
    }

    suspend fun setActiveAiProvider(provider: String) { context.dataStore.edit { it[ACTIVE_AI_PROVIDER] = provider } }

    suspend fun setGeminiModel(model: String) { context.dataStore.edit { it[GEMINI_MODEL_KEY] = model } }
    suspend fun setGroqModel(model: String) { context.dataStore.edit { it[GROQ_MODEL_KEY] = model } }
    suspend fun setOpenAiModel(model: String) { context.dataStore.edit { it[OPENAI_MODEL_KEY] = model } }
    suspend fun setDeepseekModel(model: String) { context.dataStore.edit { it[DEEPSEEK_MODEL_KEY] = model } }
    suspend fun setOpenrouterModel(model: String) { context.dataStore.edit { it[OPENROUTER_MODEL_KEY] = model } }

    suspend fun setShowLegacyModels(show: Boolean) { context.dataStore.edit { it[SHOW_LEGACY_MODELS_KEY] = show } }
    suspend fun setOnboardingCompleted(completed: Boolean) { context.dataStore.edit { it[IS_ONBOARDING_COMPLETED_KEY] = completed } }
    suspend fun setAutoCloudBackup(enabled: Boolean) { context.dataStore.edit { it[AUTO_CLOUD_BACKUP_KEY] = enabled } }
    suspend fun setLastAppVersion(version: Int) { context.dataStore.edit { it[LAST_APP_VERSION_KEY] = version } }
    suspend fun setRemindersEnabled(enabled: Boolean) { context.dataStore.edit { it[REMINDERS_ENABLED_KEY] = enabled } }
    suspend fun setReminderTime(hour: Int, minute: Int) { 
        context.dataStore.edit { 
            it[REMINDER_HOUR_KEY] = hour
            it[REMINDER_MINUTE_KEY] = minute
        } 
    }
    suspend fun setAppLanguage(langCode: String) { context.dataStore.edit { it[APP_LANGUAGE_KEY] = langCode } }
    suspend fun setSoundEnabled(enabled: Boolean) { context.dataStore.edit { it[SOUND_ENABLED_KEY] = enabled } }
    suspend fun setHapticEnabled(enabled: Boolean) { context.dataStore.edit { it[HAPTIC_ENABLED_KEY] = enabled } }
    suspend fun setExampleSentenceSource(source: String) { context.dataStore.edit { it[EXAMPLE_SENTENCE_SOURCE_KEY] = source } }
    suspend fun setDailyGoal(goal: Int) { context.dataStore.edit { it[DAILY_GOAL_KEY] = goal } }
    suspend fun setMasteryTarget(target: Int) { context.dataStore.edit { it[MASTERY_TARGET_KEY] = target } }
    suspend fun setSrsIntensity(intensity: Float) { context.dataStore.edit { it[SRS_INTENSITY_KEY] = intensity } }
    suspend fun setReminderTimeRange(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        context.dataStore.edit { 
            it[REMINDER_START_HOUR_KEY] = startHour
            it[REMINDER_START_MINUTE_KEY] = startMinute
            it[REMINDER_END_HOUR_KEY] = endHour
            it[REMINDER_END_MINUTE_KEY] = endMinute
        }
    }
    suspend fun setDailyNotificationCount(count: Int) {
        context.dataStore.edit { it[DAILY_NOTIFICATION_COUNT_KEY] = count }
    }
    suspend fun setReminderCourseIds(courseIds: Set<String>) {
        context.dataStore.edit { it[REMINDER_COURSE_IDS_KEY] = courseIds }
    }
    suspend fun setFolderViewStyle(style: FolderViewStyle) { context.dataStore.edit { it[FOLDER_VIEW_STYLE_KEY] = style.name } }
    suspend fun setHasSeenAppTutorial(seen: Boolean) { context.dataStore.edit { it[HAS_SEEN_APP_TUTORIAL_KEY] = seen } }
    suspend fun clearAll() { context.dataStore.edit { it.clear() } }
}
