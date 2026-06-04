package com.example.japanesegrammarapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AnalysisRecord::class, BookmarkedSegment::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun analysisDao(): AnalysisDao
    abstract fun bookmarkDao(): BookmarkDao

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

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigration()
                .build()
                .also { Instance = it }
            }
        }
    }
}
