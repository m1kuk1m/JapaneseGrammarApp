import paramiko

def test_live():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    
    script = """python3 - << 'EOF'
import sys, asyncio
sys.path.insert(0, '/home/deck/homebrew/plugins/decky-yomi-sync')
from main import Plugin
p = Plugin()
print("Injecting screenshot key test...")
res = p.inject_screenshot_key()
print("Inject result:", res)
EOF
"""
    _, out, err = ssh.exec_command(script)
    print(out.read().decode('utf-8', errors='replace'))
    print(err.read().decode('utf-8', errors='replace'))
    ssh.close()

if __name__ == '__main__':
    test_live()
