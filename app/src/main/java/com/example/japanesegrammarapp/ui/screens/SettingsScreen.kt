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
    val isFetchingModels = uiState.isFetchingModels
    val fetchingProvider = uiState.fetchingProvider
    val totalTokensConsumed by viewModel.totalTokensConsumed.collectAsState()
    val tokenUsageByModel by viewModel.tokenUsageByModel.collectAsState()

    var showTokenDialog by remember { mutableStateOf(false) }

    val providers = uiState.allProviders
    val baseProviders = LlmConfig.providers
    val defaultUrls = LlmConfig.defaultUrls

    var isSettingsLoaded by remember { mutableStateOf(false) }

    var selectedTtsProvider by remember { mutableStateOf("OpenAI") }
    var ttsUrl by remember { mutableStateOf("") }
    var ttsKey by remember { mutableStateOf("") }
    var ttsModel by remember { mutableStateOf("") }
    var ttsVoice by remember { mutableStateOf("") }
    var ttsRegion by remember { mutableStateOf("") }

    fun saveSettings() {
        if (!isSettingsLoaded) return
        viewModel.setTtsProvider(selectedTtsProvider)
        viewModel.setTtsApiUrl(selectedTtsProvider, ttsUrl)
        viewModel.setTtsApiKey(selectedTtsProvider, ttsKey)
        viewModel.setTtsModel(selectedTtsProvider, ttsModel)
        viewModel.setTtsVoice(selectedTtsProvider, ttsVoice)
        viewModel.setTtsRegion(selectedTtsProvider, ttsRegion)
    }

    BackHandler(enabled = isVisible) {
        saveSettings()
        onBack()
    }

    LaunchedEffect(providers) {
        selectedTtsProvider = withContext(Dispatchers.IO) { viewModel.getTtsProvider() }
        isSettingsLoaded = true
    }

    LaunchedEffect(selectedTtsProvider) {
        ttsUrl = withContext(Dispatchers.IO) { viewModel.getTtsApiUrl(selectedTtsProvider) }
        ttsKey = withContext(Dispatchers.IO) { viewModel.getTtsApiKey(selectedTtsProvider) }
        ttsModel = withContext(Dispatchers.IO) { viewModel.getTtsModel(selectedTtsProvider) }
        ttsVoice = withContext(Dispatchers.IO) { viewModel.getTtsVoice(selectedTtsProvider) }
        ttsRegion = withContext(Dispatchers.IO) { viewModel.getTtsRegion(selectedTtsProvider) }
    }


    var expandedProvider by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun startFloatingService() {
        val serviceIntent = android.content.Intent(ctx, com.example.japanesegrammarapp.ui.service.FloatingService::class.java)
        ctx.startService(serviceIntent)
    }

    fun stopFloatingService() {
        val serviceIntent = android.content.Intent(ctx, com.example.japanesegrammarapp.ui.service.FloatingService::class.java)
        ctx.stopService(serviceIntent)
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(ctx)) {
            viewModel.setGlobalFloatingEnabled(true)
            startFloatingService()
        } else {
            viewModel.setGlobalFloatingEnabled(false)
            coroutineScope.launch {
                snackbarHostState.showSnackbar(ctx.getString(R.string.overlay_permission_required))
            }
        }
    }

    var customModelInputs by remember { mutableStateOf(providers.associateWith { "" }) }

    val wallpaperLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            viewModel.setWallpaperUri(uri.toString())
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
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    if (showLogsDialog) {
        AlertDialog(
            onDismissRequest = { showLogsDialog = false },
            title = { Text(stringResource(R.string.app_logs_title)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { com.example.japanesegrammarapp.utils.AppLogger.clear() }) {
                            Text(stringResource(R.string.clear_logs))
                        }
                        TextButton(onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(logs.joinToString("\n")))
                        }) {
                            Text(stringResource(R.string.copy_logs))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(logs) { log ->
                            Text(log, fontSize = 10.sp, fontFamily = FontFamily.Monospace, lineHeight = 12.sp)
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
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
        AlertDialog(
            onDismissRequest = { showApiLogsDialog = false },
            title = { Text(stringResource(R.string.api_debug_logs_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { com.example.japanesegrammarapp.utils.AppLogger.clearApiLogs() }) {
                            Text(stringResource(R.string.clear_api_debug_logs))
                        }
                        TextButton(onClick = {
                            val copyText = apiLogs.joinToString("\n\n") { log ->
                                "[${log.apiTypeLabel}] ${log.provider} - ${log.model}\nStatus: ${log.status}\nPrompt: ${log.userPrompt}\nResponse: ${log.rawResponse ?: ""}\nError: ${log.errorMessage ?: ""}"
                            }
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(copyText))
                        }) {
                            Text(stringResource(R.string.copy_logs))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (apiLogs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
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
                        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(apiLogs) { log ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "[${log.apiTypeLabel}] ${log.provider} - ${log.model}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SumiInk
                                            )
                                            Text(
                                                text = log.status,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (log.status == "SUCCESS") Color(0xFF2E7D32) else Color(0xFFC62828)
                                            )
                                        }
                                        Text(
                                            text = stringResource(R.string.api_debug_meta, log.hasImage.toString(), log.consumedTokens, log.inputTokens, log.outputTokens),
                                            fontSize = 10.sp,
                                            color = SumiInk.copy(alpha = 0.5f)
                                        )
                                        Divider(color = SumiInk.copy(alpha = 0.1f))
                                        Text(stringResource(R.string.api_debug_user_prompt), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SumiInk)
                                        Text(log.userPrompt, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = SumiInk.copy(alpha = 0.8f))
                                        if (log.rawResponse != null) {
                                            Divider(color = SumiInk.copy(alpha = 0.05f))
                                            Text(stringResource(R.string.api_debug_raw_response), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SumiInk)
                                            Text(log.rawResponse, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = SumiInk.copy(alpha = 0.8f))
                                        }
                                        if (log.errorMessage != null) {
                                            Divider(color = SumiInk.copy(alpha = 0.05f))
                                            Text(stringResource(R.string.api_debug_error), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                            Text(log.errorMessage, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFC62828))
                                        }
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
                                        onClick = { viewModel.setWallpaperUri("") },
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
                    var tokenizerModeDropdownExpanded by remember { mutableStateOf(false) }
                    val currentModeLabel = when (uiState.imageTokenizerMode) {
                        "repair" -> stringResource(R.string.image_tokenizer_mode_repair)
                        else -> stringResource(R.string.image_tokenizer_mode_faithful)
                    }
                    Divider(color = SumiInk.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Global Floating Ball Enable
                    SettingsItem(
                        icon = Icons.Default.TouchApp,
                        title = stringResource(R.string.global_floating_ball),
                        subtitle = stringResource(R.string.global_floating_ball_desc),
                        trailingContent = {
                            Switch(
                                checked = uiState.globalFloatingEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && !Settings.canDrawOverlays(ctx)) {
                                        val intent = android.content.Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            android.net.Uri.parse("package:${ctx.packageName}")
                                        )
                                        overlayPermissionLauncher.launch(intent)
                                    } else {
                                        viewModel.setGlobalFloatingEnabled(enabled)
                                        if (enabled) {
                                            startFloatingService()
                                        } else {
                                            stopFloatingService()
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = OnPrimaryColor,
                                    checkedTrackColor = PrimaryColor,
                                    uncheckedThumbColor = SumiInk.copy(alpha = 0.4f),
                                    uncheckedTrackColor = SumiInk.copy(alpha = 0.1f)
                                )
                            )
                        }
                    )

                    // Default Action for Floating Ball
                    var floatingActionDropdownExpanded by remember { mutableStateOf(false) }
                    val currentFloatingActionLabel = when (uiState.globalFloatingAction) {
                        2 -> stringResource(R.string.action_screen_ocr)
                        3 -> stringResource(R.string.camera)
                        4 -> stringResource(R.string.action_text_input)
                        else -> stringResource(R.string.action_show_menu)
                    }
                    Divider(color = SumiInk.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItem(
                        icon = Icons.Default.Mouse,
                        title = stringResource(R.string.floating_ball_action),
                        subtitle = currentFloatingActionLabel,
                        onClick = { floatingActionDropdownExpanded = true },
                        trailingContent = {
                            Box {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = SumiInk.copy(alpha = 0.5f))
                                DropdownMenu(
                                    expanded = floatingActionDropdownExpanded,
                                    onDismissRequest = { floatingActionDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_show_menu)) },
                                        onClick = { viewModel.setGlobalFloatingAction(1); floatingActionDropdownExpanded = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_screen_ocr)) },
                                        onClick = { viewModel.setGlobalFloatingAction(2); floatingActionDropdownExpanded = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.camera)) },
                                        onClick = { viewModel.setGlobalFloatingAction(3); floatingActionDropdownExpanded = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_text_input)) },
                                        onClick = { viewModel.setGlobalFloatingAction(4); floatingActionDropdownExpanded = false }
                                    )
                                }
                            }
                        }
                    )

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

            var showAddCustomProviderDialog by remember { mutableStateOf(false) }

            providers.forEach { provider ->
                val isExpanded = expandedProvider == provider

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
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
                                val endpoints = uiState.providerEndpoints[provider] ?: emptyList()

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(R.string.api_endpoints_manage), fontWeight = FontWeight.Bold, color = SumiInk)
                                    IconButton(
                                        onClick = {
                                            val newId = java.util.UUID.randomUUID().toString()
                                            val newEndpoint = com.example.japanesegrammarapp.domain.repository.ApiEndpointConfig(
                                                id = newId, name = "New Endpoint", url = "", key = ""
                                            )
                                            viewModel.saveApiEndpoints(provider, endpoints + newEndpoint)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add Endpoint", tint = PrimaryColor)
                                    }
                                }

                                endpoints.forEachIndexed { index, endpoint ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        colors = CardDefaults.cardColors(containerColor = WashiBg),
                                        border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.05f))
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = endpoint.name,
                                                    onValueChange = { newName ->
                                                        val updated = endpoints.toMutableList()
                                                        updated[index] = endpoint.copy(name = newName)
                                                        viewModel.saveApiEndpoints(provider, updated)
                                                    },
                                                    label = { Text(stringResource(R.string.endpoint_name)) },
                                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                                    singleLine = true,
                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                                )
                                                IconButton(
                                                    onClick = {
                                                        val updated = endpoints.toMutableList()
                                                        updated.removeAt(index)
                                                        viewModel.saveApiEndpoints(provider, updated)
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            OutlinedTextField(
                                                value = endpoint.url,
                                                onValueChange = { newUrl ->
                                                    val updated = endpoints.toMutableList()
                                                    updated[index] = endpoint.copy(url = newUrl)
                                                    viewModel.saveApiEndpoints(provider, updated)
                                                },
                                                label = { Text(stringResource(R.string.base_url)) },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            OutlinedTextField(
                                                value = endpoint.key,
                                                onValueChange = { newKey ->
                                                    val updated = endpoints.toMutableList()
                                                    updated[index] = endpoint.copy(key = newKey)
                                                    viewModel.saveApiEndpoints(provider, updated)
                                                },
                                                label = { Text(stringResource(R.string.api_key)) },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            val isFetchingThis = fetchingProvider == provider
                                            Button(
                                                onClick = { viewModel.fetchModels(provider, endpoint.url, endpoint.key) },
                                                enabled = !isFetchingThis && endpoint.key.isNotBlank(),
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
                                        }
                                    }
                                }

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
                        .padding(bottom = 12.dp),
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
                                value = ttsUrl,
                                onValueChange = { ttsUrl = it },
                                label = { Text(stringResource(R.string.base_url)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (selectedTtsProvider == "Microsoft") {
                            OutlinedTextField(
                                value = ttsRegion,
                                onValueChange = { ttsRegion = it },
                                label = { Text(stringResource(R.string.tts_region_placeholder)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        OutlinedTextField(
                            value = ttsKey,
                            onValueChange = { ttsKey = it },
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
                                value = ttsModel,
                                onValueChange = { ttsModel = it },
                                label = { Text(stringResource(R.string.tts_model_placeholder)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        OutlinedTextField(
                            value = ttsVoice,
                            onValueChange = { ttsVoice = it },
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
            modifier = Modifier.fillMaxWidth(),
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
