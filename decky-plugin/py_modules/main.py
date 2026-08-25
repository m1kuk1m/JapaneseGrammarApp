import os
import json
import glob
import time
import asyncio
import urllib.request
import urllib.parse
from typing import Optional, Dict, Any

from zeroconf_helper import discover_yomillm_service

try:
    from watchdog.observers import Observer
    from watchdog.events import FileSystemEventHandler
    WATCHDOG_AVAILABLE = True
except ImportError:
    WATCHDOG_AVAILABLE = False


class ScreenshotHandler:
    def __init__(self, callback):
        self.callback = callback
        self.processed_files = set()

    def on_created(self, event):
        if not event.is_directory and event.src_path.lower().endswith(('.jpg', '.jpeg', '.png')):
            if "thumbnails" not in event.src_path and event.src_path not in self.processed_files:
                self.processed_files.add(event.src_path)
                # Ensure file write is finalized
                time.sleep(0.15)
                self.callback(event.src_path)


class Plugin:
    def __init__(self):
        self.phone_ip = ""
        self.phone_port = 8765
        self.auth_token = ""
        self.last_sync_status = "Not initialized"
        self.config_path = os.path.expanduser("~/.config/yomi-deck-sync.json")
        self.watch_dir = os.path.expanduser("~/.local/share/Steam/userdata")
        self.observer = None
        self.loop = None
        self.load_config()

    async def _main(self):
        self.loop = asyncio.get_event_loop()
        self.start_file_watcher()
        self.last_sync_status = "Idle" if self.auth_token else "Need pairing"

    async def _unload(self):
        if self.observer:
            self.observer.stop()
            self.observer.join()

    def load_config(self):
        try:
            if os.path.exists(self.config_path):
                with open(self.config_path, "r", encoding="utf-8") as f:
                    data = json.load(f)
                    self.phone_ip = data.get("phone_ip", "")
                    self.phone_port = data.get("phone_port", 8765)
                    self.auth_token = data.get("auth_token", "")
        except Exception as e:
            print(f"[YomiDeck] Config load error: {e}")

    def save_config(self):
        try:
            os.makedirs(os.path.dirname(self.config_path), exist_ok=True)
            with open(self.config_path, "w", encoding="utf-8") as f:
                json.dump({
                    "phone_ip": self.phone_ip,
                    "phone_port": self.phone_port,
                    "auth_token": self.auth_token
                }, f, indent=2)
        except Exception as e:
            print(f"[YomiDeck] Config save error: {e}")

    def start_file_watcher(self):
        if not WATCHDOG_AVAILABLE or not os.path.exists(self.watch_dir):
            print(f"[YomiDeck] Watchdog not available or directory missing: {self.watch_dir}")
            return

        try:
            class WatchdogHandler(FileSystemEventHandler):
                def __init__(self, outer):
                    self.outer = outer
                    self.seen = set()

                def on_created(self, event):
                    if not event.is_directory and event.src_path.lower().endswith(('.jpg', '.jpeg', '.png')):
                        if "thumbnails" not in event.src_path and event.src_path not in self.seen:
                            self.seen.add(event.src_path)
                            time.sleep(0.2)
                            if self.outer.loop and self.outer.loop.is_running():
                                asyncio.run_coroutine_threadsafe(
                                    self.outer.send_screenshot(event.src_path),
                                    self.outer.loop
                                )

            self.observer = Observer()
            self.observer.schedule(WatchdogHandler(self), self.watch_dir, recursive=True)
            self.observer.start()
            print(f"[YomiDeck] Screenshot observer started on: {self.watch_dir}")
        except Exception as e:
            print(f"[YomiDeck] Error starting observer: {e}")

    async def get_status(self) -> Dict[str, Any]:
        return {
            "phone_ip": self.phone_ip,
            "phone_port": self.phone_port,
            "is_paired": bool(self.auth_token and self.phone_ip),
            "last_sync_status": self.last_sync_status
        }

    async def discover_phone(self) -> Optional[Dict[str, Any]]:
        result = discover_yomillm_service(timeout=2.5)
        if result:
            ip, port = result
            self.phone_ip = ip
            self.phone_port = port
            return {"ip": ip, "port": port}
        return None

    async def pair_device(self, ip: str, port: int, pin: str) -> Dict[str, Any]:
        url = f"http://{ip}:{port}/api/v1/pair"
        payload = json.dumps({"pin": pin}).encode("utf-8")
        req = urllib.request.Request(
            url,
            data=payload,
            headers={"Content-Type": "application/json"}
        )

        try:
            def do_request():
                with urllib.request.urlopen(req, timeout=4) as resp:
                    return resp.read().decode("utf-8")

            res_body = await asyncio.get_event_loop().run_in_executor(None, do_request)
            data = json.loads(res_body)
            if data.get("status") == "paired" and "token" in data:
                self.phone_ip = ip
                self.phone_port = port
                self.auth_token = data["token"]
                self.save_config()
                self.last_sync_status = "Paired & Connected"
                return {"success": True, "token": self.auth_token}
            return {"success": False, "message": "Invalid response"}
        except urllib.error.HTTPError as e:
            msg = "Authentication failed (invalid PIN)" if e.code == 401 else f"HTTP error {e.code}"
            return {"success": False, "message": msg}
        except Exception as e:
            return {"success": False, "message": str(e)}

    async def send_screenshot(self, file_path: str) -> bool:
        if not self.auth_token or not self.phone_ip:
            self.last_sync_status = "Skipped: Not paired"
            return False

        url = f"http://{self.phone_ip}:{self.phone_port}/api/v1/screenshot"
        boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"

        try:
            def do_upload():
                with open(file_path, "rb") as f:
                    file_bytes = f.read()

                body = (
                    f"--{boundary}\r\n"
                    f'Content-Disposition: form-data; name="image"; filename="screenshot.jpg"\r\n'
                    f"Content-Type: image/jpeg\r\n\r\n"
                ).encode("utf-8") + file_bytes + f"\r\n--{boundary}--\r\n".encode("utf-8")

                req = urllib.request.Request(
                    url,
                    data=body,
                    headers={
                        "Content-Type": f"multipart/form-data; boundary={boundary}",
                        "X-Auth-Token": self.auth_token,
                        "User-Agent": "YomiDeck/1.0"
                    }
                )

                with urllib.request.urlopen(req, timeout=3) as resp:
                    return resp.status == 200

            success = await asyncio.get_event_loop().run_in_executor(None, do_upload)
            if success:
                self.last_sync_status = f"Synced at {time.strftime('%H:%M:%S')}"
                return True
            else:
                self.last_sync_status = "Upload returned non-200"
                return False
        except Exception as e:
            self.last_sync_status = f"Sync failed: {e}"
            print(f"[YomiDeck] Send screenshot failed: {e}")
            return False

    async def trigger_test_sync(self) -> Dict[str, Any]:
        # Look for the latest screenshot file
        pattern = os.path.join(self.watch_dir, "**", "*.jpg")
        files = glob.glob(pattern, recursive=True)
        files = [f for f in files if "thumbnails" not in f]
        if files:
            files.sort(key=os.path.getmtime, reverse=True)
            target = files[0]
            success = await self.send_screenshot(target)
            return {"success": success}
        else:
            # Create a 1x1 test JPEG if no screenshots exist yet
            test_file = "/tmp/yomi_test.jpg"
            # Minimal 1x1 JPEG binary
            minimal_jpeg = bytes.fromhex(
                "ffd8ffe000104a46494600010101006000600000ffdb004300080606070605080707070909"
                "080a0c140d0c0b0b0c1912130f141d1a1f1e1d1a1c1c20242e2720222c231c1c2837292c30"
                "313434341f27393d38323c2e333430ffc0000b080001000101011100ffc4001f0000010501"
                "010101010100000000000000000102030405060708090a0bffda0008010100003f007f00ffd9"
            )
            with open(test_file, "wb") as f:
                f.write(minimal_jpeg)
            success = await self.send_screenshot(test_file)
            return {"success": success}
