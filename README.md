# 🌐 Android Browser

[![Build APK](https://github.com/d2026714/android-browser/actions/workflows/build.yml/badge.svg)](https://github.com/d2026714/android-browser/actions)

A lightweight Android browser with built-in novel reader mode. Kotlin + WebView + Jetpack Compose.

## Features

### 🌐 Browser Mode (Via-style)
- Multi-tab management with bottom tab bar
- Smart search bar with suggestions (Google Suggest API)
- Multiple search engines (Google, Bing, 百度, DuckDuckGo)
- Quick links on home screen
- Ad blocker (domain + path based)
- Download manager
- Find in page
- Bookmarks & History (Room database)
- Dark mode support
- Error page with retry
- SSL error handling
- Intent handling (open links from other apps)

### 📖 Reader Mode (Novel-style)
- Auto-extract main text from any web page
- Chapter detection (第X章, Chapter X patterns)
- Chapter list navigation
- Swipe left/right to switch chapters
- Customizable font size (12-32sp)
- Adjustable line spacing (1.2-3.0x)
- 5 background themes (护眼/白色/浅灰/深色/黑色)
- Serif font for comfortable reading
- Auto-add to bookshelf

### 📚 Bookshelf
- Save novels to bookshelf
- Track last read chapter
- Quick access from home screen
- Delete/manage books

### ⚙️ Settings
- Search engine selection
- Ad blocker toggle
- Dark mode toggle
- Font size slider
- Clear all data

## Architecture

```
ui/
  MainScreen.kt          — NavHost navigation (6 routes)
  BrowserScreen.kt       — Main browser view
  HomeScreen.kt          — Home with search suggestions + quick links
  BookmarksScreen.kt     — Bookmarks list
  HistoryScreen.kt       — History list
  SettingsScreen.kt      — Settings page
  BrowserViewModel.kt    — MVVM state management
  components/
    TopNavBar.kt         — Navigation bar
    BottomTabBar.kt      — Tab switcher
    FindInPageBar.kt     — Find in page
    ErrorPage.kt         — Error state
    WebViewContent.kt    — WebView wrapper with download support
  navigation/
    Screen.kt            — Navigation routes
  theme/
    Theme.kt             — Material 3 theming

reader/
  TextExtractor.kt       — JS-based text extraction + chapter detection
  ReaderScreen.kt        — Reader UI with pagination + settings
  BookshelfScreen.kt     — Bookshelf management

web/
  BrowserWebViewClient.kt — WebView client with ad blocking
  AdBlocker.kt            — Ad domain/path blocking
  DownloadHandler.kt      — Download manager integration
  SearchSuggestions.kt    — Google Suggest API

data/
  AppDatabase.kt          — Room database
  repository/
    TabRepository.kt      — Tab persistence
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

- Kotlin · Jetpack Compose · Material 3 · MVVM · Room · WebView · KSP · API 26-34

## License

MIT
