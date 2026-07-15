# 🌐 Android Browser

[![Build APK](https://github.com/d2026714/android-browser/actions/workflows/build.yml/badge.svg)](https://github.com/d2026714/android-browser/actions)

轻量级 Android 浏览器 + 小说阅读器。Kotlin + WebView + Jetpack Compose。

## 功能

### 🌐 浏览模式
- 多标签管理 + 底部标签栏
- 搜索建议（Google Suggest API）
- 4 种搜索引擎（Google / Bing / 百度 / DuckDuckGo）
- 快捷链接首页
- 广告拦截（域名 + 路径规则）
- 文件下载（系统 DownloadManager）
- 页面内查找
- 书签 + 历史记录（Room 持久化）
- 深色模式
- SSL 错误处理
- 外部链接跳转

### 📖 阅读模式
- JS 正文提取引擎
- 章节自动检测（第X章 / Chapter X）
- 章节目录导航
- 左右滑动切换章节
- 字号调节（12-32sp）
- 行距调节（1.2-3.0x）
- 5 种背景主题（护眼 / 白色 / 浅灰 / 深色 / 黑色）

### 📚 书架
- 自动保存阅读记录
- 跟踪上次阅读章节
- 书架管理

## 架构

```
data/
  AppDatabase.kt          — Room 数据库
  entity/                 — BookmarkEntity, HistoryEntity
  dao/                    — BookmarkDao, HistoryDao

ui/
  MainScreen.kt           — NavHost 路由（6 个页面）
  BrowserScreen.kt        — 主浏览器视图
  HomeScreen.kt           — 首页（搜索 + 快捷链接）
  BookmarksScreen.kt      — 书签列表
  HistoryScreen.kt        — 历史列表
  SettingsScreen.kt       — 设置页
  BrowserViewModel.kt     — 状态管理 + 业务逻辑
  components/             — 可复用 UI 组件
  navigation/             — 路由定义
  theme/                  — Material 3 主题

reader/
  TextExtractor.kt        — JS 正文提取 + 章节检测
  ReaderScreen.kt         — 阅读器 UI + 排版设置
  BookshelfScreen.kt      — 书架管理

web/
  BrowserWebViewClient.kt — WebView 客户端 + 广告拦截
  AdBlocker.kt            — 广告规则引擎
  DownloadHandler.kt      — 下载管理
  SearchSuggestions.kt    — 搜索建议 API
```

## 下载

[Actions → Latest ✅ → debug-apk](https://github.com/d2026714/android-browser/actions)

## 构建

```bash
git clone https://github.com/d2026714/android-browser.git
cd android-browser
./gradlew assembleDebug
```

## 技术栈

Kotlin · Jetpack Compose · Material 3 · MVVM · Room · WebView · KSP · API 26-34

## License

MIT
