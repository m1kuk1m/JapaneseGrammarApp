import paramiko

def monitor():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    
    script = """python3 - << 'EOF'
import evdev, glob

for p in sorted(glob.glob('/dev/input/event*')):
    try:
        dev = evdev.InputDevice(p)
        print(f"{p}: name='{dev.name}', phys='{dev.phys}'")
    except Exception:
        pass
EOF
"""
    _, out, err = ssh.exec_command(script)
    print(out.read().decode('utf-8', errors='replace'))
    ssh.close()

if __name__ == '__main__':
    monitor()
