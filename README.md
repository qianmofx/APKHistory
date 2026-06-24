# APKHistory - APK 历史版本查看器

一款 Android 应用，用于浏览和下载应用的**历史版本**（APK 文件）。

## 功能

- 🔍 **搜索应用** — 按名称搜索 Android 应用
- 📜 **历史版本** — 查看每个应用的所有历史版本
- ⬇️ **一键下载** — 下载任意历史版本的 APK 文件
- 📂 **本地管理** — 查看下载记录、管理已下载的 APK
- ❤️ **收藏应用** — 收藏常用应用，快速访问

## 数据来源

数据来源于[豌豆荚](https://www.wandoujia.com)，通过解析公开网页获取应用信息和历史版本列表。

## 技术栈

| 技术 | 用途 |
|------|------|
| **Kotlin** | 开发语言 |
| **Jetpack Compose** | UI 框架 |
| **Material 3** | 设计语言 |
| **OkHttp** | 网络请求 + APK 下载 |
| **Jsoup** | HTML 解析 |
| **Coil** | 图片加载 |
| **Room** | 本地数据库缓存 |
| **Navigation Compose** | 页面导航 |


## 构建

```bash
# 克隆项目
git clone https://github.com/qianmofx/APKHistory.git

# 用 Android Studio 打开，或命令行构建
./gradlew assembleDebug
```

## 许可

MIT License
