import urllib.request
import urllib.parse
import re

query = urllib.parse.quote("mojidict intent OR url scheme")
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
    with open('out.html', 'w', encoding='utf-8') as out_f:
        out_f.write(html)
    print("Success")
except Exception as e:
    print("Error:", e)
