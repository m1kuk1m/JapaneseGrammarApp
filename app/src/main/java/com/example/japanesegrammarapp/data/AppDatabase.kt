package com.example.japanesegrammarapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AnalysisRecord::class, BookmarkedSegment::class, BookmarkedSentence::class], version = 9, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun analysisDao(): AnalysisDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun bookmarkedSentenceDao(): BookmarkedSentenceDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE analysis_records ADD COLUMN status TEXT NOT NULL DEFAULT 'COMPLETED'")
                database.execSQL("ALTER TABLE analysis_records ADD COLUMN errorMessage TEXT")
            }
        }

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE analysis_records ADD COLUMN consumedTokens INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE analysis_records ADD COLUMN inputTokens INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE analysis_records ADD COLUMN outputTokens INTEGER NOT NULL DEFAULT 0")
                try {
                    database.execSQL(
                        "UPDATE analysis_records " +
                        "SET inputTokens = consumedTokens * 6 / 10, " +
                        "    outputTokens = consumedTokens - (consumedTokens * 6 / 10) " +
                        "WHERE consumedTokens > 0 AND inputTokens = 0 AND outputTokens = 0"
                    )
                    database.execSQL(
                        "UPDATE analysis_records " +
                        "SET inputTokens = inputTokens + (consumedTokens - (inputTokens + outputTokens)) * 6 / 10, " +
                        "    outputTokens = outputTokens + (consumedTokens - (inputTokens + outputTokens)) - ((consumedTokens - (inputTokens + outputTokens)) * 6 / 10) " +
                        "WHERE consumedTokens > 0 AND consumedTokens != (inputTokens + outputTokens)"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        private val MIGRATION_3_4_RETROFIT = MIGRATION_3_4 // alias if needed

        private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
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
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_bookmarked_segments_recordId_segmentText " +
                    "ON bookmarked_segments (recordId, segmentText)"
                )
            }
        }

        private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add surfaceForm column to support duplicate bookmarking with different surface forms
                database.execSQL("ALTER TABLE bookmarked_segments ADD COLUMN surfaceForm TEXT")
                // Drop old unique index and create new one including surfaceForm + dictionaryForm
                database.execSQL("DROP INDEX IF EXISTS index_bookmarked_segments_recordId_segmentText")
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_bookmarked_segments_recordId_surfaceForm_dictForm " +
                    "ON bookmarked_segments (recordId, surfaceForm, dictionaryForm)"
                )
            }
        }

        private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE bookmarked_segments ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
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
                database.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarked_sentences_recordId ON bookmarked_sentences (recordId)")
            }
        }

        private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Create temp table with Foreign Key
                database.execSQL("""
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
                database.execSQL("""
                    INSERT INTO bookmarked_segments_temp (id, recordId, segmentText, surfaceForm, reading, partOfSpeech, posCategory, dictionaryForm, dictionaryFormReading, meaning, inflection, role, bookmarkedAt, sourceText, isArchived)
                    SELECT id, recordId, segmentText, surfaceForm, reading, partOfSpeech, posCategory, dictionaryForm, dictionaryFormReading, meaning, inflection, role, bookmarkedAt, sourceText, isArchived
                    FROM bookmarked_segments
                    WHERE recordId IN (SELECT id FROM analysis_records)
                """.trimIndent())
                
                // Drop old table
                database.execSQL("DROP TABLE bookmarked_segments")
                
                // Rename temp table
                database.execSQL("ALTER TABLE bookmarked_segments_temp RENAME TO bookmarked_segments")
                
                // Recreate index
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_bookmarked_segments_recordId_surfaceForm_dictionaryForm ON bookmarked_segments (recordId, surfaceForm, dictionaryForm)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
                    MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
                    MIGRATION_7_8, MIGRATION_8_9
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { Instance = it }
            }
        }
    }
}
