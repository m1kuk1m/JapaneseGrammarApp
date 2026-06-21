import re

with open('app/src/main/java/com/example/japanesegrammarapp/ui/screens/BookmarksScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Remove duplicate state variables
pattern = '''    var showExportDialog by remember \{ mutableStateOf\(false\) \}
    var showImportDialog by remember \{ mutableStateOf\(false\) \}
    var showConflictDialog by remember \{ mutableStateOf\(false\) \}
    var showImportSummaryDialog by remember \{ mutableStateOf\(false\) \}
    var importSummaryResult by remember \{ mutableStateOf<com.example.japanesegrammarapp.domain.model.ImportResult\?>\(null\) \}
    var pendingImportParams by remember \{ mutableStateOf<ImportParams\?>\(null\) \}
    var pendingImportUri by remember \{ mutableStateOf<Uri\?>\(null\) \}
    
    var showConflictDialog by remember \{ mutableStateOf\(false\) \}
    var pendingImportParams by remember \{ mutableStateOf<ImportParams\?>\(null\) \}
    
    var showImportSummaryDialog by remember \{ mutableStateOf\(false\) \}
    var importSummaryResult by remember \{ mutableStateOf<ImportResult\?>\(null\) \}'''

replacement = '''    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showConflictDialog by remember { mutableStateOf(false) }
    var pendingImportParams by remember { mutableStateOf<ImportParams?>(null) }
    var showImportSummaryDialog by remember { mutableStateOf(false) }
    var importSummaryResult by remember { mutableStateOf<ImportResult?>(null) }'''

content = re.sub(pattern, replacement, content)

with open('app/src/main/java/com/example/japanesegrammarapp/ui/screens/BookmarksScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)

with open('app/src/main/java/com/example/japanesegrammarapp/ui/screens/BookmarksDialogs.kt', 'r', encoding='utf-8') as f:
    content2 = f.read()

if 'import androidx.compose.foundation.layout.heightIn' not in content2:
    content2 = content2.replace('import androidx.compose.foundation.lazy.items', 'import androidx.compose.foundation.lazy.items\\nimport androidx.compose.foundation.layout.heightIn')

with open('app/src/main/java/com/example/japanesegrammarapp/ui/screens/BookmarksDialogs.kt', 'w', encoding='utf-8') as f:
    f.write(content2)

