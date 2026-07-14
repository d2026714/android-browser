# 🌐 Android Browser

A lightweight, modern Android browser built with **Kotlin**, **Jetpack Compose**, and **WebView**.

## ✨ Features

- 🌐 **Full Browsing** - Navigate, go back/forward, reload
- 📑 **Multi-Tab** - Open, switch, and close multiple tabs
- 🥷 **Incognito Mode** - Private browsing
- 🔖 **Bookmarks** - Save and manage your favorite sites
- 📜 **History** - Browse history with clear option
- 🚫 **Ad Blocker** - Built-in ad and tracker blocking
- 🌙 **Dark Mode** - Light/dark theme support
- 🔒 **HTTPS Indicator** - Visual security indicator
- 📥 **Downloads** - System download manager integration
- 🔍 **Smart Search** - Search or enter URL from the same bar

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM with StateFlow
- **Storage**: SharedPreferences + DataStore
- **Min SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)

## 📁 Project Structure

```
app/src/main/java/com/example/browser/
├── BrowserApp.kt              # Application class
├── MainActivity.kt            # Main entry point
├── data/
│   ├── model/                 # Data models (Tab, Bookmark, HistoryItem)
│   └── repository/            # Data persistence
├── ui/
│   ├── components/            # Reusable UI components (WebView, NavigationBar)
│   ├── screens/               # App screens (Home, Bookmarks, History, Tabs, Settings)
│   ├── theme/                 # Material 3 theme (colors, typography)
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

1. Clone the repository:
   ```bash
   git clone https://github.com/d2026714/android-browser.git
   ```

2. Open the project in Android Studio

3. Sync Gradle and build the project

4. Run on an emulator or physical device

### Build APK from Command Line

```bash
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

## 📋 Roadmap

- [ ] Reading mode
- [ ] Gesture navigation
- [ ] Custom search engines
- [ ] Tab groups
- [ ] Pull-to-refresh
- [ ] Find in page
- [ ] Share page
- [ ] Desktop mode toggle
- [ ] Download manager UI
- [ ] Custom home page shortcuts

## 📄 License

MIT License - feel free to use and modify.

## 🤝 Contributing

Contributions are welcome! Feel free to open issues and pull requests.
