package com.language.finebulary.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import coil.compose.AsyncImage
import com.language.finebulary.R
import com.language.finebulary.data.model.Course
import com.language.finebulary.ui.viewmodel.TranslationState
import com.language.finebulary.util.RichWordDetails
import com.language.finebulary.util.TtsManager
import com.language.finebulary.util.TranslationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AddWordDialog(
    course: Course,
    initialFrontText: String? = null,
    translationState: TranslationState = TranslationState.Idle,
    exampleSentenceSource: String = "WIKTIONARY",
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?, String?, String?, String?, String?) -> Unit,
    onTranslate: (String, String, String) -> Unit,
    translationResult: RichWordDetails? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isMonolingual = course.targetLanguageCode.lowercase() == course.nativeLanguageCode.lowercase()
    
    var front by remember(initialFrontText, course) { mutableStateOf(initialFrontText ?: "") }
    var back by remember(initialFrontText, course) { mutableStateOf("") }
    var definition by remember(initialFrontText, course) { mutableStateOf<String?>(null) }
    var synonyms by remember(initialFrontText, course) { mutableStateOf<String?>(null) }
    var antonyms by remember(initialFrontText, course) { mutableStateOf<String?>(null) }
    var example by remember(initialFrontText, course) { mutableStateOf<String?>(null) }
    var imageUrl by remember(initialFrontText, course) { mutableStateOf<String?>(null) }
    var baseFormInfo by remember(initialFrontText, course) { mutableStateOf<String?>(null) }

    var exampleTranslation by remember { mutableStateOf<String?>(null) }
    var isExampleTranslating by remember { mutableStateOf(false) }

    LaunchedEffect(translationResult) {
        if (translationResult != null) {
            if (translationResult.translation != null) back = translationResult.translation
            definition = translationResult.definition
            synonyms = translationResult.synonyms
            antonyms = translationResult.antonyms
            example = translationResult.example
            imageUrl = translationResult.imageUrl
            
            if (translationResult.baseForm != null && translationResult.baseFormTranslation != null) {
                baseFormInfo = "${translationResult.baseForm} (${translationResult.baseFormTranslation})"
            } else {
                baseFormInfo = null
            }
        }
    }

    val isTranslating = (translationState as? TranslationState.Loading)?.isTranslating == true
    val isDictionaryLoading = (translationState as? TranslationState.Loading)?.isDictionaryLoading == true
    val isImageLoading = (translationState as? TranslationState.Loading)?.isImageLoading == true

    LaunchedEffect(translationState) {
        (translationState as? TranslationState.Error)?.let {
            Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = MaterialTheme.shapes.large,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.quick_add_title), 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${course.name} (${course.targetLanguageCode.uppercase()} ➔ ${course.nativeLanguageCode.uppercase()})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = {
                        front = ""
                        back = ""
                        definition = null
                        synonyms = null
                        antonyms = null
                        example = null
                        imageUrl = null
                        baseFormInfo = null
                        exampleTranslation = null
                    },
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.reset_fields), tint = MaterialTheme.colorScheme.secondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val isSlowWarning = (translationState as? TranslationState.Loading)?.isSlowWarning == true
                if (isSlowWarning) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.slow_response_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                if (isImageLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
                    }
                } else if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                }

                OutlinedTextField(
                    value = front,
                    onValueChange = { front = it },
                    label = { Text(stringResource(R.string.word_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    singleLine = true,
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (front.isNotBlank()) {
                                IconButton(onClick = { TtsManager.speak(front) }) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { TtsManager.speak(front, isSlow = true) }) {
                                    Text("🐢", fontSize = 18.sp)
                                }
                            }
                            IconButton(onClick = {
                                if (front.isNotBlank()) {
                                    onTranslate(front, course.targetLanguageCode, course.nativeLanguageCode)
                                }
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.auto_translate), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    keyboardActions = KeyboardActions(onDone = {
                        if (front.isNotBlank()) {
                            onTranslate(front, course.targetLanguageCode, course.nativeLanguageCode)
                        }
                    })
                )

                if (baseFormInfo != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.base_form_label, baseFormInfo!!),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                if (!isMonolingual) {
                    OutlinedTextField(
                        value = back,
                        onValueChange = { back = it },
                        label = { Text(stringResource(R.string.meaning_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        singleLine = true,
                        trailingIcon = {
                            if (isTranslating) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(end = 12.dp), strokeWidth = 2.dp)
                            }
                        }
                    )
                }

                if (exampleSentenceSource != "NONE") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = example ?: "",
                            onValueChange = { example = it },
                            label = { Text(stringResource(R.string.example_sentence)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            trailingIcon = {
                                if (isExampleTranslating) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(end = 12.dp), strokeWidth = 2.dp)
                                } else {
                                    IconButton(onClick = {
                                        if (!example.isNullOrBlank()) {
                                            isExampleTranslating = true
                                            scope.launch(Dispatchers.IO) {
                                                val res = TranslationManager.translate(example!!, course.targetLanguageCode, course.nativeLanguageCode)
                                                withContext(Dispatchers.Main) {
                                                    exampleTranslation = res
                                                    isExampleTranslating = false
                                                }
                                            }
                                        }
                                    }) {
                                        Icon(Icons.Default.Translate, null, tint = MaterialTheme.colorScheme.tertiary)
                                    }
                                }
                            }
                        )
                        if (exampleTranslation != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "Çeviri: $exampleTranslation",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(8.dp),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
                
                if (isDictionaryLoading && definition.isNullOrBlank() && synonyms.isNullOrBlank() && antonyms.isNullOrBlank()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                } else if (!definition.isNullOrBlank() || !synonyms.isNullOrBlank() || !antonyms.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (!definition.isNullOrBlank()) {
                                Text(stringResource(R.string.definition_label), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(4.dp))
                                Text(text = definition!!, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
                                Spacer(Modifier.height(12.dp))
                            }

                            if (!synonyms.isNullOrBlank()) {
                                Text(stringResource(R.string.synonyms_label), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(4.dp))
                                Text(text = synonyms!!.replace("|", ", "), style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(12.dp))
                            }

                            if (!antonyms.isNullOrBlank()) {
                                Text(stringResource(R.string.antonyms_label), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(4.dp))
                                Text(text = antonyms!!.replace("|", ", "), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        front,
                        back,
                        definition,
                        synonyms,
                        antonyms,
                        example,
                        imageUrl
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
