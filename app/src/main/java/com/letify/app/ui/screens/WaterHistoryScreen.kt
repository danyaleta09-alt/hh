package com.letify.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.letify.app.ui.components.ScreenScaffold
import com.letify.app.ui.components.SectionTitle
import com.letify.app.ui.components.SettingsHeader
import com.letify.app.ui.components.WCard
import com.letify.app.ui.components.screenHPad
import com.letify.app.ui.icons.SolarIcon
import com.letify.app.ui.state.Dates
import com.letify.app.ui.state.LocalAppState
import com.letify.app.ui.state.WaterEntry
import com.letify.app.ui.theme.Letify
import com.letify.app.ui.theme.LetifyColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WaterHistoryScreen(onBack: () -> Unit = {}) {
    val state = LocalAppState.current

    // ── Compute last-7-days data for the bar chart ────────────────────────
    val today = LocalDate.now()
    val last7 = remember(state.waterHistory.size) {
        (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val dateKey = date.toString()
            val dayLabel = when (daysAgo) {
                0 -> "Сег"
                else -> {
                    val names = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
                    names[(date.dayOfWeek.value - 1)]
                }
            }
            Triple(dateKey, dayLabel, state.waterHistory.filter { it.dateKey == dateKey }.sumOf { it.ml })
        }
    }

    // ── Group history by date, newest first ───────────────────────────────
    val grouped = remember(state.waterHistory.size) {
        state.waterHistory
            .groupBy { it.dateKey }
            .entries
            .sortedByDescending { it.key }
    }

    val target = state.waterTarget

    ScreenScaffold(
        pinnedHeader = {
            SettingsHeader(title = "История воды", onBack = onBack)
        }
    ) {
        // ── Summary row ───────────────────────────────────────────────────
        SectionTitle("Последние 7 дней")
        WCard(modifier = Modifier.screenHPad(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)) {
            Column {
                WaterBarChart(
                    dailyTotals = last7.map { (_, label, ml) -> label to ml },
                    targetMl = target,
                )
                Box(Modifier.height(12.dp))
                // Total for the week
                val weekTotal = last7.sumOf { it.third }
                val daysWithData = last7.count { it.third > 0 }
                val avgPerDay = if (daysWithData > 0) weekTotal / daysWithData else 0
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    WaterStat("За неделю", "${weekTotal} мл")
                    WaterStatDivider()
                    WaterStat("Среднее/день", if (daysWithData > 0) "${avgPerDay} мл" else "—")
                    WaterStatDivider()
                    WaterStat("Цель", "${target} мл")
                }
            }
        }

        // ── Day-by-day history ────────────────────────────────────────────
        if (grouped.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SolarIcon(
                        name = "bottle-bold-duotone",
                        tint = Letify.colors.muted.copy(alpha = 0.4f),
                        size = 48.dp,
                    )
                    Box(Modifier.height(12.dp))
                    Text(
                        "Здесь появится история",
                        color = Letify.colors.muted,
                        style = Letify.typography.bodyMedium,
                    )
                    Box(Modifier.height(4.dp))
                    Text(
                        "Начни добавлять воду на экране питания",
                        color = Letify.colors.muted.copy(alpha = 0.6f),
                        style = Letify.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            grouped.forEach { (dateKey, entries) ->
                val dayTotal = entries.sumOf { it.ml }
                val sectionLabel = formatDateLabel(dateKey, today)
                val goalFraction = (dayTotal.toFloat() / target).coerceIn(0f, 1f)

                SectionTitle(
                    text = sectionLabel,
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Mini goal ring pill
                            val pct = (goalFraction * 100).toInt()
                            Text(
                                "$dayTotal мл",
                                color = LetifyColors.Water,
                                style = Letify.typography.titleSmall,
                            )
                            Box(Modifier.width(6.dp))
                            Box(
                                Modifier
                                    .background(LetifyColors.Water.copy(alpha = 0.14f), RoundedCornerShape(99.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "$pct%",
                                    color = LetifyColors.Water,
                                    style = Letify.typography.bodySmall,
                                )
                            }
                        }
                    }
                )
                WCard(
                    modifier = Modifier.screenHPad(),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                ) {
                    Column {
                        entries.forEachIndexed { idx, e ->
                            WaterHistoryRow(e)
                        }
                    }
                }
            }
        }
    }
}

// ── Bar chart ─────────────────────────────────────────────────────────────

@Composable
private fun WaterBarChart(dailyTotals: List<Pair<String, Int>>, targetMl: Int) {
    val maxVal = (dailyTotals.maxOfOrNull { it.second } ?: 0)
        .coerceAtLeast(targetMl)
        .coerceAtLeast(500)
    val chartH = 110.dp

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().height(chartH),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            dailyTotals.forEachIndexed { idx, (label, ml) ->
                val fraction = (ml.toFloat() / maxVal).coerceIn(0f, 1f)
                val isToday = idx == dailyTotals.lastIndex
                Box(
                    Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    // Background track
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(LetifyColors.Water.copy(alpha = 0.10f))
                    )
                    // Filled bar
                    if (fraction > 0f) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction.coerceAtLeast(0.03f))
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isToday) LetifyColors.Water
                                    else LetifyColors.Water.copy(alpha = 0.55f)
                                )
                        )
                    }
                    // Target line indicator (dashed feel via thin box)
                    val targetFraction = (targetMl.toFloat() / maxVal).coerceIn(0f, 1f)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.5.dp)
                            .align(Alignment.BottomCenter)
                            .let {
                                // position at target height — done via inner padding trick
                                // since graphicsLayer offset can cause clipping issues
                                it
                            }
                    )
                }
            }
        }
        Box(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            dailyTotals.forEachIndexed { idx, (label, _) ->
                val isToday = idx == dailyTotals.lastIndex
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = if (isToday) LetifyColors.Water else Letify.colors.muted,
                    style = if (isToday) Letify.typography.titleSmall else Letify.typography.bodySmall,
                )
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────

@Composable
private fun WaterHistoryRow(e: WaterEntry) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .background(LetifyColors.Water.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SolarIcon(name = e.icon, tint = LetifyColors.Water, size = 20.dp)
        }
        Box(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(e.label, color = Letify.colors.text, style = Letify.typography.titleSmall)
            Text("${e.time} · ${e.ml} мл", color = Letify.colors.muted, style = Letify.typography.bodySmall)
        }
        Text("+${e.ml}", color = Letify.colors.text, style = Letify.typography.titleSmall)
    }
}

@Composable
private fun WaterStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Letify.colors.text, style = Letify.typography.titleMedium)
        Box(Modifier.height(2.dp))
        Text(label, color = Letify.colors.muted, style = Letify.typography.bodySmall)
    }
}

@Composable
private fun WaterStatDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(32.dp)
            .background(Letify.colors.muted.copy(alpha = 0.15f))
    )
}

private fun formatDateLabel(dateKey: String, today: LocalDate): String {
    return try {
        val date = LocalDate.parse(dateKey)
        when {
            date == today -> "Сегодня"
            date == today.minusDays(1) -> "Вчера"
            else -> {
                val formatter = DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))
                date.format(formatter)
            }
        }
    } catch (e: Exception) {
        dateKey
    }
}
