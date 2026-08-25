import paramiko

def test_ffmpeg():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    
    script = """python3 - << 'EOF'
import subprocess, os

test_out = "/tmp/ffmpeg_screen.jpg"
if os.path.exists(test_out):
    os.remove(test_out)

cmd = ["ffmpeg", "-y", "-f", "x11grab", "-video_size", "1280x800", "-i", ":0", "-vframes", "1", test_out]
print("Running ffmpeg x11grab...")
res = subprocess.run(cmd, capture_output=True, text=True)
print("Return code:", res.returncode)
print("Stdout:", res.stdout[:200])
print("Stderr:", res.stderr[:300])

if os.path.exists(test_out):
    print("SUCCESS: Screen captured via ffmpeg! Size:", os.path.getsize(test_out))
else:
    print("FAILED: File not created")
EOF
"""
    _, out, err = ssh.exec_command(script)
    print(out.read().decode('utf-8', errors='replace'))
    print(err.read().decode('utf-8', errors='replace'))
    ssh.close()

if __name__ == '__main__':
    test_ffmpeg()
