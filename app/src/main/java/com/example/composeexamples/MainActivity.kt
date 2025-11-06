package com.example.composeexamples

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.composeexamples.ui.screens.FeatureDetailScreen
import com.example.composeexamples.ui.screens.IntroScreen
import com.example.composeexamples.ui.theme.ComposeExamplesTheme
import com.example.composeexamples.viewmodel.ComposeFeatureViewModel

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1️⃣ 傳統 onCreate() - 最先執行
        Log.d(TAG, "傳統 onCreate() 執行")

        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when(event) {
                    Lifecycle.Event.ON_CREATE -> {
                        Log.d(TAG, "Activity 已建立")
                    }
                    Lifecycle.Event.ON_START -> {
                        Log.d(TAG, "Activity 開始")
                        // 啟動資源
                        setupBackgroundService()
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        Log.d(TAG, "Activity 恢復")
                    }
                    Lifecycle.Event.ON_PAUSE -> {
                        Log.d(TAG, "Activity 暫停")
                    }
                    Lifecycle.Event.ON_STOP -> {
                        Log.d(TAG, "Activity 停止")
                        // 釋放資源
                        cleanupBackgroundService()
                    }
                    Lifecycle.Event.ON_DESTROY -> {
                        Log.d(TAG, "Activity 銷毀")
                    }
                    else -> {}
                }
            }
        })
        lifecycle.addObserver(LifecycleLoggingObserver(TAG))

        setContent {
            ComposeExamplesTheme {
                ComposeShowcaseApp()
            }
        }

        Log.d("Lifecycle", "onCreate() 即將結束")
    }

    private fun setupBackgroundService() {
        Log.d("Service", "背景服務啟動")
    }

    private fun cleanupBackgroundService() {
        Log.d("Service", "背景服務清理")
    }
}

// 通用的日誌觀察者
class LifecycleLoggingObserver(
    private val tag: String
) : DefaultLifecycleObserver {

    override fun onCreate(owner: LifecycleOwner) {
        Log.d(tag, "onCreate")
    }

    override fun onStart(owner: LifecycleOwner) {
        Log.d(tag, "onStart")
    }

    override fun onResume(owner: LifecycleOwner) {
        Log.d(tag, "onResume")
    }

    override fun onPause(owner: LifecycleOwner) {
        Log.d(tag, "onPause")
    }

    override fun onStop(owner: LifecycleOwner) {
        Log.d(tag, "onStop")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        Log.d(tag, "onDestroy")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeShowcaseApp(
    modifier: Modifier = Modifier,
    viewModel: ComposeFeatureViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                title = {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Default.LibraryBooks,
                        contentDescription = null
                    )
                },
                actions = {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Intro.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Destination.Intro.route) {
                IntroScreen(
                    features = uiState.features,
                    onFeatureSelected = { feature ->
                        viewModel.selectFeature(feature.id)
                        navController.navigate(Destination.Detail.route)
                    }
                )
            }

            composable(Destination.Detail.route) {
                val pageIndicator = buildString {
                    append(uiState.currentIndex + 1)
                    append(" / ")
                    append(uiState.features.size)
                }
                FeatureDetailScreen(
                    feature = uiState.currentFeature,
                    pageIndicator = pageIndicator,
                    canShowPrevious = uiState.canShowPrevious,
                    canShowNext = uiState.canShowNext,
                    onPrevious = { viewModel.showPrevious() },
                    onNext = { viewModel.showNext() },
                    onBackToList = {
                        navController.popBackStack(
                            route = Destination.Intro.route,
                            inclusive = false
                        )
                    }
                )
            }
        }
    }
}

private sealed class Destination(val route: String) {
    object Intro : Destination("intro")
    object Detail : Destination("detail")
}
