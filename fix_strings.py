import re

with open("app/src/main/res/values/strings.xml", "r", encoding="utf-8") as f:
    content = f.read()

new_strings = """
    <string name="conflict_resolution_overwrite">Overwrite existing</string>
    <string name="conflict_resolution_skip">Skip existing</string>
</resources>"""

content = content.replace("</resources>", new_strings)

with open("app/src/main/res/values/strings.xml", "w", encoding="utf-8") as f:
    f.write(content)

with open("app/src/main/java/com/example/japanesegrammarapp/ui/screens/BookmarksDialogs.kt", "r", encoding="utf-8") as f:
    content2 = f.read()
    
content2 = content2.replace("R.string.conflict_resolution_message", "R.string.conflict_resolution_msg")

with open("app/src/main/java/com/example/japanesegrammarapp/ui/screens/BookmarksDialogs.kt", "w", encoding="utf-8") as f:
    f.write(content2)

