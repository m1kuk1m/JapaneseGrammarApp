package com.example.japanesegrammarapp.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun migration1To9PreservesAnalysisRecordAndCreatesBookmarkTables() {
        createDatabase(version = 1, onCreate = { db ->
            createVersion1Schema(db)
            db.execSQL(
                "INSERT INTO analysis_records (id, originalText, imageUri, analysisResult, timestamp, modelUsed) " +
                    "VALUES (1, 'legacy text', NULL, '{}', 1000, 'Gemini: test')"
            )
        }).close()

        val db = openMigratedDatabase()
        try {
            val sqlDb = db.openHelper.writableDatabase
            assertEquals(1L, sqlDb.longForQuery("SELECT COUNT(*) FROM analysis_records"))
            assertEquals("COMPLETED", sqlDb.stringForQuery("SELECT status FROM analysis_records WHERE id = 1"))
            assertEquals(0L, sqlDb.longForQuery("SELECT consumedTokens FROM analysis_records WHERE id = 1"))
            assertEquals(0L, sqlDb.longForQuery("SELECT inputTokens FROM analysis_records WHERE id = 1"))
            assertEquals(0L, sqlDb.longForQuery("SELECT outputTokens FROM analysis_records WHERE id = 1"))
            assertEquals(0L, sqlDb.longForQuery("SELECT COUNT(*) FROM bookmarked_segments"))
            assertEquals(0L, sqlDb.longForQuery("SELECT COUNT(*) FROM bookmarked_sentences"))
        } finally {
            db.close()
        }
    }

    @Test
    fun migration3To9SplitsLegacyTokenUsageAndKeepsFailureState() {
        createDatabase(version = 3, onCreate = { db ->
            createVersion3Schema(db)
            db.execSQL(
                "INSERT INTO analysis_records (id, originalText, imageUri, analysisResult, timestamp, modelUsed, status, errorMessage, consumedTokens) " +
                    "VALUES (1, 'legacy token text', NULL, '{}', 1000, 'Gemini: test', 'FAILED', 'legacy error', 10)"
            )
        }).close()

        val db = openMigratedDatabase()
        try {
            val sqlDb = db.openHelper.writableDatabase
            assertEquals(10L, sqlDb.longForQuery("SELECT consumedTokens FROM analysis_records WHERE id = 1"))
            assertEquals(6L, sqlDb.longForQuery("SELECT inputTokens FROM analysis_records WHERE id = 1"))
            assertEquals(4L, sqlDb.longForQuery("SELECT outputTokens FROM analysis_records WHERE id = 1"))
            assertEquals("FAILED", sqlDb.stringForQuery("SELECT status FROM analysis_records WHERE id = 1"))
            assertEquals("legacy error", sqlDb.stringForQuery("SELECT errorMessage FROM analysis_records WHERE id = 1"))
        } finally {
            db.close()
        }
    }

    @Test
    fun migration8To13KeepsValidBookmarksAndPreservesFavoritesAfterHistoryDelete() {
        createDatabase(version = 8, onCreate = { db ->
            createVersion8Schema(db)
            seedVersion8Data(db)
        }).close()

        val db = openMigratedDatabase()
        try {
            val sqlDb = db.openHelper.writableDatabase
            assertEquals(1L, sqlDb.longForQuery("SELECT COUNT(*) FROM bookmarked_segments"))
            assertEquals(1L, sqlDb.longForQuery("SELECT COUNT(*) FROM bookmarked_segments WHERE recordId = 1"))
            assertEquals(1L, sqlDb.longForQuery("SELECT COUNT(*) FROM bookmarked_sentences WHERE recordId = 1"))

            sqlDb.execSQL("DELETE FROM analysis_records WHERE id = 1")
            assertEquals(1L, sqlDb.longForQuery("SELECT COUNT(*) FROM bookmarked_segments"))
            assertEquals(1L, sqlDb.longForQuery("SELECT COUNT(*) FROM bookmarked_sentences"))
        } finally {
            db.close()
        }
    }

    @Test
    fun migration9To13CreatesGrammarPointsTableAndPreservesAfterHistoryDelete() {
        createDatabase(version = 9, onCreate = { db ->
            createVersion9Schema(db)
        }).close()

        val db = openMigratedDatabase()
        try {
            val sqlDb = db.openHelper.writableDatabase
            assertEquals(0L, sqlDb.longForQuery("SELECT COUNT(*) FROM bookmarked_grammar_points"))
            
            // Insert a test grammar point to verify the schema is correct
            sqlDb.execSQL("INSERT INTO analysis_records (id, originalText, imageUri, analysisResult, timestamp, modelUsed, status, consumedTokens, inputTokens, outputTokens) " +
                    "VALUES (1, 'grammar test', NULL, '{}', 1000, 'Gemini: test', 'COMPLETED', 3, 1, 2)")
            sqlDb.execSQL(
                "INSERT INTO bookmarked_grammar_points (recordId, pattern, explanation, bookmarkedAt, sourceText, isArchived) " +
                "VALUES (1, '~te iru', 'Ongoing action', 1000, 'tabete iru', 0)"
            )
            assertEquals(1L, sqlDb.longForQuery("SELECT COUNT(*) FROM bookmarked_grammar_points"))
            
            // Favorites are snapshots and should survive history deletion.
            sqlDb.execSQL("PRAGMA foreign_keys=ON")
            sqlDb.execSQL("DELETE FROM analysis_records WHERE id = 1")
            assertEquals(1L, sqlDb.longForQuery("SELECT COUNT(*) FROM bookmarked_grammar_points"))
        } finally {
            db.close()
        }
    }

    @Test
    fun migration12To13AllowsImportedOrphanWordSentenceAndGrammarFavorites() {
        createDatabase(version = 12, onCreate = { db ->
            createVersion12Schema(db)
        }).close()

        val db = openMigratedDatabase()
        try {
            val sqlDb = db.openHelper.writableDatabase
            sqlDb.execSQL(
                "INSERT INTO bookmarked_segments (recordId, segmentText, surfaceForm, reading, partOfSpeech, posCategory, dictionaryForm, dictionaryFormReading, meaning, inflection, role, bookmarkedAt, sourceText, isArchived) " +
                    "VALUES (-1, 'taberu', 'taberu', 'たべる', 'verb', 'VERB', 'taberu', 'たべる', 'eat', NULL, NULL, 1000, 'source', 0)"
            )
            sqlDb.execSQL(
                "INSERT INTO bookmarked_sentences (recordId, originalText, translation, analysisResult, modelUsed, bookmarkedAt, isArchived) " +
                    "VALUES (-1, '食べる', 'eat', NULL, NULL, 1000, 1)"
            )
            sqlDb.execSQL(
                "INSERT INTO bookmarked_grammar_points (recordId, pattern, explanation, bookmarkedAt, sourceText, isArchived) " +
                    "VALUES (-1, '〜ている', 'ongoing', 1000, 'source', 0)"
            )

            assertEquals(1L, sqlDb.longForQuery("SELECT COUNT(*) FROM bookmarked_segments WHERE recordId = -1"))
            assertEquals(1L, sqlDb.longForQuery("SELECT COUNT(*) FROM bookmarked_sentences WHERE isArchived = 1"))
            assertEquals(1L, sqlDb.longForQuery("SELECT COUNT(*) FROM bookmarked_grammar_points WHERE recordId = -1"))
        } finally {
            db.close()
        }
    }

    @Test
    fun migration13To14KeepsFtsIndexSyncedAfterRecordChanges() {
        createDatabase(version = 13, onCreate = { db ->
            createVersion13Schema(db)
            db.execSQL(
                "INSERT INTO analysis_records (id, originalText, imageUri, analysisResult, timestamp, modelUsed, status, errorMessage, consumedTokens, inputTokens, outputTokens, isRead) " +
                    "VALUES (1, 'before migration', NULL, '{}', 1000, 'Gemini: test', 'COMPLETED', NULL, 0, 0, 0, 1)"
            )
            db.execSQL("INSERT INTO analysis_records_fts(analysis_records_fts) VALUES('rebuild')")
        }).close()

        val db = openMigratedDatabase()
        try {
            val sqlDb = db.openHelper.writableDatabase
            assertEquals(4L, sqlDb.longForQuery("SELECT COUNT(*) FROM sqlite_master WHERE type = 'trigger' AND name LIKE 'room_fts_content_sync_analysis_records_fts_%'"))

            sqlDb.execSQL(
                "INSERT INTO analysis_records (id, originalText, imageUri, analysisResult, timestamp, modelUsed, status, errorMessage, consumedTokens, inputTokens, outputTokens, isRead) " +
                    "VALUES (2, 'new searchable text', NULL, '{}', 2000, 'Gemini: test', 'COMPLETED', NULL, 0, 0, 0, 0)"
            )
            assertEquals(1L, sqlDb.longForQuery("SELECT COUNT(*) FROM analysis_records_fts WHERE analysis_records_fts MATCH 'searchable'"))

            sqlDb.execSQL("UPDATE analysis_records SET originalText = 'updated searchable text' WHERE id = 2")
            assertEquals(0L, sqlDb.longForQuery("SELECT COUNT(*) FROM analysis_records_fts WHERE analysis_records_fts MATCH 'new'"))
            assertEquals(1L, sqlDb.longForQuery("SELECT COUNT(*) FROM analysis_records_fts WHERE analysis_records_fts MATCH 'updated'"))

            sqlDb.execSQL("DELETE FROM analysis_records WHERE id = 2")
            assertEquals(0L, sqlDb.longForQuery("SELECT COUNT(*) FROM analysis_records_fts WHERE analysis_records_fts MATCH 'updated'"))
        } finally {
            db.close()
        }
    }

    private fun createDatabase(
        version: Int,
        onCreate: (SupportSQLiteDatabase) -> Unit
    ): SupportSQLiteDatabase {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(TEST_DB)
                .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                    override fun onCreate(db: SupportSQLiteDatabase) = onCreate(db)

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        return helper.writableDatabase
    }

    private fun openMigratedDatabase(): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .build()
    }

    private fun createVersion1Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE analysis_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                originalText TEXT NOT NULL,
                imageUri TEXT,
                analysisResult TEXT,
                timestamp INTEGER NOT NULL,
                modelUsed TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    private fun createVersion3Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE analysis_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                originalText TEXT NOT NULL,
                imageUri TEXT,
                analysisResult TEXT,
                timestamp INTEGER NOT NULL,
                modelUsed TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'COMPLETED',
                errorMessage TEXT,
                consumedTokens INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    private fun createVersion8Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE analysis_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                originalText TEXT NOT NULL,
                imageUri TEXT,
                analysisResult TEXT,
                timestamp INTEGER NOT NULL,
                modelUsed TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'COMPLETED',
                errorMessage TEXT,
                consumedTokens INTEGER NOT NULL DEFAULT 0,
                inputTokens INTEGER NOT NULL DEFAULT 0,
                outputTokens INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE bookmarked_segments (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                recordId INTEGER NOT NULL,
                segmentText TEXT NOT NULL,
                surfaceForm TEXT,
                reading TEXT,
                partOfSpeech TEXT,
                posCategory TEXT,
                dictionaryForm TEXT,
                dictionaryFormReading TEXT,
                meaning TEXT,
                inflection TEXT,
                role TEXT,
                bookmarkedAt INTEGER NOT NULL,
                sourceText TEXT NOT NULL DEFAULT '',
                isArchived INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX index_bookmarked_segments_recordId_surfaceForm_dictForm " +
                "ON bookmarked_segments (recordId, surfaceForm, dictionaryForm)"
        )
        db.execSQL(
            """
            CREATE TABLE bookmarked_sentences (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                recordId INTEGER NOT NULL,
                originalText TEXT NOT NULL,
                translation TEXT,
                analysisResult TEXT,
                modelUsed TEXT,
                bookmarkedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX index_bookmarked_sentences_recordId ON bookmarked_sentences (recordId)")
    }

    private fun createVersion9Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE analysis_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                originalText TEXT NOT NULL,
                imageUri TEXT,
                analysisResult TEXT,
                timestamp INTEGER NOT NULL,
                modelUsed TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'COMPLETED',
                errorMessage TEXT,
                consumedTokens INTEGER NOT NULL DEFAULT 0,
                inputTokens INTEGER NOT NULL DEFAULT 0,
                outputTokens INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE bookmarked_segments (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                recordId INTEGER NOT NULL,
                segmentText TEXT NOT NULL,
                surfaceForm TEXT,
                reading TEXT,
                partOfSpeech TEXT,
                posCategory TEXT,
                dictionaryForm TEXT,
                dictionaryFormReading TEXT,
                meaning TEXT,
                inflection TEXT,
                role TEXT,
                bookmarkedAt INTEGER NOT NULL,
                sourceText TEXT NOT NULL DEFAULT '',
                isArchived INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(recordId) REFERENCES analysis_records(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_bookmarked_segments_recordId_surfaceForm_dictionaryForm " +
                "ON bookmarked_segments (recordId, surfaceForm, dictionaryForm)"
        )
        db.execSQL(
            """
            CREATE TABLE bookmarked_sentences (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                recordId INTEGER NOT NULL,
                originalText TEXT NOT NULL,
                translation TEXT,
                analysisResult TEXT,
                modelUsed TEXT,
                bookmarkedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarked_sentences_recordId ON bookmarked_sentences (recordId)")
    }

    private fun createVersion12Schema(db: SupportSQLiteDatabase) {
        createVersion9Schema(db)
        db.execSQL(
            """
            CREATE TABLE bookmarked_grammar_points (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                recordId INTEGER NOT NULL,
                pattern TEXT NOT NULL,
                explanation TEXT,
                bookmarkedAt INTEGER NOT NULL,
                sourceText TEXT NOT NULL DEFAULT '',
                isArchived INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(recordId) REFERENCES analysis_records(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_bookmarked_grammar_points_recordId_pattern ON bookmarked_grammar_points (recordId, pattern)")
        db.execSQL("ALTER TABLE analysis_records ADD COLUMN isRead INTEGER NOT NULL DEFAULT 1")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_analysis_records_timestamp ON analysis_records (timestamp)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_analysis_records_consumedTokens ON analysis_records (consumedTokens)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_analysis_records_modelUsed ON analysis_records (modelUsed)")
        db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS analysis_records_fts USING FTS4(originalText, analysisResult, content=analysis_records, tokenize=unicode61)")
    }

    private fun createVersion13Schema(db: SupportSQLiteDatabase) {
        createVersion12Schema(db)
        db.execSQL("ALTER TABLE bookmarked_sentences ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            """
            CREATE TABLE bookmarked_grammar_points_temp (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                recordId INTEGER NOT NULL,
                pattern TEXT NOT NULL,
                explanation TEXT,
                bookmarkedAt INTEGER NOT NULL,
                sourceText TEXT NOT NULL DEFAULT '',
                isArchived INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("DROP TABLE bookmarked_grammar_points")
        db.execSQL("ALTER TABLE bookmarked_grammar_points_temp RENAME TO bookmarked_grammar_points")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_bookmarked_grammar_points_recordId_pattern ON bookmarked_grammar_points (recordId, pattern)")
    }

    private fun seedVersion8Data(db: SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT INTO analysis_records (id, originalText, imageUri, analysisResult, timestamp, modelUsed, status, errorMessage, consumedTokens, inputTokens, outputTokens) " +
                "VALUES (1, 'tabemasu', NULL, '{}', 1000, 'Gemini: test', 'COMPLETED', NULL, 3, 1, 2)"
        )
        db.execSQL(
            "INSERT INTO bookmarked_segments (id, recordId, segmentText, surfaceForm, reading, partOfSpeech, posCategory, dictionaryForm, dictionaryFormReading, meaning, inflection, role, bookmarkedAt, sourceText, isArchived) " +
                "VALUES (1, 1, 'taberu', 'tabemasu', 'tabemasu', 'verb', 'verb', 'taberu', 'taberu', 'eat', NULL, NULL, 1000, 'tabemasu', 0)"
        )
        db.execSQL(
            "INSERT INTO bookmarked_segments (id, recordId, segmentText, surfaceForm, reading, partOfSpeech, posCategory, dictionaryForm, dictionaryFormReading, meaning, inflection, role, bookmarkedAt, sourceText, isArchived) " +
                "VALUES (2, 999, 'miru', 'mimasu', 'mimasu', 'verb', 'verb', 'miru', 'miru', 'see', NULL, NULL, 1000, 'mimasu', 0)"
        )
        db.execSQL(
            "INSERT INTO bookmarked_sentences (id, recordId, originalText, translation, analysisResult, modelUsed, bookmarkedAt) " +
                "VALUES (1, 1, 'source sentence', 'translation', '{}', 'Gemini: test', 1000)"
        )
    }

    private fun SupportSQLiteDatabase.longForQuery(sql: String): Long {
        query(sql).use { cursor ->
            check(cursor.moveToFirst())
            return cursor.getLong(0)
        }
    }

    private fun SupportSQLiteDatabase.stringForQuery(sql: String): String {
        query(sql).use { cursor ->
            check(cursor.moveToFirst())
            return cursor.getString(0)
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
