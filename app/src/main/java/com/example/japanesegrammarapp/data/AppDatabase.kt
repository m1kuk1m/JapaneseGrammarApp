package com.example.japanesegrammarapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AnalysisRecord::class,
        BookmarkedSegment::class,
        BookmarkedSentence::class,
        BookmarkedGrammarPoint::class
    ],
    version = 11,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun analysisDao(): AnalysisDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun bookmarkedSentenceDao(): BookmarkedSentenceDao
    abstract fun bookmarkedGrammarPointDao(): BookmarkedGrammarPointDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE analysis_records ADD COLUMN status TEXT NOT NULL DEFAULT 'COMPLETED'")
                db.execSQL("ALTER TABLE analysis_records ADD COLUMN errorMessage TEXT")
            }
        }

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE analysis_records ADD COLUMN consumedTokens INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE analysis_records ADD COLUMN inputTokens INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE analysis_records ADD COLUMN outputTokens INTEGER NOT NULL DEFAULT 0")
                try {
                    db.execSQL(
                        "UPDATE analysis_records " +
                        "SET inputTokens = consumedTokens * 6 / 10, " +
                        "    outputTokens = consumedTokens - (consumedTokens * 6 / 10) " +
                        "WHERE consumedTokens > 0 AND inputTokens = 0 AND outputTokens = 0"
                    )
                    db.execSQL(
                        "UPDATE analysis_records " +
                        "SET inputTokens = inputTokens + (consumedTokens - (inputTokens + outputTokens)) * 6 / 10, " +
                        "    outputTokens = outputTokens + (consumedTokens - (inputTokens + outputTokens)) - ((consumedTokens - (inputTokens + outputTokens)) * 6 / 10) " +
                        "WHERE consumedTokens > 0 AND consumedTokens != (inputTokens + outputTokens)"
                    )
                } catch (e: Exception) {
                    com.example.japanesegrammarapp.utils.AppLogger.e(
                        "DB_MIGRATION",
                        "Failed to backfill token usage during migration 3 to 4",
                        e
                    )
                }
            }
        }

        private val MIGRATION_3_4_RETROFIT = MIGRATION_3_4 // alias if needed

        private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS bookmarked_segments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        recordId INTEGER NOT NULL,
                        segmentText TEXT NOT NULL,
                        reading TEXT,
                        partOfSpeech TEXT,
                        posCategory TEXT,
                        dictionaryForm TEXT,
                        dictionaryFormReading TEXT,
                        meaning TEXT,
                        inflection TEXT,
                        role TEXT,
                        bookmarkedAt INTEGER NOT NULL,
                        sourceText TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_bookmarked_segments_recordId_segmentText " +
                    "ON bookmarked_segments (recordId, segmentText)"
                )
            }
        }

        private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add surfaceForm column to support duplicate bookmarking with different surface forms
                db.execSQL("ALTER TABLE bookmarked_segments ADD COLUMN surfaceForm TEXT")
                // Drop old unique index and create new one including surfaceForm + dictionaryForm
                db.execSQL("DROP INDEX IF EXISTS index_bookmarked_segments_recordId_segmentText")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_bookmarked_segments_recordId_surfaceForm_dictForm " +
                    "ON bookmarked_segments (recordId, surfaceForm, dictionaryForm)"
                )
            }
        }

        private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bookmarked_segments ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS bookmarked_sentences (
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
        }

        private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Create temp table with Foreign Key
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS bookmarked_segments_temp (
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
                """.trimIndent())
                
                // Copy only valid bookmarks (non-orphans)
                db.execSQL("""
                    INSERT INTO bookmarked_segments_temp (id, recordId, segmentText, surfaceForm, reading, partOfSpeech, posCategory, dictionaryForm, dictionaryFormReading, meaning, inflection, role, bookmarkedAt, sourceText, isArchived)
                    SELECT id, recordId, segmentText, surfaceForm, reading, partOfSpeech, posCategory, dictionaryForm, dictionaryFormReading, meaning, inflection, role, bookmarkedAt, sourceText, isArchived
                    FROM bookmarked_segments
                    WHERE recordId IN (SELECT id FROM analysis_records)
                """.trimIndent())
                
                // Drop old table
                db.execSQL("DROP TABLE bookmarked_segments")
                
                // Rename temp table
                db.execSQL("ALTER TABLE bookmarked_segments_temp RENAME TO bookmarked_segments")
                
                // Recreate index
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_bookmarked_segments_recordId_surfaceForm_dictionaryForm ON bookmarked_segments (recordId, surfaceForm, dictionaryForm)")
            }
        }

        private val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS bookmarked_grammar_points (
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
            }
        }

        private val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE analysis_records ADD COLUMN isRead INTEGER NOT NULL DEFAULT 1")
            }
        }

        val ALL_MIGRATIONS = arrayOf(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
            MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11
        )

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .addMigrations(*ALL_MIGRATIONS)
                .build()
                .also { Instance = it }
            }
        }
    }
}
