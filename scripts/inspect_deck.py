import paramiko

def inspect_deck():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    
    commands = [
        "uname -a",
        "whoami",
        "node -v 2>/dev/null || echo 'node: not found'",
        "npm -v 2>/dev/null || echo 'npm: not found'",
        "pnpm -v 2>/dev/null || echo 'pnpm: not found'",
        "python3 --version",
        "python3 -c 'import watchdog; print(\"watchdog available\")' 2>/dev/null || echo 'watchdog: not installed'",
        "python3 -c 'import zeroconf; print(\"zeroconf available\")' 2>/dev/null || echo 'zeroconf: not installed'",
        "ls -la /home/deck/homebrew/plugins 2>/dev/null || echo 'homebrew plugins dir: missing'",
        "systemctl is-active plugin_loader || echo 'plugin_loader: inactive'",
        "ls -la ~/.local/share/Steam/userdata 2>/dev/null"
    ]
    
    for cmd in commands:
        print(f"=== CMD: {cmd} ===")
        stdin, stdout, stderr = ssh.exec_command(cmd)
        out = stdout.read().decode('utf-8', errors='replace').strip()
        err = stderr.read().decode('utf-8', errors='replace').strip()
        if out:
            print(out)
        if err:
            print(f"[ERR] {err}")
        print()
        
    ssh.close()

if __name__ == '__main__':
    inspect_deck()
