import paramiko, os

def fetch_img():
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect('192.168.1.16', username='deck', password='321127', timeout=10)
    sftp = ssh.open_sftp()
    local_path = r"d:\project831\JapaneseGrammarApp\scripts\test_portal.png"
    sftp.get('/home/deck/Pictures/Screenshot_20260824_022413.png', local_path)
    sftp.close()
    ssh.close()
    print("Downloaded to", local_path, "Size:", os.path.getsize(local_path))

if __name__ == '__main__':
    fetch_img()
