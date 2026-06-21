import re

with open("app/src/main/java/com/example/japanesegrammarapp/ui/screens/BookmarksDialogs.kt", "r", encoding="utf-8") as f:
    content = f.read()

# Find the index of @Composable fun ImportSummaryDialog
index = content.find("@Composable\nfun ImportSummaryDialog(")

if index != -1:
    # Truncate content right before the duplicate
    content = content[:index]

# Add ConflictResolutionDialog
conflict_dialog_code = """
@Composable
fun ConflictResolutionDialog(
    onDismiss: () -> Unit,
    onSkip: () -> Unit,
    onOverwrite: () -> Unit
) {
    val sumiInk = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.conflict_resolution_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = sumiInk
            )
        },
        text = {
            Text(
                text = stringResource(R.string.conflict_resolution_message),
                fontSize = 15.sp,
                color = sumiInk.copy(alpha = 0.8f)
            )
        },
        confirmButton = {
            TextButton(onClick = onOverwrite) {
                Text(
                    text = stringResource(R.string.conflict_resolution_overwrite),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text(
                    text = stringResource(R.string.conflict_resolution_skip),
                    color = sumiInk
                )
            }
        },
        containerColor = surfaceColor,
        tonalElevation = 6.dp
    )
}
"""

content += conflict_dialog_code

with open("app/src/main/java/com/example/japanesegrammarapp/ui/screens/BookmarksDialogs.kt", "w", encoding="utf-8") as f:
    f.write(content)

