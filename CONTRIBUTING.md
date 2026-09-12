# 貢獻指南

感謝協助改善「簡單時鐘」。提交變更前請先閱讀本文件及 [`docs/architecture.md`](docs/architecture.md)。

## 開發需求

- JDK 17
- Android SDK 36
- Git
- Windows、macOS 或 Linux

## 開發流程

1. Fork 或建立功能分支。
2. 以最小且可驗證的變更完成一個目的。
3. 為純邏輯、時間計算、色彩或幾何行為加入 JVM 單元測試。
4. 執行測試、Lint 與需要的 Build Variant。
5. 確認沒有提交 APK、金鑰、`local.properties` 或其他本機檔案。
6. 提交清楚的 commit 與 Pull Request 說明。

## 常用指令

Windows PowerShell：

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assemblePrerelease
```

macOS 或 Linux：

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assemblePrerelease
```

## 程式風格

- 遵循現有 Kotlin 與 Jetpack Compose 風格。
- 優先使用簡單、可讀且可測試的實作。
- 不在 UI 中加入新的硬編碼使用者文字，請使用 `strings.xml`。
- 新增設定時同步更新 `AppSettings`、`SettingsRepository`、設定 UI 及測試。
- 修改鬧鐘排程時，必須考慮 Room Occurrence、AlarmManager、通知與重開機恢復的一致性。
- 新增公開功能時同步更新繁體中文文件。

## Pull Request 檢查

- 說明使用者可見行為與技術變更。
- 列出已執行的測試命令。
- UI 變更應說明直向、橫向、深色及淺色驗證結果。
- 鬧鐘變更應說明通知、精準鬧鐘及全螢幕權限情境。
- 不得包含憑證、Token、簽署金鑰或個人裝置資料。

## 授權

提交程式碼即表示您同意依本專案的 [MIT License](LICENSE) 提供該貢獻。
