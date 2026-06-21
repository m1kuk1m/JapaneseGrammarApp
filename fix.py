import re
with open('app/src/main/java/com/example/japanesegrammarapp/ui/screens/BookmarksDialogs.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('import androidx.compose.foundation.lazy.items\\n',
'''import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.heightIn
''')

with open('app/src/main/java/com/example/japanesegrammarapp/ui/screens/BookmarksDialogs.kt', 'w', encoding='utf-8') as f:
    f.write(content)
