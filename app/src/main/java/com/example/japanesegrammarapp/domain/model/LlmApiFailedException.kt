package com.example.japanesegrammarapp.domain.model

class LlmApiFailedException(
    val mainProvider: String,
    val mainErrorMessage: String?,
    val isBackupUsed: Boolean,
    val backupProvider: String?,
    val backupErrorMessage: String?,
    cause: Throwable? = null
) : Exception(
    buildMessage(
        mainProvider = mainProvider,
        mainErrorMessage = mainErrorMessage,
        isBackupUsed = isBackupUsed,
        backupProvider = backupProvider,
        backupErrorMessage = backupErrorMessage
    ),
    cause
) {
    companion object {
        private fun buildMessage(
            mainProvider: String,
            mainErrorMessage: String?,
            isBackupUsed: Boolean,
            backupProvider: String?,
            backupErrorMessage: String?
        ): String {
            return if (isBackupUsed) {
                "Main ($mainProvider) and Backup ($backupProvider) both failed.\n" +
                    "Main error: ${mainErrorMessage ?: "Unknown error"}\n" +
                    "Backup error: ${backupErrorMessage ?: "Unknown error"}"
            } else {
                "Main ($mainProvider) failed.\n" +
                    "Main error: ${mainErrorMessage ?: "Unknown error"}"
            }
        }
    }
}
