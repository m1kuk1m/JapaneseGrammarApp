import paramiko

def test_kb_screenshot():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    
    script = """python3 - << 'EOF'
import evdev
from evdev import UInput, ecodes as e, AbsInfo
import time, glob, os

caps = {
    e.EV_KEY: [e.KEY_F12, e.KEY_SYSRQ, e.KEY_A, e.KEY_ENTER]
}

# Create as USB keyboard
ui = UInput(caps, name="Valve Software Steam Controller Keyboard", vendor=0x28de, product=0x1102, version=1, bustype=e.BUS_USB)
print("Virtual USB keyboard created:", ui)
time.sleep(1)

t0 = time.time()
print("Pressing F12...")
ui.write(e.EV_KEY, e.KEY_F12, 1)
ui.syn()
time.sleep(0.1)
ui.write(e.EV_KEY, e.KEY_F12, 0)
ui.syn()
ui.close()
print("F12 released!")

# Check if new screenshot appears within 3 seconds
time.sleep(1.5)
pattern = "/home/deck/.local/share/Steam/userdata/**/screenshots/*.jpg"
files = [f for f in glob.glob(pattern, recursive=True) if "thumbnails" not in f and os.path.getmtime(f) >= t0]
print("New screenshots found:", files)
EOF
"""
    _, out, err = ssh.exec_command(script)
    print(out.read().decode('utf-8', errors='replace'))
    print(err.read().decode('utf-8', errors='replace'))
    ssh.close()

if __name__ == '__main__':
    test_kb_screenshot()
