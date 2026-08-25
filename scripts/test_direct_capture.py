import paramiko

def test():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    
    script = """python3 - << 'EOF'
import os, subprocess, time

def capture_live_screen(output_path: str = "/tmp/yomi_live_capture.jpg") -> bool:
    try:
        if os.path.exists(output_path):
            try:
                os.remove(output_path)
            except OSError:
                pass
        env = os.environ.copy()
        env["DISPLAY"] = ":0"
        cmd = [
            "ffmpeg", "-y",
            "-f", "x11grab",
            "-video_size", "1280x800",
            "-i", ":0",
            "-vframes", "1",
            "-q:v", "2",
            output_path
        ]
        t0 = time.time()
        res = subprocess.run(cmd, env=env, capture_output=True, text=True, timeout=3)
        elapsed = int((time.time() - t0) * 1000)
        if res.returncode == 0 and os.path.exists(output_path) and os.path.getsize(output_path) > 1000:
            print(f"Captured {output_path} in {elapsed}ms, size={os.path.getsize(output_path)} bytes")
            return True
        else:
            print(f"Failed: code={res.returncode}, stderr={res.stderr[:200]}")
    except Exception as e:
        print(f"Error: {e}")
    return False

res = capture_live_screen()
print("Result:", res)
EOF
"""
    _, out, err = ssh.exec_command(script)
    print(out.read().decode('utf-8', errors='replace'))
    print(err.read().decode('utf-8', errors='replace'))
    ssh.close()

if __name__ == '__main__':
    test()
