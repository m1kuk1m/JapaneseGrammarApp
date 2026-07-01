package com.example.japanesegrammarapp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.japanesegrammarapp.MainActivity
import com.example.japanesegrammarapp.domain.model.AnalysisDomainRecord
import com.example.japanesegrammarapp.domain.model.AnalysisStatus
import com.example.japanesegrammarapp.domain.model.BookmarkedSentenceDomain
import com.example.japanesegrammarapp.domain.model.DetailedAnalysisResult
import com.example.japanesegrammarapp.domain.model.DetailedGrammarPoint
import com.example.japanesegrammarapp.domain.model.SentenceClause
import com.example.japanesegrammarapp.domain.model.WordSegment
import com.example.japanesegrammarapp.domain.repository.UiPreferencesRepository
import com.example.japanesegrammarapp.ui.screens.CameraPermissionState
import com.example.japanesegrammarapp.ui.screens.components.WorkspaceResultContent
import com.example.japanesegrammarapp.ui.statistics.DetailedSentenceItem
import com.example.japanesegrammarapp.ui.theme.AppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppNavigationSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun opensSettingsAndReturnsToWorkspace() {
        composeRule.onNodeWithTag("workspace-screen").assertIsDisplayed()

        composeRule.onNodeWithTag("workspace-settings-button").performClick()
        composeRule.onNodeWithTag("settings-screen").assertIsDisplayed()

        composeRule.onNodeWithTag("settings-back-button").performClick()
        composeRule.onNodeWithTag("workspace-screen").assertIsDisplayed()
    }

    @Test
    fun opensBookmarksFromWorkspace() {
        composeRule.onNodeWithTag("workspace-screen").assertIsDisplayed()

        composeRule.onNodeWithTag("workspace-bookmarks-button").performClick()
        composeRule.onNodeWithTag("bookmarks-screen").assertIsDisplayed()
    }

    @Test
    fun opensStatisticsWhileHistoryDrawerIsClosing() {
        composeRule.onNodeWithTag("workspace-screen").assertIsDisplayed()

        composeRule.mainClock.autoAdvance = false
        try {
            composeRule.onNodeWithTag("workspace-history-button").performClick()
            composeRule.mainClock.advanceTimeBy(250)
            composeRule.onNodeWithTag("history-sidebar").assertIsDisplayed()

            composeRule.onNodeWithTag("history-statistics-button").performClick()
            composeRule.mainClock.advanceTimeBy(100)

            composeRule.onNodeWithTag("statistics-screen").assertIsDisplayed()

            composeRule.mainClock.advanceTimeBy(500)
            composeRule.waitForIdle()

            composeRule.onAllNodesWithTag("history-sidebar").assertCountEquals(0)
            composeRule.onNodeWithTag("statistics-screen").assertIsDisplayed()
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }
}

@RunWith(AndroidJUnit4::class)
class AppComponentSmokeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsCameraNoPermissionState() {
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                CameraPermissionState(
                    showGoToSettings = false,
                    onRequestPermission = {},
                    onOpenSettings = {},
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithTag("camera-permission-state").assertIsDisplayed()
    }

    @Test
    fun showsAnalysisResultContent() {
        composeRule.setContent {
            AppThemeWithoutWindowSideEffects {
                WorkspaceResultContent(
                    uiState = WorkspaceUiState(
                        selectedRecord = AnalysisDomainRecord(
                            id = 1,
                            originalText = "日本語を勉強します",
                            analysisResult = "result",
                            modelUsed = "Smoke: test",
                            status = AnalysisStatus.COMPLETED
                        ),
                        detailedResult = DetailedAnalysisResult(
                            translation = "Study Japanese.",
                            segments = listOf(
                                WordSegment(
                                    text = "勉強",
                                    reading = "べんきょう",
                                    partOfSpeech = "名詞",
                                    posCategory = "NOUN",
                                    dictionaryForm = "勉強",
                                    meaning = "study"
                                )
                            ),
                            clauses = listOf(
                                SentenceClause(
                                    index = 1,
                                    role = "predicate",
                                    text = "勉強します",
                                    explanation = "Polite verb phrase"
                                )
                            ),
                            grammarPoints = listOf(
                                DetailedGrammarPoint(
                                    pattern = "を + します",
                                    explanation = "Marks the object of the action."
                                )
                            )
                        )
                    ),
                    uiPreferencesRepository = InMemoryUiPreferencesRepository()
                )
            }
        }

        composeRule.onNodeWithTag("workspace-result-content").assertIsDisplayed()
    }

    @Test
    fun wordSegmentPopupOpensAndCloses() {
        composeRule.setContent {
            AppThemeWithoutWindowSideEffects {
                WorkspaceResultContent(
                    uiState = WorkspaceUiState(
                        selectedRecord = AnalysisDomainRecord(
                            id = 1,
                            originalText = "日本語を勉強します",
                            analysisResult = "result",
                            modelUsed = "Smoke: test",
                            status = AnalysisStatus.COMPLETED
                        ),
                        detailedResult = DetailedAnalysisResult(
                            translation = "Study Japanese.",
                            segments = listOf(
                                WordSegment(
                                    text = "勉強",
                                    reading = "べんきょう",
                                    partOfSpeech = "名詞",
                                    posCategory = "NOUN",
                                    dictionaryForm = "勉強",
                                    meaning = "study"
                                )
                            )
                        ),
                        cardDetailDisplayMode = "POPUP"
                    ),
                    uiPreferencesRepository = InMemoryUiPreferencesRepository()
                )
            }
        }

        composeRule.onNodeWithText("勉強").performClick()

        composeRule.onNodeWithTag("segment-detail-popup").assertIsDisplayed()
        composeRule.onNodeWithText("study").assertIsDisplayed()

        composeRule.onNodeWithTag("segment-detail-popup-close").performClick()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag("segment-detail-popup").assertCountEquals(0)
    }

    @Test
    fun statisticsSheetSentenceNavigateButtonInvokesCallback() {
        var clicked = false

        composeRule.setContent {
            AppThemeWithoutWindowSideEffects {
                DetailedSentenceItem(
                    record = BookmarkedSentenceDomain(
                        id = 2,
                        recordId = 1,
                        originalText = "日本語を勉強します",
                        translation = "Study Japanese.",
                        analysisResult = null,
                        modelUsed = null,
                        bookmarkedAt = 1L
                    ),
                    modifier = Modifier.testTag("statistics-sheet-analyzed-sentence"),
                    navigateButtonModifier = Modifier.testTag("statistics-sheet-analyzed-sentence-navigate"),
                    onClickNavigate = { clicked = true }
                )
            }
        }

        composeRule.onNodeWithTag("statistics-sheet-analyzed-sentence-navigate").performClick()
        composeRule.runOnIdle {
            assertTrue(clicked)
        }
    }

    @Composable
    private fun AppThemeWithoutWindowSideEffects(content: @Composable () -> Unit) {
        MaterialTheme(content = content)
    }
}

private class InMemoryUiPreferencesRepository : UiPreferencesRepository {
    private var x: Float = 0f
    private var y: Float = 0f
    private var mode: String = "compact"
    private var dictionary: String = "Weblio"
    private var cropInteraction: String = "drag"

    override fun getFloatingActionBallX(defaultValue: Float): Float = x.takeIf { it != 0f } ?: defaultValue

    override fun getFloatingActionBallY(defaultValue: Float): Float = y.takeIf { it != 0f } ?: defaultValue

    override fun saveFloatingActionBallPosition(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    override fun getFloatingActionBallMode(defaultValue: String): String = mode.ifBlank { defaultValue }

    override fun saveFloatingActionBallMode(mode: String) {
        this.mode = mode
    }

    override fun getLastDictionary(defaultValue: String): String = dictionary.ifBlank { defaultValue }

    override fun saveLastDictionary(dictionary: String) {
        this.dictionary = dictionary
    }

    override fun getCropInteraction(defaultValue: String): String = cropInteraction.ifBlank { defaultValue }

    override fun saveCropInteraction(mode: String) {
        cropInteraction = mode
    }
}
