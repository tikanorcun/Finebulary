package com.language.finebulary.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.language.finebulary.data.model.Course
import com.language.finebulary.data.model.Word
import com.language.finebulary.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class DuelQuestion(
    val word: Word,
    val options: List<String>,
    val correctAnswer: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuelScreen(
    course: Course,
    viewModel: HomeViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var words by remember { mutableStateOf<List<Word>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var currentQuestion by remember { mutableStateOf<DuelQuestion?>(null) }

    var score by remember { mutableStateOf(0) }
    var timeLeft by remember { mutableStateOf(60) }
    var isGameOver by remember { mutableStateOf(false) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var isCorrectAnswer by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(course) {
        val courseWords = viewModel.getWordsForCourse(course.id).first()
        val allWords = viewModel.getAllWordsRaw()
        words = courseWords.shuffled()
        isLoading = false
        if (words.isNotEmpty()) {
            loadNextQuestion(words, allWords, 0) { q -> currentQuestion = q }
        }
    }

    LaunchedEffect(isGameOver, isLoading) {
        if (!isLoading && !isGameOver) {
            while (timeLeft > 0 && !isGameOver) {
                delay(1000L)
                timeLeft--
            }
            if (timeLeft <= 0) {
                isGameOver = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.duel_title), fontWeight = FontWeight.Bold) },
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
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (words.size < 4) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Default.Warning, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                    Text(
                        text = "Düello başlatmak için bu klasörde en az 4 kelime olmalıdır.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = onBack) { Text("Geri Dön") }
                }
            } else if (isGameOver) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(text = "🏆 Düello Sona Erdi!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = "Doğru Sayısı", style = MaterialTheme.typography.labelLarge)
                            Text(text = "$score", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(text = "Klasör: ${course.name}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Button(
                        onClick = {
                            val challengeText = context.getString(R.string.challenge_score_msg, score, course.name)
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, challengeText)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Meydan Oku")
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Arkadaşına Meydan Oku (Skoru Paylaş)", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            score = 0
                            timeLeft = 60
                            currentQuestionIndex = 0
                            isGameOver = false
                            scope.launch {
                                val allWords = viewModel.getAllWordsRaw()
                                loadNextQuestion(words, allWords, 0) { q -> currentQuestion = q }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Tekrar Oyna")
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
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "⏱️ ${timeLeft}s",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
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
                            .height(180.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = q.word.front,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
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
                                        if (correct) score++

                                        scope.launch {
                                            delay(600L)
                                            selectedAnswer = null
                                            isCorrectAnswer = null
                                            currentQuestionIndex++
                                            if (currentQuestionIndex < words.size) {
                                                val allWords = viewModel.getAllWordsRaw()
                                                loadNextQuestion(words, allWords, currentQuestionIndex) { cq -> currentQuestion = cq }
                                            } else {
                                                isGameOver = true
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
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

private fun loadNextQuestion(
    words: List<Word>,
    allWords: List<Word>,
    index: Int,
    onReady: (DuelQuestion) -> Unit
) {
    val word = words[index % words.size]
    val correctMeaning = word.backsPacked.split("|").first().trim()
    val wrongOptions = allWords
        .filter { it.id != word.id && !it.backsPacked.isNullOrBlank() }
        .map { it.backsPacked.split("|").first().trim() }
        .shuffled()
        .take(3)

    val options = (wrongOptions + correctMeaning).shuffled()
    onReady(DuelQuestion(word = word, options = options, correctAnswer = correctMeaning))
}
