import paramiko

def test_subnet_probe():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    
    probe_script = """python3 - << 'EOF'
import subprocess
import concurrent.futures
import urllib.request
import json
import time

def get_lan_subnets():
    subnets = set()
    try:
        out = subprocess.check_output(["ip", "-4", "addr", "show"], text=True)
        for line in out.splitlines():
            line = line.strip()
            # Ignore loopback and clash TUN 198.18.x.x
            if line.startswith("inet ") and "127.0.0.1" not in line and "198.18." not in line:
                parts = line.split()[1].split("/")[0].split(".")
                if len(parts) == 4:
                    subnets.add(".".join(parts[:3]))
    except Exception as e:
        print("Error getting subnets:", e)
    if not subnets:
        subnets.add("192.168.1")
    return list(subnets)

def probe_ip(ip, port=8765):
    try:
        url = f"http://{ip}:{port}/api/v1/ping"
        req = urllib.request.Request(url, headers={"User-Agent": "YomiDeck-Probe"})
        with urllib.request.urlopen(req, timeout=0.8) as resp:
            if resp.status == 200:
                data = json.loads(resp.read().decode())
                if data.get("app") == "YomiLLM":
                    return ip, port
    except Exception:
        pass
    return None

def scan_all_subnets():
    subnets = get_lan_subnets()
    print(f"Detected local LAN subnets: {subnets}")
    start = time.time()
    found = []
    
    for prefix in subnets:
        print(f"Scanning {prefix}.1-254...")
        with concurrent.futures.ThreadPoolExecutor(max_workers=80) as executor:
            futures = {executor.submit(probe_ip, f"{prefix}.{i}"): i for i in range(1, 255)}
            for f in concurrent.futures.as_completed(futures):
                res = f.result()
                if res:
                    found.append(res)
                    print(f"==> FOUND YomiLLM at: {res[0]}:{res[1]}")
                    
    print(f"Scan finished in {time.time()-start:.2f}s, results: {found}")

scan_all_subnets()
EOF
"""
    stdin, stdout, stderr = ssh.exec_command(probe_script)
    print(stdout.read().decode('utf-8', errors='replace'))
    ssh.close()

if __name__ == '__main__':
    test_subnet_probe()
