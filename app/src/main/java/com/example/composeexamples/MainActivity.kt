package com.example.composeexamples

import android.os.Bundle
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.composeexamples.ui.screens.FeatureDetailScreen
import com.example.composeexamples.ui.screens.IntroScreen
import com.example.composeexamples.ui.theme.ComposeExamplesTheme
import com.example.composeexamples.viewmodel.ComposeFeatureViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeExamplesTheme {
                ComposeShowcaseApp()
            }
        }
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
