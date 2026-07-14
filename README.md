# 🌐 Android Browser

A lightweight, modern Android browser built with **Kotlin**, **Jetpack Compose**, and **WebView**.

[![Build APK](https://github.com/d2026714/android-browser/actions/workflows/build.yml/badge.svg)](https://github.com/d2026714/android-browser/actions/workflows/build.yml)

## ✨ Features

### Core Browsing
- 🌐 Full browsing with back/forward/reload
- 📑 Multi-tab with tab counter badge
- 🥷 Incognito mode
- 🔍 Smart URL/search bar

### Content & Organization
- 🔖 Bookmarks (add/delete/browse)
- 📜 History (browse/clear)
- ⚡ Quick links on home page

### Privacy & Security
- 🚫 Built-in ad & tracker blocker
- 🔒 HTTPS/HTTP security indicator
- 🍪 Clear cookies & site data
- 🛡️ Zero analytics, zero telemetry

### Reading & Display
- 📖 Reading mode with adjustable font
- 🌙 Dark mode (light/dark themes)
- 🖥️ Desktop/mobile mode toggle

### Tools
- 🔎 Find in page
- 📄 View page source code
- 📸 Screenshot capture & share
- 📤 Share page via system share
- 📥 Download manager with progress tracking
- 👆 Swipe navigation (left/right gestures)

## 📦 Download

### From GitHub Actions (Recommended)
1. Go to [Actions](https://github.com/d2026714/android-browser/actions)
2. Click the latest ✅ build
3. Download **debug-apk** from Artifacts

### Build Locally
```bash
git clone https://github.com/d2026714/android-browser.git
cd android-browser
./gradlew assembleDebug
```

## 🛠️ Tech Stack

- **Kotlin** + **Jetpack Compose** + **Material 3**
- **MVVM** (ViewModel + StateFlow)
- **WebView** with custom ad blocking
- **Min SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)

## 📁 Project Structure

```
app/src/main/java/com/example/browser/
├── BrowserApp.kt / MainActivity.kt
├── data/
│   ├── model/        (Tab, Bookmark, HistoryItem)
│   └── repository/   (SharedPreferences persistence)
├── ui/
│   ├── components/   (WebView, NavigationBar, FindInPage, ContextMenu)
│   ├── screens/      (Home, Bookmarks, History, Tabs, Settings,
│   │                   Downloads, ReadingMode, ViewSource, SearchEngine)
│   ├── theme/        (Material 3 colors & theme)
│   └── viewmodel/    (BrowserViewModel)
└── util/
    └── AdBlocker.kt  (domain & pattern blocking)
```

## 📋 Changelog

### v1.2.0 (Latest)
- 📥 Download manager UI with progress tracking
- 📄 View page source code
- 📸 Screenshot capture & share
- 🍪 Clear cookies & site data
- 📤 Share page from menu
- 🔗 Long-press context menu for links

### v1.1.0
- 📖 Reading mode
- 👆 Swipe navigation gestures
- 🖥️ Desktop mode toggle
- 🔎 Find in page
- 🌙 Dark mode improvements

### v1.0.0
- Initial release with core browsing features

## 📄 License

MIT License

## 🤝 Contributing

Contributions welcome! Open issues and PRs.
