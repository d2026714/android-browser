# 🌐 Android Browser

A lightweight, modern Android browser built with **Kotlin**, **Jetpack Compose**, and **WebView**.

[![Build APK](https://github.com/d2026714/android-browser/actions/workflows/build.yml/badge.svg)](https://github.com/d2026714/android-browser/actions/workflows/build.yml)

## ✨ Features

### Core Browsing
- 🌐 **Full Browsing** - Navigate, go back/forward, reload
- 📑 **Multi-Tab** - Open, switch, and close multiple tabs
- 🥷 **Incognito Mode** - Private browsing
- 🔍 **Smart Search** - Search or enter URL from the same bar

### Content & Organization
- 🔖 **Bookmarks** - Save and manage your favorite sites
- 📜 **History** - Browse history with clear option
- ⚡ **Quick Links** - Customizable home page shortcuts

### Privacy & Security
- 🚫 **Ad Blocker** - Built-in ad and tracker blocking
- 🔒 **HTTPS Indicator** - Visual security indicator
- 🛡️ **No Tracking** - Zero analytics, zero telemetry

### Reading & Display
- 📖 **Reading Mode** - Distraction-free reading with adjustable font size
- 🌙 **Dark Mode** - Light/dark theme support
- 🖥️ **Desktop Mode** - Request desktop site with one tap

### Tools
- 🔎 **Find in Page** - Search within any page
- 📤 **Share Page** - Share links via any app
- 📥 **Downloads** - System download manager integration
- 👆 **Swipe Navigation** - Swipe right/left to go back/forward

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM with StateFlow
- **Storage**: SharedPreferences + DataStore
- **Min SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)

## 📦 Download APK

### Automatic Build (Recommended)
1. Go to [Actions](https://github.com/d2026714/android-browser/actions)
2. Click the latest successful build
3. Download `debug-apk` or `release-apk` from Artifacts

### Build Locally
```bash
git clone https://github.com/d2026714/android-browser.git
cd android-browser
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

## 📁 Project Structure

```
app/src/main/java/com/example/browser/
├── BrowserApp.kt              # Application class
├── MainActivity.kt            # Main entry point
├── data/
│   ├── model/                 # Data models (Tab, Bookmark, HistoryItem)
│   └── repository/            # Data persistence
├── ui/
│   ├── components/            # Reusable UI components
│   │   ├── BrowserWebView.kt  # WebView with gestures & ad blocking
│   │   ├── FindInPageBar.kt   # Find in page search bar
│   │   └── NavigationBar.kt   # URL bar + bottom toolbar + menu
│   ├── screens/               # App screens
│   │   ├── MainScreen.kt      # Main container
│   │   ├── HomeScreen.kt      # Home page with quick links
│   │   ├── ReadingModeScreen.kt # Distraction-free reading
│   │   ├── BookmarksSheet.kt  # Bookmarks management
│   │   ├── HistorySheet.kt    # History management
│   │   ├── TabsSheet.kt       # Tab manager
│   │   ├── SettingsSheet.kt   # Settings & preferences
│   │   └── SearchEngineSheet.kt # Search engine picker
│   ├── theme/                 # Material 3 theme
│   └── viewmodel/             # ViewModel for state management
└── util/
    └── AdBlocker.kt           # Ad blocking logic
```

## 🚀 Getting Started

### Prerequisites
- [Android Studio](https://developer.android.com/studio) Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34

### Build & Run
1. Open the project in Android Studio
2. Sync Gradle
3. Connect a device or start an emulator
4. Click **Run** ▶️

## 📋 Roadmap

- [x] Basic browsing
- [x] Multi-tab support
- [x] Incognito mode
- [x] Bookmarks & history
- [x] Ad blocker
- [x] Dark mode
- [x] Desktop mode
- [x] Find in page
- [x] Reading mode
- [x] Swipe navigation
- [x] Share page
- [ ] Download manager UI
- [ ] Tab groups
- [ ] Custom home page widgets
- [ ] Gesture shortcuts
- [ ] Extension support

## 📄 License

MIT License - feel free to use and modify.

## 🤝 Contributing

Contributions are welcome! Feel free to open issues and pull requests.
