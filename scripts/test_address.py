import paramiko

def test_address():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    
    script = """python3 - << 'EOF'
import os, subprocess, time, glob

def test_call():
    t0 = time.time() - 0.2
    cmd = [
        "gdbus", "call",
        "--address", "unix:path=/run/user/1000/bus",
        "--dest", "org.freedesktop.portal.Desktop",
        "--object-path", "/org/freedesktop/portal/desktop",
        "--method", "org.freedesktop.portal.Screenshot.Screenshot",
        "",
        "{'interactive': <false>}"
    ]
    res = subprocess.run(cmd, capture_output=True, text=True, timeout=3)
    print("DBus stdout:", res.stdout)
    print("DBus stderr:", res.stderr)
    
    for _ in range(25):
        time.sleep(0.08)
        for f in glob.iglob("/home/deck/Pictures/*.png"):
            try:
                if os.path.getmtime(f) >= t0:
                    print(f"Captured: {f} ({os.path.getsize(f)} bytes)")
                    return f
            except OSError:
                pass
    print("No file found")
    return None

test_call()
EOF
"""
    _, out, err = ssh.exec_command(script)
    print(out.read().decode('utf-8', errors='replace'))
    print(err.read().decode('utf-8', errors='replace'))
    ssh.close()

if __name__ == '__main__':
    test_address()
