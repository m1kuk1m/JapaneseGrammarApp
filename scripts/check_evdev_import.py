import paramiko

def check():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    
    script = """python3 - << 'EOF'
import sys
sys.path.insert(0, '/home/deck/homebrew/plugins/decky-yomi-sync')
try:
    import evdev
    print("evdev imported successfully:", evdev)
    from evdev import UInput, ecodes as e
    print("UInput imported successfully")
except Exception as ex:
    print("evdev import failed:", ex)
EOF
"""
    _, out, err = ssh.exec_command(script)
    print(out.read().decode('utf-8', errors='replace'))
    print(err.read().decode('utf-8', errors='replace'))
    ssh.close()

if __name__ == '__main__':
    check()
