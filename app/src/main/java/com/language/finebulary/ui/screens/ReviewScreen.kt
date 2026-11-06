package com.language.finebulary.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.language.finebulary.R
import com.language.finebulary.data.model.Course
import com.language.finebulary.ui.components.DailyGoalRing
import com.language.finebulary.ui.components.ConfettiView
import com.language.finebulary.ui.components.ReviewFeedbackSheet
import com.language.finebulary.ui.components.WordFullDetailView
import com.language.finebulary.ui.viewmodel.QuestionType
import com.language.finebulary.ui.viewmodel.ReviewStatus
import com.language.finebulary.ui.viewmodel.ReviewViewModel
import com.language.finebulary.ui.viewmodel.StudyMode
import com.language.finebulary.util.SoundManager
import com.language.finebulary.util.SpeechRecognizerManager
import com.language.finebulary.util.TtsManager
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    course: Course,
    viewModel: ReviewViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentWord by viewModel.currentWord.collectAsState()
    val options by viewModel.options.collectAsState()
    val questionType by viewModel.questionType.collectAsState()
    val studyMode by viewModel.studyMode.collectAsState()
    val isFinished by viewModel.isFinished.collectAsState()
    val progress by viewModel.sessionProgress.collectAsState()
    val reviewResult by viewModel.reviewResult.collectAsState()
    val questionKey by viewModel.questionKey.collectAsState()
    
    val matchingWords by viewModel.matchingWords.collectAsState()
    val matchingMeanings by viewModel.matchingMeanings.collectAsState()
    val selectedLeftWord by viewModel.selectedLeftWord.collectAsState()
    val selectedRightMeaning by viewModel.selectedRightMeaning.collectAsState()
    val matchedWordIds by viewModel.matchedWordIds.collectAsState()
    
    // User Settings
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val hapticEnabled by viewModel.hapticEnabled.collectAsState()
    
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")
    val haptic = LocalHapticFeedback.current
    
    var isListening by remember { mutableStateOf(false) }
    var pronunciationAttempts by remember(questionKey) { mutableIntStateOf(0) }
    val speechManager = remember { SpeechRecognizerManager(context) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Shake animation for mistakes
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(reviewResult) {
        if (reviewResult != null) {
            keyboardController?.hide()
            if (reviewResult!!.status == ReviewStatus.CORRECT) {
                if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (soundEnabled) SoundManager.playSuccess()
                if (questionType == QuestionType.PRONUNCIATION) {
                    TtsManager.speak(reviewResult!!.word.front)
                }
            } else if (reviewResult!!.status == ReviewStatus.WRONG) {
                if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (soundEnabled) SoundManager.playError()
                TtsManager.speak(reviewResult!!.word.front)
                
                if (questionType == QuestionType.PRONUNCIATION) {
                    pronunciationAttempts++
                }

                // Shake effect
                shakeOffset.animateTo(15f, spring(stiffness = Spring.StiffnessHigh))
                shakeOffset.animateTo(-15f, spring(stiffness = Spring.StiffnessHigh))
                shakeOffset.animateTo(10f, spring(stiffness = Spring.StiffnessHigh))
                shakeOffset.animateTo(-10f, spring(stiffness = Spring.StiffnessHigh))
                shakeOffset.animateTo(0f, spring(stiffness = Spring.StiffnessHigh))
            } else if (reviewResult!!.status == ReviewStatus.CLOSE) {
                if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (soundEnabled) SoundManager.playError()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isListening = true
            speechManager.startListening(course.targetLanguageCode) { result ->
                isListening = false
                viewModel.submitAnswer(result)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { 
            speechManager.stopListening()
            speechManager.destroy()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = course.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .padding(end = 16.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.go_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isFinished) {
                val todayProgress by viewModel.todayProgress.collectAsState()
                val dailyGoal by viewModel.dailyGoal.collectAsState()
                val isGoalReached = todayProgress >= dailyGoal && dailyGoal > 0

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (isGoalReached) {
                        ConfettiView(modifier = Modifier.fillMaxSize())
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(text = if (isGoalReached) "🏆" else "🎉", fontSize = 64.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (isGoalReached) stringResource(R.string.daily_goal_reached) else stringResource(R.string.session_finished),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        
                        if (isGoalReached) {
                            Text(
                                text = stringResource(R.string.daily_goal_sub, todayProgress),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                        
                        DailyGoalRing(
                            progress = todayProgress,
                            goal = dailyGoal,
                            modifier = Modifier.size(120.dp)
                        )

                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(stringResource(R.string.go_back))
                        }
                    }
                }
            } else {
                currentWord?.let { word ->
                    key(questionKey) {
                        var selectedOption by remember { mutableStateOf<String?>(null) }
                        var isInternalAnswerCorrect by remember { mutableStateOf<Boolean?>(null) }
                        var spellingText by remember { mutableStateOf("") }
                        val drawingPaths = remember { mutableStateListOf<Path>() }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp)
                                .padding(top = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Header Guidance
                            Text(
                                text = when(questionType) {
                                    QuestionType.MULTIPLE_CHOICE -> stringResource(R.string.choose_meaning)
                                    QuestionType.SPELLING -> stringResource(R.string.type_meaning)
                                    QuestionType.PRONUNCIATION -> stringResource(R.string.speak_answer)
                                    QuestionType.WRITING -> stringResource(R.string.draw_characters)
                                    QuestionType.MATCHING -> stringResource(R.string.matching_title)
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            
                            Spacer(Modifier.height(12.dp))

                            // MAIN QUESTION CARD
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 200.dp) // Minimum height instead of full weight
                                    .offset(x = shakeOffset.value.dp), // Shake animation
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize().padding(24.dp)
                                ) {
                                    if (questionType == QuestionType.WRITING) {
                                        // INVERSE PROMPT: Show Native meaning for drawing practice
                                        Text(
                                            text = word.backsPacked.split("|").first(),
                                            style = MaterialTheme.typography.displaySmall,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(R.string.draw_instruction, course.targetLanguageCode.uppercase()),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    } else if (studyMode == StudyMode.WORD || word.example.isNullOrBlank()) {
                                        Text(
                                            text = word.front,
                                            style = if (word.front.length > 10) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayLarge,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    } else {
                                        // ULTRA-ROBUST UNDERLINE Logic
                                        val annotatedSentence = buildAnnotatedString {
                                            val sentence = word.example!!
                                            val target = word.front.trim()
                                            
                                            // 1. Try exact word boundary match with common English suffixes
                                            // Suffixes: s, es, ed, ing, ly, er, est, 's, n't
                                            val suffixes = "(s|es|ed|ing|ly|er|est|'s|n't)?"
                                            val pattern = Regex("(?i)\\b${Regex.escape(target)}$suffixes\\b")
                                            var matches = pattern.findAll(sentence).toList()
                                            
                                            // 2. If target contains spaces (a phrase), look for literal match first
                                            if (matches.isEmpty() && target.contains(" ")) {
                                                matches = Regex("(?i)${Regex.escape(target)}").findAll(sentence).toList()
                                            }
                                            
                                            // 3. Special case for 'y' endings (e.g. study -> studied)
                                            if (matches.isEmpty() && target.endsWith("y", ignoreCase = true) && target.length > 3) {
                                                val root = target.substring(0, target.length - 1)
                                                matches = Regex("(?i)\\b${Regex.escape(root)}(ied|ies|ying)\\b").findAll(sentence).toList()
                                            }
                                            
                                            // 4. Last resort: Simple case-insensitive substring match anywhere
                                            if (matches.isEmpty() && target.length >= 2) {
                                                matches = Regex("(?i)${Regex.escape(target)}").findAll(sentence).toList()
                                            }

                                            // Sort matches by start index to ensure sequential building
                                            val sortedMatches = matches.sortedBy { it.range.first }

                                            var lastIndex = 0
                                            sortedMatches.forEach { match ->
                                                // Avoid overlapping matches or out-of-order processing
                                                if (match.range.first >= lastIndex) {
                                                    append(sentence.substring(lastIndex, match.range.first))
                                                    withStyle(style = SpanStyle(
                                                        color = MaterialTheme.colorScheme.primary, 
                                                        fontWeight = FontWeight.Bold,
                                                        textDecoration = TextDecoration.Underline
                                                    )) {
                                                        append(match.value)
                                                    }
                                                    lastIndex = match.range.last + 1
                                                }
                                            }
                                            if (lastIndex < sentence.length) {
                                                append(sentence.substring(lastIndex))
                                            }
                                        }
                                        
                                        Text(
                                            text = annotatedSentence,
                                            style = MaterialTheme.typography.headlineSmall,
                                            textAlign = TextAlign.Center,
                                            fontStyle = FontStyle.Italic
                                        )
                                    }

                                    if (studyMode == StudyMode.SENTENCE && !word.example.isNullOrBlank()) {
                                        Spacer(Modifier.height(16.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { TtsManager.speak(word.example!!) }) {
                                                Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = MaterialTheme.colorScheme.primary)
                                            }
                                            Text(
                                                text = "Listen Sentence",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    if (questionType == QuestionType.PRONUNCIATION) {
                                        Spacer(Modifier.height(16.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { TtsManager.speak(word.front) }) {
                                                Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = MaterialTheme.colorScheme.primary)
                                            }
                                            IconButton(onClick = { TtsManager.speak(word.front, isSlow = true) }) {
                                                Text("🐢", fontSize = 20.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            // INTERACTION AREA
                            when (questionType) {
                                QuestionType.MULTIPLE_CHOICE -> {
                                    options.forEach { option ->
                                        val isThisOptionSelected = selectedOption == option
                                        val buttonColor by animateColorAsState(
                                            targetValue = when {
                                                isThisOptionSelected && isInternalAnswerCorrect == true -> Color(0xFF4CAF50)
                                                isThisOptionSelected && isInternalAnswerCorrect == false -> Color(0xFFF44336)
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            }, label = "color"
                                        )
                                        
                                        val contentColor = if (isThisOptionSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                                        Surface(
                                            onClick = {
                                                if (selectedOption == null) {
                                                    selectedOption = option
                                                    val correct = word.backsPacked.split("|").first().trim()
                                                    isInternalAnswerCorrect = (option == correct)
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(56.dp),
                                            shape = MaterialTheme.shapes.medium,
                                            color = buttonColor,
                                            enabled = selectedOption == null,
                                            tonalElevation = if (isThisOptionSelected) 0.dp else 2.dp
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = option,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = contentColor,
                                                    fontWeight = if (isThisOptionSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                QuestionType.SPELLING -> {
                                    OutlinedTextField(
                                        value = spellingText,
                                        onValueChange = { spellingText = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text(stringResource(R.string.meaning_label)) },
                                        shape = MaterialTheme.shapes.medium,
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = {
                                            if (spellingText.isNotBlank()) {
                                                viewModel.submitAnswer(spellingText)
                                            }
                                        })
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Button(
                                        onClick = { viewModel.submitAnswer(spellingText) },
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        shape = MaterialTheme.shapes.medium,
                                        enabled = spellingText.isNotBlank()
                                    ) {
                                        Icon(Icons.Default.Check, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.check_spelling))
                                    }
                                }

                                QuestionType.PRONUNCIATION -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        LargeFloatingActionButton(
                                            onClick = { 
                                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                                    isListening = true
                                                    speechManager.startListening(course.targetLanguageCode) { result ->
                                                        isListening = false
                                                        viewModel.submitAnswer(result)
                                                    }
                                                } else {
                                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                }
                                            },
                                            containerColor = if (isListening) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                            shape = MaterialTheme.shapes.extraLarge,
                                            modifier = Modifier.size(100.dp)
                                        ) {
                                            Icon(if (isListening) Icons.Default.Mic else Icons.Default.MicNone, null, modifier = Modifier.size(48.dp))
                                        }
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            text = if (isListening) stringResource(R.string.listening) else stringResource(R.string.tap_to_speak),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Spacer(Modifier.height(8.dp))
                                        TextButton(
                                            onClick = { viewModel.setCantTalkRightNow() }
                                        ) {
                                            Icon(Icons.Default.VolumeOff, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(stringResource(R.string.cant_talk_right_now), color = MaterialTheme.colorScheme.secondary)
                                        }

                                        // Allow skip after 2 failed attempts instead of 3
                                        if (pronunciationAttempts >= 2) {
                                            Spacer(Modifier.height(16.dp))
                                            TextButton(
                                                onClick = { viewModel.submitAnswer(word.front) } // Submit correct answer to skip
                                            ) {
                                                Icon(Icons.Default.SkipNext, null)
                                                Spacer(Modifier.width(8.dp))
                                                Text("Skip and Continue")
                                            }
                                        }
                                    }
                                }

                                QuestionType.WRITING -> {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        WritingCanvas(
                                            modifier = Modifier.fillMaxWidth().height(200.dp),
                                            paths = drawingPaths
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Button(
                                            onClick = { viewModel.submitAnswer(word.front) },
                                            modifier = Modifier.fillMaxWidth().height(56.dp),
                                            shape = MaterialTheme.shapes.medium,
                                            enabled = drawingPaths.isNotEmpty()
                                        ) {
                                            Text(stringResource(R.string.written_it), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                QuestionType.MATCHING -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // Left Column (Words)
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                matchingWords.forEach { w ->
                                                    val isMatched = matchedWordIds.contains(w.id)
                                                    val isSelected = selectedLeftWord == w
                                                    Surface(
                                                        onClick = { if (!isMatched) viewModel.selectLeftWord(w) },
                                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                                        shape = MaterialTheme.shapes.small,
                                                        color = when {
                                                            isMatched -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                                        },
                                                        enabled = !isMatched
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                                                            Text(
                                                                text = w.front,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isMatched) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Right Column (Meanings)
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                matchingMeanings.forEach { meaning ->
                                                    val isSelected = selectedRightMeaning == meaning
                                                    val isMatched = matchingWords.any { w -> matchedWordIds.contains(w.id) && w.backsPacked.split("|").first().trim().equals(meaning, true) }
                                                    Surface(
                                                        onClick = { if (!isMatched && selectedLeftWord != null) viewModel.selectRightMeaning(meaning) },
                                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                                        shape = MaterialTheme.shapes.small,
                                                        color = when {
                                                            isMatched -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                                            isSelected -> MaterialTheme.colorScheme.secondaryContainer
                                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                                        },
                                                        enabled = !isMatched && selectedLeftWord != null
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                                                            Text(
                                                                text = meaning,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isMatched) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(16.dp))

                            TextButton(
                                onClick = { viewModel.passCurrentWord() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.SkipNext, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.pass_word), color = MaterialTheme.colorScheme.secondary)
                            }

                            LaunchedEffect(selectedOption) {
                                if (selectedOption != null && questionType == QuestionType.MULTIPLE_CHOICE) {
                                    delay(if (isInternalAnswerCorrect == true) 600 else 1000) 
                                    viewModel.submitAnswer(selectedOption!!)
                                }
                            }
                        }
                    }
                }
            }

            ReviewFeedbackSheet(
                result = reviewResult,
                onContinue = { viewModel.dismissResult() },
                onRetry = { viewModel.retryQuestion() },
                questionKey = questionKey
            )
        }
    }
}

@Composable
fun WritingCanvas(
    modifier: Modifier = Modifier,
    paths: MutableList<Path>
) {
    var currentPath by remember { mutableStateOf<Path?>(null) }

    Surface(
        modifier = modifier.clip(MaterialTheme.shapes.medium),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Box {
            Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val path = Path().apply { moveTo(offset.x, offset.y) }
                        currentPath = path
                        paths.add(path)
                    },
                    onDrag = { change, _ ->
                        currentPath?.lineTo(change.position.x, change.position.y)
                        // This hack triggers a recompose of the Canvas
                        if (paths.isNotEmpty()) {
                            val last = paths.last()
                            paths.removeAt(paths.size - 1)
                            paths.add(last)
                        }
                    },
                    onDragEnd = { currentPath = null }
                )
            }) {
                paths.forEach { path ->
                    drawPath(
                        path = path,
                        color = Color.Black,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
            
            IconButton(
                onClick = { paths.clear() },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(Icons.Default.Clear, contentDescription = "Clear Canvas", modifier = Modifier.size(20.dp))
            }
        }
    }
}
