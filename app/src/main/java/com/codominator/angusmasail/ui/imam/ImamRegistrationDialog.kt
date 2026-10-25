package com.codominator.angusmasail.ui.imam

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import com.codominator.angusmasail.network.MosqueDto
import com.codominator.angusmasail.ui.theme.*
import com.codominator.angusmasail.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImamRegistrationDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onRegistrationSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    var imamName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var masjidName by remember { mutableStateOf("") }
    var masjidAddress by remember { mutableStateOf("") }
    var masjidCity by remember { mutableStateOf("") }
    var selectedFiqh by remember { mutableStateOf("Hanafi") }
    var qualification by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val availableMosques by viewModel.availableMosques.collectAsState()
    var selectedMosqueId by remember { mutableStateOf<String?>(null) }
    var isNewMosqueMode by remember { mutableStateOf(false) }
    var expandedMosqueDropdown by remember { mutableStateOf(false) }

    val fiqhOptions = listOf("Hanafi", "Shafi'i", "Maliki", "Hanbali", "Ahle Sunnat (Barelvi)")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(24.dp),
            color = PureCream,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(BgGoldTint)
                                .border(1.5.dp, IslamicGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AdminPanelSettings,
                                contentDescription = null,
                                tint = DeepEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Register as an Imam",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Onboard your mosque & leadership",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(WarmAlabaster)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFE8DFD0))
                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Form Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Section 1: Scholar Information
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "SCHOLAR CREDENTIALS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepEmerald,
                            letterSpacing = 1.sp
                        )

                        OutlinedTextField(
                            value = imamName,
                            onValueChange = { imamName = it },
                            label = { Text("Imam / Scholar Full Name *") },
                            placeholder = { Text("e.g. Maulana Syed Ahmed") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Person, contentDescription = null, tint = DeepEmerald)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = WarmAlabaster,
                                unfocusedContainerColor = WarmAlabaster,
                                focusedBorderColor = DeepEmerald,
                                unfocusedBorderColor = Color(0xFFDFD5C5)
                            )
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Mobile Phone (for Scholar Login) *") },
                            placeholder = { Text("e.g. 9876543210") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Phone, contentDescription = null, tint = DeepEmerald)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = WarmAlabaster,
                                unfocusedContainerColor = WarmAlabaster,
                                focusedBorderColor = DeepEmerald,
                                unfocusedBorderColor = Color(0xFFDFD5C5)
                            )
                        )

                        OutlinedTextField(
                            value = qualification,
                            onValueChange = { qualification = it },
                            label = { Text("Scholarly Qualification / Degree") },
                            placeholder = { Text("e.g. Fazil / Dars-e-Nizami, Mufti, Hafiz") },
                            leadingIcon = {
                                Icon(Icons.Rounded.School, contentDescription = null, tint = DeepEmerald)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = WarmAlabaster,
                                unfocusedContainerColor = WarmAlabaster,
                                focusedBorderColor = DeepEmerald,
                                unfocusedBorderColor = Color(0xFFDFD5C5)
                            )
                        )

                        // Fiqh School Selection - Horizontally scrollable so chips NEVER get squeezed
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Fiqh School / Jurisprudence (Manhaj) *",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                fiqhOptions.forEach { f ->
                                    val isSelected = selectedFiqh == f
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedFiqh = f },
                                        label = {
                                            Text(
                                                text = f,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                softWrap = false,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = DeepEmerald,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE8DFD0))

                    // Section 2: Mosque Selection / Registration
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MOSQUE & CONGREGATION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepEmerald,
                                letterSpacing = 1.sp
                            )
                            if (availableMosques.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        isNewMosqueMode = !isNewMosqueMode
                                        if (isNewMosqueMode) {
                                            selectedMosqueId = null
                                            masjidName = ""
                                            masjidAddress = ""
                                            masjidCity = ""
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        text = if (isNewMosqueMode) "Pick from available" else "+ Create New Mosque",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DeepEmerald
                                    )
                                }
                            }
                        }

                        if (!isNewMosqueMode && availableMosques.isNotEmpty()) {
                            // Available Mosques Dropdown
                            ExposedDropdownMenuBox(
                                expanded = expandedMosqueDropdown,
                                onExpandedChange = { expandedMosqueDropdown = !expandedMosqueDropdown },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = if (selectedMosqueId != null) masjidName else "Select from available mosques...",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Select Available Mosque *") },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Apartment, contentDescription = null, tint = DeepEmerald)
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMosqueDropdown)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = WarmAlabaster,
                                        unfocusedContainerColor = WarmAlabaster,
                                        focusedBorderColor = DeepEmerald,
                                        unfocusedBorderColor = Color(0xFFDFD5C5)
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedMosqueDropdown,
                                    onDismissRequest = { expandedMosqueDropdown = false }
                                ) {
                                    availableMosques.forEach { mosque ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(mosque.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text("${mosque.city} • ${mosque.fiqhSchool ?: "Sunni"}", fontSize = 11.sp, color = TextSecondary)
                                                }
                                            },
                                            onClick = {
                                                selectedMosqueId = mosque.id
                                                masjidName = mosque.name
                                                masjidAddress = mosque.address ?: ""
                                                masjidCity = mosque.city
                                                mosque.fiqhSchool?.let { selectedFiqh = it }
                                                expandedMosqueDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            if (selectedMosqueId != null) {
                                Surface(
                                    color = BgMintTint,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(masjidName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DeepEmerald)
                                            Text("${masjidAddress}, ${masjidCity}", fontSize = 11.sp, color = TextPrimary)
                                        }
                                    }
                                }
                            }
                        } else {
                            // Manual Mosque Details Input
                            OutlinedTextField(
                                value = masjidName,
                                onValueChange = { masjidName = it },
                                label = { Text("Mosque / Masjid Name *") },
                                placeholder = { Text("e.g. Masjid Al-Noor / Jama Masjid") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.LocationCity, contentDescription = null, tint = DeepEmerald)
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = WarmAlabaster,
                                    unfocusedContainerColor = WarmAlabaster,
                                    focusedBorderColor = DeepEmerald,
                                    unfocusedBorderColor = Color(0xFFDFD5C5)
                                )
                            )

                            OutlinedTextField(
                                value = masjidAddress,
                                onValueChange = { masjidAddress = it },
                                label = { Text("Mosque Address / Locality *") },
                                placeholder = { Text("e.g. Station Road, Mohalla / Ward 4") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Place, contentDescription = null, tint = DeepEmerald)
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = WarmAlabaster,
                                    unfocusedContainerColor = WarmAlabaster,
                                    focusedBorderColor = DeepEmerald,
                                    unfocusedBorderColor = Color(0xFFDFD5C5)
                                )
                            )

                            OutlinedTextField(
                                value = masjidCity,
                                onValueChange = { masjidCity = it },
                                label = { Text("City / District *") },
                                placeholder = { Text("e.g. Kolkata, Hooghly") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Map, contentDescription = null, tint = DeepEmerald)
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = WarmAlabaster,
                                    unfocusedContainerColor = WarmAlabaster,
                                    focusedBorderColor = DeepEmerald,
                                    unfocusedBorderColor = Color(0xFFDFD5C5)
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE8DFD0))

                    // Section 3: Bio & Congregation Message
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "SCHOLAR BIO & COMMUNITY WELCOME",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepEmerald,
                            letterSpacing = 1.sp
                        )

                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("Brief Bio / Congregation Welcome Message") },
                            placeholder = { Text("e.g. Available for daily prayers, Nikah consultation, and Fiqh questions.") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 70.dp, max = 110.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = WarmAlabaster,
                                unfocusedContainerColor = WarmAlabaster,
                                focusedBorderColor = DeepEmerald,
                                unfocusedBorderColor = Color(0xFFDFD5C5)
                            )
                        )
                    }

                    // Admin Verification Notice Card
                    Surface(
                        color = BgGoldTint,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, IslamicGold.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Awaiting Admin Approval", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepEmerald)
                                Text("Your application will be verified and approved by the administrator before your Imam portal is activated.", fontSize = 11.sp, color = TextPrimary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFE8DFD0))
                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons
                val canSubmit = imamName.isNotBlank() && phone.isNotBlank() && masjidName.isNotBlank() && masjidAddress.isNotBlank()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(0.9f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        Text("Cancel", fontSize = 13.sp, maxLines = 1, softWrap = false)
                    }

                    Button(
                        onClick = {
                            if (canSubmit) {
                                isLoading = true
                                viewModel.registerImam(
                                    imamName = imamName.trim(),
                                    phone = phone.trim(),
                                    masjidName = masjidName.trim(),
                                    masjidAddress = masjidAddress.trim(),
                                    masjidCity = masjidCity.trim().ifEmpty { "Local Area" },
                                    fiqhSchool = selectedFiqh,
                                    qualification = qualification.trim(),
                                    bio = bio.trim(),
                                    mosqueId = selectedMosqueId
                                ) { success, errorMsg ->
                                    isLoading = false
                                    if (success) {
                                        Toast.makeText(
                                            context,
                                            "Application submitted for $masjidName! Awaiting administrator approval.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        onRegistrationSuccess()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Registration failed: ${errorMsg ?: "Please check network"}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                        enabled = !isLoading && canSubmit
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Submit Application", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
    }
}
