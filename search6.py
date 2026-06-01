import urllib.request
import urllib.parse
import re

query = urllib.parse.quote('"mojidict" "intent" OR "tasker" OR "url scheme"')
url = "https://www.bing.com/search?q=" + query
req = urllib.request.Request(
    url, 
    headers={
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
        'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8'
    }
)

try:
    f = urllib.request.urlopen(req)
    html = f.read().decode('utf-8')
    matches = re.findall(r'<li class="b_algo".*?>(.*?)</li>', html, re.DOTALL)
    
    with open('bing_results2.txt', 'w', encoding='utf-8') as out:
        for m in matches:
            text = re.sub(r'<[^>]+>', ' ', m)
            text = re.sub(r'\s+', ' ', text)
            out.write(text + "\n")
    print("Success")
except Exception as e:
    print("Error:", e)
