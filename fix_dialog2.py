import re

with open("app/src/main/java/com/example/japanesegrammarapp/ui/screens/BookmarksDialogs.kt", "r", encoding="utf-8") as f:
    content = f.read()

content = content.replace('Text("- ", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)',
                          'Text("- ${reason}", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)')

with open("app/src/main/java/com/example/japanesegrammarapp/ui/screens/BookmarksDialogs.kt", "w", encoding="utf-8") as f:
    f.write(content)
