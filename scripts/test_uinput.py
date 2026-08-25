import paramiko

def test_uinput():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    
    script = """python3 - << 'EOF'
import evdev
from evdev import UInput, ecodes as e
import time

capabilities = {
    e.EV_KEY: [e.KEY_F12, e.KEY_SYSRQ, e.KEY_LEFTMETA, e.KEY_R]
}

ui = UInput(capabilities, name="yomi-virtual-keyboard")
print("Virtual keyboard created!")
time.sleep(0.5)

# Press and release F12
ui.write(e.EV_KEY, e.KEY_F12, 1)
ui.syn()
time.sleep(0.1)
ui.write(e.EV_KEY, e.KEY_F12, 0)
ui.syn()
ui.close()
print("F12 sent successfully!")
EOF
"""
    stdin, stdout, stderr = ssh.exec_command(f"echo 321127 | sudo -S {script}")
    print(stdout.read().decode('utf-8', errors='replace'))
    print(stderr.read().decode('utf-8', errors='replace'))
    ssh.close()

if __name__ == '__main__':
    test_uinput()
