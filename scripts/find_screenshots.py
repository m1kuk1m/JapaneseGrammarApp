import paramiko

def check_screenshots():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    stdin, stdout, stderr = ssh.exec_command('find /home/deck/.local/share/Steam/userdata -type f -name "*.jpg" ! -path "*/thumbnails/*" -printf "%T+ %p\\n" | sort -r | head -n 30')
    print(stdout.read().decode('utf-8', errors='replace'))
    ssh.close()

if __name__ == '__main__':
    check_screenshots()
