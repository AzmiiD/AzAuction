# 🛒 AzAuction - Advanced Auction House Plugin

AzAuction is a modern, feature-rich, and user-friendly Auction House plugin for Spigot/Paper Minecraft servers. Designed for performance and flexibility, it supports in-game GUI trading, web interface, and advanced permission controls — all while remaining lightweight.

---

## ✨ Features

- ✅ GUI-based Auction House for easy selling and buying
- 💰 Sell items with `/ah sell <price>` or through the GUI
- 🛍️ Buy items by ID using `/ah buy <id>`
- 🌐 Optional **Web Interface** to browse auctions from your browser
- 🗃️ MySQL & SQLite database support
- ⚙️ Integrated with Vault economy
- 🔁 Hot-reload support via `/ahreload`
- 🧾 Tab completion for smooth command usage
- 🔐 Permission-based access control
- 📝 Fully customizable messages via `messages.yml`

---

## 🖥️ Web Interface

A built-in lightweight web server allows players to browse auctions using a browser.

> 📸 **Preview** *[(Preview](https://i.imgur.com/4X4vumj.png))*:  

Enable it in `config.yml`:
```yaml
auction:
  web-enabled: true
  web-port: 8080
