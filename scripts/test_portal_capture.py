import paramiko

def test_portal():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    
    script = """python3 - << 'EOF'
import subprocess, time, glob, os, urllib.parse

print("Calling desktop portal screenshot...")
t0 = time.time()
cmd = ["gdbus", "call", "--session", "--dest", "org.freedesktop.portal.Desktop", "--object-path", "/org/freedesktop/portal/desktop", "--method", "org.freedesktop.portal.Screenshot.Screenshot", "", "{'interactive': <false>}"]
res = subprocess.run(cmd, capture_output=True, text=True)
print("Result:", res.stdout, res.stderr)

# Check Pictures or cache for new screenshots
time.sleep(1)
recent_files = []
for root in ['/home/deck/Pictures', '/home/deck/.cache', '/home/deck/.local/share/Steam/userdata']:
    for f in glob.glob(f'{root}/**', recursive=True):
        try:
            if os.path.isfile(f) and f.endswith(('.png', '.jpg')) and os.path.getmtime(f) >= t0:
                recent_files.append((f, os.path.getsize(f)))
        except OSError:
            pass

print("Portal generated files:", recent_files)
EOF
"""
    _, out, err = ssh.exec_command(script)
    print(out.read().decode('utf-8', errors='replace'))
    print(err.read().decode('utf-8', errors='replace'))
    ssh.close()

if __name__ == '__main__':
    test_portal()
