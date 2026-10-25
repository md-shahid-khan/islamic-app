package com.codominator.angusmasail.ui.quran

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codominator.angusmasail.network.SurahHeaderDto
import com.codominator.angusmasail.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranHubScreen(
    surahs: List<SurahHeaderDto>,
    onBack: () -> Unit,
    onNavigateToSurahList: () -> Unit,
    onOpenSurahReader: (surahNumber: Int, initialAyah: Int?) -> Unit,
    onOpenBookmarks: () -> Unit
) {
    val context = LocalContext.current
    var showSelectSurahDialog by remember { mutableStateOf(false) }
    var searchAyahInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Quran Shareef",
                        fontWeight = FontWeight.Bold,
                        color = DeepEmerald
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = DeepEmerald
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmAlabaster)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.radialGradient(listOf(WarmAlabaster, WarmSand)))
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. SELECT SURAH DROPDOWN CARD (Matches Screenshot 4)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSelectSurahDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureCream),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, Color(0xFFE5DACB))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Surah",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = TextSecondary
                    )
                }
            }

            // 2. SEARCH SURAH AYAH INPUT (Matches Screenshot 4)
            OutlinedTextField(
                value = searchAyahInput,
                onValueChange = { searchAyahInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                placeholder = {
                    Text("Search Surah Ayah...", color = TextSecondary, fontSize = 14.sp)
                },
                trailingIcon = {
                    IconButton(onClick = {
                        val trimmed = searchAyahInput.trim()
                        if (trimmed.contains(":")) {
                            val parts = trimmed.split(":")
                            val sNum = parts.getOrNull(0)?.toIntOrNull()
                            val aNum = parts.getOrNull(1)?.toIntOrNull()
                            if (sNum != null && sNum in 1..114) {
                                onOpenSurahReader(sNum, aNum ?: 1)
                            } else {
                                Toast.makeText(context, "Please enter a valid Surah (1-114)", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val sNum = trimmed.toIntOrNull()
                            if (sNum != null && sNum in 1..114) {
                                onOpenSurahReader(sNum, null)
                            } else {
                                val match = surahs.firstOrNull { it.englishName.contains(trimmed, ignoreCase = true) || it.name.contains(trimmed) }
                                if (match != null) {
                                    onOpenSurahReader(match.number, null)
                                } else {
                                    Toast.makeText(context, "Enter format Surah:Ayah (e.g. 24:17)", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Rounded.Search, contentDescription = "Search", tint = DeepEmerald)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PureCream,
                    unfocusedContainerColor = PureCream,
                    focusedBorderColor = DeepEmerald,
                    unfocusedBorderColor = Color(0xFFE2D8C7)
                )
            )

            // 3. EXPLANATORY HINT CARD (Matches Screenshot 4)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PureCream),
                border = BorderStroke(1.dp, Color(0xFFE5DACB))
            ) {
                Text(
                    text = "To search specific ayah number 17 of Surah noor. This is format 24(Surah number) 17 (Ayah number) so enter 24:17",
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // 4. MENU CARDS (Matches Screenshot 4)
            // Option 1: Surah Wise
            QuranFeatureMenuCard(
                title = "Surah Wise",
                description = "Read Quran chapters (Surahs) with tarjuma, tafseer, and audio of ayahs",
                onClick = onNavigateToSurahList
            )

            // Option 2: Listen
            QuranFeatureMenuCard(
                title = "Listen",
                description = "Listen to Quran chapters wise with complete tarjuma, tafseer & recitations.",
                onClick = {
                    // Opens reader ready to listen
                    onOpenSurahReader(1, null)
                }
            )

            // Option 3: Bookmarks
            QuranFeatureMenuCard(
                title = "Bookmarks",
                description = "Save your favorite, last read ayahs and find them easily later.",
                onClick = onOpenBookmarks
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showSelectSurahDialog) {
        SelectSurahDialog(
            surahs = surahs,
            onDismiss = { showSelectSurahDialog = false },
            onSelectSurah = { surahNumber ->
                showSelectSurahDialog = false
                onOpenSurahReader(surahNumber, null)
            }
        )
    }
}

@Composable
fun QuranFeatureMenuCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureCream),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.2.dp, IslamicGold.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DeepEmerald
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 19.sp
            )
        }
    }
}
