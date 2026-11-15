package com.language.finebulary.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.language.finebulary.R
import com.language.finebulary.data.model.DuelChallengePacket
import com.language.finebulary.data.model.WordSnapshot
import com.language.finebulary.ui.viewmodel.HomeViewModel
import com.language.finebulary.util.DuelChallengeManager
import com.language.finebulary.util.SoundManager
import com.language.finebulary.util.TtsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Random

enum class ChallengeMode {
    TIME_ATTACK,
    UNTIMED_COOP
}

data class ChallengeQuestion(
    val snapshot: WordSnapshot,
    val options: List<String>,
    val correctAnswer: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuelChallengeScreen(
    packet: DuelChallengePacket,
    viewModel: HomeViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedMode by remember { mutableStateOf<ChallengeMode?>(null) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var questions by remember { mutableStateOf<List<ChallengeQuestion>>(emptyList()) }
    var currentQuestion by remember { mutableStateOf<ChallengeQuestion?>(null) }

    var score by remember { mutableStateOf(0) }
    var timeLeft by remember { mutableStateOf(60) }
    var isFinished by remember { mutableStateOf(false) }

    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var isCorrectAnswer by remember { mutableStateOf<Boolean?>(null) }

    var wrongWords by remember { mutableStateOf<List<WordSnapshot>>(emptyList()) }
    var correctWords by remember { mutableStateOf<List<WordSnapshot>>(emptyList()) }
    var isSavedToLibrary by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        TtsManager.init(context)
        SoundManager.init()
    }

    LaunchedEffect(packet, selectedMode) {
        if (selectedMode != null) {
            val generated = generateQuestionsFromPacket(packet)
            questions = generated
            if (questions.isNotEmpty()) {
                currentQuestion = questions[0]
            }
        }
    }

    LaunchedEffect(isFinished, selectedMode, timeLeft) {
        if (selectedMode == ChallengeMode.TIME_ATTACK && !isFinished && selectedMode != null) {
            while (timeLeft > 0 && !isFinished) {
                delay(1000L)
                timeLeft--
            }
            if (timeLeft <= 0) {
                isFinished = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = packet.courseName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Meydan Okuma: ${packet.creatorName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            if (packet.words.size < 4) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Default.Warning, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                    Text(
                        text = stringResource(R.string.duel_min_words_warning),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = onBack) { Text(stringResource(R.string.go_back)) }
                }
            } else if (selectedMode == null) {
                // Mode selection
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.frozen_mode_info),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "Toplam ${packet.words.size} dondurulmuş kelime paketi hazır.",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    Text(
                        text = "Düello Modunu Seçin",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Card(
                        onClick = { selectedMode = ChallengeMode.TIME_ATTACK },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Timer, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("⚡ Zamanlı Düello (60s)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Süre dolmadan en yüksek doğru sayısına ulaş!", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    Card(
                        onClick = { selectedMode = ChallengeMode.UNTIMED_COOP },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.School, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("🤝 Zamansız Ortak Çalışma", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Stressiz, eğitici ve sesli alıştırma seansı yap.", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            } else if (isFinished) {
                // Summary Screen
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.coop_summary_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = stringResource(R.string.correct_count_label), style = MaterialTheme.typography.labelLarge)
                            Text(text = "$score / ${questions.size}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            val accuracy = if (questions.isNotEmpty()) (score * 100) / questions.size else 0
                            Text(text = "Başarı Oranı: %$accuracy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (wrongWords.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(R.string.both_wrong_words),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                wrongWords.forEach { w ->
                                    Text(
                                        text = "• ${w.front} = ${w.backsPacked.split("|").first()}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val challengeText = "⚔️ Finebulary '${packet.courseName}' düellosunda $score doğru yaptım! Sen de katıl: https://tikanorcun.github.io/Finebulary/duel?data=${DuelChallengeManager.encodePacketToUrlData(packet)}"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, challengeText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.challenge_share_title)))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.challenge_friend_button), fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            if (!isSavedToLibrary) {
                                viewModel.saveDuelChallengeCourse(packet) { success ->
                                    if (success) {
                                        isSavedToLibrary = true
                                        Toast.makeText(context, context.getString(R.string.saved_to_library_success), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        enabled = !isSavedToLibrary,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(if (isSavedToLibrary) Icons.Default.Check else Icons.Default.Download, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isSavedToLibrary) "Kütüphaneye Kaydedildi" else stringResource(R.string.save_to_my_library))
                    }

                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.close))
                    }
                }
            } else if (currentQuestion != null) {
                val q = currentQuestion!!
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedMode == ChallengeMode.TIME_ATTACK) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "⏱️ ${timeLeft}s",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "📖 Soru ${currentQuestionIndex + 1}/${questions.size}",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "⭐ Puan: $score",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = q.snapshot.front,
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center
                                    )
                                    IconButton(onClick = { TtsManager.speak(q.snapshot.front) }) {
                                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen")
                                    }
                                }

                                if (!q.snapshot.example.isNullOrBlank()) {
                                    Text(
                                        text = "\"${q.snapshot.example}\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        q.options.forEach { option ->
                            val isSelected = selectedAnswer == option
                            val buttonColor = when {
                                selectedAnswer == null -> ButtonDefaults.buttonColors()
                                option == q.correctAnswer -> ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                isSelected -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                else -> ButtonDefaults.buttonColors()
                            }

                            Button(
                                onClick = {
                                    if (selectedAnswer == null) {
                                        selectedAnswer = option
                                        val correct = option == q.correctAnswer
                                        isCorrectAnswer = correct
                                        if (correct) {
                                            score++
                                            correctWords = correctWords + q.snapshot
                                            SoundManager.playSuccess()
                                        } else {
                                            wrongWords = wrongWords + q.snapshot
                                            SoundManager.playError()
                                        }

                                        scope.launch {
                                            delay(600L)
                                            selectedAnswer = null
                                            isCorrectAnswer = null
                                            currentQuestionIndex++
                                            if (currentQuestionIndex < questions.size) {
                                                currentQuestion = questions[currentQuestionIndex]
                                            } else {
                                                isFinished = true
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = MaterialTheme.shapes.medium,
                                colors = buttonColor
                            ) {
                                Text(text = option, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun generateQuestionsFromPacket(packet: DuelChallengePacket): List<ChallengeQuestion> {
    val random = Random(packet.seed)
    val words = packet.words.shuffled(random)
    val allMeanings = words.map { it.backsPacked.split("|").first().trim() }

    return words.map { word ->
        val correct = word.backsPacked.split("|").first().trim()
        val wrongOptions = allMeanings
            .filter { it != correct }
            .shuffled(random)
            .take(3)
        val options = (wrongOptions + correct).shuffled(random)
        ChallengeQuestion(
            snapshot = word,
            options = options,
            correctAnswer = correct
        )
    }
}
