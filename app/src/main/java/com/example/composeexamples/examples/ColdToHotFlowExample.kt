@file:Suppress("unused", "UNUSED_PARAMETER")

package com.example.composeexamples.examples

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * 冷流轉熱流完整範例
 *
 * 這個文件展示：
 * 1. 為什麼需要轉換（問題場景）
 * 2. stateIn() 的使用（需要當前值）
 * 3. shareIn() 的使用（不需要當前值）
 * 4. 常見錯誤和最佳實踐
 */

// ==========================================
// 問題場景：不轉換會怎樣？
// ==========================================

class ProblemExample {

    /**
     * ❌ 問題：冷流被多次收集，造成重複執行
     */
    class BadViewModel {
        // ❄️ 這是冷流！每次 collect 都會重新執行
        val userData: Flow<SimpleUser> = flow {
            println("❌ 開始昂貴的資料庫查詢...")
            delay(2000) // 模擬耗時操作
            emit(SimpleUser("Alice", 25))
            println("❌ 查詢完成")
        }
    }

    /**
     * 模擬 Compose 中多個地方收集
     */
    fun demonstrateProblem() = runBlocking {
        val viewModel = BadViewModel()

        println("=== 問題示範：冷流被多次收集 ===\n")

        // Composable A 收集
        launch {
            println("👤 Composable A 開始收集")
            viewModel.userData.collect { user ->
                println("👤 Composable A 收到：$user")
            }
        }

        delay(100)

        // Composable B 也收集（會觸發第二次查詢！）
        launch {
            println("👥 Composable B 開始收集")
            viewModel.userData.collect { user ->
                println("👥 Composable B 收到：$user")
            }
        }

        delay(5000)
        println("\n結果：資料庫被查詢了 2 次！浪費資源！\n")
    }
}

// ==========================================
// 解決方案 1：stateIn() - 需要當前值
// ==========================================

/**
 * ✅ 解決方案：使用 stateIn 轉為 StateFlow
 *
 * 使用時機：
 * - Room 資料庫觀察
 * - DataStore 偏好設定
 * - UI 狀態管理
 * - 任何需要「當前值」的場景
 */
class StateInExample {

    // 模擬 Repository
    class UserRepository {
        // ❄️ Room 的 DAO 回傳冷流
        fun observeUser(userId: String): Flow<SimpleUser> = flow {
            println("🔵 資料庫查詢開始（userId: $userId）")
            delay(1500)
            emit(SimpleUser("Alice", 25))

            // 模擬資料庫變化
            delay(3000)
            println("🔵 資料庫有變化！")
            emit(SimpleUser("Alice", 26))
        }
    }

    // ✅ 正確的 ViewModel
    class GoodViewModel(
        private val repository: UserRepository = UserRepository()
    ) : ViewModel() {

        // ❄️➡️🔥 使用 stateIn 轉換！
        val userData: StateFlow<SimpleUser?> = repository
            .observeUser("user123") // ❄️ 冷流（每次收集都會重新執行）
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null // 必須提供初始值
            )
        // 🔥 現在是 StateFlow（熱流），多個收集者共享同一個流
    }

    /**
     * 示範 stateIn 的好處
     */
    fun demonstrateSolution() = runBlocking {
        val viewModel = GoodViewModel()

        println("\n=== 解決方案：使用 stateIn ===\n")

        // Composable A 收集
        launch {
            println("👤 Composable A 訂閱 StateFlow")
            viewModel.userData.collect { user ->
                println("👤 Composable A 收到：$user")
            }
        }

        delay(500)

        // Composable B 也收集（不會觸發第二次查詢！）
        launch {
            println("👥 Composable B 訂閱 StateFlow")
            viewModel.userData.collect { user ->
                println("👥 Composable B 收到：$user")
            }
        }

        delay(6000)
        println("\n✅ 結果：資料庫只被查詢 1 次！兩個收集者共享數據！\n")
    }
}

// ==========================================
// stateIn 的三個 SharingStarted 策略
// ==========================================

class SharingStartedStrategies {

    private val repository = StateInExample.UserRepository()

    /**
     * 策略 1: Eagerly - 立即開始，永不停止
     * 適用：應用級別的狀態（如登入狀態）
     */
    class EagerlyExample : ViewModel() {
        val loginState: StateFlow<SimpleUser?> = flow {
            println("🟢 Eagerly：立即開始執行（即使沒有收集者）")
            delay(1000)
            emit(SimpleUser("System", 0))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly, // 立即開始
            initialValue = null
        )
    }

    /**
     * 策略 2: Lazily - 第一個訂閱者出現時開始
     * 適用：需要延遲初始化的數據
     */
    class LazilyExample : ViewModel() {
        val lazyData: StateFlow<SimpleUser?> = flow {
            println("🟡 Lazily：第一個訂閱者出現時才開始")
            delay(1000)
            emit(SimpleUser("Lazy", 0))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily, // 延遲開始
            initialValue = null
        )
    }

    /**
     * 策略 3: WhileSubscribed - 有訂閱者時活躍
     * 適用：ViewModel 的 UI 狀態（最常用）⭐
     */
    class WhileSubscribedExample : ViewModel() {
        val uiState: StateFlow<SimpleUser?> = flow {
            println("🔵 WhileSubscribed：有訂閱者時活躍")
            var count = 0
            while (true) {
                delay(1000)
                emit(SimpleUser("Active $count", count))
                count++
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // 最後訂閱者消失 5 秒後停止
            initialValue = null
        )
    }

    /**
     * 示範 WhileSubscribed 的停止機制
     */
    fun demonstrateWhileSubscribed() = runBlocking {
        val viewModel = WhileSubscribedExample()

        println("\n=== WhileSubscribed 停止機制示範 ===\n")

        val job = launch {
            println("👤 開始訂閱")
            viewModel.uiState.collect { user ->
                println("👤 收到：$user")
            }
        }

        delay(3000)
        println("👤 取消訂閱")
        job.cancel()

        println("⏳ 等待 5 秒... (stopTimeout)")
        delay(6000)
        println("✅ 上游 flow 已停止（節省資源）")
    }
}

// ==========================================
// 解決方案 2：shareIn() - 不需要當前值
// ==========================================

/**
 * ✅ 使用 shareIn 轉為 SharedFlow
 *
 * 使用時機：
 * - 感應器數據
 * - WebSocket 訊息
 * - 昂貴的計算需要共享
 * - 事件流（不需要初始值）
 */
class ShareInExample {

    /**
     * 範例：股價更新（昂貴的計算）
     */
    class StockViewModel : ViewModel() {

        // ❄️ 冷流：昂貴的股價計算
        private val stockPriceFlow: Flow<StockPrice> = flow {
            var price = 100.0
            while (true) {
                delay(1000)
                // 模擬昂貴的計算
                println("💰 執行昂貴的股價計算...")
                price += (-5..5).random()
                emit(StockPrice("AAPL", price))
            }
        }

        // ❄️➡️🔥 使用 shareIn 轉換
        val stockPrices: SharedFlow<StockPrice> = stockPriceFlow.shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            replay = 1 // 新訂閱者可獲得最後一個價格
        )
    }

    /**
     * 示範 shareIn 的好處
     */
    fun demonstrateShareIn() = runBlocking {
        val viewModel = StockViewModel()

        println("\n=== shareIn 示範 ===\n")

        // 訂閱者 1
        launch {
            println("📱 App 畫面訂閱")
            viewModel.stockPrices.collect { price ->
                println("📱 App 收到：$price")
            }
        }

        delay(2500)

        // 訂閱者 2（不會觸發新的計算！）
        launch {
            println("⌚ Widget 訂閱（因為 replay=1，立即收到最新價格）")
            viewModel.stockPrices.collect { price ->
                println("⌚ Widget 收到：$price")
            }
        }

        delay(5000)
        println("\n✅ 只有一個計算流，多個訂閱者共享！")
    }
}

// ==========================================
// shareIn 的 replay 參數
// ==========================================

class ReplayExample {

    /**
     * replay = 0：不重播（適合一次性事件）
     */
    class NoReplayExample : ViewModel() {
        val events: SharedFlow<String> = flow {
            emit("Event 1")
            delay(1000)
            emit("Event 2")
        }.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 0 // 晚加入的訂閱者收不到歷史事件
        )
    }

    /**
     * replay = 1：重播最後一個（最常用）
     */
    class ReplayOneExample : ViewModel() {
        val sensorData: SharedFlow<Float> = flow {
            var value = 0f
            while (true) {
                delay(1000)
                emit(value++)
            }
        }.shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            replay = 1 // 新訂閱者立即獲得最新數據
        )
    }

    /**
     * replay = N：重播最後 N 個
     */
    class ReplayMultipleExample : ViewModel() {
        val chatMessages: SharedFlow<String> = flow {
            var count = 0
            while (true) {
                delay(1000)
                emit("Message ${count++}")
            }
        }.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 10 // 新用戶可看到最近 10 條訊息
        )
    }

    /**
     * 示範 replay 的效果
     */
    fun demonstrateReplay() = runBlocking {
        println("\n=== replay 參數示範 ===\n")

        // replay = 1 的例子
        val viewModel = ReplayOneExample()

        delay(3500) // 等待產生幾個值

        println("🆕 新訂閱者加入（晚了 3.5 秒）")
        viewModel.sensorData.take(2).collect { value ->
            println("🆕 收到：$value（立即收到最新值，因為 replay=1）")
        }
    }
}

// ==========================================
// stateIn vs shareIn 對比範例
// ==========================================

class ComparisonExample {

    class ComparisonViewModel : ViewModel() {

        private val dataFlow: Flow<Int> = flow {
            var count = 0
            while (true) {
                delay(1000)
                emit(count++)
            }
        }

        // 使用 stateIn（總是有值）
        val stateFlowData: StateFlow<Int> = dataFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = -1 // 必須提供
        )

        // 使用 shareIn（可能沒值）
        val sharedFlowData: SharedFlow<Int> = dataFlow.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            replay = 0 // 可選
        )
    }

    fun demonstrateComparison() = runBlocking {
        val viewModel = ComparisonViewModel()

        println("\n=== stateIn vs shareIn 對比 ===\n")

        // StateFlow 立即有值
        println("StateFlow 當前值：${viewModel.stateFlowData.value}") // -1 (initialValue)

        // SharedFlow 沒有 .value 屬性
        // println(viewModel.sharedFlowData.value) // ❌ 編譯錯誤

        launch {
            println("📊 收集 StateFlow（立即獲得當前值）")
            viewModel.stateFlowData.take(3).collect {
                println("📊 StateFlow: $it")
            }
        }

        launch {
            println("📡 收集 SharedFlow（等待新值）")
            viewModel.sharedFlowData.take(3).collect {
                println("📡 SharedFlow: $it")
            }
        }

        delay(4000)
    }
}

// ==========================================
// 常見錯誤
// ==========================================

class CommonMistakes {

    /**
     * ❌ 錯誤 1：在 Composable 中轉換
     */
    // @Composable
    // fun BadScreen(flow: Flow<Data>) {
    //     val scope = rememberCoroutineScope()
    //     val stateFlow = flow.stateIn(scope, ...) // ❌ 每次重組都創建！
    // }

    /**
     * ✅ 正確：在 ViewModel 中轉換
     */
    class GoodViewModel : ViewModel() {
        val data: StateFlow<String?> = flow { emit("Data") }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )
    }

    /**
     * ❌ 錯誤 2：使用 Eagerly 造成記憶體洩漏
     */
    class LeakyViewModel : ViewModel() {
        val data = flow {
            while (true) {
                delay(1000)
                emit("Data")
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly, // ❌ 永遠不會停止！
            null
        )
    }

    /**
     * ✅ 正確：使用 WhileSubscribed
     */
    class NoLeakViewModel : ViewModel() {
        val data = flow {
            while (true) {
                delay(1000)
                emit("Data")
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000), // ✅ 沒訂閱者時會停止
            null
        )
    }
}

// ==========================================
// 資料模型
// ==========================================

data class SimpleUser(val name: String, val age: Int)
data class StockPrice(val symbol: String, val price: Double)

// ==========================================
// 主程式：執行所有示範
// ==========================================

fun main() {
    // 1. 問題示範
    ProblemExample().demonstrateProblem()

    // 2. stateIn 解決方案
    StateInExample().demonstrateSolution()

    // 3. WhileSubscribed 示範
    SharingStartedStrategies().demonstrateWhileSubscribed()

    // 4. shareIn 示範
    ShareInExample().demonstrateShareIn()

    // 5. replay 示範
    ReplayExample().demonstrateReplay()

    // 6. 對比示範
    ComparisonExample().demonstrateComparison()

    println("\n" + "=".repeat(50))
    println("完整示範結束！")
    println("=".repeat(50))
}

/**
 * ==========================================
 * 快速參考
 * ==========================================
 *
 * 何時使用 stateIn()？
 * ✅ Room 資料庫
 * ✅ DataStore
 * ✅ UI 狀態
 * ✅ 需要當前值
 *
 * 何時使用 shareIn()？
 * ✅ 感應器數據
 * ✅ WebSocket
 * ✅ 昂貴的計算
 * ✅ 不需要當前值
 *
 * SharingStarted 建議：
 * - UI 狀態：WhileSubscribed(5000) ⭐ 最常用
 * - App 級狀態：Eagerly
 * - 延遲初始化：Lazily
 *
 * ==========================================
 */

