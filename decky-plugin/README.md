# YomiDeck - Steam Deck Drop Plugin for YomiLLM

YomiDeck is a [Decky Loader](https://decky.xyz/) plugin for Steam Deck that captures in-game screenshots and wirelessly synchronizes them to the **YomiLLM** Android app for real-time Japanese text recognition, grammar breakdown, and audio pronunciation.

---

## Features

* **Zero-Lag Wireless Sync**: In-game screenshots are pushed to your phone over local Wi-Fi or phone hotspot in milliseconds.
* **Zero Configuration Discovery**: Automatically locates your Android phone using mDNS (`_yomillm._tcp`).
* **Secure Authentication**: 4-digit PIN pairing ensures only your Steam Deck can send screenshots to your phone.
* **Native SteamOS Compatibility**: Uses lightweight filesystem monitoring (`watchdog`) on Steam's screenshot directory.

---

## Installation & Setup

### 1. Requirements
* Steam Deck with [Decky Loader](https://decky.xyz/) installed.
* Android phone running YomiLLM with **Steam Deck Drop** enabled in Settings.
* Both devices connected to the same Wi-Fi network or phone hotspot.

### 2. Install the Plugin on Steam Deck

1. Switch to **Desktop Mode** on your Steam Deck.
2. Clone or copy the `decky-plugin/` directory to `~/homebrew/plugins/decky-yomi-sync`:
   ```bash
   cp -r /path/to/decky-plugin ~/homebrew/plugins/decky-yomi-sync
   ```
3. Return to **Gaming Mode**.

### 3. Pair with Phone

1. On your phone: Open **YomiLLM** -> **Settings** -> **Steam Deck Drop** -> Turn ON the service and note the 4-digit PIN.
2. On your Steam Deck:
   - Press the `...` (QAM) button and open the **Decky** tab.
   - Select **YomiDeck**.
   - Click **Auto-Discover Phone (mDNS)** or enter the IP shown on your phone.
   - Enter the 4-digit PIN and click **Pair & Connect**.

### 4. How to Use

* In any game, configure your controller settings (e.g. Back Grip `L4` or `R4`) to trigger **Take Screenshot** (`Steam + R1`).
* When you encounter unfamiliar Japanese text, press the back grip key.
* Your phone will instantly receive the screenshot, highlight the dialogue boxes with `PP-OCRv4`, and parse the grammar upon confirmation!
