import paramiko

def check_portal_img():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    
    script = """python3 - << 'EOF'
import subprocess
res = subprocess.run(["file", "/home/deck/Pictures/Screenshot_20260824_022413.png"], capture_output=True, text=True)
print("File info:", res.stdout)

# Check average color / non-black
from PIL import Image
try:
    im = Image.open('/home/deck/Pictures/Screenshot_20260824_022413.png')
    print("Image format:", im.format, "size:", im.size, "mode:", im.mode)
    extrema = im.getextrema()
    print("Extrema (min/max per channel):", extrema)
except Exception as ex:
    print("PIL check:", ex)
EOF
"""
    _, out, err = ssh.exec_command(script)
    print(out.read().decode('utf-8', errors='replace'))
    print(err.read().decode('utf-8', errors='replace'))
    ssh.close()

if __name__ == '__main__':
    check_portal_img()
