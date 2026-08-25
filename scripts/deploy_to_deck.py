import os
import sys
import paramiko

def deploy():
    plugin_dir = os.path.abspath("decky-plugin")
    target_remote_dir = "/home/deck/homebrew/plugins/decky-yomi-sync"
    
    deck_ips = ["192.168.1.16", "192.168.1.15"]
    connected_ip = None
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    
    for ip in deck_ips:
        try:
            print(f"[Deploy] Trying SSH to {ip}...")
            ssh.connect(ip, username='deck', password='321127', timeout=6)
            connected_ip = ip
            print(f"[Deploy] Connected successfully to Steam Deck at {ip}!")
            break
        except Exception as e:
            print(f"[Deploy] Failed to connect to {ip}: {e}")
            
    if not connected_ip:
        print("[Deploy] ERROR: Could not connect to any Steam Deck IP.")
        sys.exit(1)
        
    sftp = ssh.open_sftp()
    
    # 1. Create remote target directory
    print(f"[Deploy] Preparing remote directory: {target_remote_dir}")
    stdin, stdout, stderr = ssh.exec_command(f"echo 321127 | sudo -S mkdir -p {target_remote_dir} && echo 321127 | sudo -S chown -R deck:deck {target_remote_dir}")
    stdout.channel.recv_exit_status()
    
    # 2. Upload files
    files_to_upload = [
        ("plugin.json", "plugin.json"),
        ("package.json", "package.json"),
        ("main.py", "main.py"),
        ("dist/index.js", "dist/index.js"),
        ("py_modules/zeroconf_helper.py", "py_modules/zeroconf_helper.py"),
        ("README.md", "README.md")
    ]
    
    # Ensure remote subdirectories
    for _, rel_dest in files_to_upload:
        dest_dir = os.path.dirname(f"{target_remote_dir}/{rel_dest}")
        if dest_dir != target_remote_dir:
            try:
                sftp.mkdir(dest_dir)
            except IOError:
                pass

    for rel_src, rel_dest in files_to_upload:
        local_path = os.path.join(plugin_dir, rel_src)
        remote_path = f"{target_remote_dir}/{rel_dest}"
        if os.path.exists(local_path):
            print(f"[Deploy] Uploading {rel_src} -> {remote_path} ({os.path.getsize(local_path)} bytes)")
            sftp.put(local_path, remote_path)
        else:
            print(f"[Deploy] WARNING: Local file not found: {local_path}")
            
    sftp.close()
    
    # 3. Set file permissions
    print("[Deploy] Setting permissions...")
    stdin, stdout, stderr = ssh.exec_command(f"echo 321127 | sudo -S chmod -R 755 {target_remote_dir}")
    stdout.channel.recv_exit_status()
    
    # 4. Restart Decky service
    print("[Deploy] Restarting Decky Loader (systemctl restart plugin_loader)...")
    stdin, stdout, stderr = ssh.exec_command("echo 321127 | sudo -S systemctl restart plugin_loader")
    stdout.channel.recv_exit_status()
    
    import time
    time.sleep(2)
    
    # 5. Check service status and recent journal logs
    print("[Deploy] Checking plugin_loader service status...")
    stdin, stdout, stderr = ssh.exec_command("systemctl is-active plugin_loader")
    status_out = stdout.read().decode().strip()
    print(f"plugin_loader status: {status_out}")
    
    print("\n--- Recent Decky Loader Logs ---")
    stdin, stdout, stderr = ssh.exec_command("journalctl -u plugin_loader -n 25 --no-pager")
    print(stdout.read().decode(errors='replace'))
    
    ssh.close()
    print("\n[Deploy] Deployment completed successfully!")

if __name__ == '__main__':
    deploy()
