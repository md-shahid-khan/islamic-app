package com.codominator.angusmasail.ui.admin

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.codominator.angusmasail.network.ApiClient
import com.codominator.angusmasail.network.BannerDto
import com.codominator.angusmasail.ui.components.GlassmorphicSurface
import com.codominator.angusmasail.ui.components.StatusPill
import com.codominator.angusmasail.ui.theme.*
import com.codominator.angusmasail.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: MainViewModel,
    onNavigateToImamPortal: () -> Unit = {}
) {
    val context = LocalContext.current
    val imams by viewModel.imams.collectAsState()
    val pendingImams by viewModel.pendingImams.collectAsState()
    val pendingMosques by viewModel.pendingMosques.collectAsState()
    val queries by viewModel.imamQueries.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val banners by viewModel.banners.collectAsState()
    val libraryBooks by viewModel.libraryBooks.collectAsState()

    var showBroadcastDialog by remember { mutableStateOf(false) }
    var showAddBannerDialog by remember { mutableStateOf(false) }
    var imamToDelete by remember { mutableStateOf<com.codominator.angusmasail.network.ImamProfileDto?>(null) }
    var bookToDelete by remember { mutableStateOf<com.codominator.angusmasail.network.ImamResourceDto?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadPendingImams()
        viewModel.fetchBanners()
        viewModel.fetchLibraryBooks()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Security, contentDescription = null, tint = IslamicGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Masjid Central Administration", fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmAlabaster)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.radialGradient(listOf(WarmAlabaster, WarmSand)))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Role Switcher Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PureCream),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Active Role Switcher", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                            StatusPill(text = userRole, bgColor = BgMintTint, textColor = DeepEmerald)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Switch personas instantly to test all features as Muqtadi, Imam, or Trustee:", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = userRole == "ROLE_USER",
                                onClick = {
                                    viewModel.switchRole("ROLE_USER")
                                    Toast.makeText(context, "Switched role to Congregant (ROLE_USER)", Toast.LENGTH_SHORT).show()
                                },
                                label = { Text("Congregant") }
                            )
                            FilterChip(
                                selected = userRole == "ROLE_IMAM",
                                onClick = {
                                    viewModel.switchRole("ROLE_IMAM")
                                    Toast.makeText(context, "Switched role to Imam Scholar (ROLE_IMAM)", Toast.LENGTH_SHORT).show()
                                },
                                label = { Text("Imam") }
                            )
                            FilterChip(
                                selected = userRole == "ROLE_ADMIN",
                                onClick = {
                                    viewModel.switchRole("ROLE_ADMIN")
                                    Toast.makeText(context, "Switched role to Admin (ROLE_ADMIN)", Toast.LENGTH_SHORT).show()
                                },
                                label = { Text("Admin") }
                            )
                        }
                    }
                }
            }

            // High-Level Statistics
            item {
                Text("Central Overview", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminStatCard(
                        title = "Masjids",
                        value = "${imams.size}",
                        icon = Icons.Rounded.LocationCity,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        title = "Questions",
                        value = "${queries.size}",
                        icon = Icons.Rounded.LiveHelp,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        title = "Books",
                        value = "${libraryBooks.size}",
                        icon = Icons.Rounded.MenuBook,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        title = "Pending",
                        value = "${pendingImams.size + pendingMosques.size}",
                        icon = Icons.Rounded.PendingActions,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Empty Queue State
            if (pendingImams.isEmpty() && pendingMosques.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = BgMintTint.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Approval Queues Clear", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DeepEmerald)
                                Text("No pending Imam or Mosque applications awaiting review.", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            // Pending Mosque Registrations Section Awaiting Admin Approval
            if (pendingMosques.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pending Mosque Registrations (${pendingMosques.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFFC0392B)
                        )
                        StatusPill(
                            text = "REQUIRES APPROVAL",
                            bgColor = Color(0xFFFADBD8),
                            textColor = Color(0xFFC0392B)
                        )
                    }
                }

                items(pendingMosques, key = { "pending_mosque_${it.id}" }) { mosque ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PureCream),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, IslamicGold)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mosque.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${mosque.city} • ${mosque.fiqhSchool ?: "Sunni"}",
                                        fontSize = 12.sp,
                                        color = DeepEmerald,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                StatusPill(text = "PENDING", bgColor = BgGoldTint, textColor = DeepEmerald)
                            }

                            if (!mosque.address.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("📍 ${mosque.address}", fontSize = 11.sp, color = TextSecondary)
                            }

                            if (!mosque.registeredByName.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("👤 Registered by: ${mosque.registeredByName}", fontSize = 11.sp, color = TextSecondary)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color(0xFFE8DFD0))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.rejectMosque(mosque.id) {
                                            Toast.makeText(context, "Mosque registration rejected", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Reject", fontSize = 12.sp, color = Color(0xFFC0392B))
                                }

                                Button(
                                    onClick = {
                                        viewModel.approveMosque(mosque.id) { success ->
                                            if (success) {
                                                Toast.makeText(context, "Approved! ${mosque.name} is now publicly listed", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Approval failed", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1.5f),
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Approve Mosque", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Pending Imam Registrations Section Awaiting Admin Approval
            if (pendingImams.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pending Imam Applications (${pendingImams.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFFC0392B)
                        )
                        StatusPill(
                            text = "REQUIRES APPROVAL",
                            bgColor = Color(0xFFFADBD8),
                            textColor = Color(0xFFC0392B)
                        )
                    }
                }

                items(pendingImams, key = { "pending_${it.id}" }) { pending ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PureCream),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, IslamicGold)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pending.imamName ?: "Scholar Applicant",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${pending.masjidName} • ${pending.masjidCity ?: "Local"}",
                                        fontSize = 12.sp,
                                        color = DeepEmerald,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                StatusPill(text = "PENDING", bgColor = BgGoldTint, textColor = DeepEmerald)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (!pending.phone.isNullOrBlank()) {
                                    Text("📞 ${pending.phone}", fontSize = 11.sp, color = TextSecondary)
                                }
                                if (!pending.fiqhSchool.isNullOrBlank()) {
                                    Text("📚 ${pending.fiqhSchool}", fontSize = 11.sp, color = TextSecondary)
                                }
                            }

                            if (!pending.bio.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = pending.bio,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color(0xFFE8DFD0))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.rejectImam(pending.id) {
                                            Toast.makeText(context, "Application rejected", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Reject", fontSize = 12.sp, color = Color(0xFFC0392B))
                                }

                                Button(
                                    onClick = {
                                        viewModel.approveImam(pending.id) { success ->
                                            if (success) {
                                                Toast.makeText(context, "Approved! ${pending.imamName} is now active Imam", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Approval failed", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1.5f),
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Approve Imam", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Quick Actions
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PureCream)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Administrative Actions", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onNavigateToImamPortal,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald)
                            ) {
                                Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Imam Portal", fontSize = 13.sp)
                            }

                            Button(
                                onClick = { showBroadcastDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IslamicGold)
                            ) {
                                Icon(Icons.Rounded.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Broadcast", fontSize = 13.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Home Screen Banners Management (Admin Controlled - Max 4, 35% height, 5s rotation)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PureCream),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5DACB))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.ViewCarousel, contentDescription = null, tint = DeepEmerald)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Home Screen Banners", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                            }

                            StatusPill(
                                text = "${banners.size}/4 ACTIVE",
                                bgColor = if (banners.size >= 4) BgPeachTint else BgMintTint,
                                textColor = if (banners.size >= 4) Color(0xFFC0392B) else DeepEmerald
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Promotional images rotate every 5 seconds on the home screen (covers 35% screen height). Maximum 4 banners.",
                            fontSize = 11.5.sp,
                            color = TextSecondary,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (banners.isEmpty()) {
                            Surface(
                                color = WarmAlabaster,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No banners configured yet. Tap below to upload.",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                banners.forEachIndexed { index, banner ->
                                    val resolvedUrl = if (banner.imageUrl.startsWith("http")) banner.imageUrl
                                    else ApiClient.BASE_URL.removeSuffix("/") + if (banner.imageUrl.startsWith("/")) banner.imageUrl else "/${banner.imageUrl}"

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = WarmAlabaster),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEDE4D6))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = resolvedUrl,
                                                contentDescription = banner.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = banner.title ?: "Banner #${index + 1}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Slide ${index + 1} of ${banners.size} • 5s rotation",
                                                    fontSize = 10.5.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteBanner(banner.id) { success, err ->
                                                        if (success) {
                                                            Toast.makeText(context, "Banner deleted", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, err ?: "Delete failed", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.DeleteOutline,
                                                    contentDescription = "Delete",
                                                    tint = Color(0xFFC0392B)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { showAddBannerDialog = true },
                            enabled = banners.size < 4,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepEmerald,
                                disabledContainerColor = Color(0xFFD6CEBF)
                            )
                        ) {
                            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (banners.size < 4) "+ Upload New Banner (${banners.size}/4)" else "Maximum 4 Banners Reached",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Registered Masjids Directory with Admin Delete Capability
            item {
                Text("Registered Masjids & Imams Directory", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            }

            items(imams, key = { "imam_${it.id}" }) { imam ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureCream),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5DACB))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(imam.masjidName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("${imam.imamName ?: "Imam"} • ${imam.masjidCity ?: "City"} • ${imam.fiqhSchool ?: "Sunni"}", fontSize = 12.sp, color = TextSecondary)
                            }
                            StatusPill(
                                text = if (imam.verified) "VERIFIED" else "PENDING",
                                bgColor = if (imam.verified) BgMintTint else BgPeachTint,
                                textColor = if (imam.verified) DeepEmerald else Color(0xFFC0392B)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFEFE8DC))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ID: ${imam.id.take(8)}...",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )

                            // Admin Delete Mosque & Imam Action Button
                            OutlinedButton(
                                onClick = { imamToDelete = imam },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC0392B)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE6B0AA)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete", modifier = Modifier.size(15.dp), tint = Color(0xFFC0392B))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete Mosque & Imam", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Public Islamic Library Books & Resources Section
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Islamic Library Books (${libraryBooks.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                    StatusPill(text = "PUBLIC LIBRARY", bgColor = BgGoldTint, textColor = DeepEmerald)
                }
            }

            if (libraryBooks.isEmpty()) {
                item {
                    Text("No books uploaded to the library yet.", fontSize = 12.sp, color = TextSecondary)
                }
            } else {
                items(libraryBooks, key = { "book_${it.id}" }) { book ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PureCream),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5DACB))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(book.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (!book.description.isNullOrBlank()) {
                                            Text(book.description ?: "", fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = { bookToDelete = book },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC0392B)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE6B0AA)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(14.dp), tint = Color(0xFFC0392B))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete Book", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBroadcastDialog) {
        var noticeText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBroadcastDialog = false },
            title = { Text("Global Mosque Announcement", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = noticeText,
                    onValueChange = { noticeText = it },
                    placeholder = { Text("Enter community broadcast notice...") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        Toast.makeText(context, "Global announcement broadcasted to all congregations", Toast.LENGTH_SHORT).show()
                        showBroadcastDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald)
                ) {
                    Text("Broadcast Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBroadcastDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (imamToDelete != null) {
        val target = imamToDelete!!
        AlertDialog(
            onDismissRequest = { imamToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFC0392B))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Mosque & Imam?", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Text(
                    "Are you sure you want to permanently delete \"${target.masjidName}\" and Imam \"${target.imamName}\"?\n\nAll followers will be unlinked, and this mosque will be removed from Angus Masail.",
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = target.id
                        viewModel.deleteImamOrMosque(id) { success, err ->
                            imamToDelete = null
                            if (success) {
                                Toast.makeText(context, "Mosque and Imam deleted successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, err ?: "Deletion failed", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC0392B))
                ) {
                    Text("Delete Permanently", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { imamToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = PureCream,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (bookToDelete != null) {
        val target = bookToDelete!!
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            icon = {
                Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = Color(0xFFC0392B), modifier = Modifier.size(32.dp))
            },
            title = {
                Text("Delete Book from Library?", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Text(
                    "Are you sure you want to permanently delete \"${target.title}\"?\n\nThis will remove the book and its PDF file from the public Islamic library for all users.",
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = target.id
                        bookToDelete = null
                        viewModel.deleteBookResource(id) { success, err ->
                            if (success) {
                                Toast.makeText(context, "Book deleted successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, err ?: "Deletion failed", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC0392B))
                ) {
                    Text("Delete Permanently", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = PureCream,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showAddBannerDialog) {
        AddBannerDialog(
            viewModel = viewModel,
            onDismiss = { showAddBannerDialog = false }
        )
    }
}

@Composable
fun AddBannerDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var imageUrlInput by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedUri = uri
    }

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, tint = IslamicGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload Home Banner", fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Banners display prominently on the home screen (35% height) and rotate every 5 seconds. Maximum 4 banners.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Banner Title / Headline (Optional)") },
                    placeholder = { Text("e.g. Ramadan Mubarak • Taraweeh Schedule") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Banner Image:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))

                // Option A: Pick File
                OutlinedButton(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedUri != null) "Selected: ${selectedUri?.lastPathSegment?.take(18)}..." else "Choose Image File from Device",
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("— OR —", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(8.dp))

                // Option B: Enter Web URL
                OutlinedTextField(
                    value = imageUrlInput,
                    onValueChange = { imageUrlInput = it },
                    label = { Text("Direct Image URL") },
                    placeholder = { Text("https://example.com/banner.jpg") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedUri != null) {
                        isUploading = true
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            try {
                                val contentResolver = context.contentResolver
                                val inputStream = contentResolver.openInputStream(selectedUri!!)
                                val bytes = inputStream?.readBytes() ?: ByteArray(0)
                                inputStream?.close()

                                val reqBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                                val part = MultipartBody.Part.createFormData("file", "banner_${System.currentTimeMillis()}.jpg", reqBody)

                                val res = ApiClient.api.uploadChatFile(part)
                                if (res.isSuccessful && res.body() != null) {
                                    val uploadedUrl = res.body()!!.url
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        viewModel.uploadBanner(uploadedUrl, title.ifBlank { null }) { success, err ->
                                            isUploading = false
                                            if (success) {
                                                Toast.makeText(context, "Banner added successfully!", Toast.LENGTH_SHORT).show()
                                                onDismiss()
                                            } else {
                                                Toast.makeText(context, err ?: "Upload failed", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                } else {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        isUploading = false
                                        Toast.makeText(context, "Image upload failed (${res.code()})", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    isUploading = false
                                    Toast.makeText(context, e.localizedMessage ?: "Upload error", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else if (imageUrlInput.isNotBlank()) {
                        isUploading = true
                        viewModel.uploadBanner(imageUrlInput.trim(), title.ifBlank { null }) { success, err ->
                            isUploading = false
                            if (success) {
                                Toast.makeText(context, "Banner added successfully!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, err ?: "Upload failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Please choose an image file or enter an image URL", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isUploading && (selectedUri != null || imageUrlInput.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                } else {
                    Text("Save Banner")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isUploading) {
                Text("Cancel")
            }
        },
        containerColor = PureCream,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun AdminStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureCream)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            Text(title, fontSize = 11.sp, color = TextSecondary)
        }
    }
}
