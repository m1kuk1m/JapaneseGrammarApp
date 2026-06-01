import urllib.request
import urllib.parse
import re

query = urllib.parse.quote('site:github.com com.mojitec.mojidict')
url = "https://html.duckduckgo.com/html/?q=" + query
req = urllib.request.Request(
    url, 
    headers={
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
    }
)

try:
    f = urllib.request.urlopen(req)
    html = f.read().decode('utf-8')
    with open('ddg_github.txt', 'w', encoding='utf-8') as out:
        snippets = re.findall(r'<a class="result__snippet[^>]*>(.*?)</a>', html)
        for s in snippets:
            out.write(re.sub(r'<[^>]+>', '', s) + "\n")
    print("Success")
except Exception as e:
    print("Error:", e)
