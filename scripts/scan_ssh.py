import paramiko
import socket

def test_ip(ip):
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(0.6)
        res = s.connect_ex((ip, 22))
        s.close()
        if res == 0:
            print(f"[FOUND PORT 22] {ip}")
            # Try connect
            ssh = paramiko.SSHClient()
            ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
            try:
                ssh.connect(ip, username='deck', password='321127', timeout=2)
                stdin, stdout, stderr = ssh.exec_command('whoami; uname -a')
                print(f"SUCCESS on {ip}: {stdout.read().decode().strip()}")
                ssh.close()
                return ip
            except Exception as e:
                print(f"SSH Auth error on {ip}: {e}")
    except Exception:
        pass
    return None

if __name__ == '__main__':
    candidates = [
        "192.168.1.15", "192.168.1.14", "192.168.1.16", "192.168.1.18",
        "192.168.1.12", "192.168.1.10", "192.168.1.9", "192.168.1.7",
        "192.168.1.5", "192.168.1.4", "192.168.1.2", "192.168.1.13"
    ]
    for ip in candidates:
        if test_ip(ip):
            break
