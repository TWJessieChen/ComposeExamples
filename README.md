# Compose Examples

這是一個使用 Jetpack Compose Material 3 的 Android 範例專案，示範如何以卡片化排版呈現旅遊靈感清單。

## 功能亮點
- 使用 `Scaffold` 與 `CenterAlignedTopAppBar` 建立頂部導覽與浮動按鈕。
- 以 `LazyColumn` 與 `ElevatedCard` 呈現多個推薦區塊。
- 透過 `AssistChip`、`FilledTonalButton` 等 Material 3 元件增加互動性。
- 客製化字體層級與色票，搭配字串資源顯示在地化文案。

## 建置說明
1. 安裝 [Android Studio](https://developer.android.com/studio) 或具有 Android SDK 的本機環境。
2. 於專案根目錄執行：
   ```bash
   ./gradlew assembleDebug
   ```
   > 注意：儲存庫未包含 `gradle-wrapper.jar`，若為首次建置請先安裝本機 Gradle 並執行 `gradle wrapper` 以產生 Wrapper 所需檔案。
3. 將 `app` 模組部署至模擬器或實體裝置即可瀏覽範例畫面。

## 預覽
`MainActivity` 內已提供 `@Preview`，可在 Android Studio 的 Compose Preview 中快速檢視 UI。
