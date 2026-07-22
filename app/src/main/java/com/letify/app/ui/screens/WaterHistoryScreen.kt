package com.letify.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.letify.app.ui.components.IconButtonRound
import com.letify.app.ui.components.ScreenScaffold
import com.letify.app.ui.components.WCard
import com.letify.app.ui.components.screenHPad
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.theme.Letify
import com.letify.app.ui.water.WaterDayStat
import com.letify.app.ui.water.WaterRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val WaterBlue = Color(0xFF3FA8F5)

/**
 * История воды — отдельный экран.
 *
 * Layout:
 *  1. Заголовок + кнопка назад
 *  2. Сводная карточка (текущий день)
 *  3. Бар-чарт по последним 14 дням
 *  4. Список записей за сегодня (по времени)
 */
@Composable
fun WaterHistoryScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { WaterRepository.get(context) }
    val waterState by repo.state.collectAsState()
    val history = remember(waterState) { repo.getHistory(14) }

    val todayTotal = waterState.todayMl
    val goalMl = waterState.goalMl
    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayEntries = waterState.entries
        .filter { it.dateKey == todayKey }
        .sortedByDescending { it.timestampMs }

    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // Общая выпито за все дни
    val allTimeMl = waterState.entries.sumOf { it.amountMl }
    val daysWithData = waterState.entries.map { it.dateKey }.distinct().size

    Box(
        Modifier
            .fillMaxSize()
            .background(Letify.colors.bg)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .navigationBarsPadding()
        ) {
            // ── Header ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .screenHPad()
                    .padding(top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButtonRound(
                    icon = "alt-arrow-left-outline",
                    onClick = onBack,
                    size = 40.dp,
                    iconSize = 26.dp,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "История воды",
                    color = Letify.colors.text,
                    style = Letify.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 40.dp)
            ) {
                Spacer(Modifier.height(12.dp))

                // ── Today summary card ────────────────────────────────────
                WCard(
                    modifier = Modifier
                        .screenHPad()
                        .padding(bottom = 14.dp),
                ) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Сегодня",
                                color = Letify.colors.muted,
                                style = Letify.typography.titleSmall,
                            )
                            Box(
                                Modifier
                                    .background(WaterBlue.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "${(todayTotal.toFloat() / goalMl * 100).toInt()}%",
                                    color = WaterBlue,
                                    style = Letify.typography.labelMedium,
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            SummaryCell(
                                icon = "waterdrop-bold-duotone",
                                label = "Выпито",
                                value = "$todayTotal мл",
                                modifier = Modifier.weight(1f),
                            )
                            SummaryCell(
                                icon = "target-bold-duotone",
                                label = "Цель",
                                value = "$goalMl мл",
                                modifier = Modifier.weight(1f),
                            )
                            SummaryCell(
                                icon = "graph-up-bold-duotone",
                                label = "Всего",
                                value = "${allTimeMl / 1000}.${(allTimeMl % 1000) / 100} л",
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // ── Bar chart: 14 days ─────────────────────────────────────
                WCard(
                    modifier = Modifier
                        .screenHPad()
                        .padding(bottom = 14.dp),
                ) {
                    Column {
                        Text(
                            "14 дней",
                            color = Letify.colors.muted,
                            style = Letify.typography.titleSmall,
                        )
                        Spacer(Modifier.height(16.dp))
                        WaterBarChart(
                            days = history,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        // X-axis labels — show every 7th
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            history.forEachIndexed { idx, day ->
                                if (idx == 0 || idx == 6 || idx == 13) {
                                    Text(
                                        day.displayLabel,
                                        color = Letify.colors.muted,
                                        style = Letify.typography.bodySmall,
                                    )
                                } else {
                                    Spacer(Modifier.width(1.dp))
                                }
                            }
                        }
                    }
                }

                // ── Today log ─────────────────────────────────────────────
                if (todayEntries.isNotEmpty()) {
                    Text(
                        "ЗАПИСИ СЕГОДНЯ",
                        color = Letify.colors.muted,
                        style = Letify.typography.titleSmall,
                        modifier = Modifier
                            .screenHPad()
                            .padding(start = 4.dp, bottom = 10.dp),
                    )
                    WCard(
                        modifier = Modifier
                            .screenHPad()
                            .padding(bottom = 14.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) {
                        Column {
                            todayEntries.forEachIndexed { idx, entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        Modifier
                                            .size(36.dp)
                                            .background(WaterBlue.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        SolarIcon(name = "cup-paper-bold-duotone", tint = WaterBlue, size = 18.dp)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "${entry.amountMl} мл",
                                            color = Letify.colors.text,
                                            style = Letify.typography.titleMedium,
                                        )
                                        Text(
                                            timeFmt.format(java.util.Date(entry.timestampMs)),
                                            color = Letify.colors.muted,
                                            style = Letify.typography.bodySmall,
                                        )
                                    }
                                }
                                if (idx < todayEntries.lastIndex) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .padding(horizontal = 18.dp)
                                            .background(Letify.colors.track)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Empty state
                    Box(
                        Modifier
                            .screenHPad()
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SolarIcon(name = "waterdrop-bold-duotone", tint = Letify.colors.muted, size = 40.dp)
                            Spacer(Modifier.height(10.dp))
                            Text("Сегодня записей нет", color = Letify.colors.muted, style = Letify.typography.bodyMedium)
                        }
                    }
                }

                // ── Stats summary ─────────────────────────────────────────
                if (daysWithData > 0) {
                    val avgMl = if (daysWithData > 0) allTimeMl / daysWithData else 0
                    Spacer(Modifier.height(4.dp))
                    WCard(
                        modifier = Modifier
                            .screenHPad()
                            .padding(top = 4.dp),
                    ) {
                        Column {
                            Text(
                                "Статистика",
                                color = Letify.colors.muted,
                                style = Letify.typography.titleSmall,
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                SummaryCell(
                                    icon = "calendar-bold-duotone",
                                    label = "Дней",
                                    value = "$daysWithData",
                                    modifier = Modifier.weight(1f),
                                )
                                SummaryCell(
                                    icon = "chart-2-bold-duotone",
                                    label = "Среднее",
                                    value = "$avgMl мл",
                                    modifier = Modifier.weight(1f),
                                )
                                SummaryCell(
                                    icon = "star-bold-duotone",
                                    label = "Лучший",
                                    value = "${history.maxOfOrNull { it.totalMl } ?: 0} мл",
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCell(
    icon: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(32.dp)
                .background(WaterBlue.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SolarIcon(name = icon, tint = WaterBlue, size = 18.dp)
        }
        Spacer(Modifier.height(6.dp))
        Text(value, color = Letify.colors.text, style = Letify.typography.titleSmall)
        Text(label, color = Letify.colors.muted, style = Letify.typography.bodySmall)
    }
}

// ── Bar chart ─────────────────────────────────────────────────────────────────

@Composable
private fun WaterBarChart(
    days: List<WaterDayStat>,
    modifier: Modifier = Modifier,
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(days) {
        anim.snapTo(0f)
        anim.animateTo(1f, tween(700, easing = EaseOutCubic))
    }
    val animVal = anim.value
    val maxMl = days.maxOfOrNull { it.totalMl }?.coerceAtLeast(1) ?: 1
    val goalMl = days.firstOrNull()?.goalMl ?: 2000
    val trackColor = Letify.colors.track
    val goalLineColor = Letify.colors.muted.copy(alpha = 0.3f)
    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    Canvas(modifier) {
        val barCount = days.size
        val gap = 4.dp.toPx()
        val barW = (size.width - gap * (barCount - 1)) / barCount
        val maxH = size.height

        // Goal line
        val goalY = maxH - (goalMl.toFloat() / maxMl.coerceAtLeast(goalMl)) * maxH
        drawLine(
            color = goalLineColor,
            start = Offset(0f, goalY),
            end = Offset(size.width, goalY),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round,
        )

        days.forEachIndexed { idx, day ->
            val x = idx * (barW + gap)
            val rawFrac = day.totalMl.toFloat() / maxMl.coerceAtLeast(1)
            val frac = (rawFrac * animVal).coerceIn(0f, 1f)
            val barH = (frac * maxH).coerceAtLeast(if (day.totalMl > 0) 6.dp.toPx() else 0f)
            val isToday = day.dateKey == todayKey
            val atGoal = day.totalMl >= goalMl

            // Track bar
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(x, 0f),
                size = Size(barW, maxH),
                cornerRadius = CornerRadius(6.dp.toPx()),
            )
            // Fill bar
            if (barH > 0f) {
                val barColor = when {
                    atGoal -> Color(0xFF7CD992)
                    isToday -> WaterBlue
                    else -> WaterBlue.copy(alpha = 0.6f)
                }
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, maxH - barH),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(6.dp.toPx()),
                )
            }
        }
    }
}
