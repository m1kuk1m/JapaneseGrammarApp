package com.example.japanesegrammarapp.ui.screens

import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.domain.model.LlmConfig
import com.example.japanesegrammarapp.ui.SettingsViewModel
import com.example.japanesegrammarapp.ui.UiEvent
import com.example.japanesegrammarapp.ui.theme.ZenColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel,
    isVisible: Boolean = true,
    onBack: () -> Unit = {
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        }
    }
) {
    val SumiInk = MaterialTheme.colorScheme.onBackground
    val WashiBg = MaterialTheme.colorScheme.background
    val SurfaceColor = MaterialTheme.colorScheme.surface
    val PrimaryColor = MaterialTheme.colorScheme.primary
    val OnPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val ctx = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()
    val activeProvider = uiState.activeProvider
    val providerModels = uiState.providerModels
    val fetchingProvider = uiState.fetchingProvider
    val totalTokensConsumed by viewModel.totalTokensConsumed.collectAsState()
    val tokenUsageByModel by viewModel.tokenUsageByModel.collectAsState()

    var showTokenDialog by remember { mutableStateOf(false) }

    var showPromptEditor by remember { mutableStateOf(false) }
    var selectedPromptKey by remember { mutableStateOf("prompt_translation") }
    var promptText by remember { mutableStateOf("") }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showResetAllConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(showPromptEditor, selectedPromptKey) {
        if (showPromptEditor) {
            promptText = viewModel.getCustomPrompt(selectedPromptKey)
        }
    }

    val providers = uiState.allProviders
    val defaultUrls = LlmConfig.defaultUrls

    var isLocalSettingsInitialized by remember { mutableStateOf(false) }

    var selectedTtsProvider by remember { mutableStateOf("OpenAI") }
    val ttsUrls = remember { mutableStateMapOf<String, String>() }
    val ttsKeys = remember { mutableStateMapOf<String, String>() }
    val ttsModels = remember { mutableStateMapOf<String, String>() }
    val ttsVoices = remember { mutableStateMapOf<String, String>() }
    val ttsRegions = remember { mutableStateMapOf<String, String>() }

    val providerUrls = remember { mutableStateMapOf<String, String>() }
    val providerKeys = remember { mutableStateMapOf<String, String>() }

    fun saveSettings() {
        if (!isLocalSettingsInitialized) return
        viewModel.setTtsProvider(selectedTtsProvider)
        ttsUrls.forEach { (provider, url) ->
            viewModel.setTtsApiUrl(provider, url)
        }
        ttsKeys.forEach { (provider, key) ->
            viewModel.setTtsApiKey(provider, key)
        }
        ttsModels.forEach { (provider, model) ->
            viewModel.setTtsModel(provider, model)
        }
        ttsVoices.forEach { (provider, voice) ->
            viewModel.setTtsVoice(provider, voice)
        }
        ttsRegions.forEach { (provider, region) ->
            viewModel.setTtsRegion(provider, region)
        }

        providerUrls.forEach { (provider, url) ->
            viewModel.setApiUrl(provider, url)
        }
        providerKeys.forEach { (provider, key) ->
            viewModel.setApiKey(provider, key)
        }
    }

    val currentSaveSettings by rememberUpdatedState(newValue = ::saveSettings)

    BackHandler(enabled = isVisible && !showPromptEditor) {
        currentSaveSettings()
        onBack()
    }

    BackHandler(enabled = isVisible && showPromptEditor) {
        showPromptEditor = false
    }

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            currentSaveSettings()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            currentSaveSettings()
        }
    }

    LaunchedEffect(uiState.isSettingsLoaded) {
        if (uiState.isSettingsLoaded && !isLocalSettingsInitialized) {
            providerUrls.clear()
            providerUrls.putAll(uiState.providerUrls)
            providerKeys.clear()
            providerKeys.putAll(uiState.providerKeys)

            ttsUrls.clear()
            ttsUrls.putAll(uiState.ttsUrls)
            ttsKeys.clear()
            ttsKeys.putAll(uiState.ttsKeys)
            ttsModels.clear()
            ttsModels.putAll(uiState.ttsModels)
            ttsVoices.clear()
            ttsVoices.putAll(uiState.ttsVoices)
            ttsRegions.clear()
            ttsRegions.putAll(uiState.ttsRegions)

            selectedTtsProvider = uiState.selectedTtsProvider
            isLocalSettingsInitialized = true
        }
    }


    var expandedProvider by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    var customModelInputs by remember { mutableStateOf(providers.associateWith { "" }) }

    val wallpaperLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    ctx.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val file = java.io.File(ctx.filesDir, "custom_wallpaper.jpg")
                        java.io.FileOutputStream(file).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        val savedUri = Uri.fromFile(file).toString()
                        withContext(Dispatchers.Main) {
                            viewModel.setWallpaperUri(savedUri)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        snackbarHostState.showSnackbar(ctx.getString(R.string.unknown_error))
                    }
                }
            }
        }
    }

    val currentLangLabel = remember {
        val currentLocales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
        val currentLangTag = if (currentLocales.isEmpty) "" else currentLocales.get(0)?.toLanguageTag() ?: ""
        when {
            currentLangTag.startsWith("zh") -> "简体中文"
            currentLangTag.startsWith("ja") -> "日本語"
            currentLangTag.startsWith("en") -> "English"
            else -> "AUTO"
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            if (event is UiEvent.ShowError) {
                snackbarHostState.showSnackbar(
                    message = event.message,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    val logs by com.example.japanesegrammarapp.utils.AppLogger.logs.collectAsState()
    var showLogsDialog by remember { mutableStateOf(false) }

    val apiLogs by com.example.japanesegrammarapp.utils.AppLogger.apiLogs.collectAsState()
    var showApiLogsDialog by remember { mutableStateOf(false) }
    var selectedApiLogDetail by remember { mutableStateOf<com.example.japanesegrammarapp.utils.ApiDebugLog?>(null) }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    if (showLogsDialog) {
        var searchQuery by remember { mutableStateOf("") }
        var selectedLevel by remember { mutableStateOf("ALL") }
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        val context = LocalContext.current

        val filteredLogs = remember(logs, searchQuery, selectedLevel) {
            logs.filter { log ->
                val matchesQuery = searchQuery.isBlank() || log.contains(searchQuery, ignoreCase = true)
                val matchesLevel = when (selectedLevel) {
                    "DEBUG" -> log.contains(" D/")
                    "ERROR" -> log.contains(" E/")
                    else -> true
                }
                matchesQuery && matchesLevel
            }
        }

        AlertDialog(
            onDismissRequest = { showLogsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.app_logs_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = {
                        val uri = com.example.japanesegrammarapp.utils.AppLogger.getLogFileUri(context, false)
                        if (uri != null) {
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, context.getString(R.string.share_logs)))
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share_logs), tint = SumiInk)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.log_search_hint), fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(18.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                }
                            }
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val levels = listOf("ALL", "DEBUG", "ERROR")
                        val levelLabels = mapOf(
                            "ALL" to stringResource(R.string.log_level_all),
                            "DEBUG" to stringResource(R.string.log_level_debug),
                            "ERROR" to stringResource(R.string.log_level_error)
                        )
                        levels.forEach { lvl ->
                            val isSelected = selectedLevel == lvl
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedLevel = lvl },
                                label = { Text(levelLabels[lvl] ?: lvl, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryColor,
                                    selectedLabelColor = OnPrimaryColor
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = {
                                com.example.japanesegrammarapp.utils.AppLogger.clear()
                            }) {
                                Text(stringResource(R.string.clear_logs), color = Color.Red, fontSize = 12.sp)
                            }
                            TextButton(onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(filteredLogs.joinToString("\n")))
                                android.widget.Toast.makeText(context, context.getString(R.string.copy_success_toast), android.widget.Toast.LENGTH_SHORT).show()
                            }) {
                                Text(stringResource(R.string.copy_logs), fontSize = 12.sp)
                            }
                        }
                        
                        Row {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch { listState.animateScrollToItem(0) }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.scroll_to_top))
                            }
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        if (filteredLogs.isNotEmpty()) {
                                            listState.animateScrollToItem(filteredLogs.size - 1)
                                        }
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.scroll_to_bottom))
                            }
                        }
                    }

                    val logSize = remember(logs) { com.example.japanesegrammarapp.utils.AppLogger.getLogFileSize(context) }
                    Text(
                        text = stringResource(R.string.log_size_label, logSize),
                        fontSize = 10.sp,
                        color = SumiInk.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Divider(color = SumiInk.copy(alpha = 0.1f))

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().weight(1f).padding(vertical = 4.dp)
                    ) {
                        items(filteredLogs) { log ->
                            val color = when {
                                log.contains(" E/") -> Color(0xFFC62828)
                                log.contains(" D/") -> SumiInk.copy(alpha = 0.6f)
                                else -> SumiInk
                            }
                            Text(
                                text = log,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 12.sp,
                                color = color,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            Divider(modifier = Modifier.padding(vertical = 2.dp), color = SumiInk.copy(alpha = 0.05f))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLogsDialog = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }

    if (showApiLogsDialog) {
        var searchQuery by remember { mutableStateOf("") }
        var filterStatus by remember { mutableStateOf("ALL") }
        val context = LocalContext.current

        val filteredApiLogs = remember(apiLogs, searchQuery, filterStatus) {
            apiLogs.filter { log ->
                val matchesQuery = searchQuery.isBlank() ||
                        log.provider.contains(searchQuery, ignoreCase = true) ||
                        log.model.contains(searchQuery, ignoreCase = true) ||
                        log.apiTypeLabel.contains(searchQuery, ignoreCase = true) ||
                        log.userPrompt.contains(searchQuery, ignoreCase = true) ||
                        (log.rawResponse?.contains(searchQuery, ignoreCase = true) ?: false)

                val matchesStatus = when (filterStatus) {
                    "SUCCESS" -> log.status == "SUCCESS"
                    "ERROR" -> log.status == "ERROR"
                    else -> true
                }
                matchesQuery && matchesStatus
            }.reversed()
        }

        AlertDialog(
            onDismissRequest = { showApiLogsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.api_debug_logs_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = {
                        val uri = com.example.japanesegrammarapp.utils.AppLogger.getLogFileUri(context, true)
                        if (uri != null) {
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, context.getString(R.string.api_log_share_all)))
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.api_log_share_all), tint = SumiInk)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.log_search_hint), fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(18.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                }
                            }
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val statuses = listOf("ALL", "SUCCESS", "ERROR")
                        val statusLabels = mapOf(
                            "ALL" to stringResource(R.string.api_debug_filter_all),
                            "SUCCESS" to stringResource(R.string.api_debug_filter_success),
                            "ERROR" to stringResource(R.string.api_debug_filter_error)
                        )
                        statuses.forEach { stat ->
                            val isSelected = filterStatus == stat
                            FilterChip(
                                selected = isSelected,
                                onClick = { filterStatus = stat },
                                label = { Text(statusLabels[stat] ?: stat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryColor,
                                    selectedLabelColor = OnPrimaryColor
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { com.example.japanesegrammarapp.utils.AppLogger.clearApiLogs() }) {
                            Text(stringResource(R.string.clear_api_debug_logs), color = Color.Red, fontSize = 12.sp)
                        }
                        TextButton(onClick = {
                            val copyText = filteredApiLogs.joinToString("\n\n") { log ->
                                "[${log.apiTypeLabel}] ${log.provider} - ${log.model}\nStatus: ${log.status}\nPrompt: ${log.userPrompt}\nResponse: ${log.rawResponse ?: ""}\nError: ${log.errorMessage ?: ""}"
                            }
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(copyText))
                            android.widget.Toast.makeText(context, context.getString(R.string.copy_success_toast), android.widget.Toast.LENGTH_SHORT).show()
                        }) {
                            Text(stringResource(R.string.copy_logs), fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Divider(color = SumiInk.copy(alpha = 0.1f))

                    if (filteredApiLogs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().weight(1f).padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.api_debug_empty),
                                fontSize = 13.sp,
                                color = SumiInk.copy(alpha = 0.5f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().weight(1f).padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredApiLogs) { log ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { selectedApiLogDetail = log },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "[${log.apiTypeLabel}] ${log.provider}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SumiInk
                                            )
                                            Text(
                                                text = log.status,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (log.status == "SUCCESS") Color(0xFF2E7D32) else Color(0xFFC62828)
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = log.model,
                                                fontSize = 10.sp,
                                                color = SumiInk.copy(alpha = 0.7f)
                                            )
                                            Text(
                                                text = log.time,
                                                fontSize = 9.sp,
                                                color = SumiInk.copy(alpha = 0.4f)
                                            )
                                        }
                                        Text(
                                            text = stringResource(R.string.api_debug_meta, log.hasImage.toString(), log.consumedTokens, log.inputTokens, log.outputTokens),
                                            fontSize = 9.sp,
                                            color = SumiInk.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showApiLogsDialog = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }

    selectedApiLogDetail?.let { log ->
        val context = LocalContext.current
        val formattedResponse = remember(log.rawResponse) {
            if (log.rawResponse == null) return@remember ""
            try {
                val jsonElement = com.google.gson.JsonParser.parseString(log.rawResponse)
                val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
                gson.toJson(jsonElement)
            } catch (e: Exception) {
                log.rawResponse
            }
        }

        AlertDialog(
            onDismissRequest = { selectedApiLogDetail = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.api_details_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = { selectedApiLogDetail = null }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.api_details_metadata), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SumiInk)
                            Divider(color = SumiInk.copy(alpha = 0.05f))
                            Text(stringResource(R.string.api_log_time, log.time), fontSize = 10.sp, color = SumiInk.copy(alpha = 0.8f))
                            Text(stringResource(R.string.api_log_type, log.apiTypeLabel), fontSize = 10.sp, color = SumiInk.copy(alpha = 0.8f))
                            Text(stringResource(R.string.api_log_provider, log.provider), fontSize = 10.sp, color = SumiInk.copy(alpha = 0.8f))
                            Text(stringResource(R.string.api_log_model, log.model), fontSize = 10.sp, color = SumiInk.copy(alpha = 0.8f))
                            Text(stringResource(R.string.api_log_status, log.status), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (log.status == "SUCCESS") Color(0xFF2E7D32) else Color(0xFFC62828))
                            Text(stringResource(R.string.api_log_tokens, log.consumedTokens, log.inputTokens, log.outputTokens), fontSize = 10.sp, color = SumiInk.copy(alpha = 0.8f))
                            Text(stringResource(R.string.api_log_has_image, log.hasImage), fontSize = 10.sp, color = SumiInk.copy(alpha = 0.8f))
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.api_details_prompts), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SumiInk)
                            TextButton(
                                onClick = {
                                    val logPromptText = "System Prompt:\n${log.systemPromptPreview}\n\nUser Prompt:\n${log.userPrompt}"
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(logPromptText))
                                    android.widget.Toast.makeText(context, context.getString(R.string.copy_success_toast), android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.height(24.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text(stringResource(R.string.api_log_copy_prompt), fontSize = 10.sp)
                            }
                        }
                        Divider(color = SumiInk.copy(alpha = 0.1f))
                        
                        Text(stringResource(R.string.api_debug_system_prompt), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SumiInk.copy(alpha = 0.7f))
                        Box(
                            modifier = Modifier.fillMaxWidth().background(SumiInk.copy(alpha = 0.05f), RoundedCornerShape(4.dp)).padding(6.dp)
                        ) {
                            Text(log.systemPromptPreview, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = SumiInk.copy(alpha = 0.8f))
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stringResource(R.string.api_debug_user_prompt), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SumiInk.copy(alpha = 0.7f))
                        Box(
                            modifier = Modifier.fillMaxWidth().background(SumiInk.copy(alpha = 0.05f), RoundedCornerShape(4.dp)).padding(6.dp)
                        ) {
                            Text(log.userPrompt, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = SumiInk.copy(alpha = 0.8f))
                        }
                    }

                    if (log.status == "SUCCESS") {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.api_details_response), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SumiInk)
                                TextButton(
                                    onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(formattedResponse))
                                        android.widget.Toast.makeText(context, context.getString(R.string.copy_success_toast), android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.height(24.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text(stringResource(R.string.api_log_copy_response), fontSize = 10.sp)
                                }
                            }
                            Divider(color = SumiInk.copy(alpha = 0.1f))
                            Box(
                                modifier = Modifier.fillMaxWidth().background(SumiInk.copy(alpha = 0.05f), RoundedCornerShape(4.dp)).padding(6.dp)
                            ) {
                                Text(formattedResponse, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = SumiInk.copy(alpha = 0.8f))
                            }
                        }
                    }

                    if (log.status == "ERROR") {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.api_details_error_info), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFC62828))
                                TextButton(
                                    onClick = {
                                        val errText = "Error Message:\n${log.errorMessage}\n\nStack Trace:\n${log.stackTrace ?: ""}"
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(errText))
                                        android.widget.Toast.makeText(context, context.getString(R.string.copy_success_toast), android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.height(24.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text(stringResource(R.string.api_log_copy_error), fontSize = 10.sp, color = Color(0xFFC62828))
                                }
                            }
                            Divider(color = Color(0xFFC62828).copy(alpha = 0.2f))
                            
                            Text(stringResource(R.string.api_debug_error), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                            Box(
                                modifier = Modifier.fillMaxWidth().background(Color(0xFFC62828).copy(alpha = 0.05f), RoundedCornerShape(4.dp)).padding(6.dp)
                            ) {
                                Text(log.errorMessage ?: "Unknown error", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFC62828))
                            }
                            
                            if (!log.stackTrace.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(stringResource(R.string.api_debug_stack_trace), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                Box(
                                    modifier = Modifier.fillMaxWidth().background(Color(0xFFC62828).copy(alpha = 0.05f), RoundedCornerShape(4.dp)).padding(6.dp)
                                ) {
                                    Text(log.stackTrace, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFC62828).copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedApiLogDetail = null }) { Text(stringResource(R.string.close)) }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = WashiBg,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold, color = SumiInk) },
                navigationIcon = {
                    IconButton(onClick = {
                        saveSettings()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = SumiInk)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WashiBg,
                    titleContentColor = SumiInk
                )
            )
        }
    ) { padding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Appearance Section
            Spacer(modifier = Modifier.height(8.dp))
            SettingsGroup(title = stringResource(R.string.appearance)) {
                // Theme Mode
                    var themeDropdownExpanded by remember { mutableStateOf(false) }
                    SettingsItem(
                        icon = Icons.Default.BrightnessMedium,
                        title = stringResource(R.string.theme_mode),
                        subtitle = when (uiState.themeMode) {
                            "Light" -> stringResource(R.string.theme_light)
                            "Dark" -> stringResource(R.string.theme_dark)
                            else -> stringResource(R.string.theme_system)
                        },
                        onClick = { themeDropdownExpanded = true },
                        trailingContent = {
                            Box {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = SumiInk.copy(alpha=0.5f))
                                DropdownMenu(
                                    expanded = themeDropdownExpanded,
                                    onDismissRequest = { themeDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.theme_system)) },
                                        onClick = { viewModel.setThemeMode("System"); themeDropdownExpanded = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.theme_light)) },
                                        onClick = { viewModel.setThemeMode("Light"); themeDropdownExpanded = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.theme_dark)) },
                                        onClick = { viewModel.setThemeMode("Dark"); themeDropdownExpanded = false }
                                    )
                                }
                            }
                        }
                    )
                    Divider(color = SumiInk.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Wallpaper
                    SettingsItem(
                        icon = Icons.Default.Wallpaper,
                        title = stringResource(R.string.wallpaper),
                        subtitle = if (uiState.wallpaperUri.isNotBlank()) stringResource(R.string.custom_image_set) else stringResource(R.string.none),
                        onClick = { wallpaperLauncher.launch("image/*") },
                        trailingContent = {
                            if (uiState.wallpaperUri.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = uiState.wallpaperUri,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            viewModel.setWallpaperUri("")
                                            coroutineScope.launch(Dispatchers.IO) {
                                                try {
                                                    val file = java.io.File(ctx.filesDir, "custom_wallpaper.jpg")
                                                    if (file.exists()) {
                                                        file.delete()
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_wallpaper), tint = SumiInk.copy(alpha=0.5f))
                                    }
                                }
                            } else {
                                TextButton(onClick = { wallpaperLauncher.launch("image/*") }) {
                                    Text(stringResource(R.string.pick_wallpaper), color = SumiInk)
                                }
                            }
                        }
                    )
                }

            // General Section
            SettingsGroup(title = stringResource(R.string.general)) {
                // Language Switcher
                    var langDropdownExpanded by remember { mutableStateOf(false) }
                    val displayLangLabel = if (currentLangLabel == "AUTO") stringResource(R.string.language_auto) else currentLangLabel

                    SettingsItem(
                        icon = Icons.Default.Language,
                        title = stringResource(R.string.language),
                        subtitle = displayLangLabel,
                        onClick = { langDropdownExpanded = true },
                        trailingContent = {
                            Box {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = SumiInk.copy(alpha=0.5f))
                                DropdownMenu(expanded = langDropdownExpanded, onDismissRequest = { langDropdownExpanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.language_auto)) },
                                        onClick = {
                                            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.getEmptyLocaleList())
                                            langDropdownExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("English") },
                                        onClick = {
                                            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.forLanguageTags("en"))
                                            langDropdownExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("简体中文") },
                                        onClick = {
                                            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.forLanguageTags("zh"))
                                            langDropdownExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("日本語") },
                                        onClick = {
                                            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.forLanguageTags("ja"))
                                            langDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    )
                    Divider(color = SumiInk.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        icon = Icons.Default.ImageSearch,
                        title = stringResource(R.string.local_ocr),
                        subtitle = stringResource(R.string.local_ocr_desc),
                        trailingContent = {
                            Switch(
                                checked = uiState.useOcr,
                                onCheckedChange = { viewModel.setUseOcr(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = OnPrimaryColor,
                                    checkedTrackColor = PrimaryColor,
                                    uncheckedThumbColor = SumiInk.copy(alpha = 0.4f),
                                    uncheckedTrackColor = SumiInk.copy(alpha = 0.1f)
                                )
                            )
                        }
                    )
                    Divider(color = SumiInk.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        icon = Icons.Default.Launch,
                        title = stringResource(R.string.auto_navigate_result),
                        subtitle = stringResource(R.string.auto_navigate_result_desc),
                        trailingContent = {
                            Switch(
                                checked = uiState.autoNavigateResult,
                                onCheckedChange = { viewModel.setAutoNavigateResult(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = OnPrimaryColor,
                                    checkedTrackColor = PrimaryColor,
                                    uncheckedThumbColor = SumiInk.copy(alpha = 0.4f),
                                    uncheckedTrackColor = SumiInk.copy(alpha = 0.1f)
                                )
                            )
                        }
                    )
                    // Image Tokenizer Mode
                    var tokenizerModeDropdownExpanded by remember { mutableStateOf(false) }
                    val currentModeLabel = when (uiState.imageTokenizerMode) {
                        "repair" -> stringResource(R.string.image_tokenizer_mode_repair)
                        else -> stringResource(R.string.image_tokenizer_mode_faithful)
                    }
                    Divider(color = SumiInk.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsItem(
                        icon = Icons.Default.AutoFixHigh,
                        title = stringResource(R.string.image_tokenizer_mode_title),
                        subtitle = currentModeLabel,
                        onClick = { tokenizerModeDropdownExpanded = true },
                        trailingContent = {
                            Box {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = SumiInk.copy(alpha = 0.5f))
                                DropdownMenu(
                                    expanded = tokenizerModeDropdownExpanded,
                                    onDismissRequest = { tokenizerModeDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(stringResource(R.string.image_tokenizer_mode_faithful), fontWeight = FontWeight.Bold, color = SumiInk)
                                                Text(stringResource(R.string.image_tokenizer_mode_faithful_desc), fontSize = 11.sp, color = SumiInk.copy(alpha = 0.5f))
                                            }
                                        },
                                        onClick = {
                                            viewModel.setImageTokenizerMode("faithful")
                                            tokenizerModeDropdownExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(stringResource(R.string.image_tokenizer_mode_repair), fontWeight = FontWeight.Bold, color = SumiInk)
                                                Text(stringResource(R.string.image_tokenizer_mode_repair_desc), fontSize = 11.sp, color = SumiInk.copy(alpha = 0.5f))
                                            }
                                        },
                                        onClick = {
                                            viewModel.setImageTokenizerMode("repair")
                                            tokenizerModeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    )
                    Divider(color = SumiInk.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        icon = Icons.Default.DataUsage,
                        title = stringResource(R.string.token_usage),
                        subtitle = stringResource(R.string.token_usage_desc),
                        onClick = { showTokenDialog = true },
                        trailingContent = {
                            val formattedTotal = if (totalTokensConsumed >= 1000) String.format(java.util.Locale.US, "%.1fk", totalTokensConsumed / 1000.0) else totalTokensConsumed.toString()
                            Text(text = formattedTotal, fontWeight = FontWeight.Bold, color = SumiInk)
                        }
                    )
                    Divider(color = SumiInk.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        icon = Icons.Default.Code,
                        title = stringResource(R.string.view_api_debug_logs),
                        subtitle = stringResource(R.string.api_debug_logs_title),
                        onClick = { showApiLogsDialog = true },
                        trailingContent = {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = SumiInk.copy(alpha = 0.4f))
                        }
                    )
                    Divider(color = SumiInk.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        icon = Icons.Default.Tune,
                        title = stringResource(R.string.custom_prompts_title),
                        subtitle = stringResource(R.string.custom_prompts_desc),
                        onClick = { showPromptEditor = true },
                        trailingContent = {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = SumiInk.copy(alpha = 0.4f))
                        }
                    )
                }

            // API Priority Section
            SettingsGroup(title = stringResource(R.string.api_config)) {
                var mainProviderExpanded by remember { mutableStateOf(false) }
                    SettingsItem(
                        icon = Icons.Default.Star,
                        title = stringResource(R.string.main_api),
                        subtitle = activeProvider,
                        onClick = { mainProviderExpanded = true },
                        trailingContent = {
                            Box {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = SumiInk.copy(alpha=0.5f))
                                DropdownMenu(expanded = mainProviderExpanded, onDismissRequest = { mainProviderExpanded = false }) {
                                    providers.forEach { provider ->
                                        DropdownMenuItem(
                                            text = { Text(provider) },
                                            onClick = { viewModel.setActiveProvider(provider); mainProviderExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
                    )
                    Divider(color = SumiInk.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))

                    var mainModelExpanded by remember { mutableStateOf(false) }
                    val mainModels = providerModels[activeProvider] ?: emptyList()
                    SettingsItem(
                        icon = Icons.Default.AutoAwesome,
                        title = stringResource(R.string.main_model),
                        subtitle = uiState.activeModel.ifBlank { stringResource(R.string.unselected) },
                        onClick = { mainModelExpanded = true },
                        trailingContent = {
                            Box {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = SumiInk.copy(alpha=0.5f))
                                DropdownMenu(expanded = mainModelExpanded, onDismissRequest = { mainModelExpanded = false }) {
                                    mainModels.forEach { model ->
                                        DropdownMenuItem(
                                            text = { Text(model) },
                                            onClick = { viewModel.setActiveModel(model); mainModelExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
                    )
                    Divider(color = SumiInk.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))

                    var backupProviderExpanded by remember { mutableStateOf(false) }
                    SettingsItem(
                        icon = Icons.Default.Backup,
                        title = stringResource(R.string.backup_api),
                        subtitle = uiState.backupProvider,
                        onClick = { backupProviderExpanded = true },
                        trailingContent = {
                            Box {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = SumiInk.copy(alpha=0.5f))
                                DropdownMenu(expanded = backupProviderExpanded, onDismissRequest = { backupProviderExpanded = false }) {
                                    providers.forEach { provider ->
                                        DropdownMenuItem(
                                            text = { Text(provider) },
                                            onClick = { viewModel.setBackupProvider(provider); backupProviderExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
                    )
                    Divider(color = SumiInk.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))

                    var backupModelExpanded by remember { mutableStateOf(false) }
                    val backupModels = providerModels[uiState.backupProvider] ?: emptyList()
                    SettingsItem(
                        icon = Icons.Default.AutoAwesome,
                        title = stringResource(R.string.backup_model),
                        subtitle = uiState.backupModel.ifBlank { stringResource(R.string.unselected) },
                        onClick = { backupModelExpanded = true },
                        trailingContent = {
                            Box {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = SumiInk.copy(alpha=0.5f))
                                DropdownMenu(expanded = backupModelExpanded, onDismissRequest = { backupModelExpanded = false }) {
                                    backupModels.forEach { model ->
                                        DropdownMenuItem(
                                            text = { Text(model) },
                                            onClick = { viewModel.setBackupModel(model); backupModelExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
                    )
                }

            // Credentials Section
            Text(
                text = stringResource(R.string.credentials),
                style = MaterialTheme.typography.titleMedium,
                color = SumiInk,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            providers.forEach { provider ->
                val isExpanded = expandedProvider == provider

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                    border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedProvider = if (isExpanded) null else provider }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(provider, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = SumiInk)
                            }
                            Icon(if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = SumiInk.copy(alpha=0.5f))
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(180)),
                            exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(160))
                        ) {
                            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                                var localUrl by remember(providerUrls[provider]) { mutableStateOf(providerUrls[provider] ?: defaultUrls[provider] ?: "") }
                                var localKey by remember(providerKeys[provider]) { mutableStateOf(providerKeys[provider] ?: "") }
                                var keyVisible by remember { mutableStateOf(false) }

                                OutlinedTextField(
                                    value = localUrl,
                                    onValueChange = { 
                                        localUrl = it
                                        providerUrls[provider] = it
                                    },
                                    label = { Text(stringResource(R.string.base_url)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = localKey,
                                    onValueChange = { 
                                        localKey = it
                                        providerKeys[provider] = it
                                    },
                                    label = { Text(stringResource(R.string.api_key)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { keyVisible = !keyVisible }) {
                                            Icon(if (keyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                                        }
                                    },
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val isFetchingThis = fetchingProvider == provider
                                Button(
                                    onClick = { viewModel.fetchModels(provider, localUrl, localKey) },
                                    enabled = !isFetchingThis && localKey.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (isFetchingThis) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = OnPrimaryColor, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.fetching))
                                    } else {
                                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.fetch_models))
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                // Custom Model Input
                                val customModelInput = customModelInputs[provider] ?: ""
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = customModelInput,
                                        onValueChange = { newValue -> customModelInputs = customModelInputs.toMutableMap().apply { put(provider, newValue) } },
                                        label = { Text(stringResource(R.string.add_custom_model)) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (customModelInput.isNotBlank()) {
                                                val currentModels = providerModels[provider] ?: emptyList()
                                                if (!currentModels.contains(customModelInput)) {
                                                    viewModel.saveModelsForProvider(provider, currentModels + customModelInput)
                                                }
                                                customModelInputs = customModelInputs.toMutableMap().apply { put(provider, "") }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp), tint = OnPrimaryColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TTS Settings Section
            Text(
                text = stringResource(R.string.tts_settings_title),
                style = MaterialTheme.typography.titleMedium,
                color = SumiInk,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp, top = 16.dp)
            )
            Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                    border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        var ttsProviderExpanded by remember { mutableStateOf(false) }
                        val ttsProviders = listOf("OpenAI", "Google", "Microsoft")

                        // Provider Dropdown
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { ttsProviderExpanded = true }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.tts_provider), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = SumiInk)
                            Box {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(selectedTtsProvider, color = SumiInk.copy(alpha=0.7f))
                                    Icon(Icons.Default.KeyboardArrowDown, null, tint = SumiInk.copy(alpha=0.5f))
                                }
                                DropdownMenu(expanded = ttsProviderExpanded, onDismissRequest = { ttsProviderExpanded = false }) {
                                    ttsProviders.forEach { provider ->
                                        DropdownMenuItem(
                                            text = { Text(provider) },
                                            onClick = {
                                                selectedTtsProvider = provider
                                                ttsProviderExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        var ttsKeyVisible by remember { mutableStateOf(false) }

                        if (selectedTtsProvider == "OpenAI" || selectedTtsProvider == "Google") {
                            OutlinedTextField(
                                value = ttsUrls[selectedTtsProvider] ?: "",
                                onValueChange = { newValue ->
                                    ttsUrls[selectedTtsProvider] = newValue
                                },
                                label = { Text(stringResource(R.string.base_url)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (selectedTtsProvider == "Microsoft") {
                            OutlinedTextField(
                                value = ttsRegions[selectedTtsProvider] ?: "",
                                onValueChange = { newValue ->
                                    ttsRegions[selectedTtsProvider] = newValue
                                },
                                label = { Text(stringResource(R.string.tts_region_placeholder)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        OutlinedTextField(
                            value = ttsKeys[selectedTtsProvider] ?: "",
                            onValueChange = { newValue ->
                                ttsKeys[selectedTtsProvider] = newValue
                            },
                            label = { Text(stringResource(R.string.api_key)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (ttsKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { ttsKeyVisible = !ttsKeyVisible }) {
                                    Icon(if (ttsKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (selectedTtsProvider == "OpenAI") {
                            OutlinedTextField(
                                value = ttsModels[selectedTtsProvider] ?: "",
                                onValueChange = { newValue ->
                                    ttsModels[selectedTtsProvider] = newValue
                                },
                                label = { Text(stringResource(R.string.tts_model_placeholder)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        OutlinedTextField(
                            value = ttsVoices[selectedTtsProvider] ?: "",
                            onValueChange = { newValue ->
                                ttsVoices[selectedTtsProvider] = newValue
                            },
                            label = { Text(stringResource(R.string.tts_voice_placeholder)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = { showLogsDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.view_dev_logs))
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    AnimatedVisibility(
        visible = showPromptEditor,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = WashiBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { showPromptEditor = false }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = SumiInk)
                    }
                    Text(
                        text = stringResource(R.string.prompt_editor_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = SumiInk
                    )
                    IconButton(onClick = {
                        showResetAllConfirm = true
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.reset_all_prompts), tint = Color.Red)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Selector for Prompt Type
                var dropdownExpanded by remember { mutableStateOf(false) }
                val promptKeys = listOf(
                    "prompt_translation" to R.string.prompt_type_translation,
                    "prompt_segments" to R.string.prompt_type_segments,
                    "prompt_clauses" to R.string.prompt_type_clauses,
                    "prompt_grammar" to R.string.prompt_type_grammar,
                    "prompt_tokenizer" to R.string.prompt_type_tokenizer,
                    "prompt_tokenizer_ocr" to R.string.prompt_type_tokenizer_ocr,
                    "prompt_tokenizer_image" to R.string.prompt_type_tokenizer_image,
                    "prompt_tokenizer_image_repair" to R.string.prompt_type_tokenizer_image_repair
                )

                val selectedLabelRes = promptKeys.find { it.first == selectedPromptKey }?.second ?: R.string.prompt_type_translation
                val selectedLabel = stringResource(selectedLabelRes)

                Text(
                    text = stringResource(R.string.prompt_select_type),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SumiInk.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceColor, RoundedCornerShape(8.dp))
                        .border(BorderStroke(1.dp, SumiInk.copy(alpha = 0.1f)), RoundedCornerShape(8.dp))
                        .clickable { dropdownExpanded = true }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = selectedLabel, color = SumiInk, fontWeight = FontWeight.Medium)
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = SumiInk.copy(alpha = 0.5f))
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        promptKeys.forEach { (key, labelRes) ->
                            DropdownMenuItem(
                                text = { Text(stringResource(labelRes)) },
                                onClick = {
                                    selectedPromptKey = key
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable text editor
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    ),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryColor,
                        unfocusedBorderColor = SumiInk.copy(alpha = 0.15f),
                        focusedContainerColor = SurfaceColor,
                        unfocusedContainerColor = SurfaceColor
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom control actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Reset Current Button
                    OutlinedButton(
                        onClick = { showResetConfirm = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.reset_prompt))
                    }

                    // Save Button
                    Button(
                        onClick = {
                            viewModel.saveCustomPrompt(selectedPromptKey, promptText)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(ctx.getString(R.string.prompt_save_success))
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = OnPrimaryColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.save_prompt), color = OnPrimaryColor)
                    }
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.reset_prompt), fontWeight = FontWeight.Bold, color = SumiInk) },
            text = { Text(stringResource(R.string.prompt_reset_confirm), color = SumiInk) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetCustomPrompt(selectedPromptKey)
                        promptText = viewModel.getCustomPrompt(selectedPromptKey)
                        showResetConfirm = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(ctx.getString(R.string.prompt_reset_success))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text(stringResource(R.string.reset_prompt), fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.cancel), color = SumiInk)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showResetAllConfirm) {
        AlertDialog(
            onDismissRequest = { showResetAllConfirm = false },
            title = { Text(stringResource(R.string.reset_all_prompts), fontWeight = FontWeight.Bold, color = SumiInk) },
            text = { Text(stringResource(R.string.prompt_reset_all_confirm), color = SumiInk) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllCustomPrompts()
                        promptText = viewModel.getCustomPrompt(selectedPromptKey)
                        showResetAllConfirm = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(ctx.getString(R.string.prompt_reset_all_success))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text(stringResource(R.string.reset_all_prompts), fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetAllConfirm = false }) {
                    Text(stringResource(R.string.cancel), color = SumiInk)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showTokenDialog) {
        AlertDialog(
            onDismissRequest = { showTokenDialog = false },
            title = { Text(stringResource(R.string.token_usage), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    val formattedTotal = if (totalTokensConsumed >= 1000) String.format(java.util.Locale.US, "%.1fk", totalTokensConsumed / 1000.0) else totalTokensConsumed.toString()
                    Text(stringResource(R.string.total_tokens_format, formattedTotal), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    tokenUsageByModel.forEach { usage ->
                        val u = if (usage.totalTokens >= 1000) String.format(java.util.Locale.US, "%.1fk", usage.totalTokens / 1000.0) else usage.totalTokens.toString()
                        Text("${usage.modelUsed}: $u", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTokenDialog = false }) {
                    Text(stringResource(R.string.close), color = SumiInk)
                }
            },
            containerColor = WashiBg
        )
    }

    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    val SumiInk = MaterialTheme.colorScheme.onBackground
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = SumiInk,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.08f)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val SumiInk = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, onClick = onClick ?: {})
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SumiInk.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, color = SumiInk, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 12.sp, color = SumiInk.copy(alpha = 0.5f))
            }
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(16.dp))
            trailingContent()
        }
    }
}
