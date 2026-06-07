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
    fun migration8To9KeepsValidBookmarksAndDropsOrphans() {
        createVersion8Database().close()

        val db = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .build()

        val sqlDb = db.openHelper.writableDatabase
        assertEquals(1L, sqlDb.longForQuery("SELECT COUNT(*) FROM bookmarked_segments"))
        assertEquals(1L, sqlDb.longForQuery("SELECT COUNT(*) FROM bookmarked_segments WHERE recordId = 1"))

        sqlDb.execSQL("DELETE FROM analysis_records WHERE id = 1")
        assertEquals(0L, sqlDb.longForQuery("SELECT COUNT(*) FROM bookmarked_segments"))

        db.close()
    }

    private fun createVersion8Database(): SupportSQLiteDatabase {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(TEST_DB)
                .callback(object : SupportSQLiteOpenHelper.Callback(8) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        createVersion8Schema(db)
                        seedVersion8Data(db)
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        return helper.writableDatabase
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

    private fun seedVersion8Data(db: SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT INTO analysis_records (id, originalText, imageUri, analysisResult, timestamp, modelUsed, status, errorMessage, consumedTokens, inputTokens, outputTokens) " +
                "VALUES (1, '食べます', NULL, '{}', 1000, 'Gemini: test', 'COMPLETED', NULL, 3, 1, 2)"
        )
        db.execSQL(
            "INSERT INTO bookmarked_segments (id, recordId, segmentText, surfaceForm, reading, partOfSpeech, posCategory, dictionaryForm, dictionaryFormReading, meaning, inflection, role, bookmarkedAt, sourceText, isArchived) " +
                "VALUES (1, 1, '食べる', '食べます', 'たべます', 'verb', 'verb', '食べる', 'たべる', 'eat', NULL, NULL, 1000, '食べます', 0)"
        )
        db.execSQL(
            "INSERT INTO bookmarked_segments (id, recordId, segmentText, surfaceForm, reading, partOfSpeech, posCategory, dictionaryForm, dictionaryFormReading, meaning, inflection, role, bookmarkedAt, sourceText, isArchived) " +
                "VALUES (2, 999, '見る', '見ます', 'みます', 'verb', 'verb', '見る', 'みる', 'see', NULL, NULL, 1000, '見ます', 0)"
        )
    }

    private fun SupportSQLiteDatabase.longForQuery(sql: String): Long {
        query(sql).use { cursor ->
            check(cursor.moveToFirst())
            return cursor.getLong(0)
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
