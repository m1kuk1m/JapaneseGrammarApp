package com.example.japanesegrammarapp.domain.model

class LlmApiFailedException(
    val mainProvider: String,
    val mainErrorMessage: String?,
    val isBackupUsed: Boolean,
    val backupProvider: String?,
    val backupErrorMessage: String?
) : Exception(
    if (isBackupUsed) "Main ($mainProvider) and Backup ($backupProvider) both failed."
    else "Main ($mainProvider) failed."
)