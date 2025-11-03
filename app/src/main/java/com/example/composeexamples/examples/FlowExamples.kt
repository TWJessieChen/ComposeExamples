@file:Suppress("unused")

package com.example.composeexamples.examples

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * 冷流 (Cold Flow) vs 熱流 (Hot Flow) 使用時機
 *
 * ==========================================
 * 冷流 (Cold Flow) - Flow
 * ==========================================
 * 特性：
 * 1. 惰性執行：只有在被收集（collect）時才開始執行
 * 2. 單播：每個收集者都會獲得獨立的數據流
 * 3. 每次收集都會重新執行生產邏輯
 *
 * 使用時機：
 * ✓ 資料庫查詢（每次查詢都需要最新數據）
 * ✓ 網路請求（每個訂閱者需要獨立的請求）
 * ✓ 檔案讀取
 * ✓ 一次性數據流處理
 * ✓ 按需加載數據
 *
 * ==========================================
 * 熱流 (Hot Flow) - StateFlow / SharedFlow
 * ==========================================
 * 特性：
 * 1. 立即執行：不管有沒有收集者，都可能產生數據
 * 2. 多播：所有收集者共享同一個數據流
 * 3. 可以有狀態（StateFlow）或無狀態（SharedFlow）
 *
 * 使用時機：
 * ✓ UI 狀態管理（StateFlow）
 * ✓ 事件廣播（SharedFlow）
 * ✓ 多個觀察者需要相同數據
 * ✓ 需要保持最新狀態
 * ✓ 配置變更時保留狀態
 */


// ==========================================
// 冷流範例 (Cold Flow Examples)
// ==========================================

class ColdFlowExamples {

    /**
     * 範例1：資料庫查詢
     * 每次收集都執行新的查詢，獲取最新數據
     */
    fun fetchUserFromDatabase(userId: String): Flow<User> = flow {
        println("🔵 冷流：開始查詢用戶 $userId")
        delay(1000) // 模擬資料庫查詢
        emit(User(userId, "User $userId", System.currentTimeMillis()))
        println("🔵 冷流：完成查詢")
    }

    /**
     * 範例2：網路 API 請求
     * 每個訂閱者都會觸發獨立的網路請求
     */
    fun fetchDataFromApi(endpoint: String): Flow<ApiResponse> = flow {
        println("🔵 冷流：發送 API 請求到 $endpoint")
        delay(1500) // 模擬網路延遲
        emit(ApiResponse(data = "Response from $endpoint", timestamp = System.currentTimeMillis()))
        println("🔵 冷流：收到 API 回應")
    }

    /**
     * 範例3：檔案讀取流
     * 每次收集都重新讀取檔案
     */
    fun readFileAsFlow(fileName: String): Flow<String> = flow {
        println("🔵 冷流：開始讀取檔案 $fileName")
        delay(500)
        // 模擬逐行讀取
        listOf("Line 1", "Line 2", "Line 3").forEach { line ->
            emit(line)
            delay(100)
        }
        println("🔵 冷流：檔案讀取完成")
    }

    /**
     * 範例4：分頁數據加載
     * 按需加載，每次收集都是獨立的加載過程
     */
    fun loadPageData(page: Int): Flow<List<String>> = flow {
        println("🔵 冷流：加載第 $page 頁")
        delay(800)
        val items = List(10) { "Item ${page * 10 + it}" }
        emit(items)
    }
}

// ==========================================
// 熱流範例 (Hot Flow Examples)
// ==========================================

class HotFlowExamples {

    /**
     * 範例1：StateFlow - UI 狀態管理
     * 保持最新狀態，新訂閱者立即獲得當前值
     */
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun updateUsername(name: String) {
        println("🔴 StateFlow：更新使用者名稱")
        _uiState.update { it.copy(username = name) }
    }

    fun setLoading(isLoading: Boolean) {
        println("🔴 StateFlow：設定載入狀態 = $isLoading")
        _uiState.update { it.copy(isLoading = isLoading) }
    }

    /**
     * 範例2：SharedFlow - 事件廣播
     * 用於一次性事件，如導航、顯示 Toast、錯誤訊息
     */
    private val _events = MutableSharedFlow<UiEvent>(
        replay = 0, // 不重播歷史事件
        extraBufferCapacity = 10 // 緩衝容量
    )
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    suspend fun showToast(message: String) {
        println("🔴 SharedFlow：發送 Toast 事件")
        _events.emit(UiEvent.ShowToast(message))
    }

    suspend fun navigateTo(screen: String) {
        println("🔴 SharedFlow：發送導航事件")
        _events.emit(UiEvent.Navigate(screen))
    }

    /**
     * 範例3：SharedFlow - 位置更新
     * 多個觀察者共享位置數據
     */
    private val _locationUpdates = MutableSharedFlow<Location>(
        replay = 1, // 重播最後一個位置
        extraBufferCapacity = 5
    )
    val locationUpdates: SharedFlow<Location> = _locationUpdates.asSharedFlow()

    suspend fun startLocationTracking() {
        repeat(5) { index ->
            delay(2000)
            val location = Location(lat = 25.033 + index * 0.001, lng = 121.565 + index * 0.001)
            println("🔴 SharedFlow：廣播位置更新 $location")
            _locationUpdates.emit(location)
        }
    }

    /**
     * 範例4：StateFlow - 購物車狀態
     * 全域共享狀態，所有觀察者看到相同的購物車
     */
    private val _cartState = MutableStateFlow(CartState())
    val cartState: StateFlow<CartState> = _cartState.asStateFlow()

    fun addToCart(item: String) {
        println("🔴 StateFlow：新增商品到購物車")
        _cartState.update { state ->
            state.copy(items = state.items + item)
        }
    }

    fun clearCart() {
        println("🔴 StateFlow：清空購物車")
        _cartState.update { CartState() }
    }
}

// ==========================================
// 冷流轉熱流 (Cold to Hot Flow)
// ==========================================

class FlowConversionExamples {

    /**
     * 使用 shareIn 將冷流轉為熱流
     * 適用場景：多個訂閱者需要共享昂貴的計算或網路請求結果
     */
    fun coldFlowExample(): Flow<Int> = flow {
        println("🔵 冷流：開始昂貴的計算")
        repeat(5) { index ->
            delay(1000)
            emit(index)
        }
    }

    /**
     * 使用 stateIn 將冷流轉為 StateFlow
     * 適用場景：需要將 Room 的 Flow 轉為 ViewModel 的 StateFlow
     */
    fun databaseFlow(): Flow<List<String>> = flow {
        println("🔵 冷流：監聽資料庫變化")
        repeat(3) { index ->
            delay(2000)
            emit(List(3) { "DB Item ${index * 3 + it}" })
        }
    }
}

// ==========================================
// 資料模型 (Data Models)
// ==========================================

data class User(
    val id: String,
    val name: String,
    val fetchedAt: Long
)

data class ApiResponse(
    val data: String,
    val timestamp: Long
)

data class UiState(
    val username: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class Navigate(val screen: String) : UiEvent()
    data class ShowError(val error: String) : UiEvent()
}

data class Location(
    val lat: Double,
    val lng: Double
)

data class CartState(
    val items: List<String> = emptyList()
) {
    val itemCount: Int get() = items.size
    val isEmpty: Boolean get() = items.isEmpty()
}

// ==========================================
// 實際使用範例 (Usage Examples)
// ==========================================

fun main() = runBlocking {
    println("====================================")
    println("冷流範例：每個收集者獨立執行")
    println("====================================\n")

    val coldFlow = ColdFlowExamples()
    val userFlow = coldFlow.fetchUserFromDatabase("user123")

    // 第一個收集者
    launch {
        println("👤 收集者 1 開始收集")
        userFlow.collect { user ->
            println("👤 收集者 1 收到：$user")
        }
    }

    delay(500)

    // 第二個收集者（會重新執行流）
    launch {
        println("👥 收集者 2 開始收集")
        userFlow.collect { user ->
            println("👥 收集者 2 收到：$user")
        }
    }

    delay(3000)

    println("\n====================================")
    println("熱流範例：所有收集者共享數據")
    println("====================================\n")

    val hotFlow = HotFlowExamples()

    // 第一個收集者
    launch {
        println("👤 收集者 1 訂閱 StateFlow")
        hotFlow.uiState.collect { state ->
            println("👤 收集者 1 收到狀態：$state")
        }
    }

    delay(500)

    // 第二個收集者（會立即收到當前值）
    launch {
        println("👥 收集者 2 訂閱 StateFlow")
        hotFlow.uiState.collect { state ->
            println("👥 收集者 2 收到狀態：$state")
        }
    }

    delay(500)

    // 更新狀態（所有收集者都會收到）
    hotFlow.updateUsername("Alice")
    delay(500)
    hotFlow.setLoading(true)
    delay(500)
    hotFlow.setLoading(false)

    delay(2000)
}

/**
 * ==========================================
 * 總結：選擇指南
 * ==========================================
 *
 * 使用 冷流 (Flow) 當：
 * • 每個訂閱者需要獨立的數據流
 * • 數據生產是惰性的（按需執行）
 * • 一次性操作（如單次 API 請求）
 * • 數據庫查詢
 * • 檔案讀取
 *
 * 使用 StateFlow 當：
 * • 需要管理 UI 狀態
 * • 需要當前狀態值（總是有值）
 * • 多個觀察者需要相同的狀態
 * • ViewModel 層管理狀態
 * • 配置變更後保留狀態
 *
 * 使用 SharedFlow 當：
 * • 事件廣播（導航、Toast、錯誤）
 * • 一次性事件（不需要重播給新訂閱者）
 * • 多個訂閱者但不需要初始值
 * • 需要配置 replay 和 buffer
 *
 * ==========================================
 */

