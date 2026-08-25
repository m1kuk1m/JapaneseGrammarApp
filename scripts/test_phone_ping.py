import paramiko

def test_deck_to_phone():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    
    commands = [
        "curl -v -m 3 http://192.168.1.18:8765/api/v1/ping 2>&1",
        "arp -a",
        "ip route"
    ]
    
    for cmd in commands:
        print(f"=== {cmd} ===")
        stdin, stdout, stderr = ssh.exec_command(cmd)
        print(stdout.read().decode('utf-8', errors='replace'))
        
    ssh.close()

if __name__ == '__main__':
    test_deck_to_phone()
