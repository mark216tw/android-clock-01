# 開發與發布指南

## 環境準備

需要安裝：

- JDK 17
- Android Studio 或 Android SDK Command-line Tools
- Android SDK Platform 36
- Git

建立本機 `local.properties` 並設定 Android SDK 路徑。此檔案已列入 `.gitignore`，不得提交。

Windows 範例：

```properties
sdk.dir=C\:\\Users\\YOUR_NAME\\AppData\\Local\\Android\\Sdk
```

## 建置指令

Windows PowerShell：

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat assemblePrerelease
.\gradlew.bat assembleRelease
```

macOS 或 Linux：

```bash
./gradlew assembleDebug
./gradlew assemblePrerelease
./gradlew assembleRelease
```

Prerelease APK：

```text
app/build/outputs/apk/prerelease/app-prerelease.apk
```

## 測試與靜態檢查

基本檢查：

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
git diff --check
```

Prerelease 發布前：

```powershell
.\gradlew.bat testDebugUnitTest lintPrerelease assemblePrerelease
```

## Prerelease 定義

本專案的 Prerelease：

- 不是正式上線版本。
- GitHub Release 必須啟用 **Set as a pre-release**。
- Release 說明必須明確包含「Pre-release 版本」。
- APK 使用 Debug Key 簽署。
- APK 使用 Release 的 R8 最佳化設定。
- 使用者應自行評估測試資料、裝置限制與鬧鐘可靠性。

## 發布前檢查清單

- JVM 單元測試通過。
- `lintPrerelease` 通過。
- `assemblePrerelease` 成功。
- APK 可在測試裝置安裝及啟動。
- 驗證至少一個單次及一個重複鬧鐘。
- 驗證通知、精準鬧鐘與全螢幕通知權限。
- 驗證直向、橫向、淺色、深色與彩色背景。
- 驗證 Widget 新增、調整尺寸及點擊操作。
- README、使用指南與 Release Notes 符合目前功能。
- 不包含簽署金鑰、Token、`local.properties` 或裝置資料。

## GitHub Release 建議流程

1. 確認 `main` 已包含要發布的程式與文件。
2. 建立唯一 tag，例如 `v1.3.0-prerelease.2`。
3. 建立 GitHub Release，標題清楚標示 Pre-release。
4. 啟用 Pre-release，停用 Latest Release。
5. 上傳 `app-prerelease.apk`。
6. Release Notes 至少包含版本狀態、主要功能、安裝限制與 SHA-256。

GitHub CLI 範例：

```powershell
gh release create v1.3.0-prerelease.2 `
  "app/build/outputs/apk/prerelease/app-prerelease.apk" `
  --prerelease `
  --title "簡單時鐘 v1.3.0 Pre-release 2" `
  --notes-file "release-notes.md"
```

## 正式版本注意事項

正式上線前至少需要：

- 建立受保護的正式簽署金鑰，不可提交版本庫。
- 提升 `versionCode` 並確認 `versionName`。
- 移除 Prerelease 的 Debug Key 簽署。
- 完成核心權限、排程、重開機與裝置相容性測試。
- 確認資料庫 migration 政策不會意外刪除使用者鬧鐘。
- 建立正式隱私政策與商店資訊（若發布至應用程式商店）。
