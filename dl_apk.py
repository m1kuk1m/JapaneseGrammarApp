import urllib.request
import os

url = "https://d.apkpure.com/b/APK/com.mojitec.mojidict?version=latest"
req = urllib.request.Request(
    url, 
    headers={
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
    }
)

try:
    print("Downloading...")
    with urllib.request.urlopen(req) as response, open("mojidict.apk", 'wb') as out_file:
        data = response.read()
        out_file.write(data)
    print("Downloaded bytes:", os.path.getsize("mojidict.apk"))
except Exception as e:
    print("Error:", e)
