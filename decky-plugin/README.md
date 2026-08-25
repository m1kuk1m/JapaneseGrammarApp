# YomiDeck (decky-yomi-sync)

[![GitHub Release](https://img.shields.io/github/v/release/m1kuk1m/decky-yomi-sync?style=flat-square&color=blue)](https://github.com/m1kuk1m/decky-yomi-sync/releases)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPL%203.0-green.svg?style=flat-square)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Steam%20Deck%20%7C%20SteamOS-informational?style=flat-square&logo=steam)](https://store.steampowered.com/steamdeck)
[![Companion App](https://img.shields.io/badge/Companion-YomiLLM%20(Android)-purple.svg?style=flat-square)](https://github.com/m1kuk1m/YomiLLM)

YomiDeck is a Decky Loader plugin for Steam Deck that captures in-game screenshots and wirelessly synchronizes them to the [YomiLLM](https://github.com/m1kuk1m/YomiLLM) Android application for real-time Japanese text recognition (OCR), furigana annotation, sentence grammar breakdown, and AI-powered dialogue parsing.

---

## Features

* **Zero-Lag Wireless Sync**: In-game screenshots are pushed directly to your phone over local Wi-Fi or phone hotspot in milliseconds.
* **Zero-Configuration Discovery (mDNS)**: Automatically detects your Android phone running YomiLLM across your local network without manual IP configuration.
* **Secure 4-Digit PIN Pairing**: Token-based authentication ensures only paired Steam Deck devices can transfer screenshots.
* **Native SteamOS Integration**: Works seamlessly with Steam Deck controller shortcuts (e.g. Back Grip L4/R4 mapped to Steam + R1), QAM side panel, or manual Quick Capture.
* **Ultra Lightweight**: Asynchronous Python backend with minimal resource footprint during gameplay.

---

## Installation

### Option 1: One-Line Terminal Install (Recommended)

1. Switch to **Desktop Mode** on your Steam Deck.
2. Open **Konsole** terminal and run:
   ```bash
   curl -sSL https://raw.githubusercontent.com/m1kuk1m/decky-yomi-sync/main/install.sh | bash
   ```
   *(Or download the latest `decky-yomi-sync.zip` from [Releases](https://github.com/m1kuk1m/decky-yomi-sync/releases) and extract into `~/homebrew/plugins/decky-yomi-sync`)*
3. Return to **Gaming Mode**.

### Option 2: Manual Installation

1. Download `decky-yomi-sync.zip` from the [Latest Release](https://github.com/m1kuk1m/decky-yomi-sync/releases/latest).
2. Switch to **Desktop Mode**.
3. Extract the zip archive to your Decky plugins directory:
   ```bash
   mkdir -p ~/homebrew/plugins
   unzip -o /path/to/decky-yomi-sync.zip -d ~/homebrew/plugins/
   ```
4. Return to **Gaming Mode**.

---

## Pairing & Usage

### 1. Enable Service on Android Phone
1. Open **[YomiLLM](https://github.com/m1kuk1m/YomiLLM)** on your Android phone.
2. Go to **Settings** -> **Steam Deck Drop**.
3. Toggle **Enable Sync Service** ON and note the **4-digit PIN** displayed on screen.

### 2. Connect from Steam Deck
1. Press the `...` (Quick Access Menu) button on your Steam Deck.
2. Scroll to the **Decky** tab and select **YomiDeck**.
3. Click **Auto-Discover Phone (mDNS)** (or enter your phone IP manually).
4. Enter the 4-digit PIN and click **Pair & Connect**.
5. Once paired, status will show **Connected / Paired**.

### 3. In-Game Gameplay
1. Open any Japanese game / Visual Novel on Steam Deck.
2. Configure your Steam Controller layout (e.g., set `L4` or `R4` to **Take Screenshot** `Steam + R1`).
3. Press the back grip button whenever you see Japanese text you want to analyze.
4. Your Android phone will immediately wake up, display the captured scene, highlight dialogues with `PP-OCRv4`, and perform AI grammar analysis!

---

## Development

```bash
# Clone the repository
git clone https://github.com/m1kuk1m/decky-yomi-sync.git
cd decky-yomi-sync

# Install Node.js dependencies
npm install

# Build the TypeScript frontend bundle
npm run build

# Watch mode for rapid UI development
npm run watch
```

---

## Related Projects

* **[YomiLLM (Android App)](https://github.com/m1kuk1m/YomiLLM)**: Offline/Online Japanese visual novel & manga grammar analyzer powered by Gemini / DeepSeek / Claude / Local LLMs and PaddleOCR.

---

## License

Distributed under the **GNU General Public License v3.0 (GPL-3.0)**. See [`LICENSE`](LICENSE) for more details.
