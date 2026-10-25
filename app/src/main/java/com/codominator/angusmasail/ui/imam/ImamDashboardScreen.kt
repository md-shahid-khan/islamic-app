package com.codominator.angusmasail.ui.imam

import android.widget.Toast
import java.time.LocalTime
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codominator.angusmasail.network.ChatRequestDto
import com.codominator.angusmasail.network.MasailQueryDto
import com.codominator.angusmasail.network.PrayerTimetableDto
import com.codominator.angusmasail.ui.components.GlassmorphicSurface
import com.codominator.angusmasail.ui.components.StatusPill
import com.codominator.angusmasail.ui.components.VoicePlayerWidget
import com.codominator.angusmasail.ui.components.VoiceRecorderWidget
import com.codominator.angusmasail.ui.theme.*
import com.codominator.angusmasail.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImamDashboardScreen(
    viewModel: MainViewModel? = null,
    onNavigateToChat: () -> Unit = {},
    onDisassociated: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val queries by viewModel?.imamQueries?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val chatRequests by viewModel?.imamChatRequests?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val prayerTimes by viewModel?.prayerTimes?.collectAsState() ?: remember { mutableStateOf(null) }

    // Count strictly pending queries
    val pendingQueriesCount = remember(queries) {
        queries.count { it.status.equals("PENDING", ignoreCase = true) || it.status.equals("SUBMITTED", ignoreCase = true) }
    }

    // Count strictly pending chat requests for badge
    val pendingChatRequestsCount = remember(chatRequests) {
        chatRequests.count { it.status.equals("PENDING", ignoreCase = true) }
    }

    val tabs = listOf(
        if (pendingQueriesCount > 0) "Masail ($pendingQueriesCount)" else "Masail",
        if (pendingChatRequestsCount > 0) "Chat ($pendingChatRequestsCount)" else "Chat",
        "Prayer Times"
    )

    var queryToReply by remember { mutableStateOf<MasailQueryDto?>(null) }
    var showBroadcastDialog by remember { mutableStateOf(false) }
    var showDisassociateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel?.fetchImamQueries()
        viewModel?.fetchImamChatRequests()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(WarmAlabaster, WarmSand)))
    ) {
        // Scholar Top Header
        Surface(
            color = WarmAlabaster,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(BgGoldTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Imam Scholar Portal", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        Text("Review queue & congregation schedule", fontSize = 11.sp, color = TextSecondary)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showBroadcastDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BgMintTint)
                    ) {
                        Icon(Icons.Rounded.Campaign, contentDescription = "Broadcast", tint = DeepEmerald, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { showDisassociateDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFEBEE))
                    ) {
                        Icon(Icons.Rounded.ExitToApp, contentDescription = "Step Down as Imam", tint = Color(0xFFC62828), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Active notification alert if any items are pending
        if (pendingQueriesCount > 0 || pendingChatRequestsCount > 0) {
            GlassmorphicSurface(
                backgroundColor = BgPeachTint,
                borderColor = Color(0xFFC0392B).copy(alpha = 0.35f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = Color(0xFFC0392B), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = buildString {
                                if (pendingQueriesCount > 0) append("$pendingQueriesCount pending question(s) ")
                                if (pendingQueriesCount > 0 && pendingChatRequestsCount > 0) append("• ")
                                if (pendingChatRequestsCount > 0) append("$pendingChatRequestsCount new chat request(s)")
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFC0392B)
                        )
                        Text(
                            text = "Accept to notify congregants and open live consultation.",
                            fontSize = 11.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Navigation Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = WarmAlabaster,
            contentColor = DeepEmerald,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> MasailQueueTab(
                    queries = queries,
                    onOpenReply = { queryToReply = it },
                    onAcceptQuery = { queryId ->
                        viewModel?.acceptMasailQuery(queryId) { success, err ->
                            if (success) {
                                Toast.makeText(context, "Query accepted! Congregant notified to continue.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Accept failed: ${err ?: "Check connection"}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
                1 -> ChatRequestsTab(
                    requests = chatRequests,
                    onAccept = { reqId ->
                        viewModel?.acceptChatRequest(reqId) { success ->
                            if (success) {
                                Toast.makeText(context, "Chat Request Accepted! Moved to Active Chats.", Toast.LENGTH_LONG).show()
                                viewModel.fetchImamChatRequests()
                            } else {
                                Toast.makeText(context, "Action failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onReject = { reqId ->
                        viewModel?.rejectChatRequest(reqId) { success ->
                            Toast.makeText(context, if (success) "Chat Request Rejected" else "Action failed", Toast.LENGTH_SHORT).show()
                            viewModel?.fetchImamChatRequests()
                        }
                    },
                    onOpenInChat = { reqId ->
                        viewModel?.selectChatRequest(reqId)
                        onNavigateToChat()
                    }
                )
                2 -> PrayerTimesEditTab(
                    timetable = prayerTimes,
                    onSave = { updated ->
                        viewModel?.updatePrayerTimes(updated) { success, errorMsg ->
                            val msg = if (success) "Prayer Timetable Updated & Broadcasted" else "Update failed: ${errorMsg ?: "Check connection"}"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    // Reply Dialog with Voice + Text Fatwa options
    if (queryToReply != null) {
        val query = queryToReply!!
        var replyText by remember { mutableStateOf("") }
        var isPublicFaq by remember { mutableStateOf(true) }
        var isSubmitting by remember { mutableStateOf(false) }

        var isRecordingVoice by remember { mutableStateOf(false) }
        var replyAudioUrl by remember { mutableStateOf<String?>(null) }
        var replyAudioDuration by remember { mutableIntStateOf(0) }

        AlertDialog(
            onDismissRequest = { queryToReply = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(BgGoldTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Gavel, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Provide Scholarly Fatwa", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Question Preview Box
                    Surface(
                        color = PureCream,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5DACB)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "From: ${query.userNameSnapshot ?: "Congregant"}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = DeepEmerald
                                )
                                StatusPill(text = query.tag, bgColor = BgGoldTint, textColor = DeepEmerald)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = query.questionText,
                                fontSize = 13.sp,
                                color = TextPrimary,
                                lineHeight = 18.sp
                            )

                            if (!query.questionAudioUrl.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                VoicePlayerWidget(
                                    title = "User Voice Question",
                                    audioUrl = query.questionAudioUrl,
                                    durationSec = 20,
                                    containerColor = WarmAlabaster
                                )
                            }
                        }
                    }

                    // Scholarly Ruling Input Box & Template Shortcut
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Ruling / Guidance", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TextPrimary)
                            Surface(
                                onClick = {
                                    replyText = "Based on Islamic jurisprudence, this matter is permissible under conditions of prior ritual purification and sincerity. In cases of difficulty, ease is granted according to the Sunnah. Wallahu a'lam."
                                },
                                color = BgGoldTint,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Standard Ruling", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IslamicGold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = { Text("Write scholarly ruling with evidence from Quran & Sunnah...", fontSize = 12.sp, color = TextSecondary) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 90.dp, max = 140.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = PureCream,
                                unfocusedContainerColor = PureCream,
                                focusedBorderColor = DeepEmerald,
                                unfocusedBorderColor = Color(0xFFDFD5C5)
                            )
                        )
                    }

                    // Voice Fatwa Section
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Voice Fatwa (Optional)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))

                        if (isRecordingVoice) {
                            VoiceRecorderWidget(
                                onVoiceRecorded = { url, duration ->
                                    replyAudioUrl = url
                                    replyAudioDuration = duration
                                    isRecordingVoice = false
                                    Toast.makeText(context, "Voice fatwa attached ($duration sec)", Toast.LENGTH_SHORT).show()
                                },
                                onCancel = { isRecordingVoice = false }
                            )
                        } else if (replyAudioUrl != null) {
                            VoicePlayerWidget(
                                title = "Voice Fatwa Attached",
                                audioUrl = replyAudioUrl,
                                durationSec = replyAudioDuration,
                                containerColor = BgMintTint,
                                onDelete = {
                                    replyAudioUrl = null
                                    replyAudioDuration = 0
                                }
                            )
                        } else {
                            OutlinedButton(
                                onClick = { isRecordingVoice = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepEmerald),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Record Voice Fatwa", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // Public FAQ Checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPublicFaq = !isPublicFaq }
                            .padding(vertical = 2.dp)
                    ) {
                        Checkbox(
                            checked = isPublicFaq,
                            onCheckedChange = { isPublicFaq = it },
                            colors = CheckboxDefaults.colors(checkedColor = DeepEmerald)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add to Masjid Public FAQ Library", fontSize = 12.sp, color = TextPrimary)
                    }
                }
            },
            confirmButton = {
                val canSubmit = replyText.isNotBlank() || replyAudioUrl != null
                Button(
                    onClick = {
                        if (canSubmit) {
                            isSubmitting = true
                            val finalReply = if (replyText.isNotBlank()) replyText else "Scholarly audio guidance provided via voice note."
                            viewModel?.resolveImamQuery(query.id, finalReply, replyAudioUrl, isPublicFaq) { success ->
                                isSubmitting = false
                                if (success) {
                                    Toast.makeText(context, "Query resolved and answer delivered to congregant", Toast.LENGTH_SHORT).show()
                                    queryToReply = null
                                } else {
                                    Toast.makeText(context, "Failed to resolve. Check network.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(42.dp),
                    enabled = !isSubmitting && canSubmit
                ) {
                    Text("Deliver Fatwa", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { queryToReply = null },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Text("Cancel", fontSize = 13.sp)
                }
            }
        )
    }

    // Broadcast Dialog
    
    if (showDisassociateDialog) {
        AlertDialog(
            onDismissRequest = { showDisassociateDialog = false },
            title = {
                Text("Step Down as Imam?", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Text(
                    "Are you sure you want to disassociate from this mosque? You will step down as the active Imam and your account will revert to a standard congregant (User). The mosque will remain registered in the directory for a future Imam.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDisassociateDialog = false
                        viewModel?.disassociateImam(
                            onSuccess = {
                                Toast.makeText(context, "Stepped down successfully. Switched to congregant account.", Toast.LENGTH_LONG).show()
                                onDisassociated()
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Step Down", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDisassociateDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = PureCream,
            shape = RoundedCornerShape(18.dp)
        )
    }

    if (showBroadcastDialog) {
        var announcement by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBroadcastDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Campaign, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Broadcast 24h Notice", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text("Send high-priority notice to all registered congregation members:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = announcement,
                        onValueChange = { announcement = it },
                        placeholder = { Text("e.g. Jumu'ah Jamaat will commence 10 minutes early this week...", fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp, max = 130.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = PureCream,
                            unfocusedContainerColor = PureCream,
                            focusedBorderColor = DeepEmerald,
                            unfocusedBorderColor = Color(0xFFDFD5C5)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Toast.makeText(context, "Announcement broadcasted to congregation", Toast.LENGTH_SHORT).show()
                        showBroadcastDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Text("Broadcast", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showBroadcastDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Text("Cancel", fontSize = 13.sp)
                }
            }
        )
    }
}

@Composable
fun MasailQueueTab(
    queries: List<MasailQueryDto>,
    onOpenReply: (MasailQueryDto) -> Unit,
    onAcceptQuery: (String) -> Unit
) {
    var filterStatus by remember { mutableStateOf("ALL") }

    val filteredList = remember(queries, filterStatus) {
        when (filterStatus) {
            "PENDING" -> queries.filter { it.status.equals("PENDING", ignoreCase = true) || it.status.equals("SUBMITTED", ignoreCase = true) }
            "ACCEPTED" -> queries.filter { it.status.equals("ACCEPTED", ignoreCase = true) }
            "RESOLVED" -> queries.filter { it.status.equals("RESOLVED", ignoreCase = true) || !it.imamReplyText.isNullOrBlank() }
            else -> queries
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL", "PENDING", "ACCEPTED", "RESOLVED").forEach { status ->
                val isSelected = filterStatus == status
                FilterChip(
                    selected = isSelected,
                    onClick = { filterStatus = status },
                    label = { Text(status, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DeepEmerald,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (filteredList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Inbox, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No queries found in $filterStatus queue", color = TextSecondary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList, key = { it.id }) { query ->
                    val isPending = query.status.equals("PENDING", ignoreCase = true) || query.status.equals("SUBMITTED", ignoreCase = true)
                    val isAccepted = query.status.equals("ACCEPTED", ignoreCase = true)
                    val isResolved = query.status.equals("RESOLVED", ignoreCase = true) || !query.imamReplyText.isNullOrBlank()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PureCream),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (isPending) Color(0xFFC0392B).copy(alpha = 0.35f) else if (isAccepted) DeepEmerald.copy(alpha = 0.45f) else Color(0xFFE5DACB)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Header Row: Category Tag, Congregant Name, and Status Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f, fill = false),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatusPill(text = query.tag, bgColor = BgGoldTint, textColor = DeepEmerald)
                                    Text(
                                        text = query.userNameSnapshot ?: "Congregant",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                StatusPill(
                                    text = when {
                                        isResolved -> "RESOLVED"
                                        isAccepted -> "ACCEPTED"
                                        else -> "PENDING"
                                    },
                                    bgColor = when {
                                        isResolved -> BgMintTint
                                        isAccepted -> DeepEmerald
                                        else -> BgPeachTint
                                    },
                                    textColor = when {
                                        isResolved -> DeepEmerald
                                        isAccepted -> Color.White
                                        else -> Color(0xFFC0392B)
                                    }
                                )
                            }

                            // Congregant details row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!query.localitySnapshot.isNullOrBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Place, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(query.localitySnapshot, fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                                if (!query.userContactSnapshot.isNullOrBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Phone, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(query.userContactSnapshot, fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                                if (!query.manhajSnapshot.isNullOrBlank()) {
                                    Surface(
                                        color = BgGoldTint,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            query.manhajSnapshot,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepEmerald,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = query.questionText,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )

                            if (!query.questionAudioUrl.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                VoicePlayerWidget(
                                    title = "User Voice Question",
                                    audioUrl = query.questionAudioUrl,
                                    durationSec = 20,
                                    containerColor = WarmAlabaster
                                )
                            }

                            // If accepted but not yet answered, display status banner
                            if (isAccepted && !isResolved) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(BgMintTint, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = DeepEmerald,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Query Accepted — Ready to provide scholarly fatwa/answer",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = DeepEmerald
                                    )
                                }
                            }

                            // If resolved, display answer
                            if (!query.imamReplyText.isNullOrBlank() || !query.imamReplyAudioUrl.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                GlassmorphicSurface(
                                    backgroundColor = BgMintTint,
                                    borderColor = DeepEmerald.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Answer Delivered:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepEmerald)
                                        if (!query.imamReplyText.isNullOrBlank()) {
                                            Text(query.imamReplyText, fontSize = 13.sp, color = TextPrimary)
                                        }
                                        if (!query.imamReplyAudioUrl.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            VoicePlayerWidget(
                                                title = "Recorded Voice Fatwa",
                                                audioUrl = query.imamReplyAudioUrl,
                                                durationSec = 30,
                                                containerColor = PureCream
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action Buttons Row (Unified 42dp height)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isPending) {
                                    OutlinedButton(
                                        onClick = { onAcceptQuery(query.id) },
                                        modifier = Modifier.weight(1f).height(42.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepEmerald),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald)
                                    ) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Accept", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = { onOpenReply(query) },
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Rounded.QuestionAnswer, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isResolved) "Update Answer" else "Provide Answer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatRequestsTab(
    requests: List<ChatRequestDto>,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
    onOpenInChat: (String) -> Unit
) {
    var filterTab by remember { mutableStateOf("PENDING") }

    val pendingList = remember(requests) {
        requests.filter { it.status.equals("PENDING", ignoreCase = true) }
    }

    val acceptedList = remember(requests) {
        requests.filter { it.status.equals("ACCEPTED", ignoreCase = true) || it.status.equals("ACTIVE", ignoreCase = true) }
    }

    val closedList = remember(requests) {
        requests.filter { it.status.equals("CLOSED", ignoreCase = true) || it.status.equals("REJECTED", ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterTab == "PENDING",
                onClick = { filterTab = "PENDING" },
                label = { Text("Pending (${pendingList.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DeepEmerald,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = filterTab == "ACCEPTED",
                onClick = { filterTab = "ACCEPTED" },
                label = { Text("Active Chats (${acceptedList.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DeepEmerald,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = filterTab == "CLOSED",
                onClick = { filterTab = "CLOSED" },
                label = { Text("History (${closedList.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DeepEmerald,
                    selectedLabelColor = Color.White
                )
            )
        }

        val displayList = when (filterTab) {
            "PENDING" -> pendingList
            "ACCEPTED" -> acceptedList
            else -> closedList
        }

        if (displayList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (filterTab == "PENDING") Icons.Rounded.DoneAll else Icons.Rounded.Forum,
                        contentDescription = null,
                        tint = DeepEmerald,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (filterTab == "PENDING") "All consultation requests reviewed!" else "No consultation sessions found in $filterTab.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayList, key = { it.id }) { req ->
                    val isPending = req.status.equals("PENDING", ignoreCase = true)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PureCream),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Header: Congregant avatar, name, and status pill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f, fill = false),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(36.dp).clip(CircleShape).background(BgGoldTint),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.Person, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = req.userName ?: "Congregant Member",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Congregant User",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                StatusPill(
                                    text = req.status,
                                    bgColor = if (isPending) BgPeachTint else BgMintTint,
                                    textColor = if (isPending) Color(0xFFC0392B) else DeepEmerald
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Contact info snapshots (locality, phone, manhaj)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!req.userLocality.isNullOrBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Place, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(req.userLocality, fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                                if (!req.userPhone.isNullOrBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Phone, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(req.userPhone, fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                                if (!req.userManhaj.isNullOrBlank()) {
                                    Surface(
                                        color = BgGoldTint,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            req.userManhaj,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepEmerald,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Consultation Reason Box
                            Surface(
                                color = WarmAlabaster,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Consultation Reason:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepEmerald
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = req.initialReason,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        color = TextPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (isPending) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { onReject(req.id) },
                                        modifier = Modifier.weight(1f).height(42.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC0392B)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC0392B).copy(alpha = 0.5f))
                                    ) {
                                        Text("Decline", fontSize = 12.sp, color = Color(0xFFC0392B), fontWeight = FontWeight.SemiBold)
                                    }
                                    Button(
                                        onClick = { onAccept(req.id) },
                                        modifier = Modifier.weight(1f).height(42.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Accept Chat", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { onOpenInChat(req.id) },
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Rounded.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open 1-on-1 Chat Session", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun parseToLocalTime(str: String?): LocalTime? {
    if (str.isNullOrBlank()) return null
    val trimmed = str.trim()
    return try {
        LocalTime.parse(trimmed)
    } catch (e: Exception) {
        try {
            val parts = trimmed.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: return null
            val m = parts.getOrNull(1)?.toIntOrNull() ?: return null
            LocalTime.of(h % 24, m % 60)
        } catch (e2: Exception) {
            null
        }
    }
}

@Composable
fun PrayerTimesEditTab(
    timetable: PrayerTimetableDto?,
    onSave: (PrayerTimetableDto) -> Unit
) {
    val context = LocalContext.current

    // Official start and end windows from API
    val fajrStart = timetable?.fajrStartTime ?: "04:30:00"
    val fajrEnd = timetable?.fajrEndTime ?: (timetable?.sunrise ?: "06:15:00")

    val dhuhrStart = timetable?.dhuhrStartTime ?: "12:30:00"
    val dhuhrEnd = timetable?.dhuhrEndTime ?: "16:15:00"

    val asrStart = timetable?.asrStartTime ?: "16:15:00"
    val asrEnd = timetable?.asrEndTime ?: "18:45:00"

    val maghribStart = timetable?.maghribStartTime ?: "18:45:00"
    val maghribEnd = timetable?.maghribEndTime ?: "20:00:00"

    val ishaStart = timetable?.ishaStartTime ?: "20:00:00"
    val ishaEnd = timetable?.ishaEndTime ?: "04:30:00"

    val jumuahStart = timetable?.jumuahStartTime ?: "12:30:00"
    val jumuahEnd = timetable?.jumuahEndTime ?: "16:15:00"

    var fajrAzan by remember(timetable) { mutableStateOf(timetable?.fajrAzan ?: "05:00:00") }
    var fajrJamaat by remember(timetable) { mutableStateOf(timetable?.fajrJamaat ?: "05:30:00") }
    var sunrise by remember(timetable) { mutableStateOf(timetable?.sunrise ?: "06:15:00") }
    var dhuhrAzan by remember(timetable) { mutableStateOf(timetable?.dhuhrAzan ?: "12:30:00") }
    var dhuhrJamaat by remember(timetable) { mutableStateOf(timetable?.dhuhrJamaat ?: "13:15:00") }
    var asrAzan by remember(timetable) { mutableStateOf(timetable?.asrAzan ?: "16:15:00") }
    var asrJamaat by remember(timetable) { mutableStateOf(timetable?.asrJamaat ?: "16:45:00") }
    var maghribAzan by remember(timetable) { mutableStateOf(timetable?.maghribAzan ?: "18:45:00") }
    var maghribJamaat by remember(timetable) { mutableStateOf(timetable?.maghribJamaat ?: "18:55:00") }
    var ishaAzan by remember(timetable) { mutableStateOf(timetable?.ishaAzan ?: "20:00:00") }
    var ishaJamaat by remember(timetable) { mutableStateOf(timetable?.ishaJamaat ?: "20:30:00") }
    var jumuahAzan by remember(timetable) { mutableStateOf(timetable?.jumuahAzan ?: "12:30:00") }
    var jumuahJamaat by remember(timetable) { mutableStateOf(timetable?.jumuahJamaat ?: "13:30:00") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = PureCream),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Today's Congregation Schedule", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                Text("Times are broadcasted instantly to all congregants.", fontSize = 11.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(14.dp))

                // Table Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WarmAlabaster, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Prayer & Window", modifier = Modifier.weight(1.2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepEmerald)
                    Text("Azan Time", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepEmerald, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Jamaat Time", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepEmerald, textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(8.dp))

                PrayerInputRow("Fajr", fajrStart, fajrEnd, fajrAzan, { fajrAzan = it }, fajrJamaat, { fajrJamaat = it })
                PrayerInputRow("Dhuhr", dhuhrStart, dhuhrEnd, dhuhrAzan, { dhuhrAzan = it }, dhuhrJamaat, { dhuhrJamaat = it })
                PrayerInputRow("Asr", asrStart, asrEnd, asrAzan, { asrAzan = it }, asrJamaat, { asrJamaat = it })
                PrayerInputRow("Maghrib", maghribStart, maghribEnd, maghribAzan, { maghribAzan = it }, maghribJamaat, { maghribJamaat = it })
                PrayerInputRow("Isha", ishaStart, ishaEnd, ishaAzan, { ishaAzan = it }, ishaJamaat, { ishaJamaat = it })
                PrayerInputRow("Jumu'ah", jumuahStart, jumuahEnd, jumuahAzan, { jumuahAzan = it }, jumuahJamaat, { jumuahJamaat = it })
            }
        }

        fun sanitizeTime(t: String): String {
            val trimmed = t.trim()
            return if (trimmed.matches(Regex("^\\d{1,2}:\\d{2}$"))) "$trimmed:00" else trimmed
        }

        fun validateRow(name: String, azan: String, jamaat: String, winStart: String, winEnd: String): String? {
            val s = parseToLocalTime(winStart) ?: return null
            val e = parseToLocalTime(winEnd) ?: return null
            val az = parseToLocalTime(azan) ?: return "$name Azan time format is invalid (HH:mm)"
            val jm = parseToLocalTime(jamaat) ?: return "$name Jamaat time format is invalid (HH:mm)"

            val azValid = if (e.isAfter(s)) (!az.isBefore(s) && !az.isAfter(e)) else (!az.isBefore(s) || !az.isAfter(e))
            if (!azValid) return "$name Azan must be within prayer window (${winStart.take(5)} - ${winEnd.take(5)})"

            val jmValid = if (e.isAfter(s)) (!jm.isBefore(s) && !jm.isAfter(e)) else (!jm.isBefore(s) || !jm.isAfter(e))
            if (!jmValid) return "$name Jamaat must be within prayer window (${winStart.take(5)} - ${winEnd.take(5)})"

            if (e.isAfter(s) && jm.isBefore(az)) {
                return "$name Jamaat cannot be earlier than Azan"
            }
            return null
        }

        Button(
            onClick = {
                val validationErr = validateRow("Fajr", fajrAzan, fajrJamaat, fajrStart, fajrEnd)
                    ?: validateRow("Dhuhr", dhuhrAzan, dhuhrJamaat, dhuhrStart, dhuhrEnd)
                    ?: validateRow("Asr", asrAzan, asrJamaat, asrStart, asrEnd)
                    ?: validateRow("Maghrib", maghribAzan, maghribJamaat, maghribStart, maghribEnd)
                    ?: validateRow("Isha", ishaAzan, ishaJamaat, ishaStart, ishaEnd)
                    ?: validateRow("Jumu'ah", jumuahAzan, jumuahJamaat, jumuahStart, jumuahEnd)

                if (validationErr != null) {
                    Toast.makeText(context, validationErr, Toast.LENGTH_LONG).show()
                    return@Button
                }

                val updated = PrayerTimetableDto(
                    id = timetable?.id,
                    imamProfileId = timetable?.imamProfileId,
                    date = timetable?.date,
                    fajrStartTime = sanitizeTime(fajrStart),
                    fajrEndTime = sanitizeTime(fajrEnd),
                    fajrAzan = sanitizeTime(fajrAzan),
                    fajrJamaat = sanitizeTime(fajrJamaat),
                    sunriseStartTime = sanitizeTime(fajrEnd),
                    sunriseEndTime = sanitizeTime(dhuhrStart),
                    sunrise = sanitizeTime(sunrise),
                    dhuhrStartTime = sanitizeTime(dhuhrStart),
                    dhuhrEndTime = sanitizeTime(dhuhrEnd),
                    dhuhrAzan = sanitizeTime(dhuhrAzan),
                    dhuhrJamaat = sanitizeTime(dhuhrJamaat),
                    asrStartTime = sanitizeTime(asrStart),
                    asrEndTime = sanitizeTime(asrEnd),
                    asrAzan = sanitizeTime(asrAzan),
                    asrJamaat = sanitizeTime(asrJamaat),
                    maghribStartTime = sanitizeTime(maghribStart),
                    maghribEndTime = sanitizeTime(maghribEnd),
                    maghribAzan = sanitizeTime(maghribAzan),
                    maghribJamaat = sanitizeTime(maghribJamaat),
                    ishaStartTime = sanitizeTime(ishaStart),
                    ishaEndTime = sanitizeTime(ishaEnd),
                    ishaAzan = sanitizeTime(ishaAzan),
                    ishaJamaat = sanitizeTime(ishaJamaat),
                    jumuahStartTime = sanitizeTime(jumuahStart),
                    jumuahEndTime = sanitizeTime(jumuahEnd),
                    jumuahAzan = sanitizeTime(jumuahAzan),
                    jumuahJamaat = sanitizeTime(jumuahJamaat),
                    hijriOffsetDays = timetable?.hijriOffsetDays ?: 0
                )
                onSave(updated)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald)
        ) {
            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save & Broadcast Timetable", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun PrayerInputRow(
    title: String,
    windowStart: String,
    windowEnd: String,
    azanVal: String,
    onAzanChange: (String) -> Unit,
    jamaatVal: String,
    onJamaatChange: (String) -> Unit
) {
    val s = parseToLocalTime(windowStart)
    val e = parseToLocalTime(windowEnd)
    val az = parseToLocalTime(azanVal)
    val jm = parseToLocalTime(jamaatVal)

    val cleanWinStart = windowStart.take(5)
    val cleanWinEnd = windowEnd.take(5)

    var warning: String? = null
    if (s != null && e != null) {
        if (az != null) {
            val valid = if (e.isAfter(s)) (!az.isBefore(s) && !az.isAfter(e)) else (!az.isBefore(s) || !az.isAfter(e))
            if (!valid) warning = "Azan must be between $cleanWinStart–$cleanWinEnd"
        }
        if (warning == null && jm != null) {
            val valid = if (e.isAfter(s)) (!jm.isBefore(s) && !jm.isAfter(e)) else (!jm.isBefore(s) || !jm.isAfter(e))
            if (!valid) warning = "Jamaat must be between $cleanWinStart–$cleanWinEnd"
        }
        if (warning == null && az != null && jm != null && e.isAfter(s) && jm.isBefore(az)) {
            warning = "Jamaat cannot be earlier than Azan"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "$cleanWinStart – $cleanWinEnd",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }

            OutlinedTextField(
                value = azanVal,
                onValueChange = onAzanChange,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                textStyle = TextStyle(fontSize = 12.sp, textAlign = TextAlign.Center),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = WarmAlabaster,
                    unfocusedContainerColor = WarmAlabaster,
                    focusedBorderColor = if (warning != null) Color(0xFFBA1A1A) else DeepEmerald,
                    unfocusedBorderColor = if (warning != null) Color(0xFFFFB4AB) else Color(0xFFDFD5C5)
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            OutlinedTextField(
                value = jamaatVal,
                onValueChange = onJamaatChange,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                textStyle = TextStyle(fontSize = 12.sp, textAlign = TextAlign.Center),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = WarmAlabaster,
                    unfocusedContainerColor = WarmAlabaster,
                    focusedBorderColor = if (warning != null) Color(0xFFBA1A1A) else DeepEmerald,
                    unfocusedBorderColor = if (warning != null) Color(0xFFFFB4AB) else Color(0xFFDFD5C5)
                )
            )
        }

        if (warning != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "⚠ $warning",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFBA1A1A),
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }
}
