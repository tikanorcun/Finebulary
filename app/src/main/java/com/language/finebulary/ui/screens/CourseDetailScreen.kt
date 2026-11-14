package com.language.finebulary.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.language.finebulary.R
import com.language.finebulary.data.model.Course
import com.language.finebulary.data.model.Word
import com.language.finebulary.ui.components.EmptyStateView
import com.language.finebulary.ui.viewmodel.HomeViewModel
import com.language.finebulary.ui.viewmodel.QuestionType
import com.language.finebulary.ui.viewmodel.StudyMode
import com.language.finebulary.util.TtsManager
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailsScreen(
    course: Course,
    viewModel: HomeViewModel,
    onReviewClick: (String, String) -> Unit,
    onStartDuel: () -> Unit,
    onAddClick: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    val words by viewModel.getWordsForCourse(course.id).collectAsState(initial = emptyList())
    val context = LocalContext.current
    var selectedWordForDetails by remember { mutableStateOf<Word?>(null) }
    var wordToEdit by remember { mutableStateOf<Word?>(null) }
    var showReviewConfig by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val reviewState by viewModel.selectedWordReviewState.collectAsState()
    
    // Pagination State
    var currentPage by remember { mutableIntStateOf(0) }
    val pageSize = 20

    // Search State
    var searchQuery by remember { mutableStateOf("") }
    
    // Reset page on search
    LaunchedEffect(searchQuery) {
        currentPage = 0
    }

    val allFilteredWords = words.filter { 
        it.front.contains(searchQuery, ignoreCase = true) || 
        it.backsPacked.contains(searchQuery, ignoreCase = true) 
    }
    
    val totalPages = if (allFilteredWords.isEmpty()) 0 else ((allFilteredWords.size + pageSize - 1) / pageSize)
    val startIndex = currentPage * pageSize
    val endIndex = minOf(startIndex + pageSize, allFilteredWords.size)
    val pagedWords = if (allFilteredWords.isNotEmpty()) allFilteredWords.subList(startIndex, endIndex) else emptyList()

    // Selection State
    var selectedWordIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedWordIds.isNotEmpty()

    if (selectedWordForDetails != null) {
        WordDetailsBottomSheet(
            word = selectedWordForDetails!!,
            reviewState = reviewState,
            onEdit = {
                wordToEdit = selectedWordForDetails
                selectedWordForDetails = null
            },
            onDismiss = {
                selectedWordForDetails = null
                viewModel.clearSelectedWord()
            }
        )
    }

    if (wordToEdit != null) {
        EditWordDialog(
            word = wordToEdit!!,
            onDismiss = { wordToEdit = null },
            onConfirm = { updatedWord ->
                viewModel.updateWord(updatedWord)
                wordToEdit = null
            }
        )
    }

    Scaffold(
        topBar = {
            Surface(
                color = if (isSelectionMode) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelectionMode) {
                        IconButton(onClick = { selectedWordIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_selection))
                        }
                        Text(
                            text = stringResource(R.string.selected_count, selectedWordIds.size),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f).padding(start = 12.dp)
                        )
                        IconButton(onClick = {
                            if (selectedWordIds.size == words.size) {
                                selectedWordIds = emptySet()
                            } else {
                                selectedWordIds = words.map { it.id }.toSet()
                            }
                        }) {
                            Icon(
                                imageVector = if (selectedWordIds.size == words.size) Icons.Default.CloseFullscreen else Icons.Default.SelectAll,
                                contentDescription = if (selectedWordIds.size == words.size) stringResource(R.string.deselect_all) else stringResource(R.string.select_all)
                            )
                        }
                        IconButton(onClick = {
                            val wordsToDelete = words.filter { it.id in selectedWordIds }
                            viewModel.deleteWordsBatch(wordsToDelete)
                            selectedWordIds = emptySet()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.go_back))
                        }
                        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = course.name, 
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            ) 
                        }
                        
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.share_course)) },
                                    leadingIcon = { Icon(Icons.Default.Share, null) },
                                    onClick = {
                                        showMoreMenu = false
                                        val json = com.language.finebulary.util.BackupManager.createBackupJson(
                                            courses = listOf(course),
                                            words = words
                                        )
                                        val file = java.io.File(context.cacheDir, "${course.name.replace(" ", "_")}_course.finebulary")
                                        file.writeText(json)
                                        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Kursu Paylaş"))
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.start_duel)) },
                                    leadingIcon = { Icon(Icons.Default.SportsEsports, null) },
                                    onClick = {
                                        showMoreMenu = false
                                        onStartDuel()
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.restart_progress)) },
                                    leadingIcon = { Icon(Icons.Default.Refresh, null) },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.restartCourse(course.id)
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMoreMenu = false
                                        showDeleteConfirm = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                Column(horizontalAlignment = Alignment.End) {
                    if (onAddClick != null) {
                        SmallFloatingActionButton(
                            onClick = onAddClick,
                            modifier = Modifier.padding(bottom = 8.dp),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.quick_add_word))
                        }
                    }
                    if (words.isNotEmpty()) {
                        ExtendedFloatingActionButton(
                            onClick = { showReviewConfig = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                            text = { Text(stringResource(R.string.start_review), fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (totalPages > 1) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { if (currentPage > 0) currentPage-- },
                            enabled = currentPage > 0
                        ) {
                            Icon(Icons.Default.ChevronLeft, null)
                            Text(stringResource(R.string.previous))
                        }

                        Text(
                            text = stringResource(R.string.page_info, currentPage + 1, totalPages),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        TextButton(
                            onClick = { if (currentPage < totalPages - 1) currentPage++ },
                            enabled = currentPage < totalPages - 1
                        ) {
                            Text(stringResource(R.string.next))
                            Icon(Icons.Default.ChevronRight, null)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (words.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(stringResource(R.string.search_words)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, null)
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.small,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            if (pagedWords.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                    EmptyStateView(
                        icon = if (searchQuery.isEmpty()) Icons.Default.Description else Icons.Default.SearchOff,
                        title = if (searchQuery.isEmpty()) stringResource(R.string.no_words) else stringResource(R.string.no_matches),
                        description = if (searchQuery.isEmpty()) "Tap the + button to add your first word." else "Try adjusting your search query."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pagedWords, key = { it.id }) { word ->
                        val isSelected = selectedWordIds.contains(word.id)
                        
                        WordRowItem(
                            word = word,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onToggleSelection = {
                                selectedWordIds = if (isSelected) selectedWordIds - word.id else selectedWordIds + word.id
                            },
                            onClick = {
                                if (isSelectionMode) {
                                    selectedWordIds = if (isSelected) selectedWordIds - word.id else selectedWordIds + word.id
                                } else {
                                    selectedWordForDetails = word
                                    viewModel.fetchReviewState(word.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    
    if (showReviewConfig) {
        LaunchedEffect(Unit) {
            showReviewConfig = false
            onReviewClick(StudyMode.WORD.name, "MULTIPLE_CHOICE,SPELLING,PRONUNCIATION,WRITING,MATCHING")
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.delete_course_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.delete_course_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCourse(course)
                        showDeleteConfirm = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun WordRowItem(
    word: Word,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onToggleSelection() },
                    onTap = { onClick() }
                )
            },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            } else if (!word.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = word.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word.front,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = word.backsPacked.replace("|", ", "),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!word.definition.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = word.definition,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            if (!isSelectionMode) {
                IconButton(
                    onClick = { TtsManager.speak(word.front) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Pronounce",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
