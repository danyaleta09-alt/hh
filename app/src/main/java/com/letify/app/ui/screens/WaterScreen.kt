package com.letify.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.letify.app.ui.components.NoFeedbackButton
import com.letify.app.ui.components.ScreenHeader
import com.letify.app.ui.components.WCard
import com.letify.app.ui.components.screenHPad
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.theme.Letify
import com.letify.app.ui.water.WaterFillCircle
import com.letify.app.ui.water.WaterRepository
import kotlin.math.roundToInt

private val WaterBlue = Color(0xFF3FA8F5)

/**
 * Redesigned Nutrition / Water screen.
 *
 * — Compact segmented tab bar (Еда / Вода) with smooth animated slide+fade transition
 * — Water tab: smooth fill circle (no ring border), slider amount picker,
 *   single "Добавить" button, История row at the bottom
 * — Water balance starts at 0 and resets every calendar day
 * — History is stored in WaterRepository (SharedPreferences)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    onOpenWaterHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repo = remember { WaterRepository.get(context) }
    val waterState by repo.state.collectAsState()

    // Refresh daily reset check whenever screen comes to the front
    LaunchedEffect(Unit) { repo.refresh() }

    var tab by remember { mutableStateOf("water") }

    Column(
        modifier
            .fillMaxSize()
            .background(Letify.colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Page title ────────────────────────────────────────────────────
        ScreenHeader(title = "Питание")

        // ── Compact tab switcher ──────────────────────────────────────────
        CompactSegmentedTabs(
            items = listOf(
                TabItem(key = "food",  title = "Питание", icon = "apple-bold-duotone"),
                TabItem(key = "water", title = "Вода",    icon = "waterdrop-bold-duotone"),
            ),
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier
                .screenHPad()
                .padding(bottom = 18.dp),
        )

        // ── Tab content with smooth slide+fade transition ─────────────────
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                val toRight = targetState == "water"
                (slideInHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    )
                ) { if (toRight) it else -it } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(220)) { if (toRight) -it else it } + fadeOut(tween(180)))
            },
            label = "tab-content",
            modifier = Modifier.fillMaxSize(),
        ) { currentTab ->
            when (currentTab) {
                "water" -> WaterTab(
                    currentMl = waterState.todayMl,
                    goalMl = waterState.goalMl,
                    onAdd = { ml -> repo.addWater(ml) },
                    onOpenHistory = onOpenWaterHistory,
                )
                else -> FoodTab()
            }
        }
    }
}

// ── Data ─────────────────────────────────────────────────────────────────────

private data class TabItem(val key: String, val title: String, val icon: String? = null)

// ── Compact segmented tab ─────────────────────────────────────────────────────

@Composable
private fun CompactSegmentedTabs(
    items: List<TabItem>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val itemWidthDp = with(density) { (containerSize.width / items.size).toDp() }
    val selectedIndex = items.indexOfFirst { it.key == selected }.coerceAtLeast(0)
    val pillOffsetX by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
        label = "seg-pill",
    )

    Box(
        modifier
            .fillMaxWidth()
            .background(Letify.colors.container, RoundedCornerShape(14.dp))
            .padding(4.dp)
            .onSizeChanged { containerSize = it }
    ) {
        // Sliding pill
        if (containerSize != IntSize.Zero) {
            Box(
                Modifier
                    .offset(x = itemWidthDp * pillOffsetX, y = 0.dp)
                    .width(itemWidthDp)
                    .height(36.dp)
                    .background(Letify.colors.accentSoft, RoundedCornerShape(10.dp))
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            items.forEach { item ->
                val active = item.key == selected
                NoFeedbackButton(
                    onClick = { onSelect(item.key) },
                    modifier = Modifier.weight(1f).height(36.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        if (item.icon != null) {
                            SolarIcon(
                                name = item.icon,
                                tint = if (active) Letify.colors.accent else Letify.colors.muted,
                                size = 16.dp,
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = item.title,
                            color = if (active) Letify.colors.accent else Letify.colors.muted,
                            style = Letify.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

// ── Water tab ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaterTab(
    currentMl: Int,
    goalMl: Int,
    onAdd: (Int) -> Unit,
    onOpenHistory: () -> Unit,
) {
    val progress = if (goalMl > 0) currentMl.toFloat() / goalMl else 0f

    // Slider: 0..1 mapped to 50..1000 ml, snapped to 25 ml steps
    var sliderValue by remember { mutableFloatStateOf(0.3f) }
    val minMl = 50f
    val maxMl = 1000f
    val rawMl = (minMl + sliderValue * (maxMl - minMl)).roundToInt()
    val selectedMl = rawMl - (rawMl % 25)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))

        // ── Smooth fill circle (no ring border) ───────────────────────────
        WaterFillCircle(
            progress = progress,
            currentMl = currentMl,
            goalMl = goalMl,
            size = 196.dp,
            fillColor = WaterBlue,
            trackColor = Letify.colors.container,
        )

        Spacer(Modifier.height(28.dp))

        // ── Slider card ───────────────────────────────────────────────────
        WCard(
            modifier = Modifier
                .screenHPad()
                .padding(bottom = 12.dp),
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Добавить воды",
                        color = Letify.colors.text,
                        style = Letify.typography.titleMedium,
                    )
                    Box(
                        modifier = Modifier
                            .background(WaterBlue.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "$selectedMl мл",
                            color = WaterBlue,
                            style = Letify.typography.titleSmall,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Slider with custom thumb
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = WaterBlue,
                        activeTrackColor = WaterBlue,
                        inactiveTrackColor = Letify.colors.track,
                    ),
                    thumb = {
                        Box(
                            Modifier
                                .size(28.dp)
                                .background(WaterBlue, RoundedCornerShape(999.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(999.dp))
                            )
                        }
                    },
                )

                Row(
                    Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${minMl.toInt()} мл", color = Letify.colors.muted, style = Letify.typography.bodySmall)
                    Text("${maxMl.toInt()} мл", color = Letify.colors.muted, style = Letify.typography.bodySmall)
                }

                Spacer(Modifier.height(16.dp))

                // Add button
                NoFeedbackButton(
                    onClick = { onAdd(selectedMl) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(WaterBlue, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            SolarIcon(name = "add-circle-bold-duotone", tint = Color.White, size = 20.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Добавить $selectedMl мл",
                                color = Color.White,
                                style = Letify.typography.titleMedium,
                            )
                        }
                    }
                }
            }
        }

        // ── Stats row ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .screenHPad()
                .fillMaxWidth()
                .background(Letify.colors.container, RoundedCornerShape(20.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WaterStatCell(
                icon = "cup-paper-bold-duotone",
                label = "Выпито",
                value = "$currentMl мл",
                modifier = Modifier.weight(1f),
            )
            Box(Modifier.width(1.dp).height(36.dp).background(Letify.colors.track))
            WaterStatCell(
                icon = "target-bold-duotone",
                label = "Цель",
                value = "$goalMl мл",
                modifier = Modifier.weight(1f),
            )
            Box(Modifier.width(1.dp).height(36.dp).background(Letify.colors.track))
            WaterStatCell(
                icon = "chart-2-bold-duotone",
                label = "Прогресс",
                value = "${(progress * 100).toInt()}%",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── История row ───────────────────────────────────────────────────
        NoFeedbackButton(
            onClick = onOpenHistory,
            modifier = Modifier
                .screenHPad()
                .fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Letify.colors.container, RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .background(WaterBlue.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        SolarIcon(name = "history-bold-duotone", tint = WaterBlue, size = 20.dp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "История",
                            color = Letify.colors.text,
                            style = Letify.typography.titleMedium,
                        )
                        Text(
                            "Статистика по дням",
                            color = Letify.colors.muted,
                            style = Letify.typography.bodySmall,
                        )
                    }
                }
                SolarIcon(name = "alt-arrow-right-outline", tint = Letify.colors.muted, size = 20.dp)
            }
        }
    }
}

@Composable
private fun WaterStatCell(
    icon: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        SolarIcon(name = icon, tint = WaterBlue, size = 20.dp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = Letify.colors.text, style = Letify.typography.titleSmall)
        Text(label, color = Letify.colors.muted, style = Letify.typography.bodySmall)
    }
}

// ── Food tab placeholder ──────────────────────────────────────────────────────

@Composable
private fun FoodTab() {
    Box(
        modifier = Modifier.fillMaxSize().padding(top = 60.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SolarIcon(name = "apple-bold-duotone", tint = Letify.colors.muted, size = 52.dp)
            Spacer(Modifier.height(14.dp))
            Text("Питание", color = Letify.colors.text, style = Letify.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Здесь будет трекинг еды",
                color = Letify.colors.muted.copy(alpha = 0.7f),
                style = Letify.typography.bodySmall,
            )
        }
    }
}
