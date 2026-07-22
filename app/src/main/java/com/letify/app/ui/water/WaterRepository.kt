package com.letify.app.ui.water

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WaterEntry(
    val amountMl: Int,
    val timestampMs: Long,
    val dateKey: String, // "yyyy-MM-dd"
)

data class WaterDayStat(
    val dateKey: String,     // "yyyy-MM-dd"
    val displayLabel: String, // "22 июл"
    val totalMl: Int,
    val goalMl: Int,
)

data class WaterState(
    val todayMl: Int = 0,
    val goalMl: Int = 2000,
    val entries: List<WaterEntry> = emptyList(),
    val todayDateKey: String = "",
)

/** Singleton water data repository backed by SharedPreferences. */
class WaterRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("water_data", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<WaterState> = _state.asStateFlow()

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val labelFmt = SimpleDateFormat("d MMM", Locale("ru"))

    private fun todayKey(): String = dateFmt.format(Date())

    fun addWater(ml: Int) {
        val today = todayKey()
        val entry = WaterEntry(
            amountMl = ml,
            timestampMs = System.currentTimeMillis(),
            dateKey = today,
        )
        val current = _state.value
        val newEntries = current.entries + entry
        // recalculate today total from entries (may have been reset)
        val todayTotal = newEntries.filter { it.dateKey == today }.sumOf { it.amountMl }
        val newState = current.copy(
            todayMl = todayTotal,
            entries = newEntries,
            todayDateKey = today,
        )
        _state.value = newState
        save(newState)
    }

    fun setGoal(ml: Int) {
        val new = _state.value.copy(goalMl = ml.coerceIn(500, 5000))
        _state.value = new
        save(new)
    }

    /** Returns per-day stats for the last [days] days (oldest first). */
    fun getHistory(days: Int = 14): List<WaterDayStat> {
        val entries = _state.value.entries
        val goalMl = _state.value.goalMl
        val result = mutableListOf<WaterDayStat>()
        val cal = java.util.Calendar.getInstance()
        for (i in days - 1 downTo 0) {
            cal.time = Date()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
            val key = dateFmt.format(cal.time)
            val total = entries.filter { it.dateKey == key }.sumOf { it.amountMl }
            result.add(
                WaterDayStat(
                    dateKey = key,
                    displayLabel = labelFmt.format(cal.time),
                    totalMl = total,
                    goalMl = goalMl,
                )
            )
        }
        return result
    }

    /** Reload state — checks if the day has changed and resets today's counter. */
    fun refresh() {
        val current = _state.value
        val today = todayKey()
        if (current.todayDateKey != today) {
            val todayTotal = current.entries.filter { it.dateKey == today }.sumOf { it.amountMl }
            val new = current.copy(todayMl = todayTotal, todayDateKey = today)
            _state.value = new
            save(new)
        }
    }

    // ── persistence ────────────────────────────────────────────────────────

    private fun save(state: WaterState) {
        val arr = JSONArray()
        state.entries.forEach { e ->
            arr.put(
                JSONObject().apply {
                    put("ml", e.amountMl)
                    put("ts", e.timestampMs)
                    put("day", e.dateKey)
                }
            )
        }
        prefs.edit()
            .putString("entries", arr.toString())
            .putInt("goal", state.goalMl)
            .apply()
    }

    private fun load(): WaterState {
        val goalMl = prefs.getInt("goal", 2000)
        val raw = prefs.getString("entries", null)
        val entries = mutableListOf<WaterEntry>()
        if (raw != null) {
            runCatching {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    entries.add(
                        WaterEntry(
                            amountMl = obj.getInt("ml"),
                            timestampMs = obj.getLong("ts"),
                            dateKey = obj.getString("day"),
                        )
                    )
                }
            }
        }
        val today = todayKey()
        val todayTotal = entries.filter { it.dateKey == today }.sumOf { it.amountMl }
        return WaterState(
            todayMl = todayTotal,
            goalMl = goalMl,
            entries = entries,
            todayDateKey = today,
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: WaterRepository? = null

        fun get(context: Context): WaterRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: WaterRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
