import paramiko

def probe_gamescope():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    
    cmds = [
        "ps -ef | grep -i gamescope",
        "ls -la /run/user/1000/ /run/user/1000/gamescope* /tmp/gamescope* 2>/dev/null",
        "which pw-record pw-cli",
        "ffmpeg -f kmsgrab -device /dev/dri/card0 -i - -vframes 1 /tmp/kms_test.jpg 2>&1 | head -n 30"
    ]
    for c in cmds:
        print(f"=== {c} ===")
        _, out, err = ssh.exec_command(c)
        print(out.read().decode('utf-8', errors='replace'))
    ssh.close()

if __name__ == '__main__':
    probe_gamescope()
