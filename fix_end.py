with open("app/src/main/java/com/example/japanesegrammarapp/ui/screens/BookmarksScreen.kt", "r", encoding="utf-8") as f:
    lines = f.readlines()

while lines and lines[-1].strip() == "}":
    lines.pop()

with open("app/src/main/java/com/example/japanesegrammarapp/ui/screens/BookmarksScreen.kt", "w", encoding="utf-8") as f:
    f.writelines(lines)
