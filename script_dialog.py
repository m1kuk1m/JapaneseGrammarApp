import re

with open('app/src/main/java/com/example/japanesegrammarapp/ui/screens/BookmarksDialogs.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Add imports
content = content.replace('import androidx.compose.ui.res.stringResource\n',
'''import androidx.compose.ui.res.stringResource
import com.example.japanesegrammarapp.domain.model.ImportResult
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
''')

dialog_code = '''
@Composable
fun ImportSummaryDialog(
    result: ImportResult,
    onDismiss: () -> Unit
) {
    val sumiInk = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.import_history),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = sumiInk
            )
        },
        text = {
            Column {
                Text(
                    text = "Success: \\nSkipped: \\nFailed: ",
                    fontSize = 15.sp,
                    color = sumiInk.copy(alpha = 0.8f)
                )
                if (result.failureReasons.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Failure Reasons:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = sumiInk
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
                        items(result.failureReasons) { reason ->
                            Text("- ", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.ok),
                    color = sumiInk
                )
            }
        },
        containerColor = surfaceColor,
        tonalElevation = 6.dp
    )
}
'''

content += dialog_code

with open('app/src/main/java/com/example/japanesegrammarapp/ui/screens/BookmarksDialogs.kt', 'w', encoding='utf-8') as f:
    f.write(content)
