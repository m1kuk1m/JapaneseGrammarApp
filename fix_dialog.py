import re

with open("app/src/main/java/com/example/japanesegrammarapp/ui/screens/BookmarksDialogs.kt", "r", encoding="utf-8") as f:
    content = f.read()

# Replace the broken string
broken = """                Text(
                    text = "Success: \nSkipped: \nFailed: ","""

fixed = """                Text(
                    text = "Success: ${result.successCount}\\nSkipped: ${result.skippedCount}\\nFailed: ${result.failedCount}", """

content = content.replace(broken, fixed)

with open("app/src/main/java/com/example/japanesegrammarapp/ui/screens/BookmarksDialogs.kt", "w", encoding="utf-8") as f:
    f.write(content)
