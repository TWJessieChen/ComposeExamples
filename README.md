# ComposeExamples 專案架構完整解說

## 📋 目錄
1. [專案概述](#專案概述)
2. [架構模式：MVVM](#架構模式mvvm)
3. [專案結構](#專案結構)
4. [資料流向](#資料流向)
5. [執行流程](#執行流程)
6. [各層級詳細說明](#各層級詳細說明)

---

## 專案概述

這是一個 **Jetpack Compose 學習展示應用**，用來展示 Compose 的各種技巧和概念。專案採用 **MVVM (Model-View-ViewModel)** 架構模式，結合 Navigation Compose 實現多頁面導覽。

### 核心功能
- 📚 列表展示多個 Compose 學習主題
- 📖 點擊查看每個主題的詳細說明
- ◀️ ▶️ 上一個/下一個主題切換功能
- 🔙 返回列表功能

---

## 架構模式：MVVM

```
┌─────────────────────────────────────────────────┐
│                    View Layer                    │
│  (Composable Functions - UI Components)         │
│  • MainActivity.kt                               │
│  • IntroScreen.kt                                │
│  • FeatureDetailScreen.kt                        │
└────────────────┬────────────────────────────────┘
                 │ ↕ 雙向綁定
                 │ (StateFlow/collectAsState)
┌────────────────┴────────────────────────────────┐
│                 ViewModel Layer                  │
│  (Business Logic & State Management)             │
│  • ComposeFeatureViewModel.kt                    │
│    - UiState (ComposeFeatureUiState)             │
│    - Actions (selectFeature, showNext, etc.)     │
└────────────────┬────────────────────────────────┘
                 │ ↓ 取得資料
                 │
┌────────────────┴────────────────────────────────┐
│                  Model Layer                     │
│  (Data & Repository)                             │
│  • ComposeFeature.kt (Data Class)                │
│  • ComposeFeatureRepository.kt                   │
└─────────────────────────────────────────────────┘
```

---

## 專案結構

```
com.example.composeexamples/
├── MainActivity.kt                    # 應用程式入口點
├── data/                              # 資料層 (Model)
│   └── ComposeFeature.kt              # 資料模型 + Repository
├── viewmodel/                         # ViewModel 層
│   └── ComposeFeatureViewModel.kt     # 狀態管理與邏輯
├── ui/                                # UI 層 (View)
│   ├── screens/
│   │   ├── IntroScreen.kt             # 列表頁面
│   │   └── FeatureDetailScreen.kt     # 詳細頁面
│   └── theme/                         # 主題設定
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── res/
    ├── values/
    │   ├── themes.xml                 # Android 主題 (日間模式)
    │   └── strings.xml
    └── values-night/
        └── themes.xml                 # Android 主題 (夜間模式)
```

---

## 資料流向

### 1️⃣ 單向資料流 (Unidirectional Data Flow)

```
┌──────────────────────────────────────────────────┐
│  User Interaction (使用者操作)                     │
│  點擊項目、上一頁、下一頁                            │
└────────────┬─────────────────────────────────────┘
             │
             ↓ Event (事件)
┌────────────┴─────────────────────────────────────┐
│  Composable Function (UI)                        │
│  調用 ViewModel 的方法                             │
│  例如: viewModel.selectFeature(id)                │
└────────────┬─────────────────────────────────────┘
             │
             ↓ Action (動作)
┌────────────┴─────────────────────────────────────┐
│  ViewModel                                       │
│  更新 _uiState (MutableStateFlow)                │
│  _uiState.update { ... }                         │
└────────────┬─────────────────────────────────────┘
             │
             ↓ State Change (狀態變更)
┌────────────┴─────────────────────────────────────┐
│  StateFlow 發出新的 UiState                       │
│  uiState: StateFlow<ComposeFeatureUiState>       │
└────────────┬─────────────────────────────────────┘
             │
             ↓ Observe (觀察)
┌────────────┴─────────────────────────────────────┐
│  Composable Function                             │
│  val uiState by viewModel.uiState.collectAsState()│
│  自動 Recomposition (重組)                        │
└──────────────────────────────────────────────────┘
```

---

## 執行流程

### 🚀 應用程式啟動流程

```
1. MainActivity.onCreate()
   ↓
2. setContent { ComposeExamplesTheme { ... } }
   ↓
3. ComposeShowcaseApp() 被調用
   ↓
4. viewModel() 創建 ComposeFeatureViewModel 實例
   ↓
5. ViewModel 初始化時調用 repository.loadFeatures()
   ↓
6. _uiState 被初始化，包含所有 features
   ↓
7. collectAsState() 訂閱 uiState
   ↓
8. Scaffold + NavHost 建立 UI 框架
   ↓
9. 顯示 IntroScreen (startDestination = "intro")
```

### 📱 使用者互動流程範例

#### 場景 1: 點擊列表項目進入詳細頁

```
用戶點擊 "Composable 基礎" 卡片
   ↓
IntroScreen 的 onFeatureSelected 被觸發
   ↓
viewModel.selectFeature(feature.id) 被調用
   ↓
ViewModel 更新 currentIndex (找到對應的 feature)
   ↓
_uiState.update { state -> state.copy(currentIndex = index) }
   ↓
navController.navigate("detail") 導航到詳細頁
   ↓
FeatureDetailScreen 顯示，並從 uiState.currentFeature 取得資料
   ↓
畫面顯示該主題的詳細內容
```

#### 場景 2: 在詳細頁點擊「下一個」

```
用戶點擊 "Next" 按鈕
   ↓
onNext() 回調被觸發
   ↓
viewModel.showNext() 被調用
   ↓
ViewModel 檢查 canShowNext (currentIndex < features.lastIndex)
   ↓
_uiState.update { state -> state.copy(currentIndex = currentIndex + 1) }
   ↓
StateFlow 發出新的 UiState
   ↓
collectAsState() 接收到新狀態
   ↓
Compose Recomposition (重組)
   ↓
FeatureDetailScreen 重新渲染，顯示下一個主題
```

---

## 各層級詳細說明

### 🎯 1. MainActivity.kt (入口點)

**職責：**
- Android 應用程式的啟動點
- 設置 Compose 的主題和根組件

**關鍵代碼：**
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeExamplesTheme {  // 套用 Material3 主題
                ComposeShowcaseApp()  // 根組件
            }
        }
    }
}
```

**ComposeShowcaseApp 組件：**
- 創建 ViewModel 實例
- 訂閱 UiState
- 設置導航系統 (NavHost)
- 提供 Scaffold 框架 (TopBar + 內容區)

---

### 📊 2. Data Layer (資料層)

#### `ComposeFeature.kt`

**Data Class:**
```kotlin
data class ComposeFeature(
    val id: String,           // 唯一識別符
    val title: String,        // 主題標題
    val summary: String,      // 摘要說明
    val highlights: List<String>,  // 重點列表
    val codeHint: String      // 程式碼範例
)
```

**Repository Pattern:**
```kotlin
class ComposeFeatureRepository {
    fun loadFeatures(): List<ComposeFeature> = listOf(...)
}
```

**職責：**
- 封裝資料來源 (目前是硬編碼，未來可替換成網路 API 或資料庫)
- 提供資料給 ViewModel

---

### 🧠 3. ViewModel Layer (業務邏輯層)

#### `ComposeFeatureViewModel.kt`

**核心組成：**

**1. UiState (UI 狀態資料類別)**
```kotlin
data class ComposeFeatureUiState(
    val features: List<ComposeFeature> = emptyList(),  // 所有主題
    val currentIndex: Int = 0                          // 當前選中的索引
) {
    // 計算屬性：當前顯示的 feature
    val currentFeature: ComposeFeature?
        get() = features.getOrNull(currentIndex)

    // 是否可以顯示上一個
    val canShowPrevious: Boolean
        get() = currentIndex > 0

    // 是否可以顯示下一個
    val canShowNext: Boolean
        get() = currentIndex < features.lastIndex
}
```

**2. StateFlow (狀態流)**
```kotlin
private val _uiState = MutableStateFlow(...)  // 私有可變狀態
val uiState: StateFlow<...> = _uiState        // 對外只讀狀態
```

**3. Actions (用戶操作處理)**
```kotlin
fun selectFeature(featureId: String) {
    // 根據 ID 找到對應的索引並更新
    val index = _uiState.value.features.indexOfFirst { it.id == featureId }
    if (index >= 0) {
        _uiState.update { state -> state.copy(currentIndex = index) }
    }
}

fun showNext() {
    // 顯示下一個 (如果可以)
    _uiState.update { state ->
        if (state.canShowNext) 
            state.copy(currentIndex = state.currentIndex + 1) 
        else 
            state
    }
}

fun showPrevious() {
    // 顯示上一個 (如果可以)
    _uiState.update { state ->
        if (state.canShowPrevious) 
            state.copy(currentIndex = state.currentIndex - 1) 
        else 
            state
    }
}
```

**設計原則：**
- ✅ 單一事實來源 (Single Source of Truth)
- ✅ 不可變狀態 (Immutable State)
- ✅ 單向資料流 (Unidirectional Data Flow)

---

### 🎨 4. UI Layer (視圖層)

#### `IntroScreen.kt` - 列表頁面

**職責：**
- 顯示所有 Compose 主題的列表
- 處理項目點擊事件

**關鍵設計：**
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntroScreen(
    features: List<ComposeFeature>,           // 資料
    onFeatureSelected: (ComposeFeature) -> Unit,  // 事件回調
    modifier: Modifier = Modifier
) {
    LazyColumn {  // 可滾動列表
        item {
            // 標題與說明
        }
        items(features, key = { it.id }) { feature ->
            ElevatedCard(onClick = { onFeatureSelected(feature) }) {
                // 顯示每個主題的卡片
            }
        }
    }
}
```

**State Hoisting (狀態提升)：**
- ✅ IntroScreen 是無狀態的 (Stateless)
- ✅ 所有資料和事件都由父組件提供
- ✅ 易於測試和重用

---

#### `FeatureDetailScreen.kt` - 詳細頁面

**職責：**
- 顯示單個主題的詳細資訊
- 提供上一個/下一個/返回列表按鈕

**參數設計：**
```kotlin
@Composable
fun FeatureDetailScreen(
    feature: ComposeFeature?,      // 當前主題 (可能為 null)
    pageIndicator: String,         // 頁碼指示器 "2 / 5"
    canShowPrevious: Boolean,      // 是否可以上一頁
    canShowNext: Boolean,          // 是否可以下一頁
    onPrevious: () -> Unit,        // 上一頁回調
    onNext: () -> Unit,            // 下一頁回調
    onBackToList: () -> Unit,      // 返回列表回調
    modifier: Modifier = Modifier
)
```

**特點：**
- ✅ 完全由外部控制狀態和行為
- ✅ 按鈕 enabled 狀態由 ViewModel 計算
- ✅ Null safety 處理 (feature 可能為 null)

---

### 🧭 5. Navigation (導航系統)

**導航路由定義：**
```kotlin
private sealed class Destination(val route: String) {
    object Intro : Destination("intro")    // 列表頁
    object Detail : Destination("detail")  // 詳細頁
}
```

**NavHost 設置：**
```kotlin
NavHost(
    navController = navController,
    startDestination = Destination.Intro.route,  // 起始路由
    modifier = Modifier.fillMaxSize().padding(innerPadding)
) {
    composable(Destination.Intro.route) {
        IntroScreen(
            features = uiState.features,
            onFeatureSelected = { feature ->
                viewModel.selectFeature(feature.id)  // 更新 ViewModel
                navController.navigate(Destination.Detail.route)  // 導航
            }
        )
    }

    composable(Destination.Detail.route) {
        FeatureDetailScreen(
            feature = uiState.currentFeature,
            // ... 其他參數
            onBackToList = {
                navController.popBackStack(  // 返回上一頁
                    route = Destination.Intro.route,
                    inclusive = false
                )
            }
        )
    }
}
```

**導航特點：**
- ✅ 類型安全的路由定義 (sealed class)
- ✅ ViewModel 跨頁面共享狀態
- ✅ 返回堆疊自動管理

---

## 🎯 架構優勢

### 1. **關注點分離 (Separation of Concerns)**
- UI 只負責顯示
- ViewModel 負責邏輯
- Repository 負責資料

### 2. **可測試性 (Testability)**
- ViewModel 可以獨立測試 (不依賴 Android 框架)
- Composable 可以用 Preview 測試
- Repository 可以 mock

### 3. **可維護性 (Maintainability)**
- 清晰的資料流向
- 狀態管理集中在 ViewModel
- UI 無狀態易於理解

### 4. **擴展性 (Scalability)**
- 新增頁面只需加入 composable 路由
- 替換資料源只需修改 Repository
- UI 邏輯變更不影響 ViewModel

---

## 🔄 完整互動循環範例

```
【初始狀態】
Repository 載入 5 個 ComposeFeature
ViewModel 初始化 currentIndex = 0
IntroScreen 顯示 5 個卡片

【用戶點擊第 3 個項目】
1. IntroScreen 觸發 onFeatureSelected(feature)
2. viewModel.selectFeature(feature.id)
3. ViewModel 找到索引 2，更新 _uiState.copy(currentIndex = 2)
4. StateFlow 發出新狀態
5. navController.navigate("detail")
6. FeatureDetailScreen 顯示索引 2 的內容
   - pageIndicator = "3 / 5"
   - canShowPrevious = true
   - canShowNext = true

【用戶點擊「下一個」】
1. onNext() 被觸發
2. viewModel.showNext()
3. currentIndex 變為 3
4. StateFlow 發出新狀態
5. FeatureDetailScreen Recomposition
6. 顯示索引 3 的內容
   - pageIndicator = "4 / 5"
   - canShowNext = true

【用戶點擊「返回列表」】
1. onBackToList() 被觸發
2. navController.popBackStack("intro", inclusive = false)
3. 返回 IntroScreen
4. currentIndex 保持為 3 (ViewModel 狀態未變)
```

---

## 💡 關鍵技術點總結

| 技術 | 用途 | 檔案位置 |
|------|------|----------|
| **Jetpack Compose** | 宣告式 UI 框架 | 所有 @Composable 函式 |
| **Material3** | UI 設計系統 | Theme.kt, 各 Screen |
| **ViewModel** | 狀態管理與邏輯 | ComposeFeatureViewModel.kt |
| **StateFlow** | 響應式狀態流 | ViewModel 中的 uiState |
| **Navigation Compose** | 多頁面導航 | MainActivity 中的 NavHost |
| **State Hoisting** | 狀態提升模式 | IntroScreen, FeatureDetailScreen |
| **Repository Pattern** | 資料封裝 | ComposeFeatureRepository |
| **Sealed Class** | 類型安全路由 | Destination |
| **Data Class** | 不可變資料模型 | ComposeFeature, UiState |

---

## 🎓 學習建議

1. **追蹤資料流**：從用戶點擊開始，追蹤到 ViewModel 再回到 UI
2. **理解 Recomposition**：觀察狀態變更如何觸發 UI 重組
3. **實踐 State Hoisting**：讓 Composable 保持無狀態
4. **掌握 StateFlow**：理解響應式編程的核心
5. **Navigation 架構**：學習如何在 Compose 中管理多頁面

這個專案是學習 **Compose + MVVM + Navigation** 的絕佳範例！🚀
