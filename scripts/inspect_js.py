import paramiko

def inspect_js():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    
    cmd = """python3 - << 'EOF'
with open("/home/deck/homebrew/plugins/decky-lsfg-vk/dist/index.js", "r") as f:
    lines = f.readlines()
print("Total lines in decky-lsfg-vk/dist/index.js:", len(lines))
print("First 30 lines:")
for l in lines[:30]:
    print(l, end="")
print("\nLast 30 lines:")
for l in lines[-30:]:
    print(l, end="")
EOF
"""
    stdin, stdout, stderr = ssh.exec_command(cmd)
    print(stdout.read().decode('utf-8', errors='replace'))
    ssh.close()

if __name__ == '__main__':
    inspect_js()
