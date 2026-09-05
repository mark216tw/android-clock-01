# 簡單時鐘

「簡單時鐘」是一款以大字時鐘、多組鬧鐘與桌面小工具為核心的 Android 應用程式。介面使用 Jetpack Compose 製作，支援橫式與直式顯示，並提供多種配色及時鐘樣式。

## v1.3.0 Pre-release

- APP 指針時鐘圖示改用白色鐘面。
- 主題色彩效果提供靜態、流動漸層、漂浮極光、旋轉光暈、擴散波紋、散景粒子及隨機動態。
- 修正淺色設定與鬧鐘頁的 Android 狀態列文字對比。
- 新增 `prerelease` Build Type，啟用 R8 壓縮並使用 Android Debug Key 簽署。
- 此版本為測試發行版本，不是正式版本。

## 功能特色

### 大字時鐘

- 支援橫式與直式畫面，並可從主畫面直接切換方向。
- 可在一般模式與全螢幕模式間切換，並記住選擇。
- 支援系統、12 小時與 24 小時格式。
- 可顯示秒數、讓冒號閃爍及保持螢幕常亮。
- 提供極簡、細字、霓虹燈、動態光暈、鏤空、LED、液態玻璃、復古輝光管、經典指針、極簡指針、包浩斯幾何及瑞士國鐵等 12 種樣式。
- 提供珊瑚紅、蜜柑橙、向日黃、薄荷綠、晴空藍、葡萄紫、彩虹及隨機彩虹主題（支援即時隨機產生漸層）。
- 隨機動態會在 APP 啟動時隨機呈現一種效果，僅在 APP 從背景回到前景時更換；設定、鬧鐘與主時鐘之間的切換不會更換效果。
- 支援淺色、深色與跟隨系統外觀。

### 鬧鐘

- 可建立多組單次或每週重複鬧鐘。
- 支援自訂名稱、重複星期與專屬識別色（含 20 種預設色彩與 RGB/HEX 自訂調色）。
- 鬧鐘清單卡片以專屬識別色呈現底色與識別標記。
- 響鈴時可停止或稍後 10 分鐘再提醒，延後時通知欄保留提醒並顯示延後時間。
- 鬧鐘前 10 分鐘顯示靜音通知，並可取消本次提醒。
- 裝置重新開機、時間或時區變更後會重新安排鬧鐘。

### 桌面小工具

- 一列式時鐘，可水平調整寬度。
- 顯示時間、日期與手機系統的下一個鬧鐘。
- 每個小工具可獨立設定配色、外觀模式及透明背景。
- 時間字級可直接輸入 `sp` 數值，不限制預設最大值。
- 提供彩虹、純黑與純白等額外配色。
- 點選時間可開啟本 APP；點選鬧鐘區域可開啟系統鬧鐘。

## 系統需求

- Android 8.0（API 26）或以上版本。
- 部分鬧鐘功能需要通知、精準鬧鐘及全螢幕通知權限。
- 不同品牌的 Launcher 可能會以不同尺寸呈現桌面小工具。

## 安裝 Pre-release 版本

可從 GitHub 的 [Releases](../../releases) 下載標示為 **Pre-release** 的測試發行 APK。

> Pre-release APK 啟用 R8 壓縮並使用 Android Debug Key 簽署，僅供測試，並非正式發行版本。安裝前請自行評估資料備份、穩定性與安全風險。

若裝置阻擋安裝，需在 Android 系統設定中允許目前使用的瀏覽器或檔案管理器安裝未知來源應用程式。

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
.\gradlew.bat assemblePrerelease
```

macOS 或 Linux：

```bash
./gradlew assemblePrerelease
```

`prerelease` Build Type 會啟用 R8 壓縮、套用 Release ProGuard 規則，並使用 Debug 金鑰簽署。建置完成後 APK 位於：

```text
app/build/outputs/apk/prerelease/app-prerelease.apk
```

## 測試與檢查

Windows PowerShell：

```powershell
.\gradlew.bat testPrereleaseUnitTest lintPrerelease assemblePrerelease
```

若要執行所有常用變體的建置：

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assemblePrerelease assembleRelease
```

## 專案結構

- `app/src/main/java/com/simpleclock/app/alarm/`：鬧鐘排程、通知與響鈴服務。
- `app/src/main/java/com/simpleclock/app/data/`：Room、DataStore 與設定模型。
- `app/src/main/java/com/simpleclock/app/ui/`：Compose 畫面、主題與時鐘樣式。
- `app/src/main/java/com/simpleclock/app/widget/`：桌面小工具與設定畫面。
- `app/src/test/`：單元測試。

## 授權

本專案採用 [MIT License](LICENSE) 授權。
