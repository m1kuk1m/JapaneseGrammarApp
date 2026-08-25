import paramiko

def check_img():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    
    script = """python3 - << 'EOF'
import subprocess
res = subprocess.run(["file", "/tmp/ffmpeg_screen.jpg"], capture_output=True, text=True)
print(res.stdout)
EOF
"""
    _, out, err = ssh.exec_command(script)
    print(out.read().decode('utf-8', errors='replace'))
    ssh.close()

if __name__ == '__main__':
    check_img()
