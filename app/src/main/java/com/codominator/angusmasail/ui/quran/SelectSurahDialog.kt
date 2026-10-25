package com.codominator.angusmasail.ui.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.codominator.angusmasail.network.SurahHeaderDto
import com.codominator.angusmasail.ui.theme.DeepEmerald
import com.codominator.angusmasail.ui.theme.PureCream
import com.codominator.angusmasail.ui.theme.TextPrimary
import com.codominator.angusmasail.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectSurahDialog(
    surahs: List<SurahHeaderDto>,
    onDismiss: () -> Unit,
    onSelectSurah: (Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = PureCream,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with title and red close button (matches Screenshot 1)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(32.dp))
                    Text(
                        text = "Select Surah",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar (matches Screenshot 1)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    placeholder = {
                        Text("Search Surah", color = TextSecondary, fontSize = 14.sp)
                    },
                    trailingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = TextSecondary)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = DeepEmerald,
                        unfocusedBorderColor = Color(0xFFE2D8C7)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = Color(0xFFEEE6D8), thickness = 1.dp)

                // Scrollable List of Surahs (matches Screenshot 1)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(filteredSurahs, key = { it.number }) { surah ->
                        SurahModalRowItem(
                            surah = surah,
                            onClick = {
                                onSelectSurah(surah.number)
                                onDismiss()
                            }
                        )
                        HorizontalDivider(color = Color(0xFFF3ECE1), thickness = 0.8.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SurahModalRowItem(
    surah: SurahHeaderDto,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Green Circle Number Badge: (1), (2), (3)...
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, DeepEmerald, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = surah.number.toString(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepEmerald
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // English Surah Name: Sureh Faatiha, Sureh Baqarah...
            Text(
                text = surah.englishName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        // Arabic Surah Name: سورة الفاتحة, سورة البقرة...
        Text(
            text = surah.name,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.End
        )
    }
}
