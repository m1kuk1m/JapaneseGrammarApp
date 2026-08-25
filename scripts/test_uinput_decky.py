import paramiko, time

def test():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    
    script = """sudo -S python3 - << 'EOF'
import evdev
from evdev import UInput, ecodes as e
import time, glob, os

print("Testing UInput F12 as root...")
capabilities = {
    e.EV_KEY: [e.KEY_F12, e.KEY_SYSRQ, e.KEY_LEFTMETA, e.KEY_R1, e.BTN_MODE, e.BTN_TR]
}

ui = UInput(capabilities, name="Valve Software Steam Controller Virtual Keyboard", vendor=0x28de, product=0x1102)
time.sleep(0.5)

t0 = time.time()
print("Emitting KEY_F12...")
ui.write(e.EV_KEY, e.KEY_F12, 1)
ui.syn()
time.sleep(0.1)
ui.write(e.EV_KEY, e.KEY_F12, 0)
ui.syn()

time.sleep(1.5)
pattern = "/home/deck/.local/share/Steam/userdata/**/screenshots/*.jpg"
files = [f for f in glob.glob(pattern, recursive=True) if "thumbnails" not in f and os.path.getmtime(f) >= t0]
print("New screenshots with F12:", files)

if not files:
    print("Trying Guide + R1 (BTN_MODE + BTN_TR)...")
    t1 = time.time()
    ui.write(e.EV_KEY, e.BTN_MODE, 1)
    ui.syn()
    time.sleep(0.05)
    ui.write(e.EV_KEY, e.BTN_TR, 1)
    ui.syn()
    time.sleep(0.1)
    ui.write(e.EV_KEY, e.BTN_TR, 0)
    ui.syn()
    time.sleep(0.05)
    ui.write(e.EV_KEY, e.BTN_MODE, 0)
    ui.syn()
    
    time.sleep(1.5)
    files = [f for f in glob.glob(pattern, recursive=True) if "thumbnails" not in f and os.path.getmtime(f) >= t1]
    print("New screenshots with Guide+R1:", files)

ui.close()
EOF
"""
    stdin, stdout, stderr = ssh.exec_command(script, get_pty=True)
    stdin.write("321127\n")
    stdin.flush()
    print(stdout.read().decode('utf-8', errors='replace'))
    ssh.close()

if __name__ == '__main__':
    test()
