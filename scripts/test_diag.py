import sys
import io
import paramiko

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

def test_diag():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    cmd = """python3 - << 'EOF'
import sys, asyncio
sys.path.insert(0, '/home/deck/homebrew/plugins/decky-yomi-sync')
from main import Plugin
p = Plugin()
res = asyncio.run(p.run_diagnostics('192.168.1.18', 8765))
for l in res['logs']:
    print(l)
EOF
"""
    stdin, stdout, stderr = ssh.exec_command(cmd)
    out = stdout.read().decode('utf-8', errors='replace')
    print("DIAGNOSTICS OUTPUT FROM DECK:")
    print(out)
    ssh.close()

if __name__ == '__main__':
    test_diag()
