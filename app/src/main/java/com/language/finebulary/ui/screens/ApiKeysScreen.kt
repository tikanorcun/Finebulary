package com.language.finebulary.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.language.finebulary.R
import com.language.finebulary.ui.viewmodel.HomeViewModel
import com.language.finebulary.util.AiServiceProvider
import com.language.finebulary.util.DeviceTtsEngines
import com.language.finebulary.util.TtsManager

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ApiKeysScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val pexelsKey by viewModel.pexelsApiKey.collectAsState()
    
    val activeProvider by viewModel.activeAiProvider.collectAsState()
    val geminiKey by viewModel.geminiApiKey.collectAsState()
    val groqKey by viewModel.groqApiKey.collectAsState()
    val openaiKey by viewModel.openaiApiKey.collectAsState()
    val deepseekKey by viewModel.deepseekApiKey.collectAsState()
    val openrouterKey by viewModel.openrouterApiKey.collectAsState()

    val availableModels by viewModel.availableModels.collectAsState()
    val selectedModel by viewModel.currentSelectedModel.collectAsState()
    val isRefreshing by viewModel.isRefreshingModels.collectAsState()

    val keysSavedMsg = stringResource(R.string.keys_saved)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.connectivity_ai), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Pexels Section
            ApiKeySection(
                title = "Pexels API",
                description = stringResource(R.string.visual_learning_desc),
                currentKey = pexelsKey ?: "",
                onSave = { viewModel.setPexelsApiKey(it); Toast.makeText(context, keysSavedMsg, Toast.LENGTH_SHORT).show() },
                getApiKeyUrl = "https://www.pexels.com/api/new/"
            )

            HorizontalDivider()

            // TTS / Audio Providers Section
            val elevenlabsKey by viewModel.elevenlabsApiKey.collectAsState()
            val azureKey by viewModel.azureSpeechApiKey.collectAsState()
            val deepgramKey by viewModel.deepgramApiKey.collectAsState()
            val audioProvider by viewModel.audioProvider.collectAsState()
            var showAudioMenu by remember { mutableStateOf(false) }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.tts_provider_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.tts_provider_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        onClick = { showAudioMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val audioLabel = when (audioProvider) {
                                "OPENAI" -> "OpenAI (TTS-1)"
                                "ELEVENLABS" -> "ElevenLabs"
                                "AZURE" -> "Microsoft Azure Speech"
                                "DEEPGRAM" -> "Deepgram Aura"
                                "STANDARD" -> stringResource(R.string.tts_device_default)
                                else -> audioProvider
                            }
                            Text(
                                text = audioLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }
                    DropdownMenu(expanded = showAudioMenu, onDismissRequest = { showAudioMenu = false }) {
                        listOf("OPENAI", "ELEVENLABS", "AZURE", "DEEPGRAM", "STANDARD").forEach { prov ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        when (prov) {
                                            "OPENAI" -> "OpenAI (TTS-1)"
                                            "ELEVENLABS" -> "ElevenLabs"
                                            "AZURE" -> "Microsoft Azure Speech"
                                            "DEEPGRAM" -> "Deepgram Aura"
                                            "STANDARD" -> stringResource(R.string.tts_device_default)
                                            else -> prov
                                        }
                                    ) 
                                },
                                onClick = { viewModel.setAudioProvider(prov); showAudioMenu = false }
                            )
                        }
                    }
                }

                when (audioProvider) {
                    "OPENAI" -> {
                        ApiKeySection(
                            title = "OpenAI TTS API",
                            description = "OpenAI'ın akıcı tts-1 ses modeli (OpenAI bakiye gerektirir, ücretsiz değildir)",
                            currentKey = openaiKey ?: "",
                            onSave = { viewModel.setOpenAiApiKey(it); Toast.makeText(context, keysSavedMsg, Toast.LENGTH_SHORT).show() },
                            getApiKeyUrl = "https://platform.openai.com/api-keys"
                        )
                    }
                    "ELEVENLABS" -> {
                        ApiKeySection(
                            title = "ElevenLabs API",
                            description = "Dünyanın en gerçekçi ve duygusal yapay zeka sesleri",
                            currentKey = elevenlabsKey ?: "",
                            onSave = { viewModel.setElevenlabsApiKey(it); Toast.makeText(context, keysSavedMsg, Toast.LENGTH_SHORT).show() },
                            getApiKeyUrl = "https://elevenlabs.io/"
                        )
                    }
                    "AZURE" -> {
                        ApiKeySection(
                            title = "Microsoft Azure Speech API",
                            description = "Gelişmiş bulut tabanlı nöral ses servisleri (Ücretsiz katman mevcut)",
                            currentKey = azureKey ?: "",
                            onSave = { viewModel.setAzureSpeechApiKey(it); Toast.makeText(context, keysSavedMsg, Toast.LENGTH_SHORT).show() },
                            getApiKeyUrl = "https://azure.microsoft.com/products/cognitive-services/speech-services"
                        )
                    }
                    "DEEPGRAM" -> {
                        ApiKeySection(
                            title = "Deepgram Aura API",
                            description = "Ultra düşük gecikmeli ve doğal akıcı sesler",
                            currentKey = deepgramKey ?: "",
                            onSave = { viewModel.setDeepgramApiKey(it); Toast.makeText(context, keysSavedMsg, Toast.LENGTH_SHORT).show() },
                            getApiKeyUrl = "https://deepgram.com/"
                        )
                    }
                    "STANDARD" -> {
                        // Dynamically fetch installed device TTS engines
                        val installedEngines = remember { DeviceTtsEngines.getInstalledEngines(context) }
                        var showEngineMenu by remember { mutableStateOf(false) }
                        val deviceEngine by viewModel.deviceTtsEngine.collectAsState()

                        Column(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(stringResource(R.string.device_tts_selection), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.device_tts_selection_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedCard(
                                    onClick = { showEngineMenu = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.small,
                                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = installedEngines.find { it.packageName == deviceEngine }?.label ?: stringResource(R.string.system_default_tts),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                }
                                DropdownMenu(expanded = showEngineMenu, onDismissRequest = { showEngineMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.system_default_tts)) },
                                        onClick = { 
                                            viewModel.setDeviceTtsEngine(null)
                                            TtsManager.init(context, null)
                                            showEngineMenu = false 
                                        }
                                    )
                                    installedEngines.forEach { engine ->
                                        DropdownMenuItem(
                                            text = { Text(engine.label) },
                                            onClick = { 
                                                viewModel.setDeviceTtsEngine(engine.packageName)
                                                TtsManager.init(context, engine.packageName)
                                                showEngineMenu = false 
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            val exampleSentenceSource by viewModel.exampleSentenceSource.collectAsState()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.example_sentence_service), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.example_sentence_service_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

                if (exampleSentenceSource == "WIKTIONARY") {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.wiktionary_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                var showExampleSourceMenu by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        onClick = { showExampleSourceMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val sourceText = when (exampleSentenceSource) {
                                "WIKTIONARY" -> stringResource(R.string.source_wiktionary)
                                "AI" -> stringResource(R.string.source_ai)
                                "NONE" -> stringResource(R.string.source_none)
                                else -> stringResource(R.string.source_wiktionary)
                            }
                            Text(
                                text = sourceText,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = showExampleSourceMenu,
                        onDismissRequest = { showExampleSourceMenu = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.source_wiktionary)) },
                            onClick = {
                                viewModel.setExampleSentenceSource("WIKTIONARY")
                                showExampleSourceMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.source_ai)) },
                            onClick = {
                                viewModel.setExampleSentenceSource("AI")
                                showExampleSourceMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.source_none)) },
                            onClick = {
                                viewModel.setExampleSentenceSource("NONE")
                                showExampleSourceMenu = false
                            }
                        )
                    }
                }
            }

            HorizontalDivider()

            // AI Service Selection
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.primary_ai_service), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                var showAiMenu by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        onClick = { showAiMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (activeProvider) {
                                    AiServiceProvider.GEMINI -> "Google Gemini"
                                    AiServiceProvider.GROQ -> "Groq Cloud"
                                    AiServiceProvider.OPENAI -> "OpenAI"
                                    AiServiceProvider.DEEPSEEK -> "DeepSeek"
                                    AiServiceProvider.OPENROUTER -> "OpenRouter"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }
                    DropdownMenu(expanded = showAiMenu, onDismissRequest = { showAiMenu = false }) {
                        AiServiceProvider.entries.forEach { prov ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        when (prov) {
                                            AiServiceProvider.GEMINI -> "Google Gemini"
                                            AiServiceProvider.GROQ -> "Groq Cloud"
                                            AiServiceProvider.OPENAI -> "OpenAI"
                                            AiServiceProvider.DEEPSEEK -> "DeepSeek"
                                            AiServiceProvider.OPENROUTER -> "OpenRouter"
                                        }
                                    ) 
                                },
                                onClick = { viewModel.setActiveAiProvider(prov); showAiMenu = false }
                            )
                        }
                    }
                }

                when (activeProvider) {
                    AiServiceProvider.GEMINI -> {
                        ApiKeySection(
                            title = "Gemini AI API",
                            description = stringResource(R.string.ai_generate_desc),
                            currentKey = geminiKey ?: "",
                            onSave = { viewModel.setGeminiApiKey(it); Toast.makeText(context, keysSavedMsg, Toast.LENGTH_SHORT).show() },
                            getApiKeyUrl = "https://aistudio.google.com/app/apikey"
                        )
                    }
                    AiServiceProvider.GROQ -> {
                        ApiKeySection(
                            title = "Groq Cloud API",
                            description = "Blazing fast inference. Very high limits for free tier.",
                            currentKey = groqKey ?: "",
                            onSave = { viewModel.setGroqApiKey(it); Toast.makeText(context, keysSavedMsg, Toast.LENGTH_SHORT).show() },
                            getApiKeyUrl = "https://console.groq.com/keys"
                        )
                    }
                    AiServiceProvider.OPENAI -> {
                        ApiKeySection(
                            title = "OpenAI API",
                            description = "The industry standard for quality. Requires prepaid balance.",
                            currentKey = openaiKey ?: "",
                            onSave = { viewModel.setOpenAiApiKey(it); Toast.makeText(context, keysSavedMsg, Toast.LENGTH_SHORT).show() },
                            getApiKeyUrl = "https://platform.openai.com/api-keys"
                        )
                    }
                    AiServiceProvider.DEEPSEEK -> {
                        ApiKeySection(
                            title = "DeepSeek API",
                            description = "Extremely low cost with high performance.",
                            currentKey = deepseekKey ?: "",
                            onSave = { viewModel.setDeepseekApiKey(it); Toast.makeText(context, keysSavedMsg, Toast.LENGTH_SHORT).show() },
                            getApiKeyUrl = "https://platform.deepseek.com/"
                        )
                    }
                    AiServiceProvider.OPENROUTER -> {
                        ApiKeySection(
                            title = "OpenRouter API",
                            description = "Access any model (Claude, GPT-4, Llama) through a single API key. Recommended for flexibility.",
                            currentKey = openrouterKey ?: "",
                            onSave = { viewModel.setOpenrouterApiKey(it); Toast.makeText(context, keysSavedMsg, Toast.LENGTH_SHORT).show() },
                            getApiKeyUrl = "https://openrouter.ai/keys"
                        )
                    }
                }

                // Dynamic Model Selector
                val currentKey = when(activeProvider) {
                    AiServiceProvider.GEMINI -> geminiKey
                    AiServiceProvider.GROQ -> groqKey
                    AiServiceProvider.OPENAI -> openaiKey
                    AiServiceProvider.DEEPSEEK -> deepseekKey
                    AiServiceProvider.OPENROUTER -> openrouterKey
                }

                if (!currentKey.isNullOrBlank()) {
                    Text("Model Selection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    var showModelMenu by remember { mutableStateOf(false) }

                    Box {
                        OutlinedTextField(
                            value = selectedModel.removePrefix("models/"),
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth().clickable { if (availableModels.isNotEmpty()) showModelMenu = true },
                            enabled = false,
                            readOnly = true,
                            label = { Text("Model for ${activeProvider.name}") },
                            trailingIcon = {
                                if (isRefreshing) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                else Icon(Icons.Default.ArrowDropDown, null)
                            },
                            shape = MaterialTheme.shapes.small
                        )
                        DropdownMenu(
                            expanded = showModelMenu,
                            onDismissRequest = { showModelMenu = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            availableModels.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model.removePrefix("models/")) },
                                    onClick = {
                                        viewModel.setSelectedModel(model)
                                        showModelMenu = false
                                    }
                                )
                            }
                        }
                    }

                    if (activeProvider == AiServiceProvider.GEMINI) {
                        val showLegacy by viewModel.showLegacyModels.collectAsState()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.show_legacy_models), style = MaterialTheme.typography.bodyLarge)
                                Text(stringResource(R.string.legacy_models_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }
                            Switch(checked = showLegacy, onCheckedChange = { viewModel.setShowLegacyModels(it) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.AiProviderChip(
    provider: AiServiceProvider,
    isSelected: Boolean,
    onClick: (AiServiceProvider) -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = { onClick(provider) },
        label = { Text(provider.name.lowercase().replaceFirstChar { it.uppercase() }) },
        modifier = Modifier.weight(1f)
    )
}

@Composable
fun ApiKeySection(
    title: String,
    description: String,
    currentKey: String,
    onSave: (String) -> Unit,
    getApiKeyUrl: String
) {
    val context = LocalContext.current
    var tempKey by remember(currentKey) { mutableStateOf(currentKey) }
    var isVisible by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)

        OutlinedTextField(
            value = tempKey,
            onValueChange = { tempKey = it },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { isVisible = !isVisible }) {
                    Icon(if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, getApiKeyUrl.toUri())) }) {
                Text(stringResource(R.string.get_free_key), fontSize = 12.sp)
            }
            Button(
                onClick = { onSave(tempKey) },
                modifier = Modifier.height(36.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Text(stringResource(R.string.save_key), fontSize = 12.sp)
            }
        }
    }
}
