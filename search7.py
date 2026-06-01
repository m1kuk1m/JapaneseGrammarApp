import urllib.request
import urllib.parse
import re

query = urllib.parse.quote('mojidict "android.intent.action"')
url = "https://html.duckduckgo.com/html/?q=" + query
req = urllib.request.Request(
    url, 
    headers={
        'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
    }
)

try:
    f = urllib.request.urlopen(req)
    html = f.read().decode('utf-8')
    with open('ddg_results.txt', 'w', encoding='utf-8') as out:
        matches = re.findall(r'<a class="result__url" href="([^"]+)">', html)
        snippets = re.findall(r'<a class="result__snippet[^>]*>(.*?)</a>', html)
        for i in range(len(matches)):
            snip = re.sub(r'<[^>]+>', '', snippets[i]) if i < len(snippets) else ""
            out.write(matches[i] + "\n" + snip + "\n\n")
    print("Success")
except Exception as e:
    print("Error:", e)
