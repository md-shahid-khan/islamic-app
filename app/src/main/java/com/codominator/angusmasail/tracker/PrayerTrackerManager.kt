package com.codominator.angusmasail.tracker

import android.content.Context
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

enum class PrayerType(
    val englishName: String,
    val arabicName: String,
    val rakats: Int,
    val fridayName: String = englishName,
    val fridayArabic: String = arabicName,
    val fridayRakats: Int = rakats
) {
    FAJR("Fajr", "الفجر", 2),
    DHUHR("Dhuhr", "الظهر", 4, fridayName = "Jumu'ah", fridayArabic = "الجمعة", fridayRakats = 2),
    ASR("Asr", "العصر", 4),
    MAGHRIB("Maghrib", "المغرب", 3),
    ISHA("Isha", "العشاء", 4);

    fun displayName(date: LocalDate): String {
        return if (this == DHUHR && date.dayOfWeek == DayOfWeek.FRIDAY) fridayName else englishName
    }

    fun displayArabic(date: LocalDate): String {
        return if (this == DHUHR && date.dayOfWeek == DayOfWeek.FRIDAY) fridayArabic else arabicName
    }

    fun displayRakats(date: LocalDate): Int {
        return if (this == DHUHR && date.dayOfWeek == DayOfWeek.FRIDAY) fridayRakats else rakats
    }
}

enum class PrayerStatus {
    NONE,        // Not marked yet / upcoming
    OFFERED,     // Prayed on time
    MISSED,      // Missed / pending Qaza
    QAZA_DONE    // Missed previously, but Qaza has now been prayed!
}

data class DayPrayerStats(
    val date: LocalDate,
    val statuses: Map<PrayerType, PrayerStatus>,
    val offeredCount: Int,
    val missedCount: Int,
    val qazaDoneCount: Int
)

data class DaySummary(
    val date: LocalDate,
    val dayLabel: String,
    val offeredCount: Int,
    val isFriday: Boolean,
    val isToday: Boolean
)

data class WeekPrayerStats(
    val days: List<DaySummary>,
    val totalOffered: Int,
    val totalPossible: Int,
    val totalMissed: Int,
    val completionPercentage: Int
)

data class MonthPrayerStats(
    val yearMonth: YearMonth,
    val weeksSummary: List<Pair<String, Int>>, // e.g. ("W1", 32), ("W2", 35)
    val totalOffered: Int,
    val totalPossible: Int,
    val totalMissed: Int,
    val completionPercentage: Int
)

data class YearPrayerStats(
    val year: Int,
    val monthlyAverages: List<Pair<String, Int>>, // e.g. ("Jan", 85%), ("Feb", 92%)
    val totalOffered: Int,
    val totalPossible: Int,
    val totalMissed: Int,
    val completionPercentage: Int
)

object PrayerTrackerManager {
    private const val PREFS_PREFIX = "angus_prayer_tracker_"
    private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
    private val prefsCache = java.util.concurrent.ConcurrentHashMap<String, android.content.SharedPreferences>()

    private fun getPrefs(context: Context, userId: String): android.content.SharedPreferences {
        val key = userId.ifBlank { "guest" }
        return prefsCache.computeIfAbsent(key) {
            context.applicationContext.getSharedPreferences("${PREFS_PREFIX}$key", Context.MODE_PRIVATE)
        }
    }

    /**
     * Get the logged status of a specific prayer on a specific date.
     */
    fun getStatus(context: Context, userId: String, date: LocalDate, prayer: PrayerType): PrayerStatus {
        val prefs = getPrefs(context, userId)
        val key = "${date.format(DATE_FORMATTER)}_${prayer.name}"
        val raw = prefs.getString(key, PrayerStatus.NONE.name) ?: PrayerStatus.NONE.name
        return try {
            PrayerStatus.valueOf(raw)
        } catch (e: Exception) {
            PrayerStatus.NONE
        }
    }

    /**
     * Set the status of a specific prayer on a specific date.
     * Automatically adjusts pending Qaza count if prayer transitions between OFFERED / MISSED / QAZA_DONE.
     */
    fun setStatus(context: Context, userId: String, date: LocalDate, prayer: PrayerType, newStatus: PrayerStatus) {
        val currentStatus = getStatus(context, userId, date, prayer)
        if (currentStatus == newStatus) return

        val prefs = getPrefs(context, userId)
        val key = "${date.format(DATE_FORMATTER)}_${prayer.name}"
        prefs.edit().putString(key, newStatus.name).apply()

        // Sync Qaza counters:
        // Transition to MISSED -> increment Qaza count for this prayer
        if (newStatus == PrayerStatus.MISSED && currentStatus != PrayerStatus.MISSED) {
            adjustQazaCount(context, userId, prayer, +1)
        }
        // Transition from MISSED to OFFERED -> decrement Qaza count
        if (currentStatus == PrayerStatus.MISSED && newStatus == PrayerStatus.OFFERED) {
            adjustQazaCount(context, userId, prayer, -1)
        }
        // Transition from MISSED to QAZA_DONE -> decrement Qaza count
        if (currentStatus == PrayerStatus.MISSED && newStatus == PrayerStatus.QAZA_DONE) {
            adjustQazaCount(context, userId, prayer, -1)
        }
    }

    fun getDayStats(context: Context, userId: String, date: LocalDate): DayPrayerStats {
        val statuses = PrayerType.values().associateWith { getStatus(context, userId, date, it) }
        val offered = statuses.values.count { it == PrayerStatus.OFFERED }
        val missed = statuses.values.count { it == PrayerStatus.MISSED }
        val qazaDone = statuses.values.count { it == PrayerStatus.QAZA_DONE }
        return DayPrayerStats(date, statuses, offered, missed, qazaDone)
    }

    /**
     * Week stats: last 7 days ending at anchorDate (inclusive).
     */
    fun getWeekStats(context: Context, userId: String, anchorDate: LocalDate = LocalDate.now()): WeekPrayerStats {
        val days = mutableListOf<DaySummary>()
        var totalOffered = 0
        var totalMissed = 0
        val today = LocalDate.now()

        // 7 days from (anchorDate - 6) to anchorDate
        for (i in 6 downTo 0) {
            val d = anchorDate.minusDays(i.toLong())
            val dayStats = getDayStats(context, userId, d)
            val offered = dayStats.offeredCount + dayStats.qazaDoneCount
            totalOffered += offered
            totalMissed += dayStats.missedCount

            val label = when (d.dayOfWeek) {
                DayOfWeek.MONDAY -> "Mon"
                DayOfWeek.TUESDAY -> "Tue"
                DayOfWeek.WEDNESDAY -> "Wed"
                DayOfWeek.THURSDAY -> "Thu"
                DayOfWeek.FRIDAY -> "Fri"
                DayOfWeek.SATURDAY -> "Sat"
                DayOfWeek.SUNDAY -> "Sun"
            }

            days.add(
                DaySummary(
                    date = d,
                    dayLabel = label,
                    offeredCount = offered,
                    isFriday = d.dayOfWeek == DayOfWeek.FRIDAY,
                    isToday = d == today
                )
            )
        }

        val totalPossible = 7 * 5
        val pct = if (totalPossible > 0) ((totalOffered * 100f) / totalPossible).toInt().coerceIn(0, 100) else 0

        return WeekPrayerStats(
            days = days,
            totalOffered = totalOffered,
            totalPossible = totalPossible,
            totalMissed = totalMissed,
            completionPercentage = pct
        )
    }

    /**
     * Month stats: for the given YearMonth, broken into 4 weeks and overall summary.
     */
    fun getMonthStats(context: Context, userId: String, yearMonth: YearMonth = YearMonth.now()): MonthPrayerStats {
        val daysInMonth = yearMonth.lengthOfMonth()
        val today = LocalDate.now()
        val limitDay = if (yearMonth == YearMonth.now()) today.dayOfMonth else daysInMonth

        var totalOffered = 0
        var totalMissed = 0
        val weekBuckets = mutableListOf(0, 0, 0, 0) // W1 (1-7), W2 (8-14), W3 (15-21), W4 (22-end)

        for (day in 1..limitDay) {
            val date = yearMonth.atDay(day)
            val stats = getDayStats(context, userId, date)
            val offered = stats.offeredCount + stats.qazaDoneCount
            totalOffered += offered
            totalMissed += stats.missedCount

            val wIdx = when {
                day <= 7 -> 0
                day <= 14 -> 1
                day <= 21 -> 2
                else -> 3
            }
            weekBuckets[wIdx] += offered
        }

        val totalPossible = limitDay * 5
        val pct = if (totalPossible > 0) ((totalOffered * 100f) / totalPossible).toInt().coerceIn(0, 100) else 0

        val weeksSummary = listOf(
            "W1" to weekBuckets[0],
            "W2" to weekBuckets[1],
            "W3" to weekBuckets[2],
            "W4" to weekBuckets[3]
        )

        return MonthPrayerStats(
            yearMonth = yearMonth,
            weeksSummary = weeksSummary,
            totalOffered = totalOffered,
            totalPossible = totalPossible,
            totalMissed = totalMissed,
            completionPercentage = pct
        )
    }

    /**
     * Year stats: 12 months overview with completion percentages.
     * High-performance single-pass in-memory aggregation.
     */
    fun getYearStats(context: Context, userId: String, year: Int = LocalDate.now().year): YearPrayerStats {
        val prefs = getPrefs(context, userId)
        val all = prefs.all
        val currentMonth = if (year == LocalDate.now().year) LocalDate.now().monthValue else 12
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val monthlyAverages = mutableListOf<Pair<String, Int>>()

        var grandTotalOffered = 0
        var grandTotalPossible = 0
        var grandTotalMissed = 0

        for (m in 1..12) {
            if (m <= currentMonth) {
                val ym = YearMonth.of(year, m)
                val daysInMonth = ym.lengthOfMonth()
                val limitDay = if (year == LocalDate.now().year && m == currentMonth) LocalDate.now().dayOfMonth else daysInMonth

                var monthOffered = 0
                var monthMissed = 0

                val monthPrefix = String.format(java.util.Locale.ENGLISH, "%04d-%02d-", year, m)
                for (entry in all) {
                    val k = entry.key
                    if (k.startsWith(monthPrefix)) {
                        val dayPart = k.substring(monthPrefix.length).takeWhile { it.isDigit() }.toIntOrNull()
                        if (dayPart != null && dayPart in 1..limitDay) {
                            val v = entry.value as? String
                            if (v == PrayerStatus.OFFERED.name || v == PrayerStatus.QAZA_DONE.name) {
                                monthOffered++
                            } else if (v == PrayerStatus.MISSED.name) {
                                monthMissed++
                            }
                        }
                    }
                }

                val totalPossible = limitDay * 5
                val pct = if (totalPossible > 0) ((monthOffered * 100f) / totalPossible).toInt().coerceIn(0, 100) else 0

                monthlyAverages.add(monthNames[m - 1] to pct)
                grandTotalOffered += monthOffered
                grandTotalPossible += totalPossible
                grandTotalMissed += monthMissed
            } else {
                monthlyAverages.add(monthNames[m - 1] to 0)
            }
        }

        val pct = if (grandTotalPossible > 0) ((grandTotalOffered * 100f) / grandTotalPossible).toInt().coerceIn(0, 100) else 0

        return YearPrayerStats(
            year = year,
            monthlyAverages = monthlyAverages,
            totalOffered = grandTotalOffered,
            totalPossible = grandTotalPossible,
            totalMissed = grandTotalMissed,
            completionPercentage = pct
        )
    }

    // ==========================================
    // QAHA (MISSED PRAYER) LIFETIME COUNTER
    // ==========================================

    private const val QAHA_KEY_PREFIX = "qaza_pending_"

    fun getPendingQazaCount(context: Context, userId: String, prayer: PrayerType): Int {
        val prefs = getPrefs(context, userId)
        return prefs.getInt("${QAHA_KEY_PREFIX}${prayer.name}", 0).coerceAtLeast(0)
    }

    fun getTotalPendingQaza(context: Context, userId: String): Int {
        return PrayerType.values().sumOf { getPendingQazaCount(context, userId, it) }
    }

    fun setPendingQazaCount(context: Context, userId: String, prayer: PrayerType, count: Int) {
        val prefs = getPrefs(context, userId)
        prefs.edit().putInt("${QAHA_KEY_PREFIX}${prayer.name}", count.coerceAtLeast(0)).apply()
    }

    fun adjustQazaCount(context: Context, userId: String, prayer: PrayerType, delta: Int) {
        val current = getPendingQazaCount(context, userId, prayer)
        val updated = (current + delta).coerceAtLeast(0)
        setPendingQazaCount(context, userId, prayer, updated)
    }

    /**
     * Records a completed Qaza prayer (decrements pending Qaza by 1 and marks it).
     */
    fun recordQazaPrayed(context: Context, userId: String, prayer: PrayerType): Int {
        adjustQazaCount(context, userId, prayer, -1)
        return getPendingQazaCount(context, userId, prayer)
    }

    /**
     * Bulk add missed prayers by Years, Months, and Days (Qaza-e-Umri Calculator).
     * e.g. 1 Year = 365 days * 5 Farz = 1,825 prayers.
     */
    fun addBulkQaza(context: Context, userId: String, years: Int, months: Int, days: Int) {
        val totalDays = (years * 365) + (months * 30) + days
        if (totalDays <= 0) return
        PrayerType.values().forEach { prayer ->
            adjustQazaCount(context, userId, prayer, totalDays)
        }
    }

    /**
     * Record 1 full day of Qaza completed (clears 1 Fajr, 1 Dhuhr, 1 Asr, 1 Maghrib, 1 Isha).
     */
    fun recordFullDayQazaPrayed(context: Context, userId: String) {
        PrayerType.values().forEach { prayer ->
            adjustQazaCount(context, userId, prayer, -1)
        }
    }

    /**
     * Reset or set all Qaza counters to 0.
     */
    fun resetAllQaza(context: Context, userId: String) {
        PrayerType.values().forEach { prayer ->
            setPendingQazaCount(context, userId, prayer, 0)
        }
    }
}
