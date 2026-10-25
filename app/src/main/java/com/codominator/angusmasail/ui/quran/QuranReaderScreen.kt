package com.codominator.angusmasail.ui.quran

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codominator.angusmasail.network.AyahDto
import com.codominator.angusmasail.network.SurahDetailDto
import com.codominator.angusmasail.network.SurahHeaderDto
import com.codominator.angusmasail.ui.theme.*
import com.codominator.angusmasail.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReaderScreen(
    surahNumber: Int,
    initialAyahNumber: Int? = null,
    viewModel: MainViewModel,
    surahs: List<SurahHeaderDto>,
    onSurahChanged: (Int) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentSurah by viewModel.currentSurah.collectAsState()
    val selectedSurahNum by viewModel.selectedSurahNumber.collectAsState()
    val isLoading by viewModel.isLoadingQuran.collectAsState()
    val bookmarkedAyahs by viewModel.bookmarkedAyahs.collectAsState()
    val playingAyahKey by viewModel.currentlyPlayingAyahKey.collectAsState()

    val activeSurahNum = currentSurah?.number ?: selectedSurahNum

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSurahSelectModal by remember { mutableStateOf(false) }
    var selectedTafseerAyah by remember { mutableStateOf<AyahDto?>(null) }

    // User adjustable font sizes
    var arabicFontSize by remember { mutableStateOf(24f) }
    var urduFontSize by remember { mutableStateOf(16f) }

    val listState = rememberLazyListState()

    LaunchedEffect(surahNumber) {
        viewModel.selectAndFetchSurah(surahNumber)
    }

    // Scroll to top when changing Surahs (unless navigating to a specific Ayah)
    LaunchedEffect(currentSurah?.number) {
        if (initialAyahNumber == null) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(currentSurah, initialAyahNumber) {
        if (initialAyahNumber != null && currentSurah != null) {
            val idx = currentSurah!!.ayahs.indexOfFirst { it.numberInSurah == initialAyahNumber }
            if (idx != -1) {
                // Scroll to target ayah (+1 for header item)
                listState.animateScrollToItem(idx + 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentSurah?.name?.removePrefix("سورة ")?.removePrefix("سُورَةُ ") ?: "الفَاتِحَة",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepEmerald
                        )
                        val engName = currentSurah?.englishName
                        if (!engName.isNullOrBlank()) {
                            Text(
                                text = "$activeSurahNum. $engName",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stopAyahAudio()
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = DeepEmerald
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.FormatSize,
                            contentDescription = "Font Settings",
                            tint = DeepEmerald
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmAlabaster)
            )
        },
        bottomBar = {
            // Surah navigation bar (Previous Surah, Current Name Selector, Next Surah)
            Surface(
                color = PureCream,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0xFFE5DACB)),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val canGoPrev = activeSurahNum > 1
                    val canGoNext = activeSurahNum < 114

                    // Previous Surah Button
                    TextButton(
                        onClick = {
                            if (canGoPrev) {
                                val prevNum = activeSurahNum - 1
                                viewModel.stopAyahAudio()
                                viewModel.selectAndFetchSurah(prevNum)
                                onSurahChanged(prevNum)
                            }
                        },
                        enabled = canGoPrev
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronLeft,
                            contentDescription = "Previous Surah",
                            tint = if (canGoPrev) DeepEmerald else Color.LightGray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Prev Surah",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (canGoPrev) DeepEmerald else Color.LightGray
                        )
                    }

                    // Surah Selector Quick Pill
                    Surface(
                        onClick = { showSurahSelectModal = true },
                        shape = RoundedCornerShape(20.dp),
                        color = BgMintTint,
                        border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${currentSurah?.number ?: activeSurahNum}. ${currentSurah?.englishName ?: ""}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepEmerald
                            )
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = DeepEmerald)
                        }
                    }

                    // Next Surah Button
                    TextButton(
                        onClick = {
                            if (canGoNext) {
                                val nextNum = activeSurahNum + 1
                                viewModel.stopAyahAudio()
                                viewModel.selectAndFetchSurah(nextNum)
                                onSurahChanged(nextNum)
                            }
                        },
                        enabled = canGoNext
                    ) {
                        Text(
                            text = "Next Surah",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (canGoNext) DeepEmerald else Color.LightGray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = "Next Surah",
                            tint = if (canGoNext) DeepEmerald else Color.LightGray
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.radialGradient(listOf(WarmAlabaster, WarmSand)))
        ) {
            if (isLoading && currentSurah == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DeepEmerald)
                }
            } else {
                val surah = currentSurah
                if (surah != null) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. SURAH HEADER CARD (Matches Screenshot 5 Mint Card)
                        item {
                            SurahHeaderBismillahCard(
                                surah = surah,
                                onOpenTafseer = {
                                    selectedTafseerAyah = AyahDto(
                                        numberInSurah = 0,
                                        globalNumber = 0,
                                        arabicText = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                        urduTranslation = "اللہ کے نام سے شروع جو بہت مہربان رحمت والا (کنز الایمان)",
                                        audioUrl = null,
                                        juz = 1,
                                        page = 1
                                    )
                                }
                            )
                        }

                        // 2. AYAH CARDS (Matches Screenshot 5 Individual Ayah Cards)
                        items(surah.ayahs, key = { it.numberInSurah }) { ayah ->
                            val isBookmarked = bookmarkedAyahs.contains("${surah.number}:${ayah.numberInSurah}")
                            val isPlaying = playingAyahKey == "${surah.number}:${ayah.numberInSurah}"

                            AyahCardItem(
                                ayah = ayah,
                                surahNumber = surah.number,
                                isBookmarked = isBookmarked,
                                isPlaying = isPlaying,
                                arabicFontSize = arabicFontSize,
                                urduFontSize = urduFontSize,
                                onToggleBookmark = {
                                    viewModel.toggleBookmark(surah.number, ayah.numberInSurah)
                                    val msg = if (isBookmarked) "Bookmark removed" else "Ayah bookmarked"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                onPlayAudio = {
                                    viewModel.playAyahAudio(surah.number, ayah.numberInSurah, ayah.audioUrl)
                                },
                                onOpenTafseer = {
                                    selectedTafseerAyah = ayah
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Settings Dialog (Font size adjustments)
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Reading Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Urdu Translation: Kanzul Iman by Ala Hazrat Ahmad Raza Khan Barelvi", fontSize = 12.sp, color = DeepEmerald, fontWeight = FontWeight.SemiBold)

                    Text("Arabic Font Size: ${arabicFontSize.toInt()} sp", fontSize = 13.sp)
                    Slider(
                        value = arabicFontSize,
                        onValueChange = { arabicFontSize = it },
                        valueRange = 18f..36f,
                        colors = SliderDefaults.colors(thumbColor = DeepEmerald, activeTrackColor = LiveEmerald)
                    )

                    Text("Urdu Translation Font Size: ${urduFontSize.toInt()} sp", fontSize = 13.sp)
                    Slider(
                        value = urduFontSize,
                        onValueChange = { urduFontSize = it },
                        valueRange = 13f..24f,
                        colors = SliderDefaults.colors(thumbColor = DeepEmerald, activeTrackColor = LiveEmerald)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Done", color = DeepEmerald, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = PureCream,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Surah Quick Selector Modal
    if (showSurahSelectModal) {
        SelectSurahDialog(
            surahs = surahs,
            onDismiss = { showSurahSelectModal = false },
            onSelectSurah = { num ->
                showSurahSelectModal = false
                viewModel.stopAyahAudio()
                viewModel.selectAndFetchSurah(num)
                onSurahChanged(num)
            }
        )
    }

    // Tafseer / Explanation Dialog
    if (selectedTafseerAyah != null) {
        val ayah = selectedTafseerAyah!!
        AlertDialog(
            onDismissRequest = { selectedTafseerAyah = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = DeepEmerald)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (ayah.numberInSurah == 0) "Tafseer: Bismillah" else "Tafseer Ayah ${ayah.numberInSurah}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = ayah.arabicText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        color = DeepEmerald,
                        modifier = Modifier.fillMaxWidth()
                    )
                    HorizontalDivider(color = Color(0xFFE2D8C7))
                    Text(
                        text = "ترجمہ کنز الایمان (امام احمد رضا خان بریلوی):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = ayah.urduTranslation,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.End,
                        color = DeepEmerald,
                        modifier = Modifier.fillMaxWidth()
                    )
                    HorizontalDivider(color = Color(0xFFE2D8C7))
                    Text(
                        text = "تفسیر خزائن العرفان و فیوضات اہلسنت:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "یہ مبارک کلام ربِ کریم کا نازل کردہ ہے جس میں مومنین کیلئے شفا، رحمت اور ہدایت کاملہ ہے۔ کنز الایمان شریف کے مطابق عقائد صحیحہ اور احکامِ الٰہیہ کا جامع بیان فرمایا گیا ہے۔",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.End,
                        color = TextSecondary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedTafseerAyah = null }) {
                    Text("Close", color = DeepEmerald, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = PureCream,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

/**
 * Mint Bismillah Header Card matching Screenshot 5 exactly!
 */
@Composable
fun SurahHeaderBismillahCard(
    surah: SurahDetailDto,
    onOpenTafseer: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFD6F5E8)), // Mint green from Screenshot 5
        border = BorderStroke(1.dp, Color(0xFFBCE8D3))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Row: English name (purple) left, Arabic name (purple) right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = surah.englishName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B46C1) // Purple
                )
                Text(
                    text = surah.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B46C1) // Purple
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFB6E6CE), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Tafseer tag button (matches Screenshot 5)
            Surface(
                onClick = onOpenTafseer,
                shape = RoundedCornerShape(6.dp),
                color = Color.Transparent,
                border = BorderStroke(1.2.dp, DeepEmerald)
            ) {
                Text(
                    text = "Tafseer",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepEmerald,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ornate Bismillah Calligraphy (green)
            Text(
                text = surah.bismillahArabic ?: "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = DeepEmerald,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Kanzul Iman translation of Bismillah
            Text(
                text = surah.bismillahTranslation ?: "Allaah Ke Naam Se Shuru Jo Bahot Maherbaan Rehmat Waala.",
                fontSize = 13.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Individual Ayah Card matching Screenshot 5 exactly!
 */
@Composable
fun AyahCardItem(
    ayah: AyahDto,
    surahNumber: Int,
    isBookmarked: Boolean,
    isPlaying: Boolean,
    arabicFontSize: Float,
    urduFontSize: Float,
    onToggleBookmark: () -> Unit,
    onPlayAudio: () -> Unit,
    onOpenTafseer: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureCream),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        border = BorderStroke(1.dp, Color(0xFFE8DFD0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Action Bar: [+] Bookmark, [▷] Audio, [Tafseer], and ornate ayah number ﴿ 1 ﴾
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // [+] Bookmark Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isBookmarked) LiveEmerald else Color.Transparent)
                            .border(1.2.dp, if (isBookmarked) LiveEmerald else DeepEmerald, RoundedCornerShape(6.dp))
                            .clickable(onClick = onToggleBookmark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.Add,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) Color.White else DeepEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // [▷] Audio Recitation Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isPlaying) ForestSlate else Color.Transparent)
                            .border(1.2.dp, DeepEmerald, RoundedCornerShape(6.dp))
                            .clickable(onClick = onPlayAudio),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Recite",
                            tint = if (isPlaying) Color.White else DeepEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // [Tafseer] Button
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.2.dp, DeepEmerald, RoundedCornerShape(6.dp))
                            .clickable(onClick = onOpenTafseer)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tafseer",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepEmerald
                        )
                    }
                }

                // Ornate Ayah Number: ﴿ 1 ﴾
                Text(
                    text = "﴿ ${ayah.numberInSurah} ﴾",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Arabic Ayah Text with Uthmani script and tashkeel (Proper RTL alignment)
            Text(
                text = ayah.arabicText,
                fontSize = arabicFontSize.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                style = TextStyle(textDirection = TextDirection.Rtl),
                lineHeight = (arabicFontSize * 1.7f).sp,
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFEEE6D8), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Urdu Translation in Kanzul Iman by Ala Hazrat Ahmad Raza Khan Barelvi (Proper RTL alignment)
            Text(
                text = ayah.urduTranslation,
                fontSize = urduFontSize.sp,
                lineHeight = (urduFontSize * 1.6f).sp,
                textAlign = TextAlign.End,
                style = TextStyle(textDirection = TextDirection.Rtl),
                color = DeepEmerald,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
