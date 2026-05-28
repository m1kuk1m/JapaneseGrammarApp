package com.example.japanesegrammarapp.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("api_keys", Context.MODE_PRIVATE)
    
    // Traditional Japanese Colors
    val SumiInk = Color(0xFF2B2A28)
    val WashiBg = Color(0xFFFCF8F2)
    val AizomeIndigo = Color(0xFFBCCCD4)

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
    var expandedProvider by remember { mutableStateOf<String?>("Gemini") }

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
                        .padding(bottom = 16.dp), 
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
                                Text(
                                    provider, 
                                    style = MaterialTheme.typography.titleMedium, 
                                    fontWeight = FontWeight.Bold,
                                    color = SumiInk
                                )
                                if (provider == "Vertex AI") {
                                    Text(
                                        "AQ.キー用のExpress Modeを使用します。",
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
                            }
                        }
                    }
                }
            }
        }
    }
}
