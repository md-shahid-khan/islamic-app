package com.codominator.angusmasail.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.codominator.angusmasail.network.UserDto
import com.codominator.angusmasail.ui.theme.*
import com.codominator.angusmasail.viewmodel.MainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val prefs = remember { context.getSharedPreferences("angus_app_settings", Context.MODE_PRIVATE) }

    // Form fields pre-populated with current user details
    var fullName by remember(currentUser) { mutableStateOf(currentUser?.name ?: "") }
    var city by remember(currentUser) { mutableStateOf(currentUser?.city ?: "") }
    var locality by remember(currentUser) { mutableStateOf(currentUser?.locality ?: "") }
    var address by remember(currentUser) { mutableStateOf(currentUser?.address ?: "") }
    var selectedManhaj by remember(currentUser) { mutableStateOf(currentUser?.manhaj ?: "Hanafi") }
    var preferredLang by remember(currentUser) { mutableStateOf(currentUser?.preferredLanguage ?: "Urdu (Kanzul Iman)") }
    var bio by remember(currentUser) { mutableStateOf(currentUser?.bio ?: "") }

    var isLiveClockEnabled by remember { mutableStateOf(prefs.getBoolean("pref_live_clock", true)) }
    var isRecitationAutoPlay by remember { mutableStateOf(prefs.getBoolean("pref_auto_recitation", true)) }

    var isSaving by remember { mutableStateOf(false) }

    val manhajOptions = listOf(
        "Hanafi",
        "Shafi'i",
        "Maliki",
        "Hanbali",
        "Ahle Sunnat (Barelvi)"
    )

    val languageOptions = listOf(
        "Urdu (Kanzul Iman)",
        "English",
        "Hindi (हिंदी)",
        "Arabic (العربية)"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(28.dp)),
            colors = CardDefaults.cardColors(containerColor = WarmAlabaster),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DeepEmerald),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Settings, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Settings & Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DeepEmerald)
                            Text("Manage personal preferences & details", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(WarmSand)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextPrimary, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Scrollable Form Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Profile Header Card
                    Surface(
                        color = BgGoldTint,
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, IslamicGold.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(DeepEmerald),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (currentUser?.name?.firstOrNull() ?: currentUser?.username?.firstOrNull() ?: 'U').uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = IslamicGold
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentUser?.name ?: currentUser?.username ?: "Congregant User",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )

                                Text(
                                    text = if (currentUser?.email != null) currentUser!!.email!! else (currentUser?.phone ?: ""),
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        color = DeepEmerald,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = currentUser?.role?.replace("ROLE_", "") ?: "USER",
                                            color = PureCream,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    if (currentUser?.emailVerified == true) {
                                        Surface(
                                            color = LiveEmerald.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = SoftSage, modifier = Modifier.size(10.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "Verified Email",
                                                    color = DeepEmerald,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "ADDITIONAL SPECIFIC DETAILS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepEmerald,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Conveniently configure specific details not required during registration",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Full Name
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Legal / Preferred Name") },
                        placeholder = { Text("e.g. Shahid Mansoor Khan") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Badge, contentDescription = null, tint = DeepEmerald)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepEmerald,
                            unfocusedBorderColor = WarmShadowAmbient
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // City & Locality Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("City") },
                            placeholder = { Text("e.g. Kolkata") },
                            leadingIcon = {
                                Icon(Icons.Rounded.LocationCity, contentDescription = null, tint = DeepEmerald)
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DeepEmerald,
                                unfocusedBorderColor = WarmShadowAmbient
                            )
                        )

                        OutlinedTextField(
                            value = locality,
                            onValueChange = { locality = it },
                            label = { Text("Locality / Area") },
                            placeholder = { Text("e.g. Park Circus") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Place, contentDescription = null, tint = DeepEmerald)
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DeepEmerald,
                                unfocusedBorderColor = WarmShadowAmbient
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Complete Address / Mohalla
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Street Address / Mohalla / Landmark") },
                        placeholder = { Text("e.g. Near Jama Masjid, 42 Main Road") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Home, contentDescription = null, tint = DeepEmerald)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepEmerald,
                            unfocusedBorderColor = WarmShadowAmbient
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Manhaj / Fiqh School Selection
                    Text(
                        text = "Fiqh School / Maslak Preference:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        manhajOptions.forEach { option ->
                            val isSelected = selectedManhaj.equals(option, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedManhaj = option },
                                label = {
                                    Text(
                                        text = option,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DeepEmerald,
                                    selectedLabelColor = PureCream,
                                    containerColor = PureCream,
                                    labelColor = TextPrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) DeepEmerald else WarmShadowAmbient,
                                    selectedBorderColor = DeepEmerald,
                                    borderWidth = 1.2.dp,
                                    selectedBorderWidth = 1.5.dp
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Preferred Quran Recitation / Translation Language
                    Text(
                        text = "Preferred Translation Language:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        languageOptions.forEach { lang ->
                            val isSelected = preferredLang == lang
                            FilterChip(
                                selected = isSelected,
                                onClick = { preferredLang = lang },
                                label = {
                                    Text(
                                        text = lang,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DeepEmerald,
                                    selectedLabelColor = PureCream,
                                    containerColor = PureCream,
                                    labelColor = TextPrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) DeepEmerald else WarmShadowAmbient,
                                    selectedBorderColor = DeepEmerald,
                                    borderWidth = 1.2.dp,
                                    selectedBorderWidth = 1.5.dp
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bio
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Personal Bio / Notes") },
                        placeholder = { Text("Add any personal notes or preferences...") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepEmerald,
                            unfocusedBorderColor = WarmShadowAmbient
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // App Preferences Toggle
                    Text(
                        text = "APPLICATION PREFERENCES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepEmerald,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = PureCream,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WarmShadowAmbient),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Row 1: Live clock toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        isLiveClockEnabled = !isLiveClockEnabled
                                        prefs.edit().putBoolean("pref_live_clock", isLiveClockEnabled).apply()
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Live Ticking Calendar Clock", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Real-time second-by-second updates on home calendar", fontSize = 11.sp, color = TextSecondary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Switch(
                                    checked = isLiveClockEnabled,
                                    onCheckedChange = { checked ->
                                        isLiveClockEnabled = checked
                                        prefs.edit().putBoolean("pref_live_clock", checked).apply()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = PureCream,
                                        checkedTrackColor = DeepEmerald,
                                        uncheckedThumbColor = Color(0xFF8D8D8D),
                                        uncheckedTrackColor = WarmSand
                                    )
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = WarmSand)

                            // Row 2: Auto-play ayah recitations toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        isRecitationAutoPlay = !isRecitationAutoPlay
                                        prefs.edit().putBoolean("pref_auto_recitation", isRecitationAutoPlay).apply()
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Auto-Play Ayah Recitations", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Stream high-fidelity Arabic audio while reading Quran", fontSize = 11.sp, color = TextSecondary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Switch(
                                    checked = isRecitationAutoPlay,
                                    onCheckedChange = { checked ->
                                        isRecitationAutoPlay = checked
                                        prefs.edit().putBoolean("pref_auto_recitation", checked).apply()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = PureCream,
                                        checkedTrackColor = DeepEmerald,
                                        uncheckedThumbColor = Color(0xFF8D8D8D),
                                        uncheckedTrackColor = WarmSand
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Version Info
                    Text(
                        text = "Angus Masail v2.4.0 (Production Build) • Developed for Islamic Community",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Action Buttons with comfortable gap
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, WarmShadowAmbient)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            prefs.edit()
                                .putBoolean("pref_live_clock", isLiveClockEnabled)
                                .putBoolean("pref_auto_recitation", isRecitationAutoPlay)
                                .apply()

                            val current = currentUser
                            if (current != null) {
                                isSaving = true
                                val updatedDto = current.copy(
                                    name = fullName.takeIf { it.isNotBlank() },
                                    city = city.takeIf { it.isNotBlank() },
                                    locality = locality.takeIf { it.isNotBlank() },
                                    address = address.takeIf { it.isNotBlank() },
                                    manhaj = selectedManhaj,
                                    preferredLanguage = preferredLang,
                                    bio = bio.takeIf { it.isNotBlank() }
                                )

                                viewModel.updateUserProfile(updatedDto) { success, err ->
                                    isSaving = false
                                    if (success) {
                                        Toast.makeText(context, "Profile details saved successfully!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, err ?: "Failed to save profile", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Preferences saved successfully", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = PureCream, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.Save, contentDescription = null, tint = PureCream)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Profile", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PureCream)
                        }
                    }
                }
            }
        }
    }
}
