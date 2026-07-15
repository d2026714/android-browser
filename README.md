# 🌐 Android Browser

[![Build APK](https://github.com/d2026714/android-browser/actions/workflows/build.yml/badge.svg)](https://github.com/d2026714/android-browser/actions)

A lightweight Android browser built with Kotlin, Jetpack Compose, and WebView.

## Features

### Core
- 🌐 Browsing (back/forward/reload/stop)
- 📑 Multi-tab management with bottom tab bar
- 🔍 Smart search bar with multiple search engines
- 🔖 Bookmarks with Room database persistence
- 📜 History with timestamps
- 🚫 Ad blocker (domain + path based)
- 🌙 Dark mode support
- 🔍 Find in page
- 📤 Share page
- ⚙️ Configurable settings (search engine, font size, ad blocker)
- 📊 Page loading progress bar
- ❌ Error page with retry
- 🔗 Quick links on home screen

### Search Engines
- Google
- Bing
- 百度
- DuckDuckGo

## Architecture

```
ui/
  MainScreen.kt          — NavHost-based navigation
  BrowserScreen.kt       — Main browser view (WebView + nav bars)
  HomeScreen.kt          — Home page with search + quick links
  BookmarksScreen.kt     — Bookmarks list
  HistoryScreen.kt       — History list
  SettingsScreen.kt      — Settings page
  BrowserViewModel.kt    — MVVM state management
  components/
    TopNavBar.kt         — Navigation bar with URL display
    BottomTabBar.kt      — Tab switcher
    FindInPageBar.kt     — Find in page UI
    ErrorPage.kt         — Error state UI
    WebViewContent.kt    — WebView composable wrapper
  navigation/
    Screen.kt            — Navigation routes
  theme/
    Theme.kt             — Material 3 theming

web/
  BrowserWebViewClient.kt — WebView client with ad blocking
  AdBlocker.kt            — Ad domain/path blocking

data/
  AppDatabase.kt          — Room database (bookmarks, history, tabs)
  entity/                 — Room entities
  dao/                    — Room DAOs
```

## Download

[Actions → Latest ✅ → debug-apk](https://github.com/d2026714/android-browser/actions)

## Build

```bash
git clone https://github.com/d2026714/android-browser.git
cd android-browser
./gradlew assembleDebug
```

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- MVVM
- Room
- WebView
- KSP
- API 26-34

## License

MIT
