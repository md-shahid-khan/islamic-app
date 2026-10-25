package com.codominator.angusmasail.ui.imam

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codominator.angusmasail.network.ImamProfileDto
import com.codominator.angusmasail.ui.components.GlassmorphicSurface
import com.codominator.angusmasail.ui.components.bouncingClickable
import com.codominator.angusmasail.ui.theme.*
import com.codominator.angusmasail.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailableImamsScreen(
    viewModel: MainViewModel,
    onNavigateToMasail: () -> Unit,
    onNavigateToChat: () -> Unit,
    onRegisterAsImam: () -> Unit = {},
    onNavigateToImamDashboard: () -> Unit = {}
) {
    val context = LocalContext.current
    val imams by viewModel.imams.collectAsState()
    val selectedImam by viewModel.selectedImam.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val userRole by viewModel.userRole.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isCheckingStatus by remember { mutableStateOf(false) }

    val isImamPending = currentUser?.isImamPending == true
    val isImamApproved = userRole == "ROLE_IMAM" || currentUser?.isImamVerified == true

    val filteredImams = remember(imams, searchQuery) {
        if (searchQuery.isBlank()) imams
        else imams.filter {
            (it.imamName?.contains(searchQuery, ignoreCase = true) == true) ||
            (it.masjidName.contains(searchQuery, ignoreCase = true)) ||
            (it.fiqhSchool?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BgGoldTint)
                                .border(1.dp, IslamicGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.People, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Available Scholars & Imams", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                            Text("Verified religious leadership", fontSize = 11.sp, color = TextSecondary)
                        }
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Header Banner: Shows Live Status if applied, Portal if Imam, or Register prompt if not yet applied
            when {
                isImamPending -> {
                    // 1. User has applied and is awaiting Admin Approval: SHOW STATUS (never ask "Are you an Imam?")
                    GlassmorphicSurface(
                        backgroundColor = BgGoldTint,
                        borderColor = IslamicGold,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(IslamicGold.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.HourglassTop,
                                        contentDescription = null,
                                        tint = DeepEmerald,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Imam Status: Pending Review",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${currentUser?.selectedMasjidName ?: "Your mosque"} is under admin review",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (!isCheckingStatus) {
                                        isCheckingStatus = true
                                        viewModel.checkMyImamStatus { hasApp, isApproved ->
                                            isCheckingStatus = false
                                            if (isApproved) {
                                                Toast.makeText(context, "Approved! Welcome to the Imam Portal.", Toast.LENGTH_LONG).show()
                                                onNavigateToImamDashboard()
                                            } else {
                                                Toast.makeText(context, "Your application is currently under review by Admin.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp),
                                shape = RoundedCornerShape(10.dp),
                                enabled = !isCheckingStatus
                            ) {
                                if (isCheckingStatus) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                                } else {
                                    Text(
                                        text = "Check Status",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
                isImamApproved -> {
                    // 2. User is already an active verified Imam: SHOW PORTAL SHORTCUT (never ask "Are you an Imam?")
                    GlassmorphicSurface(
                        backgroundColor = BgMintTint,
                        borderColor = DeepEmerald.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(DeepEmerald.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VerifiedUser,
                                        contentDescription = null,
                                        tint = DeepEmerald,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Verified Imam Scholar",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = DeepEmerald
                                    )
                                    Text(
                                        text = currentUser?.selectedMasjidName ?: "Imam Portal Active",
                                        fontSize = 11.sp,
                                        color = TextPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onNavigateToImamDashboard,
                                colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "Imam Portal",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
                userRole != "ROLE_ADMIN" -> {
                    // 3. User is a regular Congregant who has not applied: SHOW REGISTRATION BANNER
                    GlassmorphicSurface(
                        backgroundColor = DeepEmerald,
                        borderColor = IslamicGold.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(IslamicGold.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AccountBalance,
                                        contentDescription = null,
                                        tint = IslamicGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Are you an Imam?",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Register your masjid & timetable",
                                        fontSize = 11.sp,
                                        color = WarmSand
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onRegisterAsImam,
                                colors = ButtonDefaults.buttonColors(containerColor = IslamicGold),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "Register",
                                    color = DeepEmerald,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by scholar name, masjid or fiqh...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = TextSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PureCream,
                    unfocusedContainerColor = PureCream,
                    focusedBorderColor = IslamicGold,
                    unfocusedBorderColor = Color(0xFFE2D9C8)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredImams.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.PersonSearch, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No scholars found matching search", color = TextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(filteredImams, key = { it.id }) { imam ->
                        val isSelected = selectedImam?.id == imam.id
                        ImamScholarCard(
                            imam = imam,
                            isSelected = isSelected,
                            userRole = userRole,
                            onSelect = {
                                viewModel.selectImam(imam)
                                Toast.makeText(context, "${imam.imamName ?: "Imam"} selected as your primary scholar", Toast.LENGTH_SHORT).show()
                            },
                            onAskQuestion = {
                                viewModel.selectImam(imam)
                                onNavigateToMasail()
                            },
                            onConsultChat = {
                                viewModel.openDirectChatWithImam(imam)
                                onNavigateToChat()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImamScholarCard(
    imam: ImamProfileDto,
    isSelected: Boolean,
    userRole: String = "ROLE_USER",
    onSelect: () -> Unit,
    onAskQuestion: () -> Unit,
    onConsultChat: () -> Unit
) {
    val isImamUser = userRole == "ROLE_IMAM"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) BgGoldTint else PureCream),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, IslamicGold) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Avatar, Name, Verified, and Preferred Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(DeepEmerald),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = imam.imamName ?: "Scholar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = "Verified Scholar",
                            tint = DeepEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = imam.masjidName,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "Jurisprudence: ${imam.fiqhSchool ?: "Sunni Hanbali/Hanafi"}",
                        fontSize = 11.sp,
                        color = IslamicGold
                    )
                }

                if (isSelected) {
                    Surface(
                        color = DeepEmerald,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "MY IMAM",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (!imam.bio.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = imam.bio,
                    fontSize = 12.sp,
                    color = TextPrimary.copy(alpha = 0.85f),
                    lineHeight = 18.sp,
                    maxLines = 3
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status indicator pill
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(LiveEmerald)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Accepting Masail Questions & Voice Consultations",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = DeepEmerald
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons (Single line, no clipping or text wrap)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isImamUser) {
                    // Ask Question Button (Only for Congregants, NOT for Imams)
                    Button(
                        onClick = onAskQuestion,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Rounded.QuestionAnswer, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ask Masail",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Consult in Chat Button
                OutlinedButton(
                    onClick = onConsultChat,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepEmerald),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Rounded.Forum, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isImamUser) "Scholar Chat" else "Direct Chat",
                        fontSize = 11.5.sp,
                        color = DeepEmerald,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                if (!isSelected && !isImamUser) {
                    IconButton(
                        onClick = onSelect,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BgMintTint)
                    ) {
                        Icon(Icons.Rounded.BookmarkAdd, contentDescription = "Set as My Imam", tint = DeepEmerald, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
