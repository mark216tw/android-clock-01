# 簡單時鐘

「簡單時鐘」是一款以大字時鐘、多組鬧鐘與桌面小工具為核心的 Android 應用程式。專案採用 Kotlin、Jetpack Compose、Room 與 Preferences DataStore，支援多種數字及指針時鐘、可自訂背景、可靠的鬧鐘排程，以及每個實例可獨立設定的桌面小工具。

> 目前提供的是 Pre-release 測試版本，不是正式上線版本。請勿將測試版本作為唯一的關鍵鬧鐘來源。

## 功能特色

### 主時鐘

- 支援跟隨系統、12 小時及 24 小時格式。
- 可顯示秒數、閃爍冒號、保持螢幕常亮及切換全螢幕。
- 主畫面可在跟隨系統、固定直向及感應器橫向之間切換。
- 直向及橫向可分別設定五級時鐘尺寸。
- 提供 16 種時鐘樣式：8 種數字時鐘、4 種圓形指針及 4 種方形指針。
- 方形指針時鐘採固定直立長方形比例，高度：寬度為 `1.20:1`。

### 外觀與背景

- App 介面支援跟隨系統、淺色及深色模式。
- 系統介面色彩提供六種預設色相與自訂 Hue。
- 主時鐘配色與 App 介面色彩分開設定。
- 背景支援純色或彩色配色。
- 彩色模式可使用「好運氣」產生並自動保存配色。
- 提供無紋路、彩虹斜紋、極光漸層、同心光暈、流體波浪、稜鏡漸層、星雲雜訊、放射光束及柔和棋盤。
- 各背景紋路可獨立啟用或停用動態效果。

### 鬧鐘

- 支援多組單次或每週重複鬧鐘。
- 可設定名稱、重複星期及識別色。
- 清單顯示下一次響鈴日期與剩餘時間。
- 支援拖曳排序、啟用、停用、編輯及刪除。
- 響鈴時可停止或延後 10 分鐘。
- 響鈴前 10 分鐘顯示無聲通知，可取消本次提醒。
- 裝置重新開機、App 更新、系統時間或時區變更後會復原排程。

### 桌面小工具

- 一列式時鐘，可水平調整尺寸。
- 顯示時間、日期與系統下一個鬧鐘。
- 每個小工具可獨立設定配色、明暗模式、背景透明度及時間字級。
- 點擊時間可開啟本 App；點擊鬧鐘區域可開啟對應鬧鐘或系統鬧鐘頁。

## 系統需求

- Android 8.0（API 26）或以上版本。
- Android 12 以上需允許精準鬧鐘能力。
- Android 13 以上需允許通知權限。
- Android 14 以上可能需允許全螢幕通知。
- 部分裝置另有電池最佳化或背景執行限制，可能影響鬧鐘可靠性。

## 安裝 Pre-release

請從 GitHub [Releases](https://github.com/mark216tw/android-clock-01/releases) 下載標示為 **Pre-release** 的 APK。

Pre-release APK 使用 Android Debug Key 簽署並啟用 R8 壓縮，只供測試。若系統阻擋安裝，需允許目前使用的瀏覽器或檔案管理器安裝未知來源應用程式。

詳細步驟請參考[使用指南](docs/user-guide.md)。

## 開發環境

- JDK 17
- Android SDK 36
- Android Gradle Plugin 8.10.1
- Kotlin 2.1.21
- Jetpack Compose 與 Material 3
- Room
- Preferences DataStore

## 建置

Windows PowerShell：

```powershell
.\gradlew.bat testDebugUnitTest assemblePrerelease
```

macOS 或 Linux：

```bash
./gradlew testDebugUnitTest assemblePrerelease
```

APK 輸出位置：

```text
app/build/outputs/apk/prerelease/app-prerelease.apk
```

完整開發與發行流程請參考[開發與發布指南](docs/development-and-release.md)。

## 文件

- [專案說明](docs/project-overview.md)
- [使用指南](docs/user-guide.md)
- [系統架構與技術文件](docs/architecture.md)
- [技術參考](docs/technical-reference.md)
- [系統設計文件](docs/system-design.md)
- [開發與發布指南](docs/development-and-release.md)
- [版本紀錄](CHANGELOG.md)
- [貢獻指南](CONTRIBUTING.md)
- [文件索引](docs/README.md)

## 資料與隱私

- App 未宣告網路權限，也沒有分析、廣告或遠端後端套件。
- 鬧鐘與設定保存在裝置本機。
- `android:allowBackup` 目前設為 `false`。
- 開發版本僅保證 Room 資料庫 `3 → 4` migration；其他不相容版本可能重建鬧鐘資料。

## 授權

Copyright (c) 2026 mark216tw

本專案採用 [MIT License](LICENSE) 授權。
