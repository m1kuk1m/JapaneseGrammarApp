import paramiko

def probe():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    
    cmds = [
        "python3 -c 'import evdev; print(evdev.__file__)'",
        "ls -la /dev/uinput /dev/input/",
        "which ffmpeg",
        "cat /proc/bus/input/devices | grep -E 'Name|Handlers'",
        "echo 321127 | sudo -S gamescopectl --help"
    ]
    for cmd in cmds:
        print(f"=== {cmd} ===")
        _, out, err = ssh.exec_command(cmd)
        print(out.read().decode('utf-8', errors='replace'))
        print(err.read().decode('utf-8', errors='replace'))

    ssh.close()

if __name__ == '__main__':
    probe()
