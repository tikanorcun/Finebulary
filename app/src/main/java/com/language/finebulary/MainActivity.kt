package com.language.finebulary

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.provider.Settings
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.language.finebulary.data.model.Course
import com.language.finebulary.data.pref.ThemeMode
import com.language.finebulary.service.QuickAddOverlayService
import com.language.finebulary.ui.components.getLanguageEmoji
import com.language.finebulary.ui.screens.*
import com.language.finebulary.ui.theme.FinebularyTheme
import com.language.finebulary.ui.viewmodel.HomeViewModel
import com.language.finebulary.ui.viewmodel.ReviewViewModel
import com.language.finebulary.util.SoundManager
import com.language.finebulary.util.TtsManager
import com.language.finebulary.worker.ReminderWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val homeViewModel: HomeViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Keep splash screen until ViewModel is initialized to prevent flickering
        splashScreen.setKeepOnScreenCondition {
            !homeViewModel.isInitialized.value
        }

        lifecycleScope.launch {
            val engine = homeViewModel.deviceTtsEngine.value
            TtsManager.init(applicationContext, engine)
            SoundManager.init()
        }
        
        handleIntent(intent)
        requestNotificationPermission()

        lifecycleScope.launch {
            combine(
                homeViewModel.remindersEnabled,
                homeViewModel.dailyNotificationCount,
                homeViewModel.reminderStartHour,
                homeViewModel.reminderEndHour
            ) { enabled, count, startH, endH -> 
                ReminderConfig(enabled, count, startH, endH)
            }
                .collect { config ->
                    if (config.enabled) {
                        scheduleDailyReminder(config.count, config.startHour, config.endHour)
                    } else {
                        WorkManager.getInstance(applicationContext).cancelUniqueWork("daily_reminder")
                    }
                }
        }
        
        setContent {
            val themeMode by homeViewModel.themeMode.collectAsState()
            val themeColor by homeViewModel.appThemeColor.collectAsState()
            val appLanguage by homeViewModel.appLanguage.collectAsState()
            val isInitialized by homeViewModel.isInitialized.collectAsState()

            // Wait for full initialization before rendering Compose content
            if (!isInitialized) return@setContent

            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.NIGHT -> true
                ThemeMode.AUTO -> isSystemInDarkTheme() || themeColor == com.language.finebulary.data.pref.AppThemeColor.NIGHT_PLUS
            }

            LaunchedEffect(isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    }
                )
            }

            LaunchedEffect(appLanguage) {
                val currentLocales = AppCompatDelegate.getApplicationLocales().toLanguageTags()
                if (appLanguage != currentLocales) {
                    val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(appLanguage)
                    AppCompatDelegate.setApplicationLocales(appLocale)
                }
            }

            FinebularyTheme(themeMode = themeMode, themeColor = themeColor) {
                val navController = rememberNavController()
                
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
    var selectedCourse by remember { mutableStateOf<Course?>(null) }
                    var showAddWordDialog by remember { mutableStateOf(false) }
                    var showFolderPicker by remember { mutableStateOf(false) }

                    val importedCourses by homeViewModel.importedCourses.collectAsState()
                    val personalFolders by homeViewModel.personalFolders.collectAsState()
                    val translationResult by homeViewModel.translationResult.collectAsState()
                    val translationState by homeViewModel.translationState.collectAsState()
                    val incomingText by homeViewModel.incomingSharedText.collectAsState()
                    val isOnboardingCompleted by homeViewModel.isOnboardingCompleted.collectAsState()
                    val showChangelog by homeViewModel.showChangelog.collectAsState()
                    val hasSeenTutorial by homeViewModel.hasSeenAppTutorial.collectAsState()
                    val exampleSentenceSource by homeViewModel.exampleSentenceSource.collectAsState()

                    if (showChangelog) {
                        ChangelogDialog(onDismiss = { homeViewModel.dismissChangelog() })
                    } else if (isOnboardingCompleted && !hasSeenTutorial) {
                        AppTutorialDialog(onFinish = { homeViewModel.setHasSeenAppTutorial(true) })
                    }

                    LaunchedEffect(incomingText) {
                        if (incomingText != null) {
                            showFolderPicker = true
                        }
                    }

                    // Root Container
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        NavHost(
                            navController = navController, 
                            startDestination = if (isOnboardingCompleted) "welcome" else "onboarding",
                            modifier = Modifier.fillMaxSize(),
                            enterTransition = { 
                                scaleIn(initialScale = 0.8f, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)) 
                            },
                            exitTransition = { 
                                scaleOut(targetScale = 1.2f, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400)) 
                            },
                            popEnterTransition = { 
                                scaleIn(initialScale = 1.2f, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)) 
                            },
                            popExitTransition = { 
                                scaleOut(targetScale = 0.8f, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400)) 
                            }
                        ) {
                            composable("onboarding") {
                                OnboardingScreen(
                                    viewModel = homeViewModel,
                                    onFinish = {
                                        homeViewModel.completeOnboarding()
                                        navController.navigate("welcome") {
                                            popUpTo("onboarding") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("welcome") {
                                WelcomeScreen(
                                    viewModel = homeViewModel,
                                    onNavigateToCourses = { 
                                        navController.navigate("home") {
                                            popUpTo("welcome") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onNavigateToMyWords = { 
                                        navController.navigate("my_vocabulary") {
                                            popUpTo("welcome") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onNavigateToIelts = {
                                        homeViewModel.exitIelts()
                                        navController.navigate("ielts_reading")
                                    },
                                    onNavigateToSettings = { 
                                        navController.navigate("settings") 
                                    }
                                )
                            }
                            composable("home") {
                                HomeScreen(
                                    title = stringResource(R.string.courses),
                                    courses = importedCourses,
                                    viewModel = homeViewModel,
                                    isPersonalMode = false,
                                    onCourseClick = { course ->
                                        selectedCourse = course
                                        navController.navigate("details")
                                    },
                                    onNavigateToApiKeys = { navController.navigate("settings_api_keys") }
                                )
                            }
                            composable("my_vocabulary") {
                                HomeScreen(
                                    title = stringResource(R.string.my_words),
                                    courses = personalFolders,
                                    viewModel = homeViewModel,
                                    isPersonalMode = true,
                                    onCourseClick = { course ->
                                        selectedCourse = course
                                        navController.navigate("details")
                                    },
                                    onNavigateToApiKeys = { navController.navigate("settings_api_keys") }
                                )
                            }
                            composable(
                                route = "settings",
                                enterTransition = { 
                                    slideInVertically(initialOffsetY = { it }, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400))
                                },
                                exitTransition = { 
                                    scaleOut(targetScale = 0.95f, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
                                },
                                popEnterTransition = { 
                                    scaleIn(initialScale = 0.95f, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
                                },
                                popExitTransition = { 
                                    slideOutVertically(targetOffsetY = { it }, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
                                }
                            ) {
                                SettingsScreen(
                                    viewModel = homeViewModel,
                                    onNavigateToApiKeys = { navController.navigate("settings_api_keys") },
                                    onNavigateToDangerZone = { navController.navigate("settings_danger_zone") },
                                    onNavigateToAbout = { navController.navigate("about") },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("about") {
                                AboutScreen(
                                    viewModel = homeViewModel,
                                    onBack = { navController.popBackStack() },
                                    onViewChangelog = { homeViewModel.forceShowChangelog() }
                                )
                            }
                            composable("settings_api_keys") {
                                ApiKeysScreen(
                                    viewModel = homeViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("settings_danger_zone") {
                                DangerZoneScreen(
                                    viewModel = homeViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("details") {
                                selectedCourse?.let { course ->
                                    CourseDetailsScreen(
                                        course = course,
                                        viewModel = homeViewModel,
                                        onReviewClick = { mode, methods -> 
                                            navController.navigate("review/${course.id}/$mode/$methods") 
                                        },
                                        onAddClick = { showAddWordDialog = true },
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }
                            composable(
                                route = "review/{courseId}/{studyMode}/{methods}",
                                arguments = listOf(
                                    navArgument("courseId") { type = NavType.LongType },
                                    navArgument("studyMode") { type = NavType.StringType },
                                    navArgument("methods") { type = NavType.StringType }
                                )
                            ) {
                                val reviewViewModel: ReviewViewModel = hiltViewModel()
                                selectedCourse?.let { course ->
                                    ReviewScreen(
                                        course = course,
                                        viewModel = reviewViewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }
                            composable("ielts_reading") {
                                IeltsReadingScreen(
                                    viewModel = homeViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }

                    if (showFolderPicker && incomingText != null) {
                        AlertDialog(
                            onDismissRequest = { 
                                showFolderPicker = false
                                homeViewModel.clearIncomingText()
                            },
                            shape = MaterialTheme.shapes.large,
                            title = { Text(stringResource(R.string.select_folder), fontWeight = FontWeight.Bold) },
                            text = {
                                Column {
                                    val text = incomingText ?: ""
                                    Text(stringResource(R.string.choose_where_add, text), style = MaterialTheme.typography.bodyMedium)
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
                                                    homeViewModel.autoTranslate(incomingText ?: "", item.course.targetLanguageCode, item.course.nativeLanguageCode)
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
                                TextButton(onClick = { 
                                    showFolderPicker = false
                                    homeViewModel.clearIncomingText()
                                }) {
                                    Text(stringResource(R.string.cancel))
                                }
                            }
                        )
                    }

                    if (showAddWordDialog) {
                        val folderToUse = selectedCourse ?: personalFolders.firstOrNull()?.course
                        if (folderToUse != null) {
                            AddWordDialog(
                                course = folderToUse,
                                initialFrontText = incomingText,
                                onDismiss = { 
                                    showAddWordDialog = false
                                    homeViewModel.clearIncomingText()
                                    homeViewModel.clearTranslationState()
                                },
                                onConfirm = { front, back, definition, synonyms, antonyms, example, imageUrl ->
                                    homeViewModel.addManualWord(folderToUse.id, front, back, definition, synonyms, antonyms, example, imageUrl) { success ->
                                        if (!success) {
                                            Toast.makeText(this@MainActivity, "Bu kelime zaten bu klasörde mevcut!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(this@MainActivity, "Kelime eklendi", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    showAddWordDialog = false
                                    homeViewModel.clearIncomingText()
                                },
                                onTranslate = { text, target, native ->
                                    homeViewModel.autoTranslate(text, target, native)
                                },
                                translationResult = translationResult,
                                translationState = translationState,
                                exampleSentenceSource = exampleSentenceSource
                            )
                        }
                    }

                    val selectedWordForOverlay by homeViewModel.selectedWordForOverlay.collectAsState()
                    if (selectedWordForOverlay != null) {
                        val word = selectedWordForOverlay!!
                        var showEditDialog by remember { mutableStateOf(false) }

                        WordDetailsBottomSheet(
                            word = word,
                            reviewState = null,
                            onEdit = { showEditDialog = true },
                            onDismiss = { homeViewModel.clearWordForOverlay() }
                        )

                        if (showEditDialog) {
                            EditWordDialog(
                                word = word,
                                onDismiss = { showEditDialog = false },
                                onConfirm = { updatedWord ->
                                    homeViewModel.updateWord(updatedWord)
                                    showEditDialog = false
                                    homeViewModel.clearWordForOverlay()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun scheduleDailyReminder(dailyCount: Int, startHour: Int, endHour: Int) {
        // If active hours is reversed (e.g. 23 to 6), handle it roughly as 24 hours for simplicity
        val activeHours = if (endHour > startHour) endHour - startHour else 24
        
        // Calculate interval in hours. E.g. 12 active hours / 5 count = 2.4 hours
        // Multiply by 60 to get minutes
        val intervalMinutes = ((activeHours.toFloat() / dailyCount) * 60).toLong()
        val finalInterval = if (intervalMinutes < 15L) 15L else intervalMinutes // WorkManager minimum is 15 minutes

        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(finalInterval, TimeUnit.MINUTES)
            .build()
            
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "daily_reminder",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    data class ReminderConfig(val enabled: Boolean, val count: Int, val startHour: Int, val endHour: Int)

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        TtsManager.shutdown()
        SoundManager.release()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val showOverlay = intent.getBooleanExtra("SHOW_WORD_OVERLAY", false)
        val wordId = intent.getLongExtra("WORD_ID", -1L)
        if (showOverlay && wordId != -1L) {
            homeViewModel.loadWordForOverlay(wordId)
        }

        val text = when (intent.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            else -> null
        }
        if (!text.isNullOrBlank()) {
            val serviceIntent = Intent(this, QuickAddOverlayService::class.java).apply {
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_PROCESS_TEXT, text)
            }
            if (Settings.canDrawOverlays(this)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                finish()
            } else {
                homeViewModel.handleIncomingText(text)
            }
        }
    }
}
