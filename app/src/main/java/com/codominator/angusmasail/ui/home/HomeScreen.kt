package com.codominator.angusmasail.ui.home

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codominator.angusmasail.network.ApiClient
import com.codominator.angusmasail.network.AyahDto
import com.codominator.angusmasail.network.BannerDto
import com.codominator.angusmasail.network.PrayerTimetableDto
import com.codominator.angusmasail.ui.components.GlassmorphicSurface
import com.codominator.angusmasail.ui.components.StatusPill
import com.codominator.angusmasail.ui.components.bouncingClickable
import com.codominator.angusmasail.ui.theme.*
import com.codominator.angusmasail.ui.tracker.PrayerTrackerCard
import com.codominator.angusmasail.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.util.Locale

data class NextPrayerInfo(
    val name: String,
    val azanTime: String,
    val jamaatTime: String,
    val secondsRemaining: Long
)

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit = {},
    onNavigateToMasail: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onOpenImamSelector: () -> Unit = {},
    onOpenImamStatus: () -> Unit = {},
    onQuickAction: (String) -> Unit = {}
) {
    val prayerTimes by viewModel.prayerTimes.collectAsState()
    val selectedImam by viewModel.selectedImam.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val imamQueries by viewModel.imamQueries.collectAsState()
    val banners by viewModel.banners.collectAsState()
    val ayahOfTheDay by viewModel.ayahOfTheDay.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val isImam = userRole == "ROLE_IMAM"
    val pendingQueriesCount = remember(imamQueries) {
        imamQueries.count { it.status.equals("PENDING", ignoreCase = true) || it.status.equals("SUBMITTED", ignoreCase = true) }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(WarmAlabaster, WarmSand)))
            .verticalScroll(scrollState)
            .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 100.dp)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DeepEmerald)
            }
        } else {
            // Top Bar with Hamburger Menu & Cleanly Aligned Masjid Selector Pill (Locked for Imam)
            HomeTopBar(
                masjidName = selectedImam?.masjidName ?: "Select Mosque",
                isImam = isImam,
                onOpenDrawer = onOpenDrawer,
                onOpenImamSelector = onOpenImamSelector
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Home Banner Carousel (covers 35% of home screen, auto-rotates every 5s, admin controlled max 4)
            if (banners.isNotEmpty()) {
                HomeBannerCarousel(
                    banners = banners
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Production-Grade Islamic Calendar Tile with Animated Moon & Full Hijri Details
            IslamicCalendarTile(
                hijriOffset = selectedImam?.hijriOffsetDays ?: 0
            )

            // Dynamic Pending Imam Application Banner
            if (currentUser?.isImamPending == true) {
                Spacer(modifier = Modifier.height(14.dp))
                GlassmorphicSurface(
                    backgroundColor = BgGoldTint,
                    borderColor = IslamicGold,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(IslamicGold.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HourglassTop,
                                contentDescription = null,
                                tint = DeepEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Imam Application Under Review",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Admin approval in progress for ${currentUser?.selectedMasjidName ?: "Mosque"}",
                                fontSize = 10.5.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Dynamic Next Prayer Hero Card
            HeroPrayerCard(prayerTimes)

            Spacer(modifier = Modifier.height(24.dp))

            // Prayer Times Horizontal Strip with dynamic active highlight
            DailyScheduleRow(prayerTimes)

            Spacer(modifier = Modifier.height(24.dp))

            // Dynamic Salah & Qaza Tracker with Day / Week / Month / Year Analytics Graph & Jumu'ah Handling
            PrayerTrackerCard(
                userId = currentUser?.id ?: if (isImam) "imam_user" else "local_user"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Ayah of the Day Widget (Matches Screenshot 3)
            AyahOfTheDayCard(
                ayah = ayahOfTheDay,
                onOpenQuran = {
                    val sNum = ayahOfTheDay?.surahNumber ?: 1
                    val aNum = ayahOfTheDay?.numberInSurah
                    onQuickAction(if (aNum != null) "Quran:$sNum:$aNum" else "Quran:$sNum:1")
                }
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeBannerCarousel(
    banners: List<BannerDto>,
    modifier: Modifier = Modifier,
    onBannerClick: (BannerDto) -> Unit = {}
) {
    if (banners.isEmpty()) return

    val configuration = LocalConfiguration.current
    // 35% of the home screen height
    val bannerHeight = configuration.screenHeightDp.dp * 0.35f
    val pagerState = rememberPagerState(pageCount = { banners.size })

    // Auto-rotate every 5 seconds
    LaunchedEffect(banners.size, pagerState) {
        if (banners.size > 1) {
            while (isActive) {
                delay(5000L)
                val nextPage = (pagerState.currentPage + 1) % banners.size
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(durationMillis = 750)
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(bannerHeight)
            .clip(RoundedCornerShape(22.dp))
            .border(BorderStroke(1.dp, Color(0xFFE2D9C8).copy(alpha = 0.85f)), RoundedCornerShape(22.dp))
    ) {
        HorizontalPager(
            state = pagerState,
            key = { page -> banners.getOrNull(page)?.id ?: page.toString() },
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val banner = banners[page]
            val resolvedUrl = if (banner.imageUrl.startsWith("http")) banner.imageUrl
            else ApiClient.BASE_URL.removeSuffix("/") + if (banner.imageUrl.startsWith("/")) banner.imageUrl else "/${banner.imageUrl}"

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onBannerClick(banner) }
            ) {
                AsyncImage(
                    model = resolvedUrl,
                    contentDescription = banner.title ?: "Banner Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Elegant subtle vignette gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.20f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.78f)
                                )
                            )
                        )
                )

                // Overlay Text and Badge
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                ) {
                    if (!banner.title.isNullOrBlank()) {
                        Surface(
                            color = IslamicGold,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "COMMUNITY UPDATE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepEmerald,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = banner.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        // Indicator dots at bottom center
        if (banners.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                banners.indices.forEach { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (isSelected) 22.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) IslamicGold else Color.White.copy(alpha = 0.65f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun HomeTopBar(
    masjidName: String,
    isImam: Boolean = false,
    onOpenDrawer: () -> Unit = {},
    onOpenImamSelector: () -> Unit = {}
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hamburger Menu Circle Button
        GlassmorphicSurface(
            modifier = Modifier
                .size(44.dp)
                .bouncingClickable { onOpenDrawer() },
            shape = CircleShape,
            backgroundColor = Color.White.copy(alpha = 0.90f),
            borderColor = Color(0xFFE2D9C8)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = "Menu",
                    tint = DeepEmerald,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Cleanly Aligned Frosted Mosque Selector Pill (Locked for Imam, Selectable for Congregants)
        GlassmorphicSurface(
            modifier = Modifier
                .widthIn(max = 240.dp)
                .bouncingClickable {
                    if (isImam) {
                        Toast.makeText(context, "You are registered as the Imam of $masjidName", Toast.LENGTH_SHORT).show()
                    } else {
                        onOpenImamSelector()
                    }
                },
            shape = RoundedCornerShape(50),
            backgroundColor = Color.White.copy(alpha = 0.90f),
            borderColor = if (isImam) IslamicGold.copy(alpha = 0.8f) else Color(0xFFE2D9C8)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isImam) Icons.Rounded.Verified else Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = if (isImam) IslamicGold else SoftSage,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = masjidName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!isImam) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun IslamicCalendarTile(
    hijriOffset: Int = 0
) {
    // Dynamic real-time clock updating every second
    var currentDateTime by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            currentDateTime = LocalDateTime.now()
            delay(1000L)
        }
    }

    val liveTimeFormatted = remember(currentDateTime.second) {
        currentDateTime.format(DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.ENGLISH))
    }

    val gregorianFormatted = remember(currentDateTime.dayOfYear, currentDateTime.year) {
        currentDateTime.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH))
    }

    // Dynamic, non-dummy Hijri calculation
    val (hijriDay, hijriMonth, hijriYear, hijriArabicMonth) = remember(currentDateTime.dayOfYear, currentDateTime.year, hijriOffset) {
        val adjustedDate = currentDateTime.toLocalDate().plusDays(hijriOffset.toLong())
        val monthNames = listOf(
            "Muharram" to "مُحَرَّم",
            "Safar" to "صَفَر",
            "Rabi' al-Awwal" to "رَبِيع الأَوَّل",
            "Rabi' al-Thani" to "رَبِيع الثَّانِي",
            "Jumada al-Ula" to "جُمَادَى الأُولَى",
            "Jumada al-Akhirah" to "جُمَادَى الآخِرَة",
            "Rajab" to "رَجَب",
            "Sha'ban" to "شَعْبَان",
            "Ramadan" to "رَمَضَان",
            "Shawwal" to "شَوَّال",
            "Dhu al-Qi'dah" to "ذُو القَعْدَة",
            "Dhu al-Hijjah" to "ذُو الحِجَّة"
        )
        try {
            val hijrahDate = HijrahChronology.INSTANCE.date(adjustedDate)
            val day = hijrahDate.get(ChronoField.DAY_OF_MONTH)
            val month = hijrahDate.get(ChronoField.MONTH_OF_YEAR)
            val year = hijrahDate.get(ChronoField.YEAR)
            val pair = monthNames.getOrElse(month - 1) { "Hijri" to "هجري" }
            listOf("$day", pair.first, "$year AH", pair.second)
        } catch (e: Throwable) {
            calculateAstronomicalHijri(adjustedDate, monthNames)
        }
    }

    // Smooth breathing animation for the crescent moon icon
    val infiniteTransition = rememberInfiniteTransition(label = "MoonPulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.93f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MoonPulse"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MoonGlow"
    )
    val liveDotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LiveDotAlpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PureCream),
        border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Top Row: Islamic Calendar Badge + Arabic Month + Live Pulsing Clock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusPill(
                        text = "ISLAMIC CALENDAR",
                        bgColor = BgGoldTint,
                        textColor = DeepEmerald
                    )
                    Text(
                        text = hijriArabicMonth,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGold
                    )
                }

                // Dynamic Live Current Time Pill
                Surface(
                    color = BgMintTint,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(DeepEmerald.copy(alpha = liveDotAlpha))
                        )
                        Text(
                            text = liveTimeFormatted,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepEmerald
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Row: Animated Crescent Moon + Both Dynamic Dates
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated Moon Badge
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    IslamicGold.copy(alpha = glowAlpha),
                                    BgGoldTint,
                                    PureCream
                                )
                            )
                        )
                        .border(1.5.dp, IslamicGold.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NightsStay,
                        contentDescription = "Hijri Moon",
                        tint = DeepEmerald,
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Dates Column (Hijri + Gregorian)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$hijriDay $hijriMonth $hijriYear",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = IslamicGold,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = gregorianFormatted,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Phase Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(BgGoldTint)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Brightness4,
                            contentDescription = null,
                            tint = IslamicGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "PHASE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = IslamicGold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pure astronomical mathematical calculation for Islamic Tabular / Civil Calendar.
 * Ensures zero dummy fallbacks even on platforms lacking standard HijrahChronology.
 */
private fun calculateAstronomicalHijri(
    date: LocalDate,
    monthNames: List<Pair<String, String>>
): List<String> {
    val epochDay = date.toEpochDay()
    val daysSinceEpoch = epochDay + 492148L
    val cycle = daysSinceEpoch / 10631L
    val remaining = (daysSinceEpoch % 10631L).toInt()
    val leapYears = setOf(2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29)
    var yInCycle = 1
    var accDays = 0
    for (y in 1..30) {
        val daysInY = if (leapYears.contains(y)) 355 else 354
        if (accDays + daysInY > remaining) {
            yInCycle = y
            break
        }
        accDays += daysInY
    }
    val dayOfYear = remaining - accDays
    val monthLengths = intArrayOf(30, 29, 30, 29, 30, 29, 30, 29, 30, 29, 30, 29)
    var m = 1
    var d = dayOfYear + 1
    for (idx in 0..11) {
        if (d <= monthLengths[idx]) {
            m = idx + 1
            break
        }
        d -= monthLengths[idx]
    }
    val year = (cycle * 30 + yInCycle).toInt()
    val pair = monthNames.getOrElse(m - 1) { "Hijri" to "هجري" }
    return listOf("$d", pair.first, "$year AH", pair.second)
}

@Composable
fun HeroPrayerCard(prayerTimes: PrayerTimetableDto?) {
    val nextPrayer = remember(prayerTimes) {
        calculateNextPrayer(prayerTimes)
    }

    var secondsLeft by remember(nextPrayer.name, nextPrayer.azanTime) {
        mutableLongStateOf(nextPrayer.secondsRemaining)
    }

    // Steady, drift-free second-by-second ticker without jitter
    LaunchedEffect(nextPrayer.name, nextPrayer.azanTime, prayerTimes) {
        while (true) {
            val updated = calculateNextPrayer(prayerTimes)
            secondsLeft = updated.secondsRemaining
            delay(1000L)
        }
    }

    val countdownText = remember(secondsLeft) {
        formatCountdown(secondsLeft)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(204.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(DeepEmerald, ForestSlate)))
            .bouncingClickable { }
    ) {
        val mosquePath = remember { Path() }
        // Decorative Mosque Architecture Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(WarmAmber.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width * 0.85f, 0f),
                    radius = 380f
                ),
                center = Offset(size.width * 0.85f, 0f)
            )
            val domeWidth = 140f
            val startX = size.width - 180f
            val startY = size.height
            mosquePath.reset()
            mosquePath.moveTo(startX, startY)
            mosquePath.lineTo(startX, startY - 60f)
            mosquePath.cubicTo(
                startX, startY - 140f,
                startX + domeWidth, startY - 140f,
                startX + domeWidth, startY - 60f
            )
            mosquePath.lineTo(startX + domeWidth, startY)
            mosquePath.close()
            drawPath(mosquePath, color = Color.White.copy(alpha = 0.06f))
        }

        // Foreground Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "Next Prayer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                )

                GlassmorphicSurface(
                    backgroundColor = Color.White.copy(alpha = 0.16f),
                    borderColor = Color.White.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = countdownText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFeatureSettings = "tnum"
                            ),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        PulseDot(IslamicGold)
                    }
                }
            }

            Column {
                Text(
                    text = nextPrayer.name,
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Azan: ${nextPrayer.azanTime}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    GlassmorphicSurface(
                        backgroundColor = IslamicGold.copy(alpha = 0.28f),
                        borderColor = Color(0xFFF3D078).copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "Jamaat: ${nextPrayer.jamaatTime}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PulseDot(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )
    Box(
        modifier = Modifier
            .size(7.dp)
            .graphicsLayer { this.alpha = alpha }
            .background(color, CircleShape)
    )
}

@Composable
fun ImamUpdatesRow(
    imamName: String,
    onOpenImamStatus: () -> Unit,
    onQuickAction: (String) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Imam Updates", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            PulseDot(LiveEmerald)
        }
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Imam Avatar with rotating ring
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(Color.Transparent, CircleShape)
                        .border(
                            2.dp,
                            Brush.sweepGradient(listOf(DeepEmerald, IslamicGold, DeepEmerald)),
                            CircleShape
                        )
                        .padding(4.dp)
                        .background(WarmSand, CircleShape)
                        .bouncingClickable { onOpenImamStatus() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Person, contentDescription = "Imam", tint = DeepEmerald, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = imamName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 11.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            QuickActionBubble(icon = Icons.Rounded.MenuBook, bgColor = BgMintTint, label = "Quran", onClick = { onQuickAction("Quran") })
            QuickActionBubble(icon = Icons.Rounded.Book, bgColor = BgGoldTint, label = "Books", onClick = { onQuickAction("Books") })
            QuickActionBubble(icon = Icons.Rounded.ChatBubble, bgColor = BgPeachTint, label = "Masail", onClick = { onQuickAction("Masail") })
            QuickActionBubble(icon = Icons.Rounded.Newspaper, bgColor = BgMintTint, label = "News", onClick = { onQuickAction("News") })
        }
    }
}

@Composable
fun QuickActionBubble(icon: ImageVector, bgColor: Color, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .background(bgColor, RoundedCornerShape(22.dp))
                .border(1.dp, Color.White, RoundedCornerShape(22.dp))
                .bouncingClickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = DeepEmerald, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 11.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun BentoCardsRow(
    imamName: String,
    isImam: Boolean = false,
    pendingQueriesCount: Int = 0,
    onNavigateToMasail: () -> Unit,
    onNavigateToChat: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isImam) {
            // Left Card for Imam: Incoming Masail Notification & Quick Action to Reply
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(140.dp)
                    .bouncingClickable { onNavigateToMasail() },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = if (pendingQueriesCount > 0) Color(0xFFFFF9F5) else PureCream),
                border = BorderStroke(1.2.dp, if (pendingQueriesCount > 0) Color(0xFFE57373) else IslamicGold.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp, pressedElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (pendingQueriesCount > 0) Icons.Rounded.NotificationsActive else Icons.Rounded.QuestionAnswer,
                                contentDescription = null,
                                tint = if (pendingQueriesCount > 0) Color(0xFFC0392B) else IslamicGold,
                                modifier = Modifier.size(24.dp)
                            )
                            if (pendingQueriesCount > 0) {
                                Surface(
                                    color = Color(0xFFC0392B),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "$pendingQueriesCount NEW",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Incoming Masail",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (pendingQueriesCount > 0) "$pendingQueriesCount queries waiting" else "All queries answered",
                            fontSize = 10.5.sp,
                            color = if (pendingQueriesCount > 0) Color(0xFFC0392B) else TextSecondary,
                            fontWeight = if (pendingQueriesCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        StatusPill(
                            text = if (pendingQueriesCount > 0) "Reply Solution" else "Queue Clear",
                            bgColor = if (pendingQueriesCount > 0) BgPeachTint else BgMintTint,
                            textColor = if (pendingQueriesCount > 0) Color(0xFFC0392B) else DeepEmerald
                        )
                    }
                }
            }
        } else {
            // Left Card for Congregant: Ask a Question
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(140.dp)
                    .bouncingClickable { onNavigateToMasail() },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PureCream),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp, pressedElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = null, tint = IslamicGold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Ask a Question", style = MaterialTheme.typography.titleMedium, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        StatusPill(text = "Fiqh", bgColor = BgMintTint, textColor = DeepEmerald)
                        StatusPill(text = "Aqeedah", bgColor = BgPeachTint, textColor = DeepEmerald)
                    }
                }
            }
        }

        // Right Card: 1-on-1 Consultation
        Card(
            modifier = Modifier
                .weight(1f)
                .height(140.dp)
                .bouncingClickable { onNavigateToChat() },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PureCream),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
        ) {
            Box(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                Icon(Icons.Rounded.Forum, contentDescription = null, tint = IslamicGold, modifier = Modifier.align(Alignment.TopEnd))
                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isImam) "Congregant Chats" else "Live Consultation",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        PulseDot(LiveEmerald)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isImam) "Direct chats & audio messages" else "Direct session with $imamName",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

data class PrayerScheduleItem(
    val name: String,
    val arabicName: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val isActive: Boolean,
    val remainSeconds: Long,
    val icon: ImageVector
)

@Composable
fun DailyScheduleRow(prayerTimes: PrayerTimetableDto?) {
    // Optimized ticking: schedule items only recompute on minute boundaries or timetable changes
    val currentMinute by produceState(initialValue = LocalTime.now().minute) {
        while (isActive) {
            delay(10000L)
            val nowMin = LocalTime.now().minute
            if (value != nowMin) {
                value = nowMin
            }
        }
    }

    val scheduleItems = remember(prayerTimes, currentMinute) {
        calculateScheduleItems(prayerTimes, LocalTime.now())
    }

    val isFriday = remember { LocalDate.now().dayOfWeek == DayOfWeek.FRIDAY }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PureCream),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
        border = BorderStroke(1.2.dp, IslamicGold.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // 1. Header: Title without any text-based LIVE badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(IslamicGold.copy(alpha = 0.3f), BgGoldTint))
                        )
                        .border(1.dp, IslamicGold.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = DeepEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Daily Prayer Schedule",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isFriday) "Jumu'ah & Daily Prayer Timings" else "Start & End Times for 6 Daily Phases",
                        fontSize = 11.sp,
                        color = if (isFriday) DeepEmerald else TextSecondary,
                        fontWeight = if (isFriday) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Table Column Headers: PRAYER | START | END | REMAINING
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFEFE8DC))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PRAYER",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.weight(1.4f)
                )
                Text(
                    text = "START",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1.0f)
                )
                Text(
                    text = "END",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1.0f)
                )
                Text(
                    text = "REMAINING",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1.2f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. Chronological Prayer Rows (Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha)
            scheduleItems.forEachIndexed { index, item ->
                if (item.isActive) {
                    // Active prayer: UI-based highlight (BgMintTint + Emerald border + Left accent indicator)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BgMintTint,
                        border = BorderStroke(1.5.dp, DeepEmerald),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // UI-based active vertical bar
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(DeepEmerald)
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            // Column 1: Prayer Name & Arabic
                            Row(
                                modifier = Modifier.weight(1.4f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = DeepEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = item.name,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepEmerald
                                    )
                                    Text(
                                        text = "(${item.arabicName})",
                                        fontSize = 10.5.sp,
                                        color = IslamicGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Column 2: Start Time
                            Text(
                                text = formatScheduleTime(item.startTime),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepEmerald,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1.0f)
                            )

                            // Column 3: End Time
                            Text(
                                text = formatScheduleTime(item.endTime),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepEmerald,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1.0f)
                            )

                            // Column 4: Remaining Time (Isolated ticking component so only this badge updates per second)
                            ActiveScheduleCountdownBadge(
                                endTime = item.endTime,
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                    }
                } else {
                    // Regular clean row for inactive prayers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (index % 2 == 1) Color(0xFFFAF6F0) else Color.Transparent)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Column 1: Prayer Name & Arabic
                        Row(
                            modifier = Modifier.weight(1.4f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = IslamicGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = item.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "(${item.arabicName})",
                                    fontSize = 10.sp,
                                    color = DeepEmerald
                                )
                            }
                        }

                        // Column 2: Start Time
                        Text(
                            text = formatScheduleTime(item.startTime),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1.0f)
                        )

                        // Column 3: End Time
                        Text(
                            text = formatScheduleTime(item.endTime),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1.0f)
                        )

                        // Column 4: Remaining Time (Empty / subtle dash for inactive)
                        Text(
                            text = "—",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFFD4C7B8),
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .weight(1.2f)
                                .padding(end = 12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFEFE8DC), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // 4. Footer Note
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = IslamicGold,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isFriday) "Jumu'ah prayer replaces Dhuhr today • Times align with local Masjid" else "Times align strictly with the authentic sunni prayer calculation",
                    fontSize = 10.5.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun ActiveScheduleCountdownBadge(
    endTime: LocalTime,
    modifier: Modifier = Modifier
) {
    val tickSeconds by produceState(initialValue = 0L) {
        while (isActive) {
            val now = LocalTime.now()
            val rem = if (!now.isAfter(endTime)) {
                ChronoUnit.SECONDS.between(now, endTime)
            } else {
                val toMidnight = ChronoUnit.SECONDS.between(now, LocalTime.MAX) + 1
                val fromMidnight = ChronoUnit.SECONDS.between(LocalTime.MIN, endTime)
                toMidnight + fromMidnight
            }
            value = rem.coerceAtLeast(0L)
            delay(1000L)
        }
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DeepEmerald,
        modifier = modifier
    ) {
        Text(
            text = formatScheduleRemainTime(tickSeconds),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}

fun calculateScheduleItems(dto: PrayerTimetableDto?, now: LocalTime): List<PrayerScheduleItem> {
    fun parse(raw: String?, fallback: LocalTime): LocalTime {
        if (raw.isNullOrBlank()) return fallback
        val trimmed = raw.trim()
        return try {
            LocalTime.parse(trimmed)
        } catch (e: Exception) {
            try {
                val parts = trimmed.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: fallback.hour
                val m = parts.getOrNull(1)?.toIntOrNull() ?: fallback.minute
                val s = parts.getOrNull(2)?.take(2)?.toIntOrNull() ?: 0
                LocalTime.of(h % 24, m % 60, s % 60)
            } catch (e2: Exception) {
                fallback
            }
        }
    }

    val isFriday = LocalDate.now().dayOfWeek == DayOfWeek.FRIDAY

    val fajrStart = parse(dto?.fajrStartTime ?: dto?.fajrAzan, LocalTime.of(4, 30))
    val fajrEnd = parse(dto?.fajrEndTime ?: dto?.sunrise, LocalTime.of(6, 15))

    val sunriseStart = parse(dto?.sunriseStartTime ?: dto?.sunrise, LocalTime.of(6, 15))
    val sunriseEnd = parse(dto?.sunriseEndTime ?: dto?.dhuhrStartTime ?: dto?.dhuhrAzan, LocalTime.of(12, 30))

    val dhuhrStart = if (isFriday && !dto?.jumuahStartTime.isNullOrBlank()) {
        parse(dto.jumuahStartTime, LocalTime.of(12, 30))
    } else if (isFriday && !dto?.jumuahAzan.isNullOrBlank()) {
        parse(dto.jumuahAzan, LocalTime.of(12, 30))
    } else {
        parse(dto?.dhuhrStartTime ?: dto?.dhuhrAzan, LocalTime.of(12, 30))
    }

    val dhuhrEnd = if (isFriday && !dto?.jumuahEndTime.isNullOrBlank()) {
        parse(dto.jumuahEndTime, LocalTime.of(16, 15))
    } else {
        parse(dto?.dhuhrEndTime ?: dto?.asrStartTime ?: dto?.asrAzan, LocalTime.of(16, 15))
    }

    val asrStart = parse(dto?.asrStartTime ?: dto?.asrAzan, LocalTime.of(16, 15))
    val asrEnd = parse(dto?.asrEndTime ?: dto?.maghribStartTime ?: dto?.maghribAzan, LocalTime.of(18, 45))

    val maghribStart = parse(dto?.maghribStartTime ?: dto?.maghribAzan, LocalTime.of(18, 45))
    val maghribEnd = parse(dto?.maghribEndTime ?: dto?.ishaStartTime ?: dto?.ishaAzan, LocalTime.of(20, 0))

    val ishaStart = parse(dto?.ishaStartTime ?: dto?.ishaAzan, LocalTime.of(20, 0))
    val ishaEnd = parse(dto?.ishaEndTime ?: dto?.fajrStartTime ?: dto?.fajrAzan, LocalTime.of(4, 30))

    // 1. Fajr: [fajrStart, fajrEnd)
    val isFajrActive = !now.isBefore(fajrStart) && now.isBefore(fajrEnd)
    val fajrRemain = if (isFajrActive) ChronoUnit.SECONDS.between(now, fajrEnd) else 0L

    // 2. Sunrise (Shurooq): [sunriseStart, sunriseEnd)
    val isSunriseActive = !now.isBefore(sunriseStart) && now.isBefore(sunriseEnd)
    val sunriseRemain = if (isSunriseActive) ChronoUnit.SECONDS.between(now, sunriseEnd) else 0L

    // 3. Dhuhr / Jumu'ah: [dhuhrStart, dhuhrEnd)
    val isDhuhrActive = !now.isBefore(dhuhrStart) && now.isBefore(dhuhrEnd)
    val dhuhrRemain = if (isDhuhrActive) ChronoUnit.SECONDS.between(now, dhuhrEnd) else 0L

    // 4. Asr: [asrStart, asrEnd)
    val isAsrActive = !now.isBefore(asrStart) && now.isBefore(asrEnd)
    val asrRemain = if (isAsrActive) ChronoUnit.SECONDS.between(now, asrEnd) else 0L

    // 5. Maghrib: [maghribStart, maghribEnd)
    val isMaghribActive = !now.isBefore(maghribStart) && now.isBefore(maghribEnd)
    val maghribRemain = if (isMaghribActive) ChronoUnit.SECONDS.between(now, maghribEnd) else 0L

    // 6. Isha: [ishaStart, midnight) or [midnight, ishaEnd)
    val isIshaActive = !now.isBefore(ishaStart) || now.isBefore(ishaEnd)
    val ishaRemain = if (isIshaActive) {
        if (!now.isBefore(ishaStart)) {
            val toMidnight = ChronoUnit.SECONDS.between(now, LocalTime.MAX) + 1
            val fromMidnight = ChronoUnit.SECONDS.between(LocalTime.MIN, ishaEnd)
            toMidnight + fromMidnight
        } else {
            ChronoUnit.SECONDS.between(now, ishaEnd)
        }
    } else 0L

    return listOf(
        PrayerScheduleItem(
            name = "Fajr",
            arabicName = "الفجر",
            startTime = fajrStart,
            endTime = fajrEnd,
            isActive = isFajrActive,
            remainSeconds = fajrRemain,
            icon = Icons.Rounded.NightsStay
        ),
        PrayerScheduleItem(
            name = "Sunrise",
            arabicName = "الشروق",
            startTime = sunriseStart,
            endTime = sunriseEnd,
            isActive = isSunriseActive,
            remainSeconds = sunriseRemain,
            icon = Icons.Rounded.WbTwilight
        ),
        PrayerScheduleItem(
            name = if (isFriday) "Jumu'ah" else "Dhuhr",
            arabicName = if (isFriday) "الجمعة" else "الظهر",
            startTime = dhuhrStart,
            endTime = dhuhrEnd,
            isActive = isDhuhrActive,
            remainSeconds = dhuhrRemain,
            icon = Icons.Rounded.WbSunny
        ),
        PrayerScheduleItem(
            name = "Asr",
            arabicName = "العصر",
            startTime = asrStart,
            endTime = asrEnd,
            isActive = isAsrActive,
            remainSeconds = asrRemain,
            icon = Icons.Rounded.WbSunny
        ),
        PrayerScheduleItem(
            name = "Maghrib",
            arabicName = "المغرب",
            startTime = maghribStart,
            endTime = maghribEnd,
            isActive = isMaghribActive,
            remainSeconds = maghribRemain,
            icon = Icons.Rounded.WbTwilight
        ),
        PrayerScheduleItem(
            name = "Isha",
            arabicName = "العشاء",
            startTime = ishaStart,
            endTime = ishaEnd,
            isActive = isIshaActive,
            remainSeconds = ishaRemain,
            icon = Icons.Rounded.NightsStay
        )
    )
}

fun formatScheduleTime(t: LocalTime): String {
    return String.format(Locale.ENGLISH, "%02d:%02d", t.hour, t.minute)
}

fun formatScheduleRemainTime(seconds: Long): String {
    if (seconds <= 0) return "00:00:00"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format(Locale.ENGLISH, "%02d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.ENGLISH, "%02d:%02d", m, s)
    }
}

// Helper: Dynamically calculates the next upcoming prayer & remaining seconds
fun calculateNextPrayer(dto: PrayerTimetableDto?): NextPrayerInfo {
    val now = LocalTime.now()

    fun parseTime(t: String?, fallback: LocalTime): LocalTime {
        if (t == null) return fallback
        return try {
            LocalTime.parse(t.trim())
        } catch (e: Exception) {
            try {
                LocalTime.parse(t.trim(), DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
            } catch (e2: Exception) {
                fallback
            }
        }
    }

    val fajr = parseTime(dto?.fajrAzan, LocalTime.of(4, 30))
    val sunrise = parseTime(dto?.sunrise, LocalTime.of(6, 15))
    val dhuhr = parseTime(dto?.dhuhrAzan, LocalTime.of(12, 30))
    val asr = parseTime(dto?.asrAzan, LocalTime.of(16, 15))
    val maghrib = parseTime(dto?.maghribAzan, LocalTime.of(18, 45))
    val isha = parseTime(dto?.ishaAzan, LocalTime.of(20, 0))

    val schedule = listOf(
        Triple("Fajr", fajr, dto?.fajrJamaat ?: "05:00"),
        Triple("Sunrise", sunrise, "-"),
        Triple("Dhuhr", dhuhr, dto?.dhuhrJamaat ?: "13:15"),
        Triple("Asr", asr, dto?.asrJamaat ?: "16:45"),
        Triple("Maghrib", maghrib, dto?.maghribJamaat ?: "18:55"),
        Triple("Isha", isha, dto?.ishaJamaat ?: "20:30")
    )

    for ((name, time, jamaat) in schedule) {
        if (now.isBefore(time)) {
            val seconds = ChronoUnit.SECONDS.between(now, time)
            return NextPrayerInfo(name, time.toString(), jamaat, seconds)
        }
    }

    // After Isha: Next is Fajr of tomorrow
    val secondsUntilMidnight = ChronoUnit.SECONDS.between(now, LocalTime.MAX)
    val secondsFajr = ChronoUnit.SECONDS.between(LocalTime.MIN, fajr)
    return NextPrayerInfo("Fajr", fajr.toString(), dto?.fajrJamaat ?: "05:00", secondsUntilMidnight + secondsFajr)
}

fun formatCountdown(seconds: Long): String {
    if (seconds <= 0) return "00:00:00"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return String.format(Locale.US, "-%02d:%02d:%02d", h, m, s)
}

@Composable
fun AyahOfTheDayCard(
    ayah: AyahDto? = null,
    onOpenQuran: () -> Unit
) {
    val surahLabel = if (!ayah?.surahName.isNullOrBlank() && ayah?.numberInSurah != null) {
        "${ayah.surahName} • Ayah ${ayah.numberInSurah}"
    } else {
        "Al-Quran"
    }

    val arabic = ayah?.arabicText ?: "فَإِن تَوَلَّوْا۟ فَإِنَّ ٱللَّهَ عَلِيمٌۢ بِٱلْمُفْسِدِينَ ﴿٦٣﴾"
    val urdu = ayah?.urduTranslation ?: "پھر اگر وہ منہ پھیریں تو بیشک اللہ فسادیوں کو جانتا ہے، (کنز الایمان - امام احمد رضا خان)"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenQuran),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PureCream),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.2.dp, Color(0xFF6B46C1).copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFF3E8FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FormatQuote,
                            contentDescription = null,
                            tint = Color(0xFF6B46C1),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ayah of the Day",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF553C9A)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BgMintTint
                ) {
                    Text(
                        text = surahLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepEmerald,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Arabic Verse
            Text(
                text = arabic,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                style = TextStyle(textDirection = TextDirection.Rtl),
                lineHeight = 32.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFEFE8DC), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Kanzul Iman Urdu Translation
            Text(
                text = urdu,
                fontSize = 13.sp,
                color = DeepEmerald,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                style = TextStyle(textDirection = TextDirection.Rtl),
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (!ayah?.surahName.isNullOrBlank()) "Read Surah ${ayah.surahName} with Kanzul Iman" else "Read Full Surah with Kanzul Iman",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B46C1)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF6B46C1),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

