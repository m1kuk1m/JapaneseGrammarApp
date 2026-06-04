package com.example.japanesegrammarapp.ui.screens

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.domain.model.BookmarkedSegmentDomain
import com.example.japanesegrammarapp.domain.model.effectivePosCategory
import com.example.japanesegrammarapp.ui.BookmarkViewModel
import com.example.japanesegrammarapp.ui.theme.ZenThemeColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(
    navController: NavController,
    viewModel: BookmarkViewModel,
    studyMode: String = "ja_to_zh",
    cardLimit: Int = -1,
    posFilter: String = "ALL",
    archiveScope: String = "unarchived"
) {
    val bookmarks by viewModel.allBookmarks.collectAsState()
    val isDbLoaded by viewModel.isLoaded.collectAsState()
    val SumiInk = ZenThemeColors.sumiInk()
    val WashiBg = ZenThemeColors.washiBg()

    // Capture bookmarks at the start of the session so database changes don't disrupt active practice
    val sessionCards = remember(bookmarks, isDbLoaded) { mutableStateOf<List<BookmarkedSegmentDomain>?>(null) }
    if (sessionCards.value == null && isDbLoaded) {
        val filteredByScope = when (archiveScope) {
            "archived" -> bookmarks.filter { it.isArchived }
            "all" -> bookmarks
            else -> bookmarks.filter { !it.isArchived }
        }
        val filtered = filteredByScope.filter {
            posFilter == "ALL" || it.effectivePosCategory == posFilter
        }.shuffled()
        sessionCards.value = if (cardLimit > 0) filtered.take(cardLimit) else filtered
    }
    val initialCards = sessionCards.value ?: emptyList()

    // Smart queue state
    data class CardState(val card: BookmarkedSegmentDomain, val round: Int = 1)

    var pendingQueue by remember { mutableStateOf(mutableListOf<CardState>()) }
    var retryQueue by remember { mutableStateOf(mutableListOf<CardState>()) }
    var masteredIds by remember { mutableStateOf(emptySet<Int>()) }
    var forgotIds by remember { mutableStateOf(emptySet<Int>()) }
    var currentRound by remember { mutableStateOf(1) }
    var hasInitializedQueue by remember { mutableStateOf(false) }

    LaunchedEffect(initialCards) {
        if (initialCards.isNotEmpty() && !hasInitializedQueue) {
            pendingQueue = initialCards.map { CardState(it) }.toMutableList()
            hasInitializedQueue = true
        }
    }

    var currentIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var isMarkedForgot by remember { mutableStateOf(false) }
    var sessionFinished by remember { mutableStateOf(false) }
    var autoTtsEnabled by remember { mutableStateOf(false) }

    // Gesture Animation states
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    // TTS Auto-play logic
    val currentCard = pendingQueue.getOrNull(currentIndex)
    LaunchedEffect(currentIndex, isFlipped, autoTtsEnabled, currentCard) {
        if (currentCard != null && autoTtsEnabled && !sessionFinished) {
            val isRevealed = (studyMode == "ja_to_zh" && !isFlipped) || (studyMode == "zh_to_ja" && isFlipped)
            if (isRevealed) {
                val speakText = currentCard.card.dictionaryForm?.takeIf { it.isNotBlank() } ?: currentCard.card.segmentText
                viewModel.playTts(speakText)
            }
        }
    }

    // Advance to next card
    fun advanceToNext() {
        isMarkedForgot = false
        if (currentIndex < pendingQueue.lastIndex) {
            currentIndex++
            isFlipped = false
        } else {
            // End of current pending queue
            if (retryQueue.isNotEmpty()) {
                // Shuffle retry cards into pending queue for next round
                currentRound++
                pendingQueue = retryQueue.toMutableList().also { it.shuffle() }
                retryQueue = mutableListOf()
                currentIndex = 0
                isFlipped = false
            } else {
                // All done!
                sessionFinished = true
            }
        }
    }

    // Loading State
    if (!isDbLoaded) {
        Box(Modifier.fillMaxSize().background(WashiBg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SumiInk)
        }
        return
    }

    if (initialCards.isEmpty()) {
        Box(Modifier.fillMaxSize().background(WashiBg), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.flashcard_no_cards),
                color = SumiInk.copy(alpha = 0.5f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        return
    }

    if (!sessionFinished && (!hasInitializedQueue || pendingQueue.isEmpty())) {
        Box(Modifier.fillMaxSize().background(WashiBg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SumiInk)
        }
        return
    }

    Scaffold(
        containerColor = WashiBg,
        topBar = {
            TopAppBar(
                title = {
                    if (!sessionFinished) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${currentIndex + 1} / ${pendingQueue.size}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = SumiInk
                            )
                            if (currentRound > 1) {
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = SumiInk.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.flashcard_round_label, currentRound),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SumiInk,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            // Progress bar
                            LinearProgressIndicator(
                                progress = (currentIndex + 1f) / pendingQueue.size.coerceAtLeast(1),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(1.5.dp)),
                                color = SumiInk,
                                trackColor = SumiInk.copy(alpha = 0.12f)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "${stringResource(R.string.flashcard_mastered_label)}: ${masteredIds.size}",
                                fontSize = 13.sp,
                                color = SumiInk,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            stringResource(R.string.flashcard_session_complete),
                            fontWeight = FontWeight.Bold,
                            color = SumiInk
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = SumiInk
                        )
                    }
                },
                actions = {
                    if (!sessionFinished) {
                        // Auto TTS Switch
                        IconButton(onClick = { autoTtsEnabled = !autoTtsEnabled }) {
                            Icon(
                                imageVector = if (autoTtsEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = stringResource(R.string.flashcard_auto_tts),
                                tint = if (autoTtsEnabled) SumiInk else SumiInk.copy(alpha = 0.35f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WashiBg)
            )
        }
    ) { padding ->
        if (sessionFinished) {
            ResultSummary(
                modifier = Modifier.padding(padding),
                total = initialCards.size,
                forgotIds = forgotIds,
                initialCards = initialCards,
                viewModel = viewModel,
                onRetryWeak = {
                    val weakCards = initialCards.filter { it.id in forgotIds }
                    pendingQueue = weakCards.shuffled().map { CardState(it) }.toMutableList()
                    retryQueue = mutableListOf()
                    masteredIds = emptySet()
                    forgotIds = emptySet()
                    currentRound = 1
                    currentIndex = 0
                    isFlipped = false
                    isMarkedForgot = false
                    sessionFinished = false
                },
                onRestart = {
                    pendingQueue = initialCards.shuffled().map { CardState(it) }.toMutableList()
                    retryQueue = mutableListOf()
                    masteredIds = emptySet()
                    forgotIds = emptySet()
                    currentRound = 1
                    currentIndex = 0
                    isFlipped = false
                    isMarkedForgot = false
                    sessionFinished = false
                },
                onBack = { navController.popBackStack() }
            )
        } else if (currentCard != null) {
            val card = currentCard
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(16.dp))

                Text(
                    text = if (isFlipped) stringResource(R.string.flashcard_back_hint)
                    else stringResource(R.string.flashcard_front_hint),
                    fontSize = 12.sp,
                    color = SumiInk.copy(alpha = 0.5f)
                )

                Spacer(Modifier.height(12.dp))

                // Draggable card container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .graphicsLayer {
                            translationX = offsetX.value
                            translationY = offsetY.value
                            rotationZ = (offsetX.value / 25f).coerceIn(-12f, 12f)
                        }
                        .pointerInput(currentIndex) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    scope.launch {
                                        offsetX.snapTo(offsetX.value + dragAmount.x)
                                        offsetY.snapTo(offsetY.value + dragAmount.y)
                                    }
                                },
                                onDragEnd = {
                                    val distanceX = offsetX.value
                                    if (isMarkedForgot) {
                                        // Under "Forgot" back state, swiping either side slides off and goes next
                                        if (kotlin.math.abs(distanceX) > 200f) {
                                            scope.launch {
                                                val targetX = if (distanceX > 0) 800f else -800f
                                                offsetX.animateTo(targetX, tween(200))
                                                advanceToNext()
                                                offsetX.snapTo(0f)
                                                offsetY.snapTo(0f)
                                            }
                                        } else {
                                            scope.launch {
                                                offsetX.animateTo(0f, spring())
                                                offsetY.animateTo(0f, spring())
                                            }
                                        }
                                    } else {
                                        if (isFlipped) {
                                            // Back side (not marked forgot)
                                            if (distanceX > 200f) {
                                                // Swipe Right -> Correct / Next -> Mastered
                                                scope.launch {
                                                    offsetX.animateTo(800f, tween(200))
                                                    masteredIds = masteredIds + card.card.id
                                                    advanceToNext()
                                                    offsetX.snapTo(0f)
                                                    offsetY.snapTo(0f)
                                                }
                                            } else if (distanceX < -200f) {
                                                // Swipe Left -> Incorrect / Forgot -> Retry queue
                                                scope.launch {
                                                    offsetX.animateTo(-800f, tween(200))
                                                    if (card.card.id !in retryQueue.map { it.card.id }) {
                                                        retryQueue.add(card)
                                                    }
                                                    forgotIds = forgotIds + card.card.id
                                                    advanceToNext()
                                                    offsetX.snapTo(0f)
                                                    offsetY.snapTo(0f)
                                                }
                                            } else {
                                                scope.launch {
                                                    offsetX.animateTo(0f, spring())
                                                    offsetY.animateTo(0f, spring())
                                                }
                                            }
                                        } else {
                                            // Front side (not marked forgot)
                                            if (distanceX > 200f) {
                                                // Swipe Right -> Reveal answer (Show back side, but keep isMarkedForgot = false)
                                                scope.launch {
                                                    offsetX.animateTo(800f, tween(200))
                                                    isFlipped = true
                                                    offsetX.animateTo(0f, spring())
                                                    offsetY.animateTo(0f, spring())
                                                }
                                            } else if (distanceX < -200f) {
                                                // Swipe Left -> Mark forgot (Show back side in forgot state)
                                                scope.launch {
                                                    offsetX.animateTo(-800f, tween(200))
                                                    if (card.card.id !in retryQueue.map { it.card.id }) {
                                                        retryQueue.add(card)
                                                    }
                                                    forgotIds = forgotIds + card.card.id
                                                    isFlipped = true
                                                    isMarkedForgot = true
                                                    offsetX.animateTo(0f, spring())
                                                    offsetY.animateTo(0f, spring())
                                                }
                                            } else {
                                                // Tap behavior
                                                if (kotlin.math.abs(offsetX.value) < 10f && kotlin.math.abs(offsetY.value) < 10f) {
                                                    isFlipped = !isFlipped
                                                }
                                                scope.launch {
                                                    offsetX.animateTo(0f, spring())
                                                    offsetY.animateTo(0f, spring())
                                                }
                                            }
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch {
                                        offsetX.animateTo(0f, spring())
                                        offsetY.animateTo(0f, spring())
                                    }
                                }
                            )
                        }
                ) {
                    FlipCard(
                        card = card.card,
                        isFlipped = isFlipped,
                        studyMode = studyMode,
                        onPlayTts = { text -> viewModel.playTts(text) },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Swipe overlays (monochrome)
                    if (isMarkedForgot) {
                        val swipeAlpha = (kotlin.math.abs(offsetX.value) / 300f).coerceIn(0f, 0.4f)
                        if (swipeAlpha > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SumiInk.copy(alpha = swipeAlpha)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.flashcard_next_card),
                                    color = WashiBg,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                )
                            }
                        }
                    } else if (isFlipped) {
                        // On the back side (verifying)
                        val leftAlpha = (-offsetX.value / 300f).coerceIn(0f, 0.4f)
                        if (leftAlpha > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SumiInk.copy(alpha = leftAlpha)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.flashcard_eval_oops),
                                    color = WashiBg,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                )
                            }
                        }

                        val rightAlpha = (offsetX.value / 300f).coerceIn(0f, 0.4f)
                        if (rightAlpha > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SumiInk.copy(alpha = rightAlpha)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.flashcard_next_card),
                                    color = WashiBg,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                )
                            }
                        }
                    } else {
                        // On the front side
                        val leftAlpha = (-offsetX.value / 300f).coerceIn(0f, 0.4f)
                        if (leftAlpha > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SumiInk.copy(alpha = leftAlpha)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.flashcard_forgot),
                                    color = WashiBg,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                )
                            }
                        }

                        val rightAlpha = (offsetX.value / 300f).coerceIn(0f, 0.4f)
                        if (rightAlpha > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SumiInk.copy(alpha = rightAlpha)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.flashcard_show_answer),
                                    color = WashiBg,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Bottom Action Buttons Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isMarkedForgot) {
                        // State 3: Marked forgot (Back side) -> Only next button shown
                        Button(
                            onClick = { advanceToNext() },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SumiInk,
                                contentColor = WashiBg
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.flashcard_next_card),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    } else if (isFlipped) {
                        // State 2: Back side (but not marked forgot) -> Show "记错了" and "下一张"
                        OutlinedButton(
                            onClick = {
                                if (card.card.id !in retryQueue.map { it.card.id }) {
                                    retryQueue.add(card)
                                }
                                forgotIds = forgotIds + card.card.id
                                advanceToNext()
                            },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SumiInk)
                        ) {
                            Text(
                                text = stringResource(R.string.flashcard_eval_oops),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }

                        Button(
                            onClick = {
                                masteredIds = masteredIds + card.card.id
                                scope.launch {
                                    offsetX.animateTo(800f, tween(150))
                                    advanceToNext()
                                    offsetX.snapTo(0f)
                                }
                            },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SumiInk,
                                contentColor = WashiBg
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.flashcard_next_card),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    } else {
                        // State 1: Front side -> Show "不认识" and "查看答案"
                        OutlinedButton(
                            onClick = {
                                if (card.card.id !in retryQueue.map { it.card.id }) {
                                    retryQueue.add(card)
                                }
                                forgotIds = forgotIds + card.card.id
                                isFlipped = true
                                isMarkedForgot = true
                            },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SumiInk)
                        ) {
                            Text(
                                text = stringResource(R.string.flashcard_forgot),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }

                        Button(
                            onClick = { isFlipped = true },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SumiInk,
                                contentColor = WashiBg
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.flashcard_show_answer),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlipCard(
    card: BookmarkedSegmentDomain,
    isFlipped: Boolean,
    studyMode: String,
    onPlayTts: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val SumiInk = ZenThemeColors.sumiInk()
    val SurfaceColor = ZenThemeColors.cardBg()

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        color = SurfaceColor,
        border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.12f))
    ) {
        Crossfade(
            targetState = isFlipped,
            animationSpec = tween(durationMillis = 150, easing = LinearOutSlowInEasing),
            label = "cardFlip"
        ) { flipped ->
            if (!flipped) {
                // FRONT FACE
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (studyMode == "ja_to_zh") {
                        if (card.sourceText.isNotBlank()) {
                            Text(
                                text = card.sourceText.take(30) + if (card.sourceText.length > 30) "…" else "",
                                fontSize = 13.sp,
                                color = SumiInk.copy(alpha = 0.4f),
                                fontWeight = FontWeight.Light,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        val displayWord = card.dictionaryForm?.takeIf { it.isNotBlank() } ?: card.segmentText
                        val wordFontSize = when {
                            displayWord.length <= 4 -> 40.sp
                            displayWord.length <= 7 -> 30.sp
                            displayWord.length <= 10 -> 24.sp
                            else -> 20.sp
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = displayWord,
                                fontSize = wordFontSize,
                                lineHeight = wordFontSize * 1.2f,
                                fontWeight = FontWeight.Bold,
                                color = SumiInk,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = { onPlayTts(displayWord) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = stringResource(R.string.flashcard_speak),
                                    tint = SumiInk
                                )
                            }
                        }
                    } else {
                        // FRONT: Chinese Meaning & POS (Recall Mode)
                        if (!card.partOfSpeech.isNullOrBlank()) {
                            Surface(
                                color = SumiInk.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = card.partOfSpeech,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SumiInk,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                        }

                        Text(
                            text = card.meaning ?: stringResource(R.string.flashcard_no_meaning),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = SumiInk,
                            textAlign = TextAlign.Center,
                            lineHeight = 34.sp
                        )

                        if (card.sourceText.isNotBlank()) {
                            Spacer(Modifier.height(24.dp))
                            val targetWord = card.surfaceForm?.takeIf { it.isNotBlank() } ?: card.segmentText
                            val maskedText = if (targetWord.isNotBlank()) {
                                card.sourceText.replace(targetWord, " ____ ")
                            } else {
                                card.sourceText
                            }
                            Surface(
                                color = SumiInk.copy(alpha = 0.04f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = maskedText,
                                    fontSize = 12.sp,
                                    color = SumiInk.copy(alpha = 0.45f),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // BACK FACE
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (studyMode == "ja_to_zh") {
                        if (!card.partOfSpeech.isNullOrBlank()) {
                            Surface(
                                color = SumiInk.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = card.partOfSpeech,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SumiInk,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                        }

                        Text(
                            text = card.meaning ?: stringResource(R.string.flashcard_no_meaning),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = SumiInk,
                            textAlign = TextAlign.Center,
                            lineHeight = 30.sp
                        )

                        val displayReading = card.reading
                        if (!displayReading.isNullOrBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = displayReading,
                                fontSize = 16.sp,
                                color = SumiInk.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Normal
                            )
                        }

                        val extras = buildList {
                            card.inflection?.let { add(stringResource(R.string.flashcard_inflection_label, it)) }
                            card.role?.let { add(stringResource(R.string.flashcard_role_label, it)) }
                            card.dictionaryForm?.takeIf { it != card.segmentText }?.let {
                                add(stringResource(R.string.flashcard_dict_form_label, it))
                            }
                        }
                        if (extras.isNotEmpty()) {
                            Spacer(Modifier.height(20.dp))
                            Divider(color = SumiInk.copy(alpha = 0.1f))
                            Spacer(Modifier.height(12.dp))
                            extras.forEach { line ->
                                Text(
                                    text = line,
                                    fontSize = 13.sp,
                                    color = SumiInk.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        if (card.sourceText.isNotBlank()) {
                            Spacer(Modifier.height(16.dp))
                            Surface(
                                color = SumiInk.copy(alpha = 0.04f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = card.sourceText,
                                    fontSize = 12.sp,
                                    color = SumiInk.copy(alpha = 0.45f),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    } else {
                        // BACK: Japanese spelling, reading, and unmasked sentence
                        val displayWord = card.dictionaryForm?.takeIf { it.isNotBlank() } ?: card.segmentText
                        val wordFontSize = when {
                            displayWord.length <= 4 -> 36.sp
                            displayWord.length <= 7 -> 28.sp
                            displayWord.length <= 10 -> 22.sp
                            else -> 18.sp
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = displayWord,
                                fontSize = wordFontSize,
                                lineHeight = wordFontSize * 1.2f,
                                fontWeight = FontWeight.Bold,
                                color = SumiInk,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = { onPlayTts(displayWord) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = stringResource(R.string.flashcard_speak),
                                    tint = SumiInk
                                )
                            }
                        }

                        val displayReading = card.reading
                        if (!displayReading.isNullOrBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = displayReading,
                                fontSize = 16.sp,
                                color = SumiInk.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Normal
                            )
                        }

                        val extras = buildList {
                            card.inflection?.let { add(stringResource(R.string.flashcard_inflection_label, it)) }
                            card.role?.let { add(stringResource(R.string.flashcard_role_label, it)) }
                            card.dictionaryForm?.takeIf { it != card.segmentText }?.let {
                                add(stringResource(R.string.flashcard_dict_form_label, it))
                            }
                        }
                        if (extras.isNotEmpty()) {
                            Spacer(Modifier.height(20.dp))
                            Divider(color = SumiInk.copy(alpha = 0.1f))
                            Spacer(Modifier.height(12.dp))
                            extras.forEach { line ->
                                Text(
                                    text = line,
                                    fontSize = 13.sp,
                                    color = SumiInk.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        if (card.sourceText.isNotBlank()) {
                            Spacer(Modifier.height(16.dp))
                            Surface(
                                color = SumiInk.copy(alpha = 0.04f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = card.sourceText,
                                    fontSize = 12.sp,
                                    color = SumiInk.copy(alpha = 0.45f),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultSummary(
    modifier: Modifier = Modifier,
    total: Int,
    forgotIds: Set<Int>,
    initialCards: List<BookmarkedSegmentDomain>,
    viewModel: BookmarkViewModel,
    onRetryWeak: () -> Unit,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    val SumiInk = ZenThemeColors.sumiInk()
    val SurfaceColor = ZenThemeColors.cardBg()
    val context = LocalContext.current
    var archivedState by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SumiInk,
                modifier = Modifier.size(56.dp)
            )
            Text(
                stringResource(R.string.flashcard_session_complete),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = SumiInk
            )

            Text(
                text = stringResource(R.string.flashcard_summary_total, total),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = SumiInk,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(Modifier.height(8.dp))

            // Archive mastered cards manually instead of auto-archiving
            val actuallyMasteredIds = initialCards.map { it.id }.filter { it !in forgotIds }.toSet()
            if (actuallyMasteredIds.isNotEmpty() && !archivedState) {
                Button(
                    onClick = {
                        viewModel.archiveMasteredCards(actuallyMasteredIds)
                        Toast.makeText(
                            context,
                            context.getString(R.string.flashcard_archive_success),
                            Toast.LENGTH_SHORT
                        ).show()
                        archivedState = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SumiInk,
                        contentColor = SurfaceColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(
                        stringResource(R.string.flashcard_archive_mastered_btn),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            // Retry weak cards
            if (forgotIds.isNotEmpty()) {
                OutlinedButton(
                    onClick = onRetryWeak,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SumiInk),
                    border = BorderStroke(1.dp, SumiInk),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(
                        stringResource(R.string.flashcard_retry_weak, forgotIds.size),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SumiInk,
                    contentColor = SurfaceColor
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(stringResource(R.string.flashcard_restart), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.2f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SumiInk),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(stringResource(R.string.flashcard_back_to_bookmarks), fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }

            val weakCards = initialCards.filter { it.id in forgotIds }
            val strongCards = initialCards.filter { it.id !in forgotIds }

            // Unmastered Word List section
            if (weakCards.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Divider(color = SumiInk.copy(alpha = 0.12f))
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.flashcard_unmastered_section, weakCards.size),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SumiInk,
                    modifier = Modifier.align(Alignment.Start)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                ) {
                    weakCards.forEach { card ->
                        WordItemRow(card = card, SumiInk = SumiInk, SurfaceColor = SurfaceColor, viewModel = viewModel)
                    }
                }
            }

            // Mastered Word List section
            if (strongCards.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Divider(color = SumiInk.copy(alpha = 0.12f))
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.flashcard_mastered_section, strongCards.size),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SumiInk,
                    modifier = Modifier.align(Alignment.Start)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                ) {
                    strongCards.forEach { card ->
                        WordItemRow(card = card, SumiInk = SumiInk, SurfaceColor = SurfaceColor, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun WordItemRow(
    card: BookmarkedSegmentDomain,
    SumiInk: Color,
    SurfaceColor: Color,
    viewModel: BookmarkViewModel
) {
    Surface(
        color = SurfaceColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SumiInk.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val displayWord = card.dictionaryForm?.takeIf { it.isNotBlank() } ?: card.segmentText
                Text(
                    text = displayWord,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SumiInk
                )
                val displayReading = card.reading
                if (!displayReading.isNullOrBlank() && displayReading != displayWord) {
                    Text(
                        text = displayReading,
                        fontSize = 12.sp,
                        color = SumiInk.copy(alpha = 0.5f)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = card.meaning ?: stringResource(R.string.flashcard_no_meaning),
                    fontSize = 13.sp,
                    color = SumiInk.copy(alpha = 0.7f)
                )
            }

            IconButton(
                onClick = {
                    val speakText = card.dictionaryForm?.takeIf { it.isNotBlank() } ?: card.segmentText
                    viewModel.playTts(speakText)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = stringResource(R.string.flashcard_speak),
                    tint = SumiInk
                )
            }
        }
    }
}
