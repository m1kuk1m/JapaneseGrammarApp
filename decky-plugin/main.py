import os
import sys
import json
import glob
import time
import socket
import asyncio
import threading
import subprocess
import concurrent.futures
import urllib.request
import urllib.parse
import urllib.error
from typing import Optional, Dict, Any, Tuple, List

# Ensure py_modules is in sys.path
PY_MODULES_DIR = os.path.join(os.path.dirname(__file__), "py_modules")
if PY_MODULES_DIR not in sys.path:
    sys.path.append(PY_MODULES_DIR)

try:
    import decky  # type: ignore
    LOGGER = decky.logger
except Exception:
    class FallbackLogger:
        def info(self, msg): print(f"[YomiDeck:INFO] {msg}")
        def error(self, msg): print(f"[YomiDeck:ERROR] {msg}")
        def warn(self, msg): print(f"[YomiDeck:WARN] {msg}")
    LOGGER = FallbackLogger()

try:
    from zeroconf_helper import discover_yomillm_service
except Exception as e:
    def discover_yomillm_service(timeout=2.5):
        return None

try:
    from watchdog.observers import Observer
    from watchdog.events import FileSystemEventHandler
    WATCHDOG_AVAILABLE = True
except Exception:
    WATCHDOG_AVAILABLE = False


def sanitize_ip(ip: str) -> str:
    if not ip:
        return ""
    ip = ip.strip()
    if ip.startswith("http://"):
        ip = ip[7:]
    elif ip.startswith("https://"):
        ip = ip[8:]
    ip = ip.split("/")[0].split(":")[0].strip()
    return ip


def get_lan_subnets() -> list:
    subnets = set()
    try:
        out = subprocess.check_output(["ip", "-4", "addr", "show"], text=True)
        for line in out.splitlines():
            line = line.strip()
            # Ignore loopback 127.x.x.x and clash/meta TUN 198.18.x.x
            if line.startswith("inet ") and "127.0.0.1" not in line and "198.18." not in line:
                parts = line.split()[1].split("/")[0].split(".")
                if len(parts) == 4:
                    subnets.add(".".join(parts[:3]))
    except Exception as e:
        LOGGER.warn(f"Error detecting network subnets: {e}")
    if not subnets:
        subnets.add("192.168.1")
        subnets.add("192.168.0")
        subnets.add("192.168.31")
    return list(subnets)


def probe_single_ip(ip: str, port: int = 8765) -> Optional[Tuple[str, int]]:
    try:
        url = f"http://{ip}:{port}/api/v1/ping"
        req = urllib.request.Request(url, headers={"User-Agent": "YomiDeck-Probe"})
        with urllib.request.urlopen(req, timeout=0.8) as resp:
            if resp.status == 200:
                data = json.loads(resp.read().decode())
                if data.get("app") == "YomiLLM":
                    return ip, port
    except Exception:
        pass
    return None


class PollingWatcher(threading.Thread):
    """Low CPU background polling watcher with fast response"""
    IMAGE_PATTERNS = ("*.jpg", "*.jpeg", "*.png", "*.JPG", "*.JPEG", "*.PNG")

    def __init__(self, watch_dir: str, callback, interval: float = 0.2):
        super().__init__(daemon=True)
        self.watch_dir = watch_dir
        self.callback = callback
        self.interval = interval
        self.running = True
        self.known_files = {}
        self._init_files()

    def _scan_files(self) -> List[str]:
        found = []
        for ext in self.IMAGE_PATTERNS:
            found.extend(glob.glob(os.path.join(self.watch_dir, "**", ext), recursive=True))
        return [f for f in found if "thumbnails" not in f]

    def _init_files(self):
        try:
            for f in self._scan_files():
                try:
                    self.known_files[f] = os.path.getmtime(f)
                except OSError:
                    pass
        except Exception as e:
            LOGGER.error(f"Error scanning initial files: {e}")

    def run(self):
        while self.running:
            try:
                for f in self._scan_files():
                    try:
                        mtime = os.path.getmtime(f)
                        if f not in self.known_files:
                            self.known_files[f] = mtime
                            time.sleep(0.08)
                            self.callback(f)
                        elif mtime > self.known_files[f]:
                            self.known_files[f] = mtime
                            time.sleep(0.08)
                            self.callback(f)
                    except OSError:
                        pass
            except Exception as e:
                LOGGER.error(f"Polling error: {e}")
            time.sleep(self.interval)

    def stop(self):
        self.running = False


def get_config_paths() -> List[str]:
    paths = []
    try:
        import decky
        if hasattr(decky, "DECKY_PLUGIN_SETTINGS_DIR") and decky.DECKY_PLUGIN_SETTINGS_DIR:
            paths.append(os.path.join(decky.DECKY_PLUGIN_SETTINGS_DIR, "settings.json"))
    except Exception:
        pass
    paths.append("/home/deck/homebrew/settings/decky-yomi-sync/settings.json")
    paths.append("/home/deck/.config/yomi-deck-sync.json")
    paths.append(os.path.expanduser("~/.config/yomi-deck-sync.json"))
    return list(dict.fromkeys(paths))


class Plugin:
    def __init__(self):
        self.phone_ip = ""
        self.phone_port = 8765
        self.auth_token = ""
        self.auto_upload_enabled = True
        self.hotkey_name = "F12 (Steam Screenshot / Back Grip)"
        self.last_sync_status = "Not initialized"
        self.watch_dir = "/home/deck/.local/share/Steam/userdata" if os.path.exists("/home/deck/.local/share/Steam/userdata") else os.path.expanduser("~/.local/share/Steam/userdata")
        self.observer = None
        self.polling_watcher = None
        self.loop = None
        self.load_config()

    async def _main(self):
        LOGGER.info("YomiDeck Plugin initializing...")
        self.loop = asyncio.get_event_loop()
        self.start_file_watcher()
        self.last_sync_status = "Ready" if self.auth_token else "Need pairing"

    async def _unload(self):
        LOGGER.info("YomiDeck Plugin unloading...")
        if self.observer:
            try:
                self.observer.stop()
                self.observer.join()
            except Exception:
                pass
        if self.polling_watcher:
            try:
                self.polling_watcher.stop()
            except Exception:
                pass

    def load_config(self):
        for path in get_config_paths():
            try:
                if os.path.exists(path):
                    with open(path, "r", encoding="utf-8") as f:
                        data = json.load(f)
                        self.phone_ip = sanitize_ip(data.get("phone_ip", ""))
                        self.phone_port = data.get("phone_port", 8765)
                        self.auth_token = data.get("auth_token", "")
                        self.auto_upload_enabled = data.get("auto_upload_enabled", True)
                        self.hotkey_name = data.get("hotkey_name", "F12 (Steam Screenshot / Back Grip)")
                        LOGGER.info(f"Loaded config from {path}: phone_ip={self.phone_ip}, is_paired={bool(self.auth_token)}, auto_upload={self.auto_upload_enabled}")
                        return
            except Exception as e:
                LOGGER.error(f"Config load error from {path}: {e}")

    def save_config(self):
        payload = {
            "phone_ip": sanitize_ip(self.phone_ip),
            "phone_port": self.phone_port,
            "auth_token": self.auth_token,
            "auto_upload_enabled": self.auto_upload_enabled,
            "hotkey_name": self.hotkey_name
        }
        for path in get_config_paths():
            try:
                os.makedirs(os.path.dirname(path), exist_ok=True)
                with open(path, "w", encoding="utf-8") as f:
                    json.dump(payload, f, indent=2)
                # Ensure permissions on /home/deck
                if path.startswith("/home/deck"):
                    try:
                        import pwd
                        deck_uid = pwd.getpwnam("deck").pw_uid
                        deck_gid = pwd.getpwnam("deck").pw_gid
                        os.chown(path, deck_uid, deck_gid)
                    except Exception:
                        pass
                LOGGER.info(f"Saved config to {path}")
            except Exception as e:
                LOGGER.error(f"Config save error to {path}: {e}")

    def on_screenshot_detected(self, file_path: str):
        if not self.auto_upload_enabled:
            LOGGER.info(f"Screenshot detected ({file_path}), but auto-upload is disabled. Skipping.")
            return

        LOGGER.info(f"New screenshot detected: {file_path}")
        if self.loop and self.loop.is_running():
            asyncio.run_coroutine_threadsafe(
                self.send_screenshot(file_path),
                self.loop
            )

    def start_file_watcher(self):
        if not os.path.exists(self.watch_dir):
            LOGGER.warn(f"Steam screenshot directory does not exist yet: {self.watch_dir}")
            os.makedirs(self.watch_dir, exist_ok=True)

        if WATCHDOG_AVAILABLE:
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
                                self.outer.on_screenshot_detected(event.src_path)

                self.observer = Observer()
                self.observer.schedule(WatchdogHandler(self), self.watch_dir, recursive=True)
                self.observer.start()
                LOGGER.info(f"Screenshot observer (Watchdog) started on: {self.watch_dir}")
                return
            except Exception as e:
                LOGGER.warn(f"Watchdog failed to start, falling back to PollingWatcher: {e}")

        # Fallback to PollingWatcher
        self.polling_watcher = PollingWatcher(self.watch_dir, self.on_screenshot_detected, interval=0.5)
        self.polling_watcher.start()
        LOGGER.info(f"Screenshot observer (PollingWatcher) started on: {self.watch_dir}")

    async def get_status(self, *args, **kwargs) -> Dict[str, Any]:
        return {
            "phone_ip": self.phone_ip,
            "phone_port": self.phone_port,
            "is_paired": bool(self.auth_token and self.phone_ip),
            "auto_upload_enabled": self.auto_upload_enabled,
            "last_sync_status": self.last_sync_status,
            "hotkey_name": self.hotkey_name
        }

    async def set_auto_upload(self, enabled: Any = True, *args, **kwargs) -> Dict[str, Any]:
        if isinstance(enabled, dict):
            val = enabled.get("enabled", True)
        else:
            val = enabled
        self.auto_upload_enabled = bool(val)
        self.save_config()
        LOGGER.info(f"Auto-upload toggled: {self.auto_upload_enabled}")
        return {"success": True, "auto_upload_enabled": self.auto_upload_enabled}

    async def unpair_device(self, *args, **kwargs) -> Dict[str, Any]:
        LOGGER.info("Unpairing device...")
        self.auth_token = ""
        self.last_sync_status = "Disconnected"
        self.save_config()
        return {"success": True}

    async def discover_phone(self, *args, **kwargs) -> Optional[Dict[str, Any]]:
        LOGGER.info("Starting phone discovery...")
        mdns_result = discover_yomillm_service(timeout=1.5)
        if mdns_result:
            ip, port = mdns_result
            self.phone_ip = sanitize_ip(ip)
            self.phone_port = port
            LOGGER.info(f"mDNS discovered phone at: {self.phone_ip}:{self.phone_port}")
            return {"ip": self.phone_ip, "port": port}

        subnets = get_lan_subnets()
        LOGGER.info(f"mDNS unavailable, probing LAN subnets: {subnets}")
        
        target_port = int(self.phone_port or 8765)

        def run_subnet_scan():
            for prefix in subnets:
                with concurrent.futures.ThreadPoolExecutor(max_workers=80) as executor:
                    futures = [executor.submit(probe_single_ip, f"{prefix}.{i}", target_port) for i in range(1, 255)]
                    for f in concurrent.futures.as_completed(futures):
                        try:
                            res = f.result()
                            if res:
                                return res
                        except Exception:
                            pass
            return None

        found = await asyncio.get_event_loop().run_in_executor(None, run_subnet_scan)
        if found:
            ip, port = found
            self.phone_ip = sanitize_ip(ip)
            self.phone_port = port
            LOGGER.info(f"Subnet probe discovered phone at: {self.phone_ip}:{self.phone_port}")
            return {"ip": self.phone_ip, "port": port}

        LOGGER.warn("Discovery finished: No phone found on LAN")
        return None

    async def run_diagnostics(self, ip: Any = "", port: int = 8765, **kwargs) -> Dict[str, Any]:
        if isinstance(ip, dict):
            port = ip.get("port", port)
            ip = ip.get("ip", "")
        if "ip" in kwargs:
            ip = kwargs["ip"]
        if "port" in kwargs:
            port = kwargs["port"]

        target_ip = sanitize_ip(str(ip))
        if not target_ip:
            return {"success": False, "logs": ["[ERROR] Invalid target IP address."]}

        logs = []
        logs.append(f"[Start] Network diagnostics for {target_ip}:{port}")

        def do_diag():
            # 1. Deck Local IP
            try:
                out = subprocess.check_output(["ip", "-4", "addr", "show"], text=True)
                local_ips = []
                for line in out.splitlines():
                    if "inet " in line and "127.0.0.1" not in line and "198.18." not in line:
                        local_ips.append(line.strip().split()[1])
                logs.append(f"[Local IP] Steam Deck: {', '.join(local_ips) if local_ips else 'Unknown'}")
            except Exception as e:
                logs.append(f"[Local IP] Failed: {e}")

            # 2. Ping Test
            try:
                res = subprocess.run(["ping", "-c", "2", "-W", "1", target_ip], capture_output=True, text=True)
                if res.returncode == 0:
                    for line in res.stdout.splitlines():
                        if "rtt" in line or "round-trip" in line or "time=" in line:
                            logs.append(f"[Ping] {target_ip}: OK ({line.strip()})")
                            break
                    else:
                        logs.append(f"[Ping] {target_ip}: OK (0% packet loss)")
                else:
                    logs.append(f"[Ping] {target_ip}: WARN (100% loss). Please check if Wi-Fi matches.")
            except Exception as e:
                logs.append(f"[Ping] Failed: {e}")

            # 3. TCP Port Connect
            tcp_ok = False
            t0 = time.time()
            try:
                sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                sock.settimeout(2.5)
                sock.connect((target_ip, int(port)))
                sock.close()
                elapsed = int((time.time() - t0) * 1000)
                logs.append(f"[TCP Port {port}] Handshake OK ({elapsed}ms)")
                tcp_ok = True
            except socket.timeout:
                logs.append(f"[TCP Port {port}] ERROR: Connection timed out.")
            except ConnectionRefusedError:
                logs.append(f"[TCP Port {port}] ERROR: Connection refused (Drop service is OFF on phone).")
            except Exception as e:
                logs.append(f"[TCP Port {port}] ERROR: {e}")

            # 4. HTTP Application Probe
            if tcp_ok:
                try:
                    url = f"http://target_ip:{port}/api/v1/ping".replace("target_ip", target_ip)
                    req = urllib.request.Request(url, headers={"User-Agent": "YomiDeck-Diag"})
                    t0 = time.time()
                    with urllib.request.urlopen(req, timeout=3.0) as resp:
                        elapsed = int((time.time() - t0) * 1000)
                        body = resp.read().decode("utf-8", errors="replace")
                        logs.append(f"[HTTP /api/v1/ping] OK {resp.status} ({elapsed}ms) -> {body}")
                except urllib.error.HTTPError as e:
                    logs.append(f"[HTTP /api/v1/ping] WARN: HTTP {e.code} ({e.reason})")
                except urllib.error.URLError as e:
                    if "timed out" in str(e).lower():
                        logs.append(f"[HTTP /api/v1/ping] ERROR: Timeout. Check phone VPN / network.")
                    else:
                        logs.append(f"[HTTP /api/v1/ping] ERROR: {e}")
                except Exception as e:
                    logs.append(f"[HTTP /api/v1/ping] ERROR: {e}")
            else:
                logs.append("[HTTP /api/v1/ping] Skipped (TCP port not open)")

            return logs

        res_logs = await asyncio.get_event_loop().run_in_executor(None, do_diag)
        return {"success": True, "logs": res_logs}

    async def pair_device(self, ip: Any = "", port: int = 8765, pin: str = "", **kwargs) -> Dict[str, Any]:
        if isinstance(ip, dict):
            port = ip.get("port", port)
            pin = ip.get("pin", pin)
            ip = ip.get("ip", "")
        if "ip" in kwargs:
            ip = kwargs["ip"]
        if "port" in kwargs:
            port = kwargs["port"]
        if "pin" in kwargs:
            pin = kwargs["pin"]

        clean_ip = sanitize_ip(str(ip))
        if not clean_ip:
            return {"success": False, "message": "Empty or invalid IP address"}

        url = f"http://{clean_ip}:{port}/api/v1/pair"
        payload = json.dumps({"pin": str(pin).strip()}).encode("utf-8")
        req = urllib.request.Request(
            url,
            data=payload,
            headers={"Content-Type": "application/json", "User-Agent": "YomiDeck/1.0"}
        )

        try:
            def do_request():
                with urllib.request.urlopen(req, timeout=5) as resp:
                    return resp.read().decode("utf-8")

            res_body = await asyncio.get_event_loop().run_in_executor(None, do_request)
            data = json.loads(res_body)
            if data.get("status") == "paired" and "token" in data:
                self.phone_ip = clean_ip
                self.phone_port = port
                self.auth_token = data["token"]
                self.save_config()
                self.last_sync_status = "Connected"
                return {"success": True, "token": self.auth_token}
            return {"success": False, "message": data.get("message", "Invalid response from phone")}
        except urllib.error.HTTPError as e:
            msg = "PIN code incorrect" if e.code == 401 else f"HTTP error {e.code}"
            self.last_sync_status = f"Pair failed: {msg}"
            return {"success": False, "message": msg}
        except urllib.error.URLError as e:
            if "timed out" in str(e).lower():
                msg = "Connection timed out. Please check if Drop Service is running."
            elif "refused" in str(e).lower():
                msg = "Connection refused. Please ensure Drop Service is enabled on phone."
            else:
                msg = str(e.reason if hasattr(e, 'reason') else e)
            self.last_sync_status = f"Pair failed: {msg}"
            return {"success": False, "message": msg}
        except Exception as e:
            self.last_sync_status = f"Pair error: {e}"
            return {"success": False, "message": str(e)}

    async def send_screenshot(self, file_path: str) -> bool:
        clean_ip = sanitize_ip(self.phone_ip)
        if not self.auth_token or not clean_ip:
            self.last_sync_status = "Skipped: Not paired"
            return False

        url = f"http://{clean_ip}:{self.phone_port}/api/v1/screenshot"
        boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"

        ext = os.path.splitext(file_path)[1].lower()
        mime_type = "image/png" if ext == ".png" else "image/jpeg"
        filename = f"screenshot{ext}" if ext in [".png", ".jpg", ".jpeg"] else "screenshot.jpg"

        try:
            def do_upload():
                with open(file_path, "rb") as f:
                    file_bytes = f.read()

                body = (
                    f"--{boundary}\r\n"
                    f'Content-Disposition: form-data; name="image"; filename="{filename}"\r\n'
                    f"Content-Type: {mime_type}\r\n\r\n"
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

                with urllib.request.urlopen(req, timeout=6) as resp:
                    return resp.status == 200

            success = await asyncio.get_event_loop().run_in_executor(None, do_upload)
            if success:
                self.last_sync_status = f"Synced at {time.strftime('%H:%M:%S')}"
                return True
            else:
                self.last_sync_status = "Upload failed"
                return False
        except Exception as e:
            self.last_sync_status = f"Sync failed: {e}"
            LOGGER.error(f"Send screenshot failed: {e}")
            return False

    def capture_via_portal(self) -> Optional[str]:
        """Trigger screenshot via xdg-desktop-portal-gamescope (Vulkan Compositor Direct Capture)"""
        t0 = time.time() - 0.2
        try:
            cmd = [
                "gdbus", "call",
                "--address", "unix:path=/run/user/1000/bus",
                "--dest", "org.freedesktop.portal.Desktop",
                "--object-path", "/org/freedesktop/portal/desktop",
                "--method", "org.freedesktop.portal.Screenshot.Screenshot",
                "",
                "{'interactive': <false>}"
            ]
            res = subprocess.run(cmd, capture_output=True, text=True, timeout=3)
            LOGGER.info(f"Portal screenshot response: {res.stdout.strip()}")

            search_dirs = list(dict.fromkeys(["/home/deck/Pictures", os.path.expanduser("~/Pictures")]))
            for _ in range(30):
                time.sleep(0.08)
                for p_dir in search_dirs:
                    if not os.path.exists(p_dir):
                        continue
                    for f in glob.iglob(os.path.join(p_dir, "**", "*.png"), recursive=True):
                        try:
                            if os.path.getmtime(f) >= t0 and os.path.getsize(f) > 0:
                                LOGGER.info(f"Found portal screenshot: {f} ({os.path.getsize(f)} bytes)")
                                return f
                        except OSError:
                            pass
        except Exception as e:
            LOGGER.error(f"capture_via_portal error: {e}")
        return None

    async def trigger_live_capture(self, *args, **kwargs) -> Dict[str, Any]:
        clean_ip = sanitize_ip(self.phone_ip)
        if not self.auth_token or not clean_ip:
            return {"success": False, "message": "Not paired with phone"}

        LOGGER.info("Starting live screen capture workflow...")
        # 1. Wait 200ms for sidebar/overlay to completely fade out
        await asyncio.sleep(0.2)

        # 2. Trigger Gamescope Vulkan compositor capture via Portal API
        captured_file = await asyncio.get_event_loop().run_in_executor(
            None, self.capture_via_portal
        )

        if captured_file and os.path.exists(captured_file):
            LOGGER.info(f"Uploading portal screenshot: {captured_file}")
            success = await self.send_screenshot(captured_file)
            if success:
                return {"success": True, "message": f"Captured & sent {os.path.basename(captured_file)}"}
            return {"success": False, "message": "Captured screen but failed to upload to phone"}

        LOGGER.warn("Portal capture failed, falling back to latest existing file")
        return await self.trigger_test_sync()

    async def trigger_test_sync(self, *args, **kwargs) -> Dict[str, Any]:
        files = []
        for ext in ("*.jpg", "*.jpeg", "*.png", "*.JPG", "*.JPEG", "*.PNG"):
            files.extend(glob.glob(os.path.join(self.watch_dir, "**", ext), recursive=True))
        files = [f for f in files if "thumbnails" not in f]
        if files:
            files.sort(key=os.path.getmtime, reverse=True)
            target = files[0]
            success = await self.send_screenshot(target)
            return {"success": success, "message": f"Sent {os.path.basename(target)}"}
        return {"success": False, "message": "No screenshot found to send"}

