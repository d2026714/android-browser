# 🌐 Android Browser

[![Build APK](https://github.com/d2026714/android-browser/actions/workflows/build.yml/badge.svg)](https://github.com/d2026714/android-browser/actions/workflows/build.yml)

A lightweight, modern Android browser built with **Kotlin**, **Jetpack Compose**, and **WebView**. **40+ features**, zero dependencies on third-party services.

## ✨ Features (v2.0.0)

### Core
- 🌐 Browsing (back/forward/reload) | 📑 Multi-tab + badge | 🥷 Incognito | 🔍 Smart search bar

### Content
- 🔖 Bookmarks | 📜 History | 📚 Reading List | 📑 Tab Groups | ⚡ Custom Quick Links

### Privacy
- 🚫 Ad blocker | 🔒 HTTPS indicator | 🍪 Clear cookies | ⚡ JavaScript toggle | 🛡️ Zero tracking

### Display
- 📖 Reading mode | 🌙 Dark + AMOLED | 🖥️ Desktop mode | 🌙 Blue light filter | 🎭 Custom User Agent | 🎨 Custom CSS | 🔍 Zoom control

### Tools
- 🔎 Find in page | 📄 View source | 📸 Screenshot | 📤 Share | 📥 Downloads | 🌐 Translate | 📱 QR code | 📊 Page info | 📋 Copy link | 🖨️ Print | 💾 Backup/restore

### Power
- 🔃 Swipe gestures | 📉 Data saver | 🎬 Full-screen video | 🚀 GitHub Actions CI

## 🏗️ Architecture (v2.0.0)

**v2.0.0 重构：从"上帝 ViewModel"到分层架构**

```
manager/
  TabManager.kt      — Tab 生命周期 + WebView 池
  BookmarkManager.kt  — 书签/历史/阅读列表/标签组
  SettingsManager.kt  — 设置持久化 + 快捷链接

data/local/
  BrowserDatabase.kt  — Room 数据库
  entity/             — Room Entity (6 张表)
  dao/                — Room DAO (6 个)

ui/viewmodel/
  BrowserViewModel.kt — 薄编排层，委托给 Manager
```

### 核心改进

| 改动 | v1.x | v2.0.0 |
|------|------|--------|
| 数据持久化 | SharedPreferences (JSON 序列化) | Room 数据库 |
| ViewModel | 324 行上帝类 | 拆分为 3 个 Manager + 薄 ViewModel |
| Tab 管理 | 仅存 URL/title，切换重建 | WebView 池，每个 Tab 独立 WebView |
| 异常处理 | `catch (_: Exception) {}` | 全部带 `Log.e` 日志 |
| 依赖注入 | 单例 + Application 强转 | 构造函数注入 DAO |

## 📦 Download

**[Actions → Latest ✅ → debug-apk](https://github.com/d2026714/android-browser/actions)**

```bash
git clone https://github.com/d2026714/android-browser.git && cd android-browser && ./gradlew assembleDebug
```

## 🛠️ Tech

Kotlin · Jetpack Compose · Material 3 · MVVM · Room · WebView · API 26-34

## 📋 Changelog

### v2.0.0
- 🗄️ Room 数据库替换 SharedPreferences
- 🧩 ViewModel 拆分：TabManager / BookmarkManager / SettingsManager
- 🕸️ WebView 池：每个 Tab 持有独立 WebView，切换不重建
- 📝 全面日志：替换所有空 catch，关键路径加 Log
- 📦 添加 KSP + Room 依赖，移除 DataStore

### v1.4.0
- 🌙 Blue light filter with intensity slider
- 📉 Data saver (block images, use cache)
- ⚡ JavaScript on/off toggle
- 🎭 Custom User Agent (8 presets + custom)
- 🎨 Custom CSS injection (dark reader, readability presets)
- 🔍 Zoom control with slider and presets
- 📊 Page info sheet
- 📋 Copy link to clipboard
- 🖨️ Print page support
- 💾 Backup & restore (export/import JSON)
- ⚙️ Settings reorganized with categories

### v1.3.0
- Reading List, Tab Groups, Quick Links Editor, QR Code, Translate, AMOLED

### v1.2.0
- Downloads, View Source, Screenshot, Clear Cookies, Full-screen Video

### v1.1.0
- Reading Mode, Swipe Gestures, Desktop Mode, Find in Page

### v1.0.0
- Initial release

## 📄 MIT License
