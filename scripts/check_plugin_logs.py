import paramiko

def check_logs():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    stdin, stdout, stderr = ssh.exec_command('journalctl -u plugin_loader -n 80 --no-pager')
    logs = stdout.read().decode('utf-8', errors='replace')
    print(logs)
    ssh.close()

if __name__ == '__main__':
    check_logs()
