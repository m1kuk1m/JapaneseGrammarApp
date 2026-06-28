package com.example.japanesegrammarapp.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DocumentScanner
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.domain.model.LlmEndpoint
import com.example.japanesegrammarapp.ui.SettingsViewModel
import com.example.japanesegrammarapp.ui.UiEvent
import com.example.japanesegrammarapp.ui.theme.ZenColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector

enum class SettingsCategory(val titleRes: Int, val icon: ImageVector) {
    APPEARANCE(R.string.appearance, Icons.Default.Palette),
    GENERAL(R.string.general, Icons.Default.Settings),
    OCR_SCANNING(R.string.ocr_scanning, Icons.Default.DocumentScanner),
    LLM_API(R.string.llm_prompts, Icons.Default.VpnKey),
    TTS(R.string.tts_settings_title, Icons.Default.RecordVoiceOver),
    ADVANCED(R.string.advanced_debug, Icons.Default.BugReport)
}

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
    val ctx = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentCategory by remember { mutableStateOf<SettingsCategory?>(null) }

    val uiState by viewModel.uiState.collectAsState()
    val providerModels = uiState.providerModels
    val totalTokensConsumed by viewModel.totalTokensConsumed.collectAsState()
    val tokenUsageByModel by viewModel.tokenUsageByModel.collectAsState()
    val dailyTokenUsage by viewModel.dailyTokenUsage.collectAsState()

    var showTokenDialog by remember { mutableStateOf(false) }

    var showPromptEditor by remember { mutableStateOf(false) }
    var selectedPromptKey by remember { mutableStateOf("prompt_translation") }
    var promptText by remember { mutableStateOf("") }
    var pendingTtsKeyClearProvider by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(showPromptEditor, selectedPromptKey) {
        if (showPromptEditor) {
            promptText = viewModel.getCustomPrompt(selectedPromptKey)
        }
    }

    val providers = uiState.allProviders
    var isLocalSettingsInitialized by remember { mutableStateOf(false) }

    var selectedTtsProvider by remember { mutableStateOf("OpenAI") }
    val ttsUrls = remember { mutableStateMapOf<String, String>() }
    val ttsKeys = remember { mutableStateMapOf<String, String>() }
    val ttsModels = remember { mutableStateMapOf<String, String>() }
    val ttsVoices = remember { mutableStateMapOf<String, String>() }
    val ttsRegions = remember { mutableStateMapOf<String, String>() }

    fun saveSettings() {
        if (!isLocalSettingsInitialized) return
        viewModel.setTtsProvider(selectedTtsProvider)
        ttsUrls.forEach { (provider, url) ->
            viewModel.setTtsApiUrl(provider, url)
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
    }

    val currentSaveSettings by rememberUpdatedState(newValue = ::saveSettings)

    BackHandler(enabled = isVisible && !showPromptEditor && currentCategory != null) {
        currentCategory = null
    }

    BackHandler(enabled = isVisible && !showPromptEditor && currentCategory == null) {
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

    val wallpaperLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.saveWallpaper(uri)
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
            when (event) {
                is UiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is UiEvent.ShowLocalizedError -> {
                    snackbarHostState.showSnackbar(
                        message = ctx.getString(event.resId, *event.args.toTypedArray()),
                        duration = SnackbarDuration.Short
                    )
                }
                is UiEvent.ShareFileEvent -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = event.mimeType
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        event.subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooserIntent = Intent.createChooser(shareIntent, ctx.getString(event.chooserTitleResId))
                    val resInfoList = ctx.packageManager.queryIntentActivities(
                        chooserIntent,
                        android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
                    )
                    for (resolveInfo in resInfoList) {
                        ctx.grantUriPermission(
                            resolveInfo.activityInfo.packageName,
                            event.uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                    ctx.startActivity(chooserIntent)
                }
                is UiEvent.ShareTextEvent -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, event.text)
                        event.subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
                    }
                    ctx.startActivity(
                        Intent.createChooser(shareIntent, ctx.getString(event.chooserTitleResId))
                    )
                }
                else -> Unit
            }
        }
    }

    val logs by com.example.japanesegrammarapp.utils.AppLogger.logs.collectAsState()
    var showLogsDialog by remember { mutableStateOf(false) }

    val apiLogs by com.example.japanesegrammarapp.utils.AppLogger.apiLogs.collectAsState()
    var showApiLogsDialog by remember { mutableStateOf(false) }
    var showOcrDebugDialog by remember { mutableStateOf(false) }
    var selectedApiLogDetail by remember { mutableStateOf<com.example.japanesegrammarapp.utils.ApiDebugLog?>(null) }
    var includeFullApiLogExport by remember { mutableStateOf(false) }
    var pendingShareAppLogs by remember { mutableStateOf<List<String>?>(null) }
    var pendingShareApiLogs by remember { mutableStateOf<List<com.example.japanesegrammarapp.utils.ApiDebugLog>?>(null) }
    var pendingCopyApiLogs by remember { mutableStateOf<List<com.example.japanesegrammarapp.utils.ApiDebugLog>?>(null) }
    var endpointBeingEdited by remember { mutableStateOf<LlmEndpoint?>(null) }
    var endpointAddProvider by remember { mutableStateOf<String?>(null) }
    var endpointDeleteTarget by remember { mutableStateOf<LlmEndpoint?>(null) }

    SettingsLogDialogs(
        logs = logs,
        apiLogs = apiLogs,
        showLogsDialog = showLogsDialog,
        onDismissLogs = { showLogsDialog = false },
        showApiLogsDialog = showApiLogsDialog,
        onDismissApiLogs = { showApiLogsDialog = false },
        selectedApiLogDetail = selectedApiLogDetail,
        onSelectedApiLogDetailChange = { selectedApiLogDetail = it },
        includeFullApiLogExport = includeFullApiLogExport,
        onIncludeFullApiLogExportChange = { includeFullApiLogExport = it },
        pendingShareAppLogs = pendingShareAppLogs,
        onPendingShareAppLogsChange = { pendingShareAppLogs = it },
        pendingShareApiLogs = pendingShareApiLogs,
        onPendingShareApiLogsChange = { pendingShareApiLogs = it },
        pendingCopyApiLogs = pendingCopyApiLogs,
        onPendingCopyApiLogsChange = { pendingCopyApiLogs = it },
        onShareAppLogs = viewModel::shareAppLogs,
        onShareApiLogs = viewModel::shareApiLogs
    )

    if (showOcrDebugDialog) {
        OcrBoxDebugDialog(
            settings = uiState.ocrBoxDetectionSettings,
            onSettingsChange = viewModel::setOcrBoxDetectionSettings,
            onResetDefaults = viewModel::resetOcrBoxDetectionSettings,
            onDismiss = { showOcrDebugDialog = false }
        )
    }

    pendingTtsKeyClearProvider?.let { provider ->
        AlertDialog(
            onDismissRequest = { pendingTtsKeyClearProvider = null },
            title = { Text(stringResource(R.string.clear_api_key_title), fontWeight = FontWeight.Bold, color = SumiInk) },
            text = { Text(stringResource(R.string.clear_api_key_confirm), color = SumiInk) },
            confirmButton = {
                Button(
                    onClick = {
                        ttsKeys[provider] = ""
                        viewModel.saveTtsSettings(
                            selectedProvider = selectedTtsProvider,
                            urls = ttsUrls.toMap(),
                            keyProvider = provider,
                            key = "",
                            models = ttsModels.toMap(),
                            voices = ttsVoices.toMap(),
                            regions = ttsRegions.toMap()
                        )
                        pendingTtsKeyClearProvider = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(stringResource(R.string.clear_api_key), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingTtsKeyClearProvider = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    endpointAddProvider?.let { provider ->
        EndpointEditorDialog(
            provider = provider,
            endpoint = null,
            initialKey = "",
            onDismiss = { endpointAddProvider = null },
            onSave = { name, baseUrl, apiKey, priority, weight ->
                viewModel.createEndpoint(provider, name, baseUrl, apiKey, priority, weight)
                android.widget.Toast.makeText(ctx, ctx.getString(R.string.save_success_toast), android.widget.Toast.LENGTH_SHORT).show()
                endpointAddProvider = null
            }
        )
    }

    endpointBeingEdited?.let { endpoint ->
        EndpointEditorDialog(
            provider = endpoint.provider,
            endpoint = endpoint,
            initialKey = viewModel.getApiKeyForEndpoint(endpoint.id),
            onDismiss = { endpointBeingEdited = null },
            onSave = { name, baseUrl, apiKey, priority, weight ->
                viewModel.saveEndpoint(
                    endpoint.copy(
                        name = name,
                        baseUrl = baseUrl,
                        priority = priority,
                        weight = weight
                    ),
                    apiKey
                )
                android.widget.Toast.makeText(ctx, ctx.getString(R.string.save_success_toast), android.widget.Toast.LENGTH_SHORT).show()
                endpointBeingEdited = null
            }
        )
    }

    endpointDeleteTarget?.let { endpoint ->
        AlertDialog(
            onDismissRequest = { endpointDeleteTarget = null },
            title = { Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold, color = SumiInk) },
            text = { Text(stringResource(R.string.delete_endpoint_confirm), color = SumiInk) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEndpoint(endpoint.provider, endpoint.id)
                        android.widget.Toast.makeText(ctx, ctx.getString(R.string.delete_success_toast), android.widget.Toast.LENGTH_SHORT).show()
                        endpointDeleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(stringResource(R.string.delete), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { endpointDeleteTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings-screen")
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = WashiBg,
        topBar = {
            TopAppBar(
                title = { 
                    val titleText = currentCategory?.let { stringResource(it.titleRes) } ?: stringResource(R.string.settings_title)
                    Text(titleText, fontWeight = FontWeight.Bold, color = SumiInk) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentCategory != null) {
                                currentCategory = null
                            } else {
                                saveSettings()
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("settings-back-button")
                    ) {
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
        LaunchedEffect(currentCategory) {
            scrollState.scrollTo(0)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            AnimatedContent(
                targetState = currentCategory,
                transitionSpec = {
                    when {
                        initialState == null && targetState != null -> {
                            (slideInHorizontally(
                                animationSpec = tween(260),
                                initialOffsetX = { it / 4 }
                            ) + fadeIn(animationSpec = tween(220))) togetherWith
                                    (slideOutHorizontally(
                                        animationSpec = tween(220),
                                        targetOffsetX = { -it / 4 }
                                    ) + fadeOut(animationSpec = tween(180)))
                        }
                        initialState != null && targetState == null -> {
                            (slideInHorizontally(
                                animationSpec = tween(260),
                                initialOffsetX = { -it / 4 }
                            ) + fadeIn(animationSpec = tween(220))) togetherWith
                                    (slideOutHorizontally(
                                        animationSpec = tween(220),
                                        targetOffsetX = { it / 4 }
                                    ) + fadeOut(animationSpec = tween(180)))
                        }
                        else -> {
                            fadeIn(animationSpec = tween(180)) togetherWith
                                    fadeOut(animationSpec = tween(160))
                        }
                    }.using(SizeTransform(clip = false))
                },
                label = "SettingsCategoryTransition",
                modifier = Modifier.fillMaxWidth()
            ) { targetCategory ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    if (targetCategory == null) {
                        SettingsGroup(title = stringResource(R.string.settings_title)) {
                            SettingsCategory.entries.forEachIndexed { index, category ->
                                SettingsItem(
                                    icon = category.icon,
                                    title = stringResource(category.titleRes),
                                    onClick = { currentCategory = category },
                                    trailingContent = {
                                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = SumiInk.copy(alpha = 0.4f))
                                    }
                                )
                                if (index < SettingsCategory.entries.lastIndex) {
                                    SettingsRootDivider()
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))
                    } else {
                        when (targetCategory) {
                            SettingsCategory.APPEARANCE -> {
                                SettingsAppearanceSection(
                                    uiState = uiState,
                                    onThemeModeChange = viewModel::setThemeMode,
                                    onPickWallpaper = { wallpaperLauncher.launch("image/*") },
                                    onClearWallpaper = {
                                        viewModel.clearWallpaper()
                                        android.widget.Toast.makeText(ctx, ctx.getString(R.string.clear_wallpaper_toast), android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            SettingsCategory.GENERAL -> {
                                SettingsGeneralSection(
                                    uiState = uiState,
                                    currentLangLabel = currentLangLabel,
                                    onAutoNavigateResultChange = viewModel::setAutoNavigateResult
                                )
                            }
                            SettingsCategory.OCR_SCANNING -> {
                                SettingsOcrSection(
                                    uiState = uiState,
                                    onUseOcrChange = viewModel::setUseOcr,
                                    onOcrBoxDetectorEngineChange = viewModel::setOcrBoxDetectorEngine,
                                    onTextSelectEngineChange = viewModel::setTextSelectEngine,
                                    onAutoDeskewAfterCaptureChange = viewModel::setAutoDeskewAfterCapture,
                                    onImageTokenizerModeChange = viewModel::setImageTokenizerMode,
                                    onShowOcrDebug = { showOcrDebugDialog = true }
                                )
                            }
                            SettingsCategory.LLM_API -> {
                                SettingsLlmPromptsSection(
                                    totalTokensConsumed = totalTokensConsumed,
                                    onShowTokenDialog = { showTokenDialog = true },
                                    onShowPromptEditor = { showPromptEditor = true }
                                )

                                SettingsApiPrioritySection(
                                    uiState = uiState,
                                    providers = providers,
                                    providerModels = providerModels,
                                    onActiveProviderChange = viewModel::setActiveProvider,
                                    onActiveModelChange = viewModel::setActiveModel,
                                    onBackupProviderChange = viewModel::setBackupProvider,
                                    onBackupModelChange = viewModel::setBackupModel
                                )

                                SettingsCredentialsSection(
                                    uiState = uiState,
                                    providers = providers,
                                    providerModels = providerModels,
                                    expandedProvider = expandedProvider,
                                    customModelInputs = customModelInputs,
                                    onExpandedProviderChange = { expandedProvider = it },
                                    onAddEndpoint = { endpointAddProvider = it },
                                    onEditEndpoint = { endpointBeingEdited = it },
                                    onDeleteEndpoint = { endpointDeleteTarget = it },
                                    onToggleEndpoint = { provider, endpoint, enabled ->
                                        viewModel.toggleEndpoint(provider, endpoint.id, enabled)
                                    },
                                    onFetchModels = { provider, endpoint ->
                                        viewModel.fetchModelsForEndpoint(provider, endpoint.id)
                                    },
                                    onCustomModelInputChange = { provider, value ->
                                        customModelInputs = customModelInputs.toMutableMap().apply { put(provider, value) }
                                    },
                                    onAddCustomModel = viewModel::saveModelsForProvider
                                )
                            }
                            SettingsCategory.TTS -> {
                                Text(
                                    text = stringResource(R.string.tts_settings_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SumiInk,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp, top = 16.dp)
                                )
                                SettingsTtsSection(
                                    selectedTtsProvider = selectedTtsProvider,
                                    onSelectedTtsProviderChange = { selectedTtsProvider = it },
                                    ttsUrls = ttsUrls,
                                    onTtsUrlChange = { provider, value -> ttsUrls[provider] = value },
                                    ttsKeys = ttsKeys,
                                    savedTtsKeys = uiState.ttsKeys,
                                    onTtsKeyChange = { provider, value -> ttsKeys[provider] = value },
                                    ttsModels = ttsModels,
                                    onTtsModelChange = { provider, value -> ttsModels[provider] = value },
                                    ttsVoices = ttsVoices,
                                    onTtsVoiceChange = { provider, value -> ttsVoices[provider] = value },
                                    ttsRegions = ttsRegions,
                                    onTtsRegionChange = { provider, value -> ttsRegions[provider] = value },
                                    onSaveTtsSettings = {
                                        viewModel.saveTtsSettings(
                                            selectedProvider = selectedTtsProvider,
                                            urls = ttsUrls.toMap(),
                                            keyProvider = selectedTtsProvider,
                                            key = ttsKeys[selectedTtsProvider].orEmpty(),
                                            models = ttsModels.toMap(),
                                            voices = ttsVoices.toMap(),
                                            regions = ttsRegions.toMap()
                                        )
                                    },
                                    onRequestClearTtsKey = { pendingTtsKeyClearProvider = it }
                                )
                            }
                            SettingsCategory.ADVANCED -> {
                                SettingsAdvancedSection(
                                    onShowApiLogs = { showApiLogsDialog = true },
                                    onShowAppLogs = { showLogsDialog = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    SettingsPromptEditor(
        visible = showPromptEditor,
        selectedPromptKey = selectedPromptKey,
        promptText = promptText,
        promptPresets = uiState.promptPresets,
        activePromptPresetId = uiState.activePromptPresetId,
        onDismiss = { showPromptEditor = false },
        onPromptKeyChange = { selectedPromptKey = it },
        onPromptTextChange = { promptText = it },
        onSave = {
            viewModel.saveCustomPrompt(selectedPromptKey, promptText)
            android.widget.Toast.makeText(ctx, ctx.getString(R.string.prompt_save_success), android.widget.Toast.LENGTH_SHORT).show()
        },
        onResetCurrent = {
            viewModel.resetCustomPrompt(selectedPromptKey)
            promptText = viewModel.getCustomPrompt(selectedPromptKey)
            android.widget.Toast.makeText(ctx, ctx.getString(R.string.prompt_reset_success), android.widget.Toast.LENGTH_SHORT).show()
        },
        onResetAll = {
            viewModel.resetAllCustomPrompts()
            promptText = viewModel.getCustomPrompt(selectedPromptKey)
            android.widget.Toast.makeText(ctx, ctx.getString(R.string.prompt_reset_all_success), android.widget.Toast.LENGTH_SHORT).show()
        },
        onCreatePreset = { name, copyCurrent ->
            viewModel.createPromptPreset(name, copyCurrent)
            promptText = viewModel.getCustomPrompt(selectedPromptKey)
            android.widget.Toast.makeText(ctx, ctx.getString(R.string.save_success_toast), android.widget.Toast.LENGTH_SHORT).show()
        },
        onRenamePreset = { id, newName ->
            viewModel.renamePromptPreset(id, newName)
            android.widget.Toast.makeText(ctx, ctx.getString(R.string.save_success_toast), android.widget.Toast.LENGTH_SHORT).show()
        },
        onDeletePreset = { id ->
            viewModel.deletePromptPreset(id)
            promptText = viewModel.getCustomPrompt(selectedPromptKey)
            android.widget.Toast.makeText(ctx, ctx.getString(R.string.delete_success_toast), android.widget.Toast.LENGTH_SHORT).show()
        },
        onSelectPreset = { id ->
            viewModel.setActivePromptPreset(id)
            promptText = viewModel.getCustomPrompt(selectedPromptKey)
        }
    )

    if (showTokenDialog) {
        val formatCount: (Int) -> String = { count ->
            String.format(java.util.Locale.US, "%,d", count)
        }

        val totalInput = tokenUsageByModel.sumOf { it.inputTokens }
        val totalOutput = tokenUsageByModel.sumOf { it.outputTokens }

        AlertDialog(
            onDismissRequest = { showTokenDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.token_usage),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = SumiInk
                    )
                    IconButton(
                        onClick = { showTokenDialog = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = SumiInk.copy(alpha = 0.6f)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1. Overview Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.total_tokens_format, formatCount(totalTokensConsumed)),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = SumiInk
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Ratio Progress Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SumiInk.copy(alpha = 0.1f))
                            ) {
                                val inputRatio = if (totalTokensConsumed > 0) totalInput.toFloat() / totalTokensConsumed else 0f
                                if (inputRatio > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(inputRatio.coerceAtLeast(0.01f))
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                                val outputRatio = 1f - inputRatio
                                if (outputRatio > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(outputRatio.coerceAtLeast(0.01f))
                                            .background(ZenColors.SakuraPink)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Legend
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${stringResource(R.string.input_tokens)}: ${formatCount(totalInput)}",
                                        fontSize = 11.sp,
                                        color = SumiInk.copy(alpha = 0.8f)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(ZenColors.SakuraPink)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${stringResource(R.string.output_tokens)}: ${formatCount(totalOutput)}",
                                        fontSize = 11.sp,
                                        color = SumiInk.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Model Breakdown
                    Text(
                        text = stringResource(R.string.model_breakdown),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = SumiInk
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (tokenUsageByModel.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_daily_data),
                            fontSize = 12.sp,
                            color = SumiInk.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        tokenUsageByModel.forEach { usage ->
                            val modelRatio = if (totalTokensConsumed > 0) usage.totalTokens.toFloat() / totalTokensConsumed else 0f
                            val modelPercentage = (modelRatio * 100).toInt()
                            
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = usage.modelUsed,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SumiInk
                                    )
                                    Text(
                                        text = "${formatCount(usage.totalTokens)} ($modelPercentage%)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SumiInk
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${stringResource(R.string.input_tokens)}: ${formatCount(usage.inputTokens)}  |  ${stringResource(R.string.output_tokens)}: ${formatCount(usage.outputTokens)}",
                                    fontSize = 11.sp,
                                    color = SumiInk.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = modelRatio,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = SumiInk.copy(alpha = 0.05f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Daily Usage Trend
                    Text(
                        text = stringResource(R.string.daily_usage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = SumiInk
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (dailyTokenUsage.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_daily_data),
                            fontSize = 12.sp,
                            color = SumiInk.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        dailyTokenUsage.forEach { daily ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = daily.date,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SumiInk
                                        )
                                        Text(
                                            text = daily.modelUsed,
                                            fontSize = 10.sp,
                                            color = SumiInk.copy(alpha = 0.5f)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${formatCount(daily.totalTokens)} tkn",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SumiInk
                                        )
                                        Text(
                                            text = "in: ${formatCount(daily.inputTokens)} | out: ${formatCount(daily.outputTokens)}",
                                            fontSize = 10.sp,
                                            color = SumiInk.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Divider(color = SumiInk.copy(alpha = 0.08f))
                            }
                        }
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
private fun SettingsRootDivider() {
    val sumiInk = MaterialTheme.colorScheme.onBackground
    Divider(color = sumiInk.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
}
