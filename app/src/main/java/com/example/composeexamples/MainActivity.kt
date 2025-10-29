package com.example.composeexamples

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.composeexamples.ui.theme.ComposeExamplesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeExamplesTheme {
                TravelIdeasApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelIdeasApp(modifier: Modifier = Modifier) {
    val sections = remember { travelRecommendationSections() }

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
                        imageVector = Icons.Default.Star,
                        contentDescription = null
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* TODO: hook up navigation */ },
                icon = { Icon(imageVector = Icons.Default.Explore, contentDescription = null) },
                text = { Text(text = stringResource(id = R.string.cta_discover)) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 88.dp)
        ) {
            item {
                HeroSection(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                        .fillMaxWidth()
                )
            }

            sections.forEach { section ->
                item(key = section.titleResId) {
                    SectionHeader(
                        title = stringResource(id = section.titleResId),
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                    )
                }

                items(section.recommendations, key = { it.title }) { recommendation ->
                    RecommendationCard(
                        recommendation = recommendation,
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }

                item {
                    Divider(
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.headline),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = stringResource(id = R.string.sub_headline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .padding(top = 12.dp, bottom = 4.dp),
        text = title,
        style = MaterialTheme.typography.titleMedium
    )
}

@Composable
private fun RecommendationCard(
    recommendation: TravelRecommendation,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = recommendation.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = recommendation.description,
                style = MaterialTheme.typography.bodyMedium
            )
            AssistChipRow(tags = recommendation.tags)
            Text(
                text = recommendation.budget,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            ActionRow()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssistChipRow(tags: List<String>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            AssistChip(
                onClick = { /* stub for demo */ },
                label = { Text(text = tag) },
                colors = AssistChipDefaults.assistChipColors(
                    leadingIconContentColor = MaterialTheme.colorScheme.primary,
                    labelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun ActionRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalButton(onClick = { /* share */ }) {
            Icon(imageVector = Icons.Default.Share, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(id = R.string.action_share))
        }
        OutlinedButton(onClick = { /* save */ }) {
            Text(text = stringResource(id = R.string.action_save))
        }
    }
}

data class TravelRecommendation(
    val title: String,
    val description: String,
    val tags: List<String>,
    val budget: String
)

data class TravelRecommendationSection(
    @StringRes val titleResId: Int,
    val recommendations: List<TravelRecommendation>
)

private fun travelRecommendationSections(): List<TravelRecommendationSection> {
    return listOf(
        TravelRecommendationSection(
            titleResId = R.string.section_seasonal,
            recommendations = listOf(
                TravelRecommendation(
                    title = "春櫻小旅行",
                    description = "安排 3 天 2 夜走訪郊區秘境，感受滿山遍野的櫻花與森林芬多精。",
                    tags = listOf("賞花", "秘境", "慢活"),
                    budget = "預算：NT$8,000 起"
                ),
                TravelRecommendation(
                    title = "夏日海島衝浪營",
                    description = "與專業教練在碧海藍天下練習衝浪，享受日落音樂派對與海邊 BBQ。",
                    tags = listOf("衝浪", "水上活動", "派對"),
                    budget = "預算：NT$12,500 起"
                )
            )
        ),
        TravelRecommendationSection(
            titleResId = R.string.section_local,
            recommendations = listOf(
                TravelRecommendation(
                    title = "古城文創散策",
                    description = "結合在地導覽與手作體驗，深入舊城巷弄體驗職人精神。",
                    tags = listOf("文化", "手作", "導覽"),
                    budget = "預算：NT$6,800 起"
                ),
                TravelRecommendation(
                    title = "山林部落共餐",
                    description = "走訪原民部落與族人一同採集、料理並分享山林饗宴。",
                    tags = listOf("族旅", "永續", "慢食"),
                    budget = "預算：NT$7,200 起"
                )
            )
        ),
        TravelRecommendationSection(
            titleResId = R.string.section_food,
            recommendations = listOf(
                TravelRecommendation(
                    title = "夜市隱藏版美食",
                    description = "由美食達人帶路，品嚐少見的夜市手工小吃與創意甜品。",
                    tags = listOf("宵夜", "在地", "甜點"),
                    budget = "預算：NT$2,400 起"
                ),
                TravelRecommendation(
                    title = "職人無菜單餐桌",
                    description = "拜訪田園與小農合作的無菜單料理，享受當季食材的原味。",
                    tags = listOf("小農", "無菜單", "品酒"),
                    budget = "預算：NT$3,600 起"
                )
            )
        )
    )
}

@Preview(showBackground = true)
@Composable
fun TravelIdeasPreview() {
    ComposeExamplesTheme {
        TravelIdeasApp()
    }
}
