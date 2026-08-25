import paramiko

def inspect_steam():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    
    script = """python3 - << 'EOF'
import glob, re

# Search steam client js files
files = glob.glob('/home/deck/.local/share/Steam/steamui/**/*.js', recursive=True)
print(f"Found {len(files)} steamui js files")

methods = set()
for f in files[:20]:
    try:
        with open(f, 'r', encoding='utf-8', errors='ignore') as fp:
            content = fp.read()
            for m in re.findall(r'SteamClient\.[a-zA-Z0-9_\.]+', content):
                if 'Screenshot' in m or 'screenshot' in m:
                    methods.add(m)
    except Exception:
        pass

print("Screenshot related SteamClient methods:", methods)
EOF
"""
    _, out, err = ssh.exec_command(script)
    print(out.read().decode('utf-8', errors='replace'))
    print(err.read().decode('utf-8', errors='replace'))
    ssh.close()

if __name__ == '__main__':
    inspect_steam()
