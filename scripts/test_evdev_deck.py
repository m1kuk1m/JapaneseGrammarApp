import paramiko

def test_evdev():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    
    script = """python3 - << 'EOF'
import sys
print("sys.path:", sys.path)
import evdev
print("evdev file:", evdev.__file__)
from evdev import UInput, ecodes as e
print("UInput:", UInput)
ui = UInput({e.EV_KEY: [e.KEY_F12]}, name="test-kb")
print("ui created successfully!")
ui.close()
EOF
"""
    _, out, err = ssh.exec_command(script)
    print(out.read().decode('utf-8', errors='replace'))
    print(err.read().decode('utf-8', errors='replace'))
    ssh.close()

if __name__ == '__main__':
    test_evdev()
