package com.example.japanesegrammarapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AnalysisRecord::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun analysisDao(): AnalysisDao

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

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
                .also { Instance = it }
            }
        }
    }
}
