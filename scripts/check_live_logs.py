import paramiko

def check_logs():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    
    script = "journalctl -u plugin_loader -n 50 --no-pager"
    _, out, err = ssh.exec_command(script)
    print(out.read().decode('utf-8', errors='replace'))
    ssh.close()

if __name__ == '__main__':
    check_logs()
