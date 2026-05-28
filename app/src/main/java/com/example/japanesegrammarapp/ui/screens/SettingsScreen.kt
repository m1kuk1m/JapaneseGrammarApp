package com.example.japanesegrammarapp.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.japanesegrammarapp.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, viewModel: AppViewModel) {
    val context = LocalContext.current
    val prefs = viewModel.securePrefs
    
    // Traditional Japanese Colors
    val SumiInk = Color(0xFF2B2A28)
    val WashiBg = Color(0xFFFCF8F2)
    val AizomeIndigo = Color(0xFFBCCCD4)

    val activeProvider by viewModel.activeProvider.collectAsState()
    val providerModels by viewModel.providerModels.collectAsState()
    val isFetchingModels by viewModel.isFetchingModels.collectAsState()
    val fetchingProvider by viewModel.fetchingProvider.collectAsState()

    val providers = listOf("Gemini", "Vertex AI", "DeepSeek", "Qwen", "OpenAI Compatible")
    val defaultUrls = mapOf(
        "Gemini" to "https://generativelanguage.googleapis.com/v1beta",
        "Vertex AI" to "https://aiplatform.googleapis.com/v1/publishers/google",
        "DeepSeek" to "https://api.deepseek.com",
        "Qwen" to "https://dashscope.aliyuncs.com/compatible-mode/v1",
        "OpenAI Compatible" to "https://api.openai.com/v1"
    )

    var keys by remember { mutableStateOf(providers.associateWith { prefs.getString("${it}_key", "") ?: "" }) }
    var urls by remember { mutableStateOf(providers.associateWith { prefs.getString("${it}_url", defaultUrls[it]) ?: defaultUrls[it] ?: "" }) }

    // Toggle password visibility per provider
    var keysVisibility by remember { mutableStateOf(providers.associateWith { false }) }
    // Single expandable item tracking
    var expandedProvider by remember { mutableStateOf<String?>(activeProvider) }

    fun saveSettings() {
        prefs.edit().apply {
            providers.forEach { 
                putString("${it}_key", keys[it])
                putString("${it}_url", urls[it])
            }
            apply()
        }
    }

    Scaffold(
        containerColor = WashiBg,
        topBar = {
            LargeTopAppBar(
                title = { Text("設定", fontWeight = FontWeight.Bold, color = SumiInk) },
                navigationIcon = {
                    IconButton(onClick = { 
                        saveSettings()
                        navController.popBackStack() 
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る", tint = SumiInk)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = WashiBg,
                    titleContentColor = SumiInk
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "API 設定", 
                style = MaterialTheme.typography.titleLarge, 
                color = SumiInk, 
                fontWeight = FontWeight.Bold
            )
            Text(
                "各プロバイダーのベースURLとAPIキーを設定します。", 
                style = MaterialTheme.typography.bodyMedium, 
                color = SumiInk.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            providers.forEach { provider ->
                val isExpanded = expandedProvider == provider
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .animateContentSize(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)), 
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedProvider = if (isExpanded) null else provider
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    Text(
                                        provider, 
                                        style = MaterialTheme.typography.titleMedium, 
                                        fontWeight = FontWeight.Bold,
                                        color = SumiInk
                                    )
                                    if (activeProvider == provider) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = SumiInk.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.15f))
                                        ) {
                                            Text(
                                                text = "有効",
                                                color = SumiInk,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                if (provider == "Vertex AI") {
                                    Text(
                                        "APIキー用のExpress Modeを使用します。",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SumiInk.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            val expandIcon = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
                            Icon(expandIcon, contentDescription = "展開・折りたたみ", tint = SumiInk)
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                            ) {
                                OutlinedTextField(
                                    value = urls[provider] ?: "",
                                    onValueChange = { newValue ->
                                        urls = urls.toMutableMap().apply { put(provider, newValue) }
                                        prefs.edit().putString("${provider}_url", newValue).apply()
                                    },
                                    label = { Text("ベースURL", color = SumiInk.copy(alpha = 0.7f)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(6.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SumiInk,
                                        unfocusedBorderColor = SumiInk.copy(alpha = 0.3f),
                                        focusedLabelColor = SumiInk,
                                        unfocusedLabelColor = SumiInk.copy(alpha = 0.7f),
                                        focusedTextColor = SumiInk,
                                        unfocusedTextColor = SumiInk
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                val isVisible = keysVisibility[provider] == true
                                OutlinedTextField(
                                    value = keys[provider] ?: "",
                                    onValueChange = { newValue ->
                                        keys = keys.toMutableMap().apply { put(provider, newValue) }
                                        prefs.edit().putString("${provider}_key", newValue).apply()
                                    },
                                    label = { Text("APIキー", color = SumiInk.copy(alpha = 0.7f)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        val buttonText = if (isVisible) "非表示" else "表示"
                                        TextButton(
                                            onClick = {
                                                keysVisibility = keysVisibility.toMutableMap().apply { put(provider, !isVisible) }
                                            }
                                        ) {
                                            Text(
                                                text = buttonText,
                                                color = SumiInk.copy(alpha = 0.6f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SumiInk,
                                        unfocusedBorderColor = SumiInk.copy(alpha = 0.3f),
                                        focusedLabelColor = SumiInk,
                                        unfocusedLabelColor = SumiInk.copy(alpha = 0.7f),
                                        focusedTextColor = SumiInk,
                                        unfocusedTextColor = SumiInk
                                    )
                                )
                                
                                // Active Provider Toggle
                                Spacer(modifier = Modifier.height(12.dp))
                                val isActive = activeProvider == provider
                                if (isActive) {
                                    Button(
                                        onClick = {},
                                        colors = ButtonDefaults.buttonColors(containerColor = SumiInk),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Active", tint = WashiBg, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("現在のアクティブなプロバイダー", fontWeight = FontWeight.Bold, color = WashiBg, fontSize = 13.sp)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { viewModel.setActiveProvider(provider) },
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.6f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SumiInk),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("このプロバイダーを有効にする", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                }

                                // Model Management Section
                                Spacer(modifier = Modifier.height(16.dp))
                                Divider(color = SumiInk.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "モデル管理",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = SumiInk,
                                        fontWeight = FontWeight.Bold
                                    )
                                    // Fetch models button
                                    val isFetchingThis = fetchingProvider == provider
                                    OutlinedButton(
                                        onClick = {
                                            val currentUrl = urls[provider] ?: ""
                                            val currentKey = keys[provider] ?: ""
                                            viewModel.fetchModels(provider, currentUrl, currentKey)
                                        },
                                        enabled = !isFetchingThis,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, if (isFetchingThis) SumiInk.copy(alpha = 0.2f) else SumiInk.copy(alpha = 0.5f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SumiInk),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        if (isFetchingThis) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp,
                                                color = SumiInk.copy(alpha = 0.5f)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("取得中...", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        } else {
                                            Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = "モデルを取得",
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("クラウドから取得", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val models = providerModels[provider] ?: emptyList()
                                
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SumiInk.copy(alpha = 0.02f)),
                                    border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.08f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        if (models.isEmpty()) {
                                            Text("登録されているモデルはありません。", fontSize = 12.sp, color = SumiInk.copy(alpha = 0.4f))
                                        } else {
                                            models.forEach { model ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                                ) {
                                                    Text(model, color = SumiInk, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                                    IconButton(
                                                        onClick = {
                                                            val updated = models.filter { it != model }
                                                            viewModel.saveModelsForProvider(provider, updated)
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.Close, contentDescription = "削除", tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        var newModelName by remember(provider) { mutableStateOf("") }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = newModelName,
                                                onValueChange = { newModelName = it },
                                                placeholder = { Text("新規モデル名...", fontSize = 12.sp, color = SumiInk.copy(alpha = 0.4f)) },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                                shape = RoundedCornerShape(6.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = SumiInk,
                                                    unfocusedBorderColor = SumiInk.copy(alpha = 0.2f),
                                                    focusedTextColor = SumiInk,
                                                    unfocusedTextColor = SumiInk
                                                )
                                            )
                                            
                                            Button(
                                                onClick = {
                                                    val nameTrimmed = newModelName.trim()
                                                    if (nameTrimmed.isNotBlank() && !models.contains(nameTrimmed)) {
                                                        val updated = models + nameTrimmed
                                                        viewModel.saveModelsForProvider(provider, updated)
                                                        newModelName = ""
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SumiInk),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Text("追加", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WashiBg)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
