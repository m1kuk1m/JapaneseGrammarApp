import socket
import time
from typing import Optional, Tuple

try:
    from zeroconf import Zeroconf, ServiceBrowser
    ZEROCONF_AVAILABLE = True
except ImportError:
    ZEROCONF_AVAILABLE = False


class YomiServiceListener:
    def __init__(self):
        self.found_ip: Optional[str] = None
        self.found_port: int = 8765

    def add_service(self, zc: "Zeroconf", type_: str, name: str) -> None:
        try:
            info = zc.get_service_info(type_, name)
            if info and info.addresses:
                self.found_ip = socket.inet_ntoa(info.addresses[0])
                self.found_port = info.port
        except Exception:
            pass

    def update_service(self, zc: "Zeroconf", type_: str, name: str) -> None:
        pass

    def remove_service(self, zc: "Zeroconf", type_: str, name: str) -> None:
        pass


def discover_yomillm_service(timeout: float = 3.0) -> Optional[Tuple[str, int]]:
    if not ZEROCONF_AVAILABLE:
        return None

    try:
        zc = Zeroconf()
        listener = YomiServiceListener()
        browser = ServiceBrowser(zc, "_yomillm._tcp.local.", listener)
        start = time.time()
        while time.time() - start < timeout:
            if listener.found_ip:
                break
            time.sleep(0.1)
        zc.close()
        if listener.found_ip:
            return listener.found_ip, listener.found_port
    except Exception as e:
        print(f"[YomiDeck] Error in mDNS discovery: {e}")
    return None
