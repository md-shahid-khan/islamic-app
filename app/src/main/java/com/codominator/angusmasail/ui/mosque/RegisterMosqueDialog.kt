package com.codominator.angusmasail.ui.mosque

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
import com.codominator.angusmasail.network.CreateMosqueRequest
import com.codominator.angusmasail.ui.theme.*
import com.codominator.angusmasail.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterMosqueDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var selectedFiqh by remember { mutableStateOf("Hanafi") }
    var isLoading by remember { mutableStateOf(false) }

    val fiqhOptions = listOf("Hanafi", "Shafi'i", "Maliki", "Hanbali", "Ahle Sunnat (Barelvi)", "General Sunni")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = PureCream,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
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
                                .background(BgMintTint)
                                .border(1.5.dp, DeepEmerald.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Apartment,
                                contentDescription = null,
                                tint = DeepEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Register a Mosque",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Add to the verified directory",
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
                            .background(Color.Black.copy(alpha = 0.05f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Mosque Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Mosque Name *") },
                    placeholder = { Text("e.g. Masjid Usman, Central Mosque") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Apartment, contentDescription = null, tint = DeepEmerald)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepEmerald,
                        unfocusedBorderColor = Color(0xFFE0D7C6)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // City / District Input
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City / District *") },
                    placeholder = { Text("e.g. Central District, Lucknow") },
                    leadingIcon = {
                        Icon(Icons.Rounded.LocationCity, contentDescription = null, tint = DeepEmerald)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepEmerald,
                        unfocusedBorderColor = Color(0xFFE0D7C6)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Address Input
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Locality / Address") },
                    placeholder = { Text("e.g. Main Road, Sector 4") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Place, contentDescription = null, tint = DeepEmerald)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DeepEmerald,
                        unfocusedBorderColor = Color(0xFFE0D7C6)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Fiqh School Selection
                Text(
                    text = "Fiqh / Manhaj School",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fiqhOptions.forEach { fiqh ->
                        val isSelected = selectedFiqh == fiqh
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFiqh = fiqh },
                            label = { Text(fiqh, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DeepEmerald,
                                selectedLabelColor = PureCream
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (name.isBlank() || city.isBlank()) {
                            Toast.makeText(context, "Please enter Mosque Name and City", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true
                        viewModel.createMosque(
                            CreateMosqueRequest(
                                name = name.trim(),
                                city = city.trim(),
                                address = address.trim(),
                                fiqhSchool = selectedFiqh
                            ),
                            onSuccess = {
                                isLoading = false
                                Toast.makeText(context, "Mosque application submitted! Awaiting administrator approval.", Toast.LENGTH_LONG).show()
                                onSuccess()
                                onDismiss()
                            },
                            onError = { err ->
                                isLoading = false
                                Toast.makeText(context, "Failed: $err", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = PureCream, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.AddLocationAlt, contentDescription = null, tint = PureCream)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Register Mosque", fontWeight = FontWeight.Bold, color = PureCream)
                    }
                }
            }
        }
    }
}
