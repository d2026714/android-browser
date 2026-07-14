# 🌐 Android Browser

[![Build APK](https://github.com/d2026714/android-browser/actions/workflows/build.yml/badge.svg)](https://github.com/d2026714/android-browser/actions/workflows/build.yml)

A lightweight, modern Android browser built with **Kotlin**, **Jetpack Compose**, and **WebView**. **50+ features**, zero dependencies on third-party services.

## ✨ Features (v2.0.0)

### Core
- 🌐 Browsing (back/forward/reload) | 📑 Multi-tab + badge | 🥷 Incognito | 🔍 Smart search bar with suggestions

### Content
- 🔖 Bookmarks + Folders | 📜 History with search | 📚 Reading List | 📑 Tab Groups | ⚡ Custom Quick Links

### Privacy & Security
- 🚫 Ad blocker | 🔒 HTTPS + SSL error handling | 🍪 Cookie control (all/1st-party/none) | ⚡ JavaScript toggle | 🛡️ Zero tracking | 🌐 DNS-over-HTTPS

### Display
- 📖 Reading mode | 🌙 Dark + AMOLED | 🖥️ Desktop mode | 🌙 Blue light filter | 🎭 Custom User Agent | 🎨 Custom CSS | 🔍 Zoom control

### Tools
- 🔎 Find in page | 📄 View source | 📸 Screenshot | 📤 Share | 📥 Downloads | 🌐 Translate | 📱 QR code | 📊 Page info | 📋 Copy link | 🖨️ Print | 💾 Backup/restore

### Power
- 🔃 Swipe gestures + Pull to refresh | 📉 Data saver | 🎬 Full-screen video | ⌨️ Keyboard shortcuts | ♿ Accessibility | 🕸️ WebView pool (tabs persist state) | 💾 Tab state persistence

## 🏗️ Architecture

```
manager/
  TabManager.kt       — Tab lifecycle + WebView pool + tab state persistence
  BookmarkManager.kt   — Bookmarks/History/Reading list/Tab groups/SP migration
  SettingsManager.kt   — Settings persistence + Quick links + Cookie/DOH/SP migration

data/local/
  BrowserDatabase.kt   — Room database (8 tables)
  entity/              — Room Entity
  dao/                 — Room DAO

ui/viewmodel/
  BrowserViewModel.kt  — Thin orchestrator, delegates to managers

ui/components/
  BrowserWebView.kt    — WebView pool integration, pull-to-refresh, progress bar
  ErrorPage.kt         — Custom error page
  SslErrorDialog.kt    — SSL certificate error dialog
  LongPressMenu.kt     — Long-press link context menu
  NavigationBar.kt     — Search suggestions, accessibility
```

## 📦 Download

**[Actions → Latest ✅ → debug-apk](https://github.com/d2026714/android-browser/actions)**

```bash
git clone https://github.com/d2026714/android-browser.git && cd android-browser && ./gradlew assembleDebug
```

## 🛠️ Tech

Kotlin · Jetpack Compose · Material 3 · MVVM · Room · WebView · KSP · API 26-34

## 📋 Changelog

### v2.0.0
**Architecture:**
- 🗄️ Room database replaces SharedPreferences (8 tables, 8 DAOs)
- 🧩 ViewModel split into TabManager / BookmarkManager / SettingsManager
- 🕸️ WebView pool: each tab holds its own WebView, switch without rebuild
- 📝 All silent catches replaced with proper logging

**New Features:**
- 🔍 Search suggestions (Google Suggest API)
- 📁 Bookmark folders (create/delete)
- 🔎 History search (filter by title/URL)
- 🌐 Intent handling (open links from other apps)
- 💾 Tab state persistence (restore tabs on restart)
- 📊 Page loading progress bar
- 🛡️ SSL certificate error handling
- 🚫 Custom error page
- 🍪 Cookie control (all / first-party / none)
- 🌐 DNS-over-HTTPS toggle
- ⌨️ Keyboard shortcuts (R=reload, T=new tab, W=close, L=URL bar, D=bookmark, F=find, H=back, N=reader, +/-/0=zoom)
- 🔗 Long-press link context menu (new tab, incognito, copy, share)
- ⬇️ Pull-to-refresh gesture
- ♿ Accessibility content descriptions on all interactive elements
- 🔄 SP→Room data migration (seamless upgrade from v1.x)

### v1.4.0
- Blue light filter, data saver, JavaScript toggle, custom UA/CSS, zoom, print, backup/restore

### v1.3.0
- Reading List, Tab Groups, Quick Links Editor, QR Code, Translate, AMOLED

### v1.2.0
- Downloads, View Source, Screenshot, Clear Cookies, Full-screen Video

### v1.1.0
- Reading Mode, Swipe Gestures, Desktop Mode, Find in Page

### v1.0.0
- Initial release

## 📄 MIT License
