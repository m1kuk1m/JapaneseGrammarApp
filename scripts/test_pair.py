import sys
import paramiko

def test_pair():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    stdin, stdout, stderr = ssh.exec_command('curl -v -m 5 -X POST -H "Content-Type: application/json" -d \'{"pin":"6721"}\' http://192.168.1.18:8765/api/v1/pair 2>&1')
    out = stdout.read().decode('utf-8', errors='replace')
    print(out)
    ssh.close()

if __name__ == '__main__':
    test_pair()
