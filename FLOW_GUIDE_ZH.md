# 冷流與熱流使用時機完整指南

## 📚 目錄
1. [基本概念](#基本概念)
2. [冷流 (Cold Flow)](#冷流-cold-flow)
3. [熱流 (Hot Flow)](#熱流-hot-flow)
4. [實際使用範例](#實際使用範例)
5. [選擇指南](#選擇指南)

---

## 基本概念

### 冷流 (Cold Flow) - `Flow`
**特性：**
- ❄️ **惰性執行**：只有在被收集（collect）時才開始執行
- 🔄 **單播**：每個收集者都會獲得獨立的數據流
- 🔁 **每次收集都重新執行**：生產邏輯會重新執行

### 熱流 (Hot Flow) - `StateFlow` / `SharedFlow`
**特性：**
- 🔥 **立即執行**：不管有沒有收集者，都可能產生數據
- 📢 **多播**：所有收集者共享同一個數據流
- 💾 **可以有狀態**：StateFlow 總是有值，SharedFlow 可配置

---

## 冷流 (Cold Flow)

### ✅ 使用時機

#### 1. **資料庫查詢**
```kotlin
fun getUserById(id: String): Flow<User> = flow {
    emit(database.queryUser(id))
}
```
**為什麼用冷流？**
- 每次查詢都需要獲取最新數據
- 不同的查詢應該是獨立的

#### 2. **網路 API 請求**
```kotlin
fun fetchProducts(): Flow<List<Product>> = flow {
    emit(api.getProducts())
}
```
**為什麼用冷流？**
- 每個請求應該是獨立的
- 避免共享過期的網路響應

#### 3. **檔案讀取**
```kotlin
fun readFile(path: String): Flow<String> = flow {
    File(path).forEachLine { line ->
        emit(line)
    }
}
```
**為什麼用冷流？**
- 按需讀取，節省記憶體
- 每次讀取都是獨立操作

#### 4. **分頁加載**
```kotlin
fun loadPage(pageNumber: Int): Flow<PageData> = flow {
    emit(repository.fetchPage(pageNumber))
}
```
**為什麼用冷流？**
- 每頁加載是獨立的
- 只在需要時才執行

### 📝 範例：Repository 層
```kotlin
class ProductRepository {
    // ❄️ 冷流：每次調用都是新的請求
    fun getProductDetails(id: String): Flow<Product> = flow {
        emit(api.fetchProduct(id))
    }
}
```

---

## 熱流 (Hot Flow)

### 🔥 StateFlow

#### ✅ 使用時機

##### 1. **UI 狀態管理**
```kotlin
class ViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
}
```
**為什麼用 StateFlow？**
- UI 需要當前狀態
- 配置變更時保留狀態
- 多個 Composable 觀察相同狀態

##### 2. **全域狀態（購物車、使用者資訊）**
```kotlin
object CartManager {
    private val _cartState = MutableStateFlow(CartState())
    val cartState: StateFlow<CartState> = _cartState.asStateFlow()
}
```
**為什麼用 StateFlow？**
- 所有畫面需要相同的狀態
- 總是有當前值可用

##### 3. **偏好設定**
```kotlin
class SettingsViewModel : ViewModel() {
    val darkMode: StateFlow<Boolean> = preferencesRepository
        .darkModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
}
```

### 🔥 SharedFlow

#### ✅ 使用時機

##### 1. **一次性事件（導航、Toast、錯誤訊息）**
```kotlin
class ViewModel : ViewModel() {
    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()
    
    suspend fun showError(message: String) {
        _events.emit(UiEvent.ShowError(message))
    }
}
```
**為什麼用 SharedFlow？**
- 事件不應該在配置變更時重複
- 不需要初始值
- 多個觀察者可以接收相同事件

##### 2. **位置更新、感應器數據**
```kotlin
class LocationService {
    private val _locationUpdates = MutableSharedFlow<Location>(replay = 1)
    val locationUpdates: SharedFlow<Location> = _locationUpdates.asSharedFlow()
}
```
**為什麼用 SharedFlow？**
- 多個訂閱者需要相同的位置數據
- 可以配置 replay（新訂閱者可獲得最後一個位置）

##### 3. **事件廣播（聊天訊息、推播通知）**
```kotlin
object NotificationManager {
    private val _notifications = MutableSharedFlow<Notification>()
    val notifications: SharedFlow<Notification> = _notifications.asSharedFlow()
}
```

---

## 實際使用範例

### 📱 完整的 Android 架構

#### **Repository 層：使用冷流**
```kotlin
class UserRepository {
    // ❄️ 冷流：每次調用都是獨立的 API 請求
    fun getUserProfile(userId: String): Flow<Result<User>> = flow {
        emit(Result.Loading)
        try {
            val user = api.getUser(userId)
            emit(Result.Success(user))
        } catch (e: Exception) {
            emit(Result.Error(e.message))
        }
    }
}
```

#### **ViewModel 層：使用熱流**
```kotlin
class UserProfileViewModel(
    private val repository: UserRepository
) : ViewModel() {
    
    // 🔥 StateFlow：UI 狀態
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    // 🔥 SharedFlow：一次性事件
    private val _events = MutableSharedFlow<UserEvent>()
    val events: SharedFlow<UserEvent> = _events.asSharedFlow()
    
    fun loadUser(userId: String) {
        viewModelScope.launch {
            // ❄️ 收集冷流
            repository.getUserProfile(userId).collect { result ->
                // 🔥 更新熱流狀態
                _uiState.value = when (result) {
                    is Result.Loading -> UiState.Loading
                    is Result.Success -> UiState.Success(result.data)
                    is Result.Error -> UiState.Error(result.message)
                }
            }
        }
    }
    
    fun showToast(message: String) {
        viewModelScope.launch {
            _events.emit(UserEvent.ShowToast(message))
        }
    }
}
```

#### **UI 層：Compose**
```kotlin
@Composable
fun UserProfileScreen(
    viewModel: UserProfileViewModel = viewModel()
) {
    // 🔥 收集 StateFlow
    val uiState by viewModel.uiState.collectAsState()
    
    // 🔥 處理 SharedFlow 事件
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UserEvent.ShowToast -> {
                    // 顯示 Toast
                }
            }
        }
    }
    
    when (uiState) {
        is UiState.Loading -> CircularProgressIndicator()
        is UiState.Success -> UserContent(uiState.user)
        is UiState.Error -> ErrorMessage(uiState.message)
    }
}
```

---

## 選擇指南

### 🤔 我應該選擇哪一個？

#### 使用 **冷流 (Flow)** 當你需要：

| 場景 | 原因 |
|------|------|
| 🗄️ **資料庫查詢** | 每次查詢都應該獲取最新數據 |
| 🌐 **網路請求** | 每個請求應該是獨立的 |
| 📄 **檔案讀取** | 按需執行，節省資源 |
| 🔍 **搜尋功能** | 每次搜尋都是新的查詢 |
| 📊 **分頁加載** | 每頁加載是獨立操作 |
| ⏱️ **計時器/間隔** | 每個訂閱者需要獨立的計時 |

#### 使用 **StateFlow** 當你需要：

| 場景 | 原因 |
|------|------|
| 🎨 **UI 狀態** | 總是需要當前值 |
| 🛒 **購物車狀態** | 多個畫面共享相同狀態 |
| 👤 **使用者資訊** | 配置變更時保留 |
| ⚙️ **設定/偏好** | 所有地方都需要最新值 |
| 🔐 **登入狀態** | 全域共享的狀態 |

#### 使用 **SharedFlow** 當你需要：

| 場景 | 原因 |
|------|------|
| 🧭 **導航事件** | 一次性事件，不重播 |
| 📱 **Toast/Snackbar** | 不應該在配置變更時重複 |
| ❌ **錯誤訊息** | 事件性質，不是狀態 |
| 📍 **位置更新** | 多個訂閱者，可配置 replay |
| 💬 **聊天訊息** | 事件廣播 |
| 🔔 **推播通知** | 多個接收者 |

---

## 🎯 快速決策樹

```
需要數據流？
│
├─ 每次訂閱都要重新執行嗎？
│  ├─ 是 → ❄️ Flow (冷流)
│  │     範例：API 請求、資料庫查詢
│  │
│  └─ 否 → 多個訂閱者共享數據嗎？
│         ├─ 是 → 需要當前值嗎？
│         │      ├─ 是 → 🔥 StateFlow
│         │      │     範例：UI 狀態、購物車
│         │      │
│         │      └─ 否 → 🔥 SharedFlow
│         │            範例：事件、Toast、導航
│         │
│         └─ 否 → ❄️ Flow (冷流)
```

---

## 💡 冷流轉熱流 (重要!)

### 為什麼需要轉換？

當你有一個冷流（如 Room 的資料庫查詢），但希望：
- ✅ 多個觀察者共享同一個資料流（避免重複查詢）
- ✅ 在 ViewModel 中保持活躍狀態
- ✅ 自動管理生命週期

這時就需要將冷流轉為熱流！

---

### 方法 1️⃣：使用 `stateIn()` - 轉為 StateFlow

#### ✅ 使用時機：需要當前值

**典型場景：**
- Room 資料庫觀察
- DataStore 偏好設定
- 需要初始值的數據流
- UI 狀態管理

#### 📝 完整範例

**❌ 不好的做法（每個收集者都會重複查詢資料庫）**
```kotlin
class UserViewModel(
    private val repository: UserRepository
) : ViewModel() {
    // ❄️ 冷流：每次收集都會重新查詢資料庫
    val userData: Flow<User> = repository.observeUser(userId)
}

@Composable
fun Screen(viewModel: UserViewModel) {
    // 收集者 1：觸發資料庫查詢
    val user1 by viewModel.userData.collectAsState(initial = null)
    
    // 收集者 2：又觸發一次資料庫查詢！（重複了！）
    val user2 by viewModel.userData.collectAsState(initial = null)
}
```

**✅ 正確做法（使用 stateIn 共享資料流）**
```kotlin
class UserViewModel(
    private val repository: UserRepository
) : ViewModel() {
    // ❄️➡️🔥 將冷流轉為熱流
    val userData: StateFlow<User?> = repository
        .observeUser(userId) // ❄️ Room 的冷流
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}

@Composable
fun Screen(viewModel: UserViewModel) {
    // 🔥 多個收集者共享同一個資料流
    val user by viewModel.userData.collectAsState()
    // 只會有一次資料庫查詢！
}
```

#### 🔧 stateIn 參數說明

```kotlin
flow.stateIn(
    scope = viewModelScope,           // 協程範圍
    started = SharingStarted.策略,     // 啟動策略
    initialValue = 初始值               // 必須提供初始值
)
```

**started 參數選擇：**

| SharingStarted 策略 | 何時開始 | 何時停止 | 使用時機 |
|-------------------|---------|---------|----------|
| `Eagerly` | 立即開始 | viewModelScope 取消時 | 應用級狀態（登入狀態） |
| `Lazily` | 第一個訂閱者出現時 | viewModelScope 取消時 | 延遲初始化的數據 |
| `WhileSubscribed(5000)` | 第一個訂閱者出現時 | 最後訂閱者消失 5 秒後 | **最常用**：ViewModel UI 狀態 |

#### 📱 實際範例 1：Room 資料庫

```kotlin
// Repository
class UserRepository(private val userDao: UserDao) {
    // ❄️ Room 回傳冷流
    fun observeUser(userId: String): Flow<User> {
        return userDao.observeUserById(userId)
    }
}

// ViewModel
class ProfileViewModel(
    private val repository: UserRepository
) : ViewModel() {
    // ❄️➡️🔥 轉換！
    val currentUser: StateFlow<User?> = repository
        .observeUser("user123")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}

// Compose UI
@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val user by viewModel.currentUser.collectAsState()
    
    user?.let {
        Text("歡迎，${it.name}")
    }
}
```

#### 📱 實際範例 2：DataStore 偏好設定

```kotlin
// Repository
class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    // ❄️ DataStore 回傳冷流
    val themeMode: Flow<ThemeMode> = dataStore.data
        .map { prefs -> 
            ThemeMode.valueOf(prefs[THEME_KEY] ?: "SYSTEM")
        }
}

// ViewModel
class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {
    // ❄️➡️🔥 轉換為 StateFlow
    val themeMode: StateFlow<ThemeMode> = repository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly, // 立即開始
            initialValue = ThemeMode.SYSTEM
        )
}
```

---

### 方法 2️⃣：使用 `shareIn()` - 轉為 SharedFlow

#### ✅ 使用時機：不需要當前值

**典型場景：**
- 感應器數據流
- WebSocket 訊息
- 事件流（但通常 SharedFlow 已經是熱流）
- 昂貴的計算結果需要共享

#### 📝 完整範例

**❌ 不好的做法（每個訂閱者都重複計算）**
```kotlin
class StockViewModel(
    private val repository: StockRepository
) : ViewModel() {
    // ❄️ 冷流：昂貴的股價計算
    val stockPrices: Flow<StockPrice> = flow {
        while (true) {
            delay(1000)
            val price = expensiveStockCalculation() // 很耗資源！
            emit(price)
        }
    }
}

@Composable
fun Screen(viewModel: StockViewModel) {
    // 訂閱者 1：觸發計算
    val price1 by viewModel.stockPrices.collectAsState(initial = null)
    
    // 訂閱者 2：又觸發一次計算！（浪費！）
    val price2 by viewModel.stockPrices.collectAsState(initial = null)
}
```

**✅ 正確做法（使用 shareIn 共享計算結果）**
```kotlin
class StockViewModel(
    private val repository: StockRepository
) : ViewModel() {
    // ❄️➡️🔥 轉為 SharedFlow，共享計算結果
    val stockPrices: SharedFlow<StockPrice> = flow {
        while (true) {
            delay(1000)
            val price = expensiveStockCalculation()
            emit(price)
        }
    }.shareIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        replay = 1 // 新訂閱者可獲得最後一個價格
    )
}
```

#### 🔧 shareIn 參數說明

```kotlin
flow.shareIn(
    scope = viewModelScope,           // 協程範圍
    started = SharingStarted.策略,     // 啟動策略
    replay = 0                        // 可選：重播幾個歷史值
)
```

**與 stateIn 的差異：**
- ❌ 不需要 `initialValue`（可能沒有值）
- ✅ 可設定 `replay`（重播歷史事件數量）

#### 📱 實際範例 1：感應器數據

```kotlin
class SensorViewModel(
    private val sensorManager: SensorManager
) : ViewModel() {
    // ❄️ 冷流：感應器數據
    private val sensorDataFlow: Flow<SensorData> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(SensorData(event.values))
            }
        }
        sensorManager.registerListener(listener, ...)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
    
    // ❄️➡️🔥 轉為 SharedFlow，多個訂閱者共享
    val sensorData: SharedFlow<SensorData> = sensorDataFlow.shareIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        replay = 1 // 新訂閱者獲得最新數據
    )
}
```

#### 📱 實際範例 2：網路 SSE (Server-Sent Events)

```kotlin
class NotificationViewModel(
    private val api: NotificationApi
) : ViewModel() {
    // ❄️ 冷流：SSE 連線
    private val notificationStream: Flow<Notification> = flow {
        api.connectToNotifications().collect { notification ->
            emit(notification)
        }
    }
    
    // ❄️➡️🔥 轉為 SharedFlow，所有畫面共享通知
    val notifications: SharedFlow<Notification> = notificationStream.shareIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly, // App 啟動就開始
        replay = 0 // 不重播歷史通知
    )
}
```

---

### 🆚 stateIn vs shareIn 比較表

| 特性 | stateIn() | shareIn() |
|------|-----------|-----------|
| **回傳型別** | `StateFlow<T>` | `SharedFlow<T>` |
| **必須有初始值** | ✅ 是 | ❌ 否 |
| **總是有值** | ✅ 是 | ❌ 否（可能沒收到事件） |
| **replay 參數** | ❌ 無（固定 replay=1） | ✅ 可配置 |
| **適合** | UI 狀態、資料庫 | 事件流、感應器 |
| **收集時保證** | 立即獲得當前值 | 可能要等待新事件 |

---

### 🎯 選擇建議

```
需要轉換冷流為熱流？
│
└─ 需要「當前值」嗎？（UI 狀態、設定、資料）
   ├─ ✅ 需要 → 使用 stateIn()
   │            └─ StateFlow 總是有值
   │
   └─ ❌ 不需要 → 使用 shareIn()
                └─ SharedFlow 可能沒有值，適合事件
```

---

### ⚠️ 常見錯誤

#### 錯誤 1：沒有轉換，導致重複執行

```kotlin
// ❌ 錯誤
class ViewModel(repo: Repository) : ViewModel() {
    val data: Flow<Data> = repo.getData() // 冷流
}

// 每個 collectAsState() 都會重新執行 repo.getData()
```

#### 錯誤 2：使用錯誤的 started 策略

```kotlin
// ❌ 可能造成記憶體洩漏
val data = flow.stateIn(
    viewModelScope,
    SharingStarted.Eagerly, // 永不停止！
    initialValue = null
)

// ✅ 正確：沒訂閱者時停止
val data = flow.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    initialValue = null
)
```

#### 錯誤 3：在 Composable 中轉換

```kotlin
// ❌ 絕對不要這樣做！
@Composable
fun Screen(flow: Flow<Data>) {
    val scope = rememberCoroutineScope()
    val stateFlow = flow.stateIn(scope, ...) // 錯誤！每次重組都會創建
}

// ✅ 在 ViewModel 中轉換
class ViewModel : ViewModel() {
    val data = flow.stateIn(viewModelScope, ...)
}
```

---

## 📖 完整範例文件

我已經為你創建了兩個完整的範例文件：

1. **FlowExamples.kt** - 基礎概念和範例
   - 冷流基本用法
   - 熱流基本用法
   - 轉換範例

2. **FlowInComposeExample.kt** - Compose 實際應用
   - Repository + ViewModel + UI 完整架構
   - 實際的使用者個人資料範例
   - 搜尋功能範例
   - 事件處理範例

---

## 🎓 總結

### 記住這三個核心原則：

1. **Repository 層用冷流** ❄️
   - 每次操作都是獨立的
   - 只在需要時執行

2. **ViewModel 層用熱流** 🔥
   - StateFlow 管理狀態
   - SharedFlow 處理事件

3. **按需轉換** ❄️ → 🔥
   - 使用 `stateIn` / `shareIn`
   - 優化性能，避免重複操作

---

## 🔗 相關資源

- [Kotlin Flow 官方文件](https://kotlinlang.org/docs/flow.html)
- [StateFlow 和 SharedFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow)
- [在 Android 中使用 Flow](https://developer.android.com/kotlin/flow)

---

**created by GitHub Copilot** 🤖

