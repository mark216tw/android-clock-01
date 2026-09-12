# 技術參考

## 平台與版本

| 項目 | 目前設定 |
| --- | --- |
| Application ID | `com.simpleclock.app` |
| Version Code | `4` |
| Version Name | `1.3.0` |
| Minimum SDK | `26` |
| Target SDK | `36` |
| Compile SDK | `36` |
| Java / JVM Target | `17` |
| Android Gradle Plugin | `8.10.1` |
| Kotlin | `2.1.21` |
| Gradle Wrapper | `8.11.1` |

實際版本以 `app/build.gradle.kts`、`gradle/libs.versions.toml` 與 `gradle/wrapper/gradle-wrapper.properties` 為準。

## Build Types

### Debug

- 使用 Android 預設 Debug 設定與 Debug Key。
- 包含 Compose UI Tooling 與測試 Manifest。

### Release

- 啟用 R8 最佳化與縮減。
- 使用 `proguard-android-optimize.txt` 及 `app/proguard-rules.pro`。
- 專案沒有在版本庫提供正式簽署設定。

### Prerelease

- 繼承 Release。
- 啟用 R8。
- 使用 Debug Key 簽署。
- Version Name 增加 `-prerelease`。
- 僅供測試，不是正式上線版本。

## App 設定資料

Preferences DataStore 名稱為 `clock_settings`，主要保存：

- 秒數與冒號閃爍。
- 全螢幕與保持螢幕開啟。
- 主時鐘色彩與自訂 Hue。
- 背景紋路、純色／彩色及動態開關。
- Material 顯示模式與系統色相。
- 時鐘樣式與時間格式。
- 畫面方向。
- 直向與橫向尺寸。
- 當前彩色配色及已保存配色。

Enum 以名稱保存，未知名稱會回復預設值。Hue 會正規化至 `[0, 360)`；非有限數值回復預設 200 度。字級讀寫會限制於 1 到 5。

## Room Schema

資料庫名稱：`simple-clock.db`

資料庫版本：`4`

### `alarms`

| 欄位 | 說明 |
| --- | --- |
| `id` | Primary Key |
| `hour` / `minute` | 本地時間 |
| `label` | 鬧鐘名稱 |
| `repeatDays` | 星期 bit mask，bit 0 為星期一 |
| `enabled` | 使用者是否啟用 |
| `color` | ARGB 識別色 |
| `sortOrder` | 使用者自訂排序 |

### `alarm_occurrences`

| 欄位 | 說明 |
| --- | --- |
| `token` | UUID Primary Key，同時識別 PendingIntent |
| `alarmId` | 對應鬧鐘外鍵 |
| `kind` | `REGULAR` 或 `SNOOZE` |
| `triggerAt` | Epoch milliseconds |
| `claimedAt` | Receiver 成功領取時間 |

`alarmId` 使用 cascade delete。專案提供 `3 → 4` migration；其他不相容版本可能透過 destructive migration 重建資料庫。

## Widget 設定

Widget 使用 `clock_widget_preferences` SharedPreferences。Key 包含 `appWidgetId`，因此每個實例可獨立保存：

- 配色。
- 跟隨系統／淺色／深色。
- 主題背景／透明背景。
- 時間字級。

Widget 刪除時會清除對應設定。

## Manifest 權限

- `POST_NOTIFICATIONS`
- `RECEIVE_BOOT_COMPLETED`
- `VIBRATE`
- `WAKE_LOCK`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- `USE_FULL_SCREEN_INTENT`
- `SCHEDULE_EXACT_ALARM`，最多 API 32
- `USE_EXACT_ALARM`

專案沒有宣告 `INTERNET`。

## Android 版本差異

- API 31 以上檢查 `AlarmManager.canScheduleExactAlarms()`。
- API 33 以上請求通知執行期權限。
- API 34 以上檢查全螢幕 Intent 能力。
- API 29 以上啟動 media playback 類型的前景服務。
- API 31 以上使用 `VibratorManager`。
- API 34 以上使用新的 Activity transition override API。

## 主題與色彩

App Material 色票由 Hue 計算，不使用 Android Dynamic Color API。淺色與深色模式會生成不同的 primary、background、surface 及對應前景色。測試會遍歷 0 到 359 度，檢查主要文字組合至少達到 4.5:1 對比。

主時鐘色彩是另一組設定，不會直接改變設定頁與鬧鐘頁的 Material 色票。

## 時鐘繪製

- 一般數字樣式使用系統 Sans Serif 或 Monospace，並啟用 `tnum` 等寬數字特性。
- 專案不包含外部字型。
- LED、液態玻璃、輝光管及指針鐘由 Compose Canvas 或自訂 Composable 繪製。
- LED 使用 `0.78` 視覺縮放，使高度接近一般文字樣式。
- 方形指針鐘固定採高度：寬度 `1.20:1`，刻度與數字使用長方形射線交點排列。

## 背景動畫

`ThemeMotionBackground` 使用 Canvas 繪製九種紋路。關閉動態時使用固定進度繪製；啟用時使用 `rememberInfiniteTransition`。固定亂數種子讓極光與星雲在重新組合時維持穩定配置。

## 測試現況

`app/src/test` 目前涵蓋：

- 單次與重複鬧鐘時間計算。
- DST gap 與 overlap。
- 下一次響鈴顯示與倒數。
- Hue 正規化與色票。
- Material 色彩對比。
- 背景設定模型。
- 時鐘樣式分類、LED 比例、方形幾何及更新頻率。

尚未完整涵蓋：

- Room migration 與 DAO instrumentation 測試。
- AlarmManager、Receiver、Service 與通知整合測試。
- 權限流程及程序死亡恢復。
- Compose UI、TalkBack 與大字體測試。
- Widget 在不同 Launcher 的實機測試。

## 安全與隱私

- 資料保存在本機，不傳送至遠端。
- 沒有廣告、分析或後端 SDK。
- Manifest 設定 `android:allowBackup="false"`。
- 版本庫不得提交 `local.properties`、簽署金鑰、憑證或 Token。
