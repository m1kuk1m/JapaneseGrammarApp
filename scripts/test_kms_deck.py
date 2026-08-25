import paramiko

def test_kms():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    
    script = """python3 - << 'EOF'
import subprocess, os

test_out = "/tmp/kms_screen.jpg"
if os.path.exists(test_out):
    os.remove(test_out)

# ffmpeg kmsgrab with hwdownload
cmd = [
    "ffmpeg", "-y",
    "-f", "kmsgrab",
    "-device", "/dev/dri/card0",
    "-i", "-",
    "-vf", "hwdownload,format=bgr0",
    "-vframes", "1",
    "-q:v", "2",
    test_out
]
res = subprocess.run(cmd, capture_output=True, text=True)
print("Return code:", res.returncode)
print("Stderr:", res.stderr[-500:])

if os.path.exists(test_out):
    print("SUCCESS! File size:", os.path.getsize(test_out))
    res2 = subprocess.run(["file", test_out], capture_output=True, text=True)
    print("File info:", res2.stdout)
else:
    print("FAILED")
EOF
"""
    # PluginLoader runs as root, so let's test running as root via python directly or via ssh
    _, out, err = ssh.exec_command(script)
    print(out.read().decode('utf-8', errors='replace'))
    print(err.read().decode('utf-8', errors='replace'))
    ssh.close()

if __name__ == '__main__':
    test_kms()
