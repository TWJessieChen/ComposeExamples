package com.example.composeexamples.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.composeexamples.data.ComposeFeature
import com.example.composeexamples.data.ComposeFeatureRepository
import com.example.composeexamples.ui.theme.ComposeExamplesTheme

@Composable
fun FeatureDetailScreen(
    feature: ComposeFeature?,
    pageIndicator: String,
    canShowPrevious: Boolean,
    canShowNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onBackToList: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (feature == null) {
        EmptyFeatureState(onBackToList = onBackToList, modifier = modifier)
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = pageIndicator,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = feature.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = feature.summary,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "重點筆記",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                feature.highlights.forEach { highlight ->
                    Text(
                        text = "• $highlight",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Divider(modifier = Modifier.padding(top = 4.dp))
                Text(
                    text = "程式碼提示",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = feature.codeHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBackToList) {
                Text(text = "返回功能清單")
            }
            Button(
                enabled = canShowPrevious,
                onClick = onPrevious,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "上一頁")
            }
            Button(
                enabled = canShowNext,
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "下一頁")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FeatureDetailScreenPreview() {
    val feature = ComposeFeatureRepository().loadFeatures().first()
    ComposeExamplesTheme {
        FeatureDetailScreen(
            feature = feature,
            pageIndicator = "1 / 5",
            canShowPrevious = false,
            canShowNext = true,
            onPrevious = {},
            onNext = {},
            onBackToList = {}
        )
    }
}

@Composable
private fun EmptyFeatureState(onBackToList: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "找不到功能頁面",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "請返回清單重新選擇。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onBackToList) {
            Text(text = "返回")
        }
    }
}
