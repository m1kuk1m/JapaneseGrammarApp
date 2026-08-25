import paramiko

def run_diagnostics():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    
    cmds = [
        "ping -c 2 192.168.1.18",
        "curl -i -v -m 3 http://192.168.1.18:8765/api/v1/ping 2>&1",
        "curl -i -v -m 3 -X POST http://192.168.1.18:8765/api/v1/pair -H 'Content-Type: application/json' -d '{\"pin\":\"6721\"}' 2>&1",
        "ip route",
        "journalctl -u plugin_loader -n 25 --no-pager"
    ]
    
    for c in cmds:
        print(f"\n==================== RUNNING: {c} ====================")
        stdin, stdout, stderr = ssh.exec_command(c)
        print(stdout.read().decode('utf-8', errors='replace'))
        
    ssh.close()

if __name__ == '__main__':
    run_diagnostics()
