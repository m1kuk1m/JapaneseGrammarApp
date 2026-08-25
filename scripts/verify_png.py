import paramiko

def verify_non_black():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    
    script = """python3 - << 'EOF'
import struct

with open('/home/deck/Pictures/Screenshot_20260824_022413.png', 'rb') as f:
    data = f.read()

print("PNG Header OK:", data.startswith(b'\x89PNG\r\n\x1a\n'))
print("Total size:", len(data))
# A black PNG of 1280x800 compressed is ~10KB. A colorful game screen is 1MB - 2MB!
print("Is full-color game screen (>100KB)?", len(data) > 100000)
EOF
"""
    _, out, err = ssh.exec_command(script)
    print(out.read().decode('utf-8', errors='replace'))
    ssh.close()

if __name__ == '__main__':
    verify_non_black()
