package com.language.finebulary.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.language.finebulary.R
import com.language.finebulary.data.model.ReviewState
import com.language.finebulary.data.model.Word
import com.language.finebulary.util.TtsManager
import androidx.compose.material.icons.filled.ArrowDropDown
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WordFullDetailView(
    word: Word,
    reviewState: ReviewState?,
    modifier: Modifier = Modifier,
    showStats: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        if (!word.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = word.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(MaterialTheme.shapes.large),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(16.dp))
        }

        // Header Section with Audio
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word.front, 
                    style = MaterialTheme.typography.displaySmall, 
                    fontWeight = FontWeight.Bold,
                    color = LocalContentColor.current
                )
                Text(
                    text = word.backsPacked.replace("|", ", "),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (LocalContentColor.current == MaterialTheme.colorScheme.onSurface) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                    fontWeight = FontWeight.Bold
                )
            }
            Row {
                IconButton(onClick = { TtsManager.speak(word.front) }) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { TtsManager.speak(word.front, isSlow = true) }) {
                    Text("🐢")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Info Section (Definition, Example, Synonyms)
        if (!word.definition.isNullOrBlank() || !word.example.isNullOrBlank() || !word.synonyms.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), // Subtler background
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (!word.definition.isNullOrBlank()) {
                        SectionHeader(stringResource(R.string.definition_label), LocalContentColor.current.copy(alpha = 0.6f))
                        Text(
                            text = word.definition,
                            style = MaterialTheme.typography.bodyLarge,
                            fontStyle = FontStyle.Italic,
                            color = LocalContentColor.current
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    if (!word.example.isNullOrBlank()) {
                        SectionHeader(stringResource(R.string.example_label), LocalContentColor.current.copy(alpha = 0.6f))
                        Text(
                            text = word.example,
                            style = MaterialTheme.typography.bodyLarge,
                            fontStyle = FontStyle.Italic,
                            color = LocalContentColor.current
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    if (!word.synonyms.isNullOrBlank()) {
                        SectionHeader(stringResource(R.string.synonyms_label), LocalContentColor.current.copy(alpha = 0.6f))
                        Text(
                            text = word.synonyms.replace("|", ", "),
                            style = MaterialTheme.typography.bodyLarge,
                            color = LocalContentColor.current
                        )
                    }

                    if (!word.antonyms.isNullOrBlank()) {
                        if (!word.synonyms.isNullOrBlank()) Spacer(Modifier.height(16.dp))
                        SectionHeader(stringResource(R.string.antonyms_label), LocalContentColor.current.copy(alpha = 0.6f))
                        Text(
                            text = word.antonyms.replace("|", ", "),
                            style = MaterialTheme.typography.bodyLarge,
                            color = LocalContentColor.current
                        )
                    }
                }
            }
        }

        if (showStats) {
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = stringResource(R.string.learning_progress),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    label = stringResource(R.string.total_studied), 
                    value = "${reviewState?.reps ?: 0}", 
                    unit = stringResource(R.string.reps),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                StatCard(
                    label = stringResource(R.string.forgotten), 
                    value = "${reviewState?.lapses ?: 0}", 
                    unit = stringResource(R.string.times),
                    modifier = Modifier.weight(1f),
                    valueColor = if ((reviewState?.lapses ?: 0) > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                val nextDue = reviewState?.let {
                    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                    sdf.format(Date(it.dueDateEpochMs))
                } ?: stringResource(R.string.never)
                
                StatCard(
                    label = stringResource(R.string.next_review), 
                    value = nextDue, 
                    unit = "",
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                StatCard(
                    label = stringResource(R.string.interval), 
                    value = "${reviewState?.intervalDays ?: 0}", 
                    unit = stringResource(R.string.days),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun SectionHeader(text: String, color: Color) {
    Text(
        text = text, 
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = color
    )
    Spacer(Modifier.height(4.dp))
}

@Composable
fun StatCard(
    label: String, 
    value: String, 
    unit: String,
    modifier: Modifier = Modifier,
    valueColor: Color = LocalContentColor.current
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        shape = MaterialTheme.shapes.small
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label, 
                style = MaterialTheme.typography.labelSmall, 
                color = LocalContentColor.current.copy(alpha = 0.6f),
                maxLines = 1
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value, 
                    style = MaterialTheme.typography.titleLarge, // Changed from headlineSmall for better fit
                    fontWeight = FontWeight.Bold,
                    color = valueColor,
                    maxLines = 1
                )
                if (unit.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = unit, 
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalContentColor.current.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LanguagePicker(
    selected: com.language.finebulary.data.model.Language,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSelect: (com.language.finebulary.data.model.Language) -> Unit
) {
    Box {
        OutlinedCard(
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(getLanguageEmoji(selected.code), fontSize = 24.sp)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(selected.nameRes), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, null)
            }
        }
        DropdownMenu(expanded = isExpanded, onDismissRequest = onToggle) {
            com.language.finebulary.data.model.supportedLanguages.forEach { lang ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(getLanguageEmoji(lang.code))
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(lang.nameRes))
                        }
                    },
                    onClick = { onSelect(lang) }
                )
            }
        }
    }
}

fun getLanguageEmoji(code: String): String {
    return when (code.lowercase()) {
        "tr" -> "🇹🇷"
        "en" -> "🇺🇸"
        "es" -> "🇪🇸"
        "fr" -> "🇫🇷"
        "de" -> "🇩🇪"
        "it" -> "🇮🇹"
        "ru" -> "🇷🇺"
        "ja" -> "🇯🇵"
        "ko" -> "🇰🇷"
        "zh" -> "🇨🇳"
        else -> "🏳️"
    }
}
