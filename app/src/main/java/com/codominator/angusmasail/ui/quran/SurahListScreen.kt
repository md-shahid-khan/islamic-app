package com.codominator.angusmasail.ui.quran

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codominator.angusmasail.network.SurahHeaderDto
import com.codominator.angusmasail.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahListScreen(
    surahs: List<SurahHeaderDto>,
    onBack: () -> Unit,
    onSelectSurah: (Int) -> Unit
) {
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showSurahModal by remember { mutableStateOf(false) }

    val filteredSurahs = remember(searchQuery, surahs) {
        if (searchQuery.isBlank()) {
            surahs
        } else {
            val q = searchQuery.trim().lowercase()
            surahs.filter {
                it.number.toString() == q ||
                it.englishName.lowercase().contains(q) ||
                it.name.contains(q) ||
                it.englishNameTranslation.lowercase().contains(q)
            }
        }
    }

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
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmAlabaster)
            )
        },
        floatingActionButton = {
            // Purple Grid Floating Button (matches Screenshot 2)
            FloatingActionButton(
                onClick = { showSurahModal = true },
                containerColor = Color(0xFF6B46C1),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 80.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.GridView,
                    contentDescription = "Select Surah",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.radialGradient(listOf(WarmAlabaster, WarmSand)))
        ) {
            // Optional Search Bar
            if (showSearch) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search Surah name or number...") },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PureCream,
                        unfocusedContainerColor = PureCream,
                        focusedBorderColor = DeepEmerald,
                        unfocusedBorderColor = Color(0xFFE5DACB)
                    )
                )
            }

            // List of Surah Cards (matches Screenshot 2)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredSurahs, key = { it.number }) { surah ->
                    SurahCardItem(
                        surah = surah,
                        onClick = { onSelectSurah(surah.number) }
                    )
                }
            }
        }
    }

    if (showSurahModal) {
        SelectSurahDialog(
            surahs = surahs,
            onDismiss = { showSurahModal = false },
            onSelectSurah = { num ->
                showSurahModal = false
                onSelectSurah(num)
            }
        )
    }
}

@Composable
fun SurahCardItem(
    surah: SurahHeaderDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureCream),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE8DFD0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Islamic geometric rosette / medallion with Surah number
            Row(verticalAlignment = Alignment.CenterVertically) {
                IslamicRosetteBadge(number = surah.number)

                Spacer(modifier = Modifier.width(16.dp))

                // Center: English Title (Faatiha, Baqarah, Aale Imraan...)
                Column {
                    Text(
                        text = surah.englishName.removePrefix("Sureh ").removePrefix("Surah "),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${surah.revelationType} • ${surah.numberOfAyahs} Ayahs",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            // Right: Arabic Calligraphy Title in Emerald Green
            Text(
                text = surah.name.removePrefix("سورة ").removePrefix("سُورَةُ "),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DeepEmerald,
                textAlign = TextAlign.End
            )
        }
    }
}

/**
 * Renders an ornamental Islamic rosette / medallion with the Surah number inside.
 * Matches the decorative mandala rosette from Screenshot 2.
 */
@Composable
fun IslamicRosetteBadge(number: Int) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Color(0xFFF7F3FF))
            .border(2.dp, Color(0xFF6B46C1), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Decorative inner dashed/petaled ring simulation
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .border(1.dp, Color(0xFF805AD5).copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF553C9A)
            )
        }
    }
}
