package com.example.composeexamples.examples

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

/**
 * Compose 中使用冷流與熱流的實際範例
 *
 * 這個範例展示了在實際 Android App 中如何選擇使用冷流或熱流
 */

// ==========================================
// Repository Layer - 使用冷流
// ==========================================

/**
 * Repository 層通常使用冷流 (Flow)
 * 因為每次請求都應該是獨立的，並且只在需要時執行
 */
class UserRepository {

    /**
     * ❄️ 冷流：獲取用戶詳情
     * 每次收集都會執行新的 API 請求
     */
    fun getUserDetails(userId: String): Flow<Result<UserDetails>> = flow {
        emit(Result.Loading)
        delay(1500) // 模擬網路請求

        // 模擬成功或失敗
        if (userId.isNotEmpty()) {
            emit(Result.Success(UserDetails(
                id = userId,
                name = "使用者 $userId",
                email = "$userId@example.com",
                points = (100..1000).random()
            )))
        } else {
            emit(Result.Error("無效的使用者 ID"))
        }
    }

    /**
     * ❄️ 冷流：搜尋用戶
     * 每次搜尋都是獨立的請求
     */
    fun searchUsers(query: String): Flow<List<UserDetails>> = flow {
        delay(800) // 模擬搜尋延遲
        val results = List(5) { index ->
            UserDetails(
                id = "$query-$index",
                name = "搜尋結果 $index: $query",
                email = "$query$index@example.com",
                points = (50..500).random()
            )
        }
        emit(results)
    }

    /**
     * ❄️ 冷流：資料庫觀察（模擬 Room）
     * Room 的 Flow 是冷流，但會持續觀察資料庫變化
     */
    fun observeUserPreferences(): Flow<UserPreferences> = flow {
        repeat(5) { index ->
            delay(3000)
            emit(UserPreferences(
                theme = if (index % 2 == 0) "Light" else "Dark",
                notifications = index % 2 == 0
            ))
        }
    }
}

// ==========================================
// ViewModel Layer - 使用熱流
// ==========================================

/**
 * ViewModel 層使用熱流 (StateFlow/SharedFlow)
 * 因為需要在配置變更時保持狀態，並與 UI 層共享狀態
 */
class UserProfileViewModel(
    private val repository: UserRepository = UserRepository()
) : ViewModel() {

    // 🔥 StateFlow：UI 狀態
    // 用於持有和暴露 UI 狀態給 Compose
    private val _uiState = MutableStateFlow<UserProfileUiState>(UserProfileUiState.Initial)
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    // 🔥 SharedFlow：一次性事件
    // 用於導航、Toast 等不應該在配置變更時重複觸發的事件
    private val _uiEvent = MutableSharedFlow<UserProfileEvent>()
    val uiEvent: SharedFlow<UserProfileEvent> = _uiEvent.asSharedFlow()

    // 🔥 StateFlow：搜尋查詢
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // ❄️ 轉 🔥：將冷流（資料庫）轉為熱流（StateFlow）
    // 使用 stateIn，讓多個收集者共享相同的資料流
    //
    // 為什麼需要轉換？
    // - repository.observeUserPreferences() 是冷流，每次收集都會重新執行
    // - 如果多個 Composable 收集，會造成多次資料庫查詢（浪費資源）
    // - 轉為 StateFlow 後，所有收集者共享同一個資料流
    //
    // stateIn 參數說明：
    // - scope: viewModelScope（ViewModel 清除時自動取消）
    // - started: WhileSubscribed(5000)（有訂閱者時活躍，無訂閱者 5 秒後停止）
    // - initialValue: null（初始值，必須提供）
    val userPreferences: StateFlow<UserPreferences?> = repository
        .observeUserPreferences()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // ❄️ 冷流：搜尋結果
    // 使用 flatMapLatest 將 StateFlow 轉換為 Flow
    // 每次查詢變化時，取消舊請求並開始新請求
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchResults: Flow<List<UserDetails>> = _searchQuery
        .debounce(500) // 防抖動
        .filter { it.length >= 2 }
        .flatMapLatest { query ->
            repository.searchUsers(query)
        }

    /**
     * 載入用戶資料
     * 使用冷流獲取數據，然後更新熱流狀態
     */
    fun loadUser(userId: String) {
        viewModelScope.launch {
            // ❄️ 收集冷流（每次調用都是新的請求）
            repository.getUserDetails(userId).collect { result ->
                // 🔥 更新熱流狀態（所有觀察者都會收到）
                _uiState.value = when (result) {
                    is Result.Loading -> UserProfileUiState.Loading
                    is Result.Success -> UserProfileUiState.Success(result.data)
                    is Result.Error -> UserProfileUiState.Error(result.message)
                }
            }
        }
    }

    /**
     * 更新搜尋查詢
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * 重新整理
     */
    fun refresh(userId: String) {
        viewModelScope.launch {
            _uiEvent.emit(UserProfileEvent.ShowToast("重新整理中..."))
            loadUser(userId)
        }
    }

    /**
     * 編輯個人資料
     */
    fun editProfile() {
        viewModelScope.launch {
            _uiEvent.emit(UserProfileEvent.NavigateToEdit)
        }
    }
}

// ==========================================
// UI Layer - Compose
// ==========================================

/**
 * Compose UI 使用 collectAsState() 或 collectAsStateWithLifecycle()
 * 收集熱流（StateFlow/SharedFlow）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("unused")
@Composable
fun UserProfileScreen(
    userId: String,
    viewModel: UserProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    // 🔥 收集 StateFlow - 自動處理生命週期
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val userPreferences by viewModel.userPreferences.collectAsState()

    // 🔥 收集 SharedFlow - 處理一次性事件
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UserProfileEvent.ShowToast -> {
                    // 顯示 Toast
                }
                is UserProfileEvent.NavigateToEdit -> {
                    // 導航到編輯畫面
                }
            }
        }
    }

    // 載入數據
    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 標題
        Text(
            text = "使用者個人資料",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 顯示偏好設定（來自熱流）
        userPreferences?.let { prefs ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("主題：${prefs.theme}")
                    Text("通知：${if (prefs.notifications) "開啟" else "關閉"}")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 顯示用戶狀態
        when (val state = uiState) {
            is UserProfileUiState.Initial -> {
                Text("請選擇使用者")
            }
            is UserProfileUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is UserProfileUiState.Success -> {
                UserDetailsCard(
                    user = state.user,
                    onRefresh = { viewModel.refresh(userId) },
                    onEdit = { viewModel.editProfile() }
                )
            }
            is UserProfileUiState.Error -> {
                Text(
                    text = "錯誤：${state.message}",
                    color = MaterialTheme.colorScheme.error
                )
                Button(onClick = { viewModel.loadUser(userId) }) {
                    Text("重試")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 搜尋功能（使用冷流）
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            label = { Text("搜尋使用者") },
            modifier = Modifier.fillMaxWidth()
        )

        // ❄️ 收集冷流 - 每次查詢都是獨立的
        if (searchQuery.length >= 2) {
            val searchResults by viewModel.searchResults.collectAsState(initial = emptyList())

            Spacer(modifier = Modifier.height(8.dp))
            Text("搜尋結果：", style = MaterialTheme.typography.titleSmall)

            LazyColumn {
                items(searchResults) { user ->
                    ListItem(
                        headlineContent = { Text(user.name) },
                        supportingContent = { Text(user.email) }
                    )
                }
            }
        }
    }
}

@Composable
fun UserDetailsCard(
    user: UserDetails,
    onRefresh: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = user.name,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Email: ${user.email}")
            Text("積分: ${user.points}")

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onRefresh) {
                    Text("重新整理")
                }
                OutlinedButton(onClick = onEdit) {
                    Text("編輯")
                }
            }
        }
    }
}

// ==========================================
// Data Models
// ==========================================

data class UserDetails(
    val id: String,
    val name: String,
    val email: String,
    val points: Int
)

data class UserPreferences(
    val theme: String,
    val notifications: Boolean
)

sealed class UserProfileUiState {
    object Initial : UserProfileUiState()
    object Loading : UserProfileUiState()
    data class Success(val user: UserDetails) : UserProfileUiState()
    data class Error(val message: String) : UserProfileUiState()
}

sealed class UserProfileEvent {
    data class ShowToast(val message: String) : UserProfileEvent()
    object NavigateToEdit : UserProfileEvent()
}

sealed class Result<out T> {
    object Loading : Result<Nothing>()
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}

/**
 * ==========================================
 * 重點整理
 * ==========================================
 *
 * 架構層次的使用：
 *
 * Repository 層：
 * ❄️ 使用 Flow（冷流）
 * - 每次調用都是獨立的操作
 * - 適合 API 請求、資料庫查詢
 *
 * ViewModel 層：
 * 🔥 使用 StateFlow（UI 狀態）
 * - 保存和暴露 UI 狀態
 * - 配置變更時保留
 * - 多個 Composable 可以觀察相同狀態
 *
 * 🔥 使用 SharedFlow（事件）
 * - 一次性事件（導航、Toast）
 * - 不應該在配置變更時重複
 *
 * ❄️➡️🔥 使用 stateIn/shareIn
 * - 將 Repository 的冷流轉為熱流
 * - 優化性能，避免重複請求
 *
 * UI 層 (Compose)：
 * - 使用 collectAsState() 收集 StateFlow
 * - 使用 LaunchedEffect + collect() 收集 SharedFlow 事件
 * - 使用 collectAsState() 收集 Flow（冷流）
 *
 * ==========================================
 */

