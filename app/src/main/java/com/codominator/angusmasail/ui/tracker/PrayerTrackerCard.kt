package com.codominator.angusmasail.ui.tracker

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.codominator.angusmasail.tracker.*
import com.codominator.angusmasail.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

enum class TrackerPeriodTab(val label: String) {
    TODAY("Today"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
    QAZA("Qaza Log")
}

@Composable
fun PrayerTrackerCard(
    userId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(TrackerPeriodTab.TODAY) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    // Date navigation states
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }
    var selectedWeekAnchor by remember { mutableStateOf(today) }
    var selectedYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedYear by remember { mutableIntStateOf(today.year) }

    // Efficient lazy calculation: only compute stats for the currently visible tab
    val dayStats = remember(userId, selectedDate, refreshTrigger, selectedTab) {
        if (selectedTab == TrackerPeriodTab.TODAY) {
            PrayerTrackerManager.getDayStats(context, userId, selectedDate)
        } else {
            DayPrayerStats(selectedDate, emptyMap(), 0, 0, 0)
        }
    }

    val weekStats = remember(userId, selectedWeekAnchor, refreshTrigger, selectedTab) {
        if (selectedTab == TrackerPeriodTab.WEEK) {
            PrayerTrackerManager.getWeekStats(context, userId, selectedWeekAnchor)
        } else {
            WeekPrayerStats(emptyList(), 0, 0, 0, 0)
        }
    }

    val monthStats = remember(userId, selectedYearMonth, refreshTrigger, selectedTab) {
        if (selectedTab == TrackerPeriodTab.MONTH) {
            PrayerTrackerManager.getMonthStats(context, userId, selectedYearMonth)
        } else {
            MonthPrayerStats(selectedYearMonth, emptyList(), 0, 0, 0, 0)
        }
    }

    val yearStats = remember(userId, selectedYear, refreshTrigger, selectedTab) {
        if (selectedTab == TrackerPeriodTab.YEAR) {
            PrayerTrackerManager.getYearStats(context, userId, selectedYear)
        } else {
            YearPrayerStats(selectedYear, emptyList(), 0, 0, 0, 0)
        }
    }

    val totalPendingQaza = remember(userId, refreshTrigger, selectedTab) {
        if (selectedTab == TrackerPeriodTab.QAZA) {
            PrayerTrackerManager.getTotalPendingQaza(context, userId)
        } else {
            0
        }
    }

    val isFridayToday = selectedDate.dayOfWeek == DayOfWeek.FRIDAY

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PureCream),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.2.dp, IslamicGold.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // 1. HEADER ROW: Title, Icon, Daily Progress Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(listOf(IslamicGold.copy(alpha = 0.35f), BgGoldTint))
                            )
                            .border(1.2.dp, IslamicGold.copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TaskAlt,
                            contentDescription = "Salah Tracker",
                            tint = DeepEmerald,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Salah & Qaza Tracker",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isFridayToday) "Jumu'ah Mubarak • Daily Progress & Analytics" else "5 Daily Prayers • Missed Salah Progress",
                            fontSize = 11.sp,
                            color = if (isFridayToday) DeepEmerald else TextSecondary,
                            fontWeight = if (isFridayToday) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Completion Badge
                Surface(
                    color = if (dayStats.offeredCount >= 5) LiveEmerald else BgMintTint,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = if (dayStats.offeredCount >= 5) "5/5 Prayed ✨" else "${dayStats.offeredCount}/5 Today",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (dayStats.offeredCount >= 5) Color.White else DeepEmerald,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. PERIOD TAB PILLS: Today | Week | Month | Year | Qaza Log
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TrackerPeriodTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) DeepEmerald else Color(0xFFF3ECE0),
                        label = "tab_bg"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else TextPrimary,
                        label = "tab_text"
                    )

                    Surface(
                        onClick = { selectedTab = tab },
                        shape = RoundedCornerShape(12.dp),
                        color = bgColor,
                        border = if (isSelected) null else BorderStroke(0.8.dp, Color(0xFFE2D6C5)),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. TAB CONTENT VIEWS (With smooth transition animation)
            AnimatedContent(
                targetState = selectedTab,
                label = "tracker_tab_content"
            ) { targetTab ->
                when (targetTab) {
                    TrackerPeriodTab.TODAY -> {
                        TodayPrayersView(
                            userId = userId,
                            selectedDate = selectedDate,
                            today = today,
                            dayStats = dayStats,
                            onDateChange = { selectedDate = it },
                            onStatusChanged = { refreshTrigger++ }
                        )
                    }
                    TrackerPeriodTab.WEEK -> {
                        WeekAnalyticsGraphView(
                            weekStats = weekStats,
                            anchorDate = selectedWeekAnchor,
                            onWeekChange = { selectedWeekAnchor = it }
                        )
                    }
                    TrackerPeriodTab.MONTH -> {
                        MonthAnalyticsGraphView(
                            monthStats = monthStats,
                            selectedYearMonth = selectedYearMonth,
                            onMonthChange = { selectedYearMonth = it }
                        )
                    }
                    TrackerPeriodTab.YEAR -> {
                        YearAnalyticsGraphView(
                            yearStats = yearStats,
                            selectedYear = selectedYear,
                            totalPendingQaza = totalPendingQaza,
                            onYearChange = { selectedYear = it }
                        )
                    }
                    TrackerPeriodTab.QAZA -> {
                        QazaManagementView(
                            userId = userId,
                            totalPendingQaza = totalPendingQaza,
                            onQazaUpdated = { refreshTrigger++ }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tab 1: Today's 5 Prayers with Date Switcher, Jumu'ah handling, and clear Pray / Missed buttons.
 */
@Composable
private fun TodayPrayersView(
    userId: String,
    selectedDate: LocalDate,
    today: LocalDate,
    dayStats: DayPrayerStats,
    onDateChange: (LocalDate) -> Unit,
    onStatusChanged: () -> Unit
) {
    val context = LocalContext.current
    val isFriday = selectedDate.dayOfWeek == DayOfWeek.FRIDAY

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Date Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFFAF7F2))
                .border(1.dp, Color(0xFFE5DACB), RoundedCornerShape(14.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onDateChange(selectedDate.minusDays(1)) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Previous Day",
                    tint = DeepEmerald,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = when (selectedDate) {
                        today -> "Today (${selectedDate.dayOfMonth} ${selectedDate.month.name.take(3)})"
                        today.minusDays(1) -> "Yesterday (${selectedDate.dayOfMonth} ${selectedDate.month.name.take(3)})"
                        today.plusDays(1) -> "Tomorrow (${selectedDate.dayOfMonth} ${selectedDate.month.name.take(3)})"
                        else -> "${selectedDate.dayOfWeek.name.take(3)}, ${selectedDate.dayOfMonth} ${selectedDate.month.name.take(3)} ${selectedDate.year}"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepEmerald
                )
                if (isFriday) {
                    Text(
                        text = "Friday (Jumu'ah)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedDate != today) {
                    TextButton(
                        onClick = { onDateChange(today) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Today", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IslamicGold)
                    }
                }
                IconButton(
                    onClick = { onDateChange(selectedDate.plusDays(1)) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Next Day",
                        tint = DeepEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Summary Metric Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BgMintTint)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Offered: ${dayStats.offeredCount} / 5",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = DeepEmerald
            )
            Text(
                text = "Missed: ${dayStats.missedCount} Salah",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (dayStats.missedCount > 0) Color(0xFFC0392B) else DeepEmerald
            )
            Text(
                text = "${dayStats.offeredCount * 20}% Done",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = IslamicGold
            )
        }

        // 5 Daily Prayers List
        PrayerType.values().forEach { prayer ->
            val status = dayStats.statuses[prayer] ?: PrayerStatus.NONE
            val isFridayJumuah = prayer == PrayerType.DHUHR && isFriday
            val prayerDisplayName = prayer.displayName(selectedDate)
            val prayerArabic = prayer.displayArabic(selectedDate)
            val rakats = prayer.displayRakats(selectedDate)

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = when (status) {
                    PrayerStatus.OFFERED -> BgMintTint
                    PrayerStatus.MISSED -> Color(0xFFFFF0F0)
                    PrayerStatus.QAZA_DONE -> BgPeachTint
                    PrayerStatus.NONE -> Color(0xFFFAF7F2)
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = when (status) {
                        PrayerStatus.OFFERED -> LiveEmerald.copy(alpha = 0.6f)
                        PrayerStatus.MISSED -> Color(0xFFEF9A9A)
                        PrayerStatus.QAZA_DONE -> IslamicGold
                        PrayerStatus.NONE -> Color(0xFFE5DACB)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Name, Arabic, Rakats & Friday Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    when (status) {
                                        PrayerStatus.OFFERED -> DeepEmerald
                                        PrayerStatus.MISSED -> Color(0xFFC0392B)
                                        PrayerStatus.QAZA_DONE -> IslamicGold
                                        PrayerStatus.NONE -> Color(0xFFE2D6C5)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (status) {
                                    PrayerStatus.OFFERED -> Icons.Rounded.Check
                                    PrayerStatus.MISSED -> Icons.Rounded.Close
                                    PrayerStatus.QAZA_DONE -> Icons.Rounded.Refresh
                                    PrayerStatus.NONE -> Icons.Rounded.AccessTime
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = prayerDisplayName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFridayJumuah) DeepEmerald else TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "($prayerArabic)",
                                    fontSize = 12.sp,
                                    color = DeepEmerald,
                                    fontWeight = FontWeight.Medium
                                )
                                if (isFridayJumuah) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = BgGoldTint,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "JUMU'AH",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepEmerald,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "$rakats Rakat Farz ${if (isFridayJumuah) "• Congregational" else ""}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right Column: State actions
                    when (status) {
                        PrayerStatus.OFFERED -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = LiveEmerald,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Prayed", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        PrayerTrackerManager.setStatus(context, userId, selectedDate, prayer, PrayerStatus.NONE)
                                        onStatusChanged()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Rounded.Undo, contentDescription = "Reset", tint = TextSecondary, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                        PrayerStatus.MISSED -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFFEF9A9A))
                                ) {
                                    Text(
                                        text = "Missed",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC0392B),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Button(
                                    onClick = {
                                        PrayerTrackerManager.setStatus(context, userId, selectedDate, prayer, PrayerStatus.QAZA_DONE)
                                        onStatusChanged()
                                        Toast.makeText(context, "Alhamdulillah! Qaza completed for $prayerDisplayName.", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = IslamicGold),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Pray Qaza ↺", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = DeepEmerald)
                                }
                            }
                        }
                        PrayerStatus.QAZA_DONE -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = BgGoldTint,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.TaskAlt, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Qaza Done", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepEmerald)
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        PrayerTrackerManager.setStatus(context, userId, selectedDate, prayer, PrayerStatus.NONE)
                                        onStatusChanged()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Rounded.Undo, contentDescription = "Reset", tint = TextSecondary, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                        PrayerStatus.NONE -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        PrayerTrackerManager.setStatus(context, userId, selectedDate, prayer, PrayerStatus.OFFERED)
                                        onStatusChanged()
                                        Toast.makeText(context, "$prayerDisplayName marked as Prayed. MashaAllah!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Pray", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                OutlinedButton(
                                    onClick = {
                                        PrayerTrackerManager.setStatus(context, userId, selectedDate, prayer, PrayerStatus.MISSED)
                                        onStatusChanged()
                                        Toast.makeText(context, "$prayerDisplayName marked as Missed. Added to Qaza log.", Toast.LENGTH_SHORT).show()
                                    },
                                    border = BorderStroke(1.dp, Color(0xFFEF9A9A)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Missed", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFC0392B))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 2: 7-Day Week Analytics Graph with Week Switcher, Jumu'ah highlight, and Missed prayers display.
 */
@Composable
private fun WeekAnalyticsGraphView(
    weekStats: WeekPrayerStats,
    anchorDate: LocalDate,
    onWeekChange: (LocalDate) -> Unit
) {
    val startDate = anchorDate.minusDays(6)

    Column {
        // Week Navigator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFAF7F2))
                .border(1.dp, Color(0xFFE5DACB), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onWeekChange(anchorDate.minusDays(7)) },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Previous Week", tint = DeepEmerald, modifier = Modifier.size(16.dp))
            }
            Text(
                text = "${startDate.dayOfMonth} ${startDate.month.name.take(3)} - ${anchorDate.dayOfMonth} ${anchorDate.month.name.take(3)} ${anchorDate.year}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = DeepEmerald
            )
            IconButton(
                onClick = { onWeekChange(anchorDate.plusDays(7)) },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Next Week", tint = DeepEmerald, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Summary Metrics Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(BgMintTint)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Weekly Prayers", fontSize = 11.sp, color = TextSecondary)
                Text(
                    "${weekStats.totalOffered} / ${weekStats.totalPossible} Offered",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepEmerald
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Missed Prayers", fontSize = 11.sp, color = TextSecondary)
                Text(
                    "${weekStats.totalMissed} Missed",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (weekStats.totalMissed > 0) Color(0xFFC0392B) else DeepEmerald
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Consistency", fontSize = 11.sp, color = TextSecondary)
                Text(
                    "${weekStats.completionPercentage}%",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = IslamicGold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 7-Day Visual Bar Graph
        Text(
            text = "7-DAY PERFORMANCE BREAKDOWN",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFFAF7F2))
                .border(1.dp, Color(0xFFE5DACB), RoundedCornerShape(16.dp))
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            weekStats.days.forEach { day ->
                val fraction = (day.offeredCount / 5f).coerceIn(0f, 1f)
                val animatedFraction by animateFloatAsState(
                    targetValue = fraction,
                    animationSpec = tween(durationMillis = 600),
                    label = "bar_height"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${day.offeredCount}/5",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (day.offeredCount == 5) DeepEmerald else TextSecondary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Vertical Bar Track & Fill
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEBE3D7)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(animatedFraction)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        day.offeredCount == 5 -> Brush.verticalGradient(listOf(LiveEmerald, DeepEmerald))
                                        day.offeredCount >= 3 -> Brush.verticalGradient(listOf(IslamicGold, DeepEmerald))
                                        day.offeredCount > 0 -> Brush.verticalGradient(listOf(Color(0xFFFFB74D), Color(0xFFF57C00)))
                                        else -> Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                                    }
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Day of Week Label
                    Text(
                        text = if (day.isFriday) "Fri ✨" else day.dayLabel,
                        fontSize = 10.sp,
                        fontWeight = if (day.isToday || day.isFriday) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            day.isToday -> DeepEmerald
                            day.isFriday -> IslamicGold
                            else -> TextPrimary
                        }
                    )
                }
            }
        }
    }
}

/**
 * Tab 3: Monthly Breakdown Graph with Month Switcher, 4-Week Progress (no $ bug), and Missed prayers text.
 */
@Composable
private fun MonthAnalyticsGraphView(
    monthStats: MonthPrayerStats,
    selectedYearMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit
) {
    Column {
        // Month Navigator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFAF7F2))
                .border(1.dp, Color(0xFFE5DACB), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onMonthChange(selectedYearMonth.minusMonths(1)) },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Previous Month", tint = DeepEmerald, modifier = Modifier.size(16.dp))
            }
            Text(
                text = "${selectedYearMonth.month.name} ${selectedYearMonth.year}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = DeepEmerald
            )
            IconButton(
                onClick = { onMonthChange(selectedYearMonth.plusMonths(1)) },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Next Month", tint = DeepEmerald, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Summary Metric Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(BgGoldTint)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Month Prayers", fontSize = 11.sp, color = TextSecondary)
                Text(
                    "${monthStats.totalOffered} / ${monthStats.totalPossible} Offered",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepEmerald
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Missed Prayers", fontSize = 11.sp, color = TextSecondary)
                Text(
                    "${monthStats.totalMissed} Missed",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (monthStats.totalMissed > 0) Color(0xFFC0392B) else DeepEmerald
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Consistency", fontSize = 11.sp, color = TextSecondary)
                Text(
                    "${monthStats.completionPercentage}%",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepEmerald
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "4-WEEK MONTHLY PROGRESS",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFFAF7F2))
                .border(1.dp, Color(0xFFE5DACB), RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            monthStats.weeksSummary.forEachIndexed { idx, pair ->
                val weekLabel = "Week ${idx + 1} (${pair.first})"
                val offered = pair.second
                val maxPossible = 35 // 7 days * 5
                val frac = (offered.toFloat() / maxPossible).coerceIn(0f, 1f)
                val pct = (frac * 100).toInt()

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(weekLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        // Fixed: Clean percentage without erroneous $ symbol
                        Text("$offered / $maxPossible ($pct%)", fontSize = 11.sp, color = DeepEmerald, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFFEBE3D7))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(frac)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(5.dp))
                                .background(Brush.horizontalGradient(listOf(IslamicGold, DeepEmerald)))
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tab 4: Yearly Overview with Year Navigator (< 2025 | 2026 | 2027 >), Missed prayers text, and 12-month consistency.
 */
@Composable
private fun YearAnalyticsGraphView(
    yearStats: YearPrayerStats,
    selectedYear: Int,
    totalPendingQaza: Int,
    onYearChange: (Int) -> Unit
) {
    Column {
        // Year Navigator (< Previous Year | Next Year >)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFAF7F2))
                .border(1.dp, Color(0xFFE5DACB), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onYearChange(selectedYear - 1) },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Previous Year", tint = DeepEmerald, modifier = Modifier.size(16.dp))
            }
            Text(
                text = "Year $selectedYear Overview",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = DeepEmerald
            )
            IconButton(
                onClick = { onYearChange(selectedYear + 1) },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Next Year", tint = DeepEmerald, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Summary Metric Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(BgMintTint)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Year $selectedYear Offered", fontSize = 11.sp, color = TextSecondary)
                Text(
                    "${yearStats.totalOffered} Salah",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepEmerald
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Missed in $selectedYear", fontSize = 11.sp, color = TextSecondary)
                Text(
                    "${yearStats.totalMissed} Missed",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (yearStats.totalMissed > 0) Color(0xFFC0392B) else DeepEmerald
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Consistency", fontSize = 11.sp, color = TextSecondary)
                Text(
                    "${yearStats.completionPercentage}%",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = IslamicGold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "12-MONTH CONSISTENCY OVERVIEW",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 12 Months Mini-Pillars Chart
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFFAF7F2))
                .border(1.dp, Color(0xFFE5DACB), RoundedCornerShape(16.dp))
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            yearStats.monthlyAverages.forEach { pair ->
                val monthName = pair.first
                val pct = pair.second
                val fraction = (pct / 100f).coerceIn(0f, 1f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    if (pct > 0) {
                        Text(
                            text = "$pct%",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepEmerald
                        )
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(65.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEBE3D7)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (pct >= 80) DeepEmerald
                                    else if (pct >= 50) IslamicGold
                                    else if (pct > 0) Color(0xFFFFB74D)
                                    else Color.Transparent
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = monthName,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

/**
 * Tab 5: Qaza-e-Umri & Missed Salah Full Control Center.
 * Includes Multi-Year Calculator (Years & Months of Missed Salah), Quick Bulk Add / Pray, and Per-Prayer Controls.
 */
@Composable
private fun QazaManagementView(
    userId: String,
    totalPendingQaza: Int,
    onQazaUpdated: () -> Unit
) {
    val context = LocalContext.current
    var showCalculatorDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Guidance Hadith Callout
        Surface(
            color = BgGoldTint,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "“Whoever forgets a prayer or sleeps through it, its expiation is to pray it when he remembers.” (Sahih Muslim)",
                    fontSize = 11.5.sp,
                    color = DeepEmerald,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Qaza Summary & Calculator Trigger Banner
        Surface(
            color = if (totalPendingQaza > 0) Color(0xFFFFF0F0) else BgMintTint,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (totalPendingQaza > 0) Color(0xFFEF9A9A) else DeepEmerald.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL MISSED SALAH DUE",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "$totalPendingQaza Prayers",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (totalPendingQaza > 0) Color(0xFFC0392B) else DeepEmerald
                        )
                    }

                    Button(
                        onClick = { showCalculatorDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.Calculate, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Years / Months", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                if (totalPendingQaza > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val daysNeeded = (totalPendingQaza / 5)
                    Text(
                        text = "≈ $daysNeeded days of 5 prayers to complete. Offering 1 Qaza with each Farz will clear this in ~$daysNeeded days.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Quick Action: Pray 1 full day of Qaza
                    Button(
                        onClick = {
                            PrayerTrackerManager.recordFullDayQazaPrayed(context, userId)
                            onQazaUpdated()
                            Toast.makeText(context, "MashaAllah! 1 Full Day of Qaza (5 Prayers) cleared.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IslamicGold),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(34.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("✔ Mark 1 Full Day of Qaza Completed (-5 Salah)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = DeepEmerald)
                    }
                }
            }
        }

        // Qaza Counter Items for Each of the 5 Prayers
        PrayerType.values().forEach { prayer ->
            val count = remember(userId, totalPendingQaza) {
                PrayerTrackerManager.getPendingQazaCount(context, userId, prayer)
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFAF7F2),
                border = BorderStroke(1.dp, if (count > 0) Color(0xFFEF9A9A) else Color(0xFFE5DACB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${prayer.englishName} Qaza",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${prayer.arabicName})",
                                fontSize = 11.sp,
                                color = DeepEmerald
                            )
                        }
                        Text(
                            text = "${prayer.rakats} Rakat Farz ${if (prayer == PrayerType.DHUHR) "/ Jumu'ah" else ""}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Count Badge
                        Surface(
                            color = if (count > 0) Color(0xFFFFEBEE) else BgMintTint,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (count > 0) Color(0xFFE57373) else DeepEmerald.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "$count Due",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (count > 0) Color(0xFFC0392B) else DeepEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Decrement (Prayed Qaza) Button
                        IconButton(
                            onClick = {
                                if (count > 0) {
                                    PrayerTrackerManager.recordQazaPrayed(context, userId, prayer)
                                    onQazaUpdated()
                                    Toast.makeText(context, "1 ${prayer.englishName} Qaza cleared. May Allah accept!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = count > 0,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (count > 0) DeepEmerald else Color(0xFFE0E0E0))
                        ) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Prayed 1", tint = Color.White, modifier = Modifier.size(16.dp))
                        }

                        // Increment (Missed another) Button
                        IconButton(
                            onClick = {
                                PrayerTrackerManager.adjustQazaCount(context, userId, prayer, +1)
                                onQazaUpdated()
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFC0392B))
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add 1 Missed", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    // Multi-Year & Month Qaza-e-Umri Calculator Dialog
    if (showCalculatorDialog) {
        QazaYearsCalculatorDialog(
            onDismiss = { showCalculatorDialog = false },
            onAddQaza = { years, months, days ->
                PrayerTrackerManager.addBulkQaza(context, userId, years, months, days)
                onQazaUpdated()
                showCalculatorDialog = false
                val totalDays = (years * 365) + (months * 30) + days
                Toast.makeText(context, "Added $totalDays days of Qaza prayers to your log.", Toast.LENGTH_LONG).show()
            }
        )
    }
}

/**
 * Multi-Year / Months Qaza-e-Umri Dialog
 */
@Composable
private fun QazaYearsCalculatorDialog(
    onDismiss: () -> Unit,
    onAddQaza: (years: Int, months: Int, days: Int) -> Unit
) {
    var yearsInput by remember { mutableStateOf("1") }
    var monthsInput by remember { mutableStateOf("0") }
    var daysInput by remember { mutableStateOf("0") }

    val years = yearsInput.toIntOrNull() ?: 0
    val months = monthsInput.toIntOrNull() ?: 0
    val days = daysInput.toIntOrNull() ?: 0
    val totalDays = (years * 365) + (months * 30) + days
    val totalPrayers = totalDays * 5

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PureCream,
            border = BorderStroke(1.2.dp, IslamicGold),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Calculate, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Qaza-e-Umri Calculator",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepEmerald
                    )
                }

                Text(
                    text = "Calculate and log missed prayers over years or months if you missed Salah previously in life.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 17.sp
                )

                // Quick presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("1 Year" to 1, "2 Years" to 2, "3 Years" to 3, "5 Years" to 5).forEach { (lbl, yr) ->
                        OutlinedButton(
                            onClick = {
                                yearsInput = yr.toString()
                                monthsInput = "0"
                                daysInput = "0"
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(30.dp),
                            border = BorderStroke(1.dp, if (years == yr && months == 0) DeepEmerald else Color(0xFFD4C7B8))
                        ) {
                            Text(lbl, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (years == yr && months == 0) DeepEmerald else TextPrimary)
                        }
                    }
                }

                // Custom Inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = yearsInput,
                        onValueChange = { yearsInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Years", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = monthsInput,
                        onValueChange = { monthsInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Months", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = daysInput,
                        onValueChange = { daysInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Days", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Computed summary
                Surface(
                    color = BgMintTint,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Total: $totalDays Days = $totalPrayers Missed Salah",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepEmerald
                        )
                        Text(
                            text = "($totalDays Fajr, $totalDays Dhuhr, $totalDays Asr, $totalDays Maghrib, $totalDays Isha)",
                            fontSize = 10.5.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onAddQaza(years, months, days) },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                        enabled = totalDays > 0,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add to Qaza Log", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
