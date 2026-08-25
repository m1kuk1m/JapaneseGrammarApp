import paramiko

def test_remote():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    
    cmd = """python3 - << 'EOF'
import sys, asyncio
sys.path.insert(0, "/home/deck/homebrew/plugins/decky-yomi-sync")
from main import Plugin

async def run_test():
    p = Plugin()
    status = await p.get_status()
    print("Plugin get_status:", status)
    
asyncio.run(run_test())
EOF
"""
    stdin, stdout, stderr = ssh.exec_command(cmd)
    out = stdout.read().decode('utf-8', errors='replace')
    err = stderr.read().decode('utf-8', errors='replace')
    print("STDOUT:\n" + out)
    if err:
        print("STDERR:\n" + err)
    ssh.close()

if __name__ == '__main__':
    test_remote()
