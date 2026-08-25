import sys
import os
import paramiko

def test_connection():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    try:
        print("Connecting to Steam Deck at 192.168.1.15...")
        ssh.connect('192.168.1.15', username='deck', password='321127', timeout=10)
        print("SSH Connection successful!\n")
        
        commands = [
            "uname -a",
            "whoami",
            "which node || echo 'node missing'",
            "which npm || echo 'npm missing'",
            "which pnpm || echo 'pnpm missing'",
            "python3 --version",
            "ls -la /home/deck/homebrew/plugins 2>/dev/null || echo 'homebrew plugins dir missing'",
            "systemctl is-active plugin_loader || echo 'plugin_loader not active'"
        ]
        
        for cmd in commands:
            print(f"--- Running: {cmd} ---")
            stdin, stdout, stderr = ssh.exec_command(cmd)
            out = stdout.read().decode('utf-8', errors='replace').strip()
            err = stderr.read().decode('utf-8', errors='replace').strip()
            if out:
                print(out)
            if err:
                print(f"[ERR] {err}")
            print()
            
        ssh.close()
    except Exception as e:
        print(f"SSH Failed: {e}")

if __name__ == "__main__":
    test_connection()
