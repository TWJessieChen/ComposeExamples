package com.example.composeexamples.data

/**
 * Represents a single page in the Compose showcase.
 */
data class ComposeFeature(
    val id: String,
    val title: String,
    val summary: String,
    val highlights: List<String>,
    val codeHint: String
)

class ComposeFeatureRepository {
    fun loadFeatures(): List<ComposeFeature> = listOf(
        ComposeFeature(
            id = "composable-basics",
            title = "Composable 基礎",
            summary = "了解 @Composable 函式與宣告式 UI 的核心概念。",
            highlights = listOf(
                "@Composable 函式負責描述畫面，而非直接操作 View。",
                "UI 會根據狀態改變自動重新組合 (recomposition)。",
                "可利用 Preview 迅速檢視每個組件。"
            ),
            codeHint = "@Composable\nfun Greeting(name: String) {\n    Text(\"Hello $name!\")\n}"
        ),
        ComposeFeature(
            id = "state-hoisting",
            title = "狀態管理與 State Hoisting",
            summary = "將狀態提升 (hoist) 讓可組合項保持無狀態並易於測試。",
            highlights = listOf(
                "記得使用 remember 與 mutableStateOf 保存狀態。",
                "將狀態與事件處理往呼叫端抽離，方便重複使用。",
                "ViewModel 是跨畫面儲存與分享狀態的最佳夥伴。"
            ),
            codeHint = "var query by rememberSaveable { mutableStateOf(\"\") }\nSearchBar(query, onQueryChange = { query = it })"
        ),
        ComposeFeature(
            id = "material3-layout",
            title = "Material 3 佈局與元件",
            summary = "利用 Scaffold、TopAppBar 與 Card 打造一致的 Material 3 介面。",
            highlights = listOf(
                "Scaffold 提供 TopBar、BottomBar 與 FAB 的佈局槽位。",
                "MaterialTheme.typography 與 colorScheme 維持整體風格。",
                "Card 與 Button 元件具有適合行動裝置的預設間距與樣式。"
            ),
            codeHint = "Scaffold(\n    topBar = { SmallTopAppBar(title = { Text(\"Compose\") }) }\n) { inner ->\n    Column(Modifier.padding(inner)) { /* content */ }\n}"
        ),
        ComposeFeature(
            id = "navigation-compose",
            title = "Navigation Compose",
            summary = "利用 NavHost 建立多頁面導覽，並保留狀態與返回堆疊。",
            highlights = listOf(
                "rememberNavController() 建立導覽控制器。",
                "NavHost 透過 route 區分頁面，使用 navController.navigate 切換。",
                "可結合 ViewModel 在不同頁面共用狀態。"
            ),
            codeHint = "val navController = rememberNavController()\nNavHost(navController, startDestination = \"home\") {\n    composable(\"home\") { HomeScreen(onNavigate = { navController.navigate(\"detail\") }) }\n}"
        ),
        ComposeFeature(
            id = "mvvm-architecture",
            title = "Compose 與 MVVM",
            summary = "利用 ViewModel 暴露 UiState，實踐單向資料流與可預測的 UI。",
            highlights = listOf(
                "ViewModel 管理狀態資料來源 (Repository) 與商業邏輯。",
                "UiState 以 StateFlow/LiveData 暴露，Compose 透過 collectAsState() 觀察。",
                "事件 (使用者意圖) 往 ViewModel 傳遞，維持單一事實來源 (SSOT)。"
            ),
            codeHint = "class FeatureViewModel : ViewModel() {\n    private val _uiState = MutableStateFlow(FeatureUiState())\n    val uiState: StateFlow<FeatureUiState> = _uiState\n}\n@Composable\nfun Screen(viewModel: FeatureViewModel = viewModel()) {\n    val state by viewModel.uiState.collectAsState()\n    // render UI\n}"
        )
    )
}
