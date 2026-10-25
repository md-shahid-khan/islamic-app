package com.codominator.angusmasail.ui.chat

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.codominator.angusmasail.network.ChatMessageDto
import com.codominator.angusmasail.network.ChatRequestDto
import com.codominator.angusmasail.ui.components.GlassmorphicSurface
import com.codominator.angusmasail.ui.components.VoicePlayerWidget
import com.codominator.angusmasail.ui.components.VoiceRecorderWidget
import com.codominator.angusmasail.ui.theme.*
import com.codominator.angusmasail.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private fun formatChatTime(isoString: String?): String {
    if (isoString.isNullOrBlank()) return LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
    return try {
        val ldt = LocalDateTime.parse(isoString)
        ldt.format(DateTimeFormatter.ofPattern("hh:mm a"))
    } catch (e: Exception) {
        try {
            val odt = java.time.OffsetDateTime.parse(isoString)
            odt.format(DateTimeFormatter.ofPattern("hh:mm a"))
        } catch (e2: Exception) {
            isoString.substringAfter("T").take(5)
        }
    }
}

data class ChatMessageItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isFromMe: Boolean,
    val senderName: String? = null,
    val audioUrl: String? = null,
    val audioDurationSec: Int = 0,
    val mediaUrl: String? = null,
    val fileName: String? = null,
    val fileType: String? = null, // "IMAGE", "PDF", "VIDEO", "AUDIO", "FILE"
    val fileSize: Long = 0L,
    val timestamp: String = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")),
    val isDelivered: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    imamName: String = "Imam Syed Ahmed",
    viewModel: MainViewModel? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    var isRecordingAudio by remember { mutableStateOf(false) }
    var showAttachmentDialog by remember { mutableStateOf(false) }
    var isUploadingAttachment by remember { mutableStateOf(false) }
    var uploadStatusMessage by remember { mutableStateOf<String?>(null) }

    val userRole by viewModel?.userRole?.collectAsState() ?: remember { mutableStateOf("ROLE_USER") }
    val currentUser by viewModel?.currentUser?.collectAsState() ?: remember { mutableStateOf(null) }
    val selectedImam by viewModel?.selectedImam?.collectAsState() ?: remember { mutableStateOf(null) }
    val isImam = userRole == "ROLE_IMAM"

    val userRequests by viewModel?.userChatRequests?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val imamRequests by viewModel?.imamChatRequests?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val selectedChatRequestId by viewModel?.selectedChatRequestId?.collectAsState() ?: remember { mutableStateOf(null) }
    val backendMessages by viewModel?.activeChatMessages?.collectAsState() ?: remember { mutableStateOf(emptyList()) }

    val currentChatSession = remember(isImam, selectedChatRequestId, selectedImam, userRequests, imamRequests) {
        if (isImam) {
            imamRequests.find { it.id == selectedChatRequestId }
                ?: imamRequests.firstOrNull { it.status.equals("ACCEPTED", ignoreCase = true) }
                ?: imamRequests.firstOrNull()
        } else {
            // Strictly match the selected Imam first!
            if (selectedImam != null) {
                userRequests.find { it.imamProfileId == selectedImam?.id }
            } else if (selectedChatRequestId != null) {
                userRequests.find { it.id == selectedChatRequestId }
            } else {
                userRequests.firstOrNull()
            }
        }
    }

    val isPending = currentChatSession?.status.equals("PENDING", ignoreCase = true)
    val isAccepted = currentChatSession?.status.equals("ACCEPTED", ignoreCase = true) || currentChatSession?.status.equals("ACTIVE", ignoreCase = true)
    val isRejected = currentChatSession?.status.equals("REJECTED", ignoreCase = true)
    val isNewConsultation = currentChatSession == null

    val sessionTitle = if (isImam) {
        currentChatSession?.userName ?: "Congregant Member"
    } else {
        selectedImam?.imamName ?: currentChatSession?.imamName ?: imamName
    }

    val sessionSubtitle = if (isImam) {
        val loc = currentChatSession?.userLocality
        val ph = currentChatSession?.userPhone
        when {
            !loc.isNullOrBlank() && !ph.isNullOrBlank() -> "$loc • $ph"
            !loc.isNullOrBlank() -> loc
            !ph.isNullOrBlank() -> ph
            else -> "Direct 1-on-1 Consultation"
        }
    } else {
        selectedImam?.masjidName ?: "Verified Scholar • Direct Consultation"
    }

    LaunchedEffect(Unit) {
        viewModel?.fetchUserChatRequests()
        viewModel?.fetchImamChatRequests()
    }

    // Local messages cache for optimistic immediate responsiveness
    val localMessages = remember { mutableStateListOf<ChatMessageItem>() }

    // Polling effect: while viewing this chat session, refresh messages & session state every 2.5s
    // so two instances of the app seamlessly receive messages and acceptance updates in real-time.
    LaunchedEffect(currentChatSession?.id) {
        localMessages.clear()
        val sid = currentChatSession?.id
        if (sid != null) {
            viewModel?.selectChatRequest(sid)
            while (isActive) {
                viewModel?.fetchChatMessages(sid)
                if (isImam) {
                    viewModel?.fetchImamChatRequests()
                } else {
                    viewModel?.fetchUserChatRequests()
                }
                delay(2500L)
            }
        }
    }

    // Deduplicate: compute pendingLocalMessages by removing any local message that is already confirmed in backendMessages
    val pendingLocalMessages = remember(localMessages.toList(), backendMessages) {
        val remaining = localMessages.toMutableList()
        for (backend in backendMessages) {
            val matchIndex = remaining.indexOfFirst { local ->
                local.isFromMe && (
                    local.text.trim().equals(backend.messageText.trim(), ignoreCase = true) ||
                    (!local.mediaUrl.isNullOrBlank() && local.mediaUrl == backend.mediaUrl)
                )
            }
            if (matchIndex != -1) {
                remaining.removeAt(matchIndex)
            }
        }
        remaining
    }

    // Smooth auto-scroll when new messages arrive
    val totalCount = backendMessages.size + pendingLocalMessages.size
    LaunchedEffect(totalCount) {
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount - 1)
        }
    }

    // Attachment file picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        val contentResolver = context.contentResolver
        var fileName = "attachment_${System.currentTimeMillis()}"
        var fileSize = -1L

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIdx != -1) fileName = cursor.getString(nameIdx) ?: fileName
                if (sizeIdx != -1) fileSize = cursor.getLong(sizeIdx)
            }
        }

        // Limit size validations: Max 15 MB, Min > 0 bytes
        val maxSizeBytes = 15 * 1024 * 1024L
        if (fileSize > maxSizeBytes) {
            val sizeMb = fileSize / (1024 * 1024)
            Toast.makeText(context, "File size (${sizeMb} MB) exceeds 15 MB limit. Please select a smaller file.", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }
        if (fileSize == 0L) {
            Toast.makeText(context, "Selected file is empty (0 bytes).", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }

        isUploadingAttachment = true
        uploadStatusMessage = "Uploading $fileName..."

        viewModel?.uploadChatAttachment(uri) { success, resp, error ->
            isUploadingAttachment = false
            uploadStatusMessage = null
            if (success && resp != null) {
                val typeTag = when (resp.fileType) {
                    "IMAGE" -> "[Image]"
                    "PDF" -> "[PDF]"
                    "VIDEO" -> "[Video]"
                    else -> "[File]"
                }
                val caption = if (messageText.isNotBlank()) messageText.trim() else "$typeTag ${resp.fileName}"
                messageText = ""

                localMessages.add(
                    ChatMessageItem(
                        text = caption,
                        isFromMe = true,
                        mediaUrl = resp.url,
                        fileName = resp.fileName,
                        fileType = resp.fileType,
                        fileSize = resp.fileSize
                    )
                )

                if (isAccepted) {
                    val activeId = currentChatSession?.id ?: selectedChatRequestId
                    if (activeId != null && viewModel != null) {
                        viewModel.sendChatMessage(activeId, caption, resp.url) { }
                    }
                } else if (isNewConsultation) {
                    viewModel?.requestChatSession(caption, targetImamProfileId = selectedImam?.id, mediaUrl = resp.url) { _, _ -> }
                }
                Toast.makeText(context, "Attachment sent successfully", Toast.LENGTH_SHORT).show()
                coroutineScope.launch {
                    listState.animateScrollToItem(localMessages.size + backendMessages.size)
                }
            } else {
                Toast.makeText(context, error ?: "Upload failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(WarmAlabaster, WarmSand)))
            .imePadding()
    ) {
        // 1. TOP APP BAR
        Surface(
            color = WarmAlabaster,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(BgGoldTint)
                        .border(1.5.dp, IslamicGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isImam) Icons.Rounded.People else Icons.Rounded.Person,
                        contentDescription = null,
                        tint = DeepEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = sessionTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isImam && !currentChatSession?.userManhaj.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = BgGoldTint,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    currentChatSession.userManhaj!!,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepEmerald,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isAccepted -> LiveEmerald
                                        isPending -> IslamicGold
                                        isRejected -> Color(0xFFC0392B)
                                        else -> LiveEmerald
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = when {
                                isAccepted -> "Active Consultation"
                                isPending -> "Pending Scholar Review"
                                isRejected -> "Consultation Declined"
                                else -> sessionSubtitle
                            },
                            fontSize = 11.sp,
                            color = if (isPending) IslamicGold else DeepEmerald,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 2. ACTIVE / RECENT SESSIONS SELECTOR
        if (isImam) {
            val activeConsultations = remember(imamRequests) {
                imamRequests.filter { it.status.equals("ACCEPTED", ignoreCase = true) || it.status.equals("ACTIVE", ignoreCase = true) }
            }
            if (activeConsultations.isNotEmpty()) {
                Surface(
                    color = BgMintTint,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = DeepEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Active:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepEmerald
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(activeConsultations, key = { it.id }) { req ->
                                val isSelected = req.id == currentChatSession?.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel?.selectChatRequest(req.id)
                                    },
                                    label = {
                                        Text(
                                            text = "${req.userName ?: "Congregant"}: ${req.initialReason.take(14)}...",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.height(30.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DeepEmerald,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } else {
            val userConsultations = remember(userRequests) { userRequests }
            if (userConsultations.size > 1) {
                Surface(
                    color = BgMintTint,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.HistoryEdu,
                            contentDescription = null,
                            tint = DeepEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Scholars:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepEmerald
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(userConsultations, key = { it.id }) { req ->
                                val isSelected = req.id == currentChatSession?.id
                                val reqPending = req.status.equals("PENDING", ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel?.selectChatRequest(req.id)
                                        viewModel?.imams?.value?.find { it.id == req.imamProfileId }?.let { foundImam ->
                                            viewModel.selectImam(foundImam)
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = "${req.imamName ?: "Scholar"} ${if (reqPending) "(Pending ⏳)" else "✓"}",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.height(30.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DeepEmerald,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. STATUS BANNERS (Pending Acceptance / Declined / Imam Decision)
        if (isPending) {
            if (isImam) {
                Surface(
                    color = BgGoldTint,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Incoming Consultation Request",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepEmerald
                            )
                            Text(
                                text = "Accept to unlock real-time conversation",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                currentChatSession?.let { req ->
                                    viewModel?.rejectChatRequest(req.id) { }
                                }
                            },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC0392B)),
                            border = BorderStroke(1.dp, Color(0xFFC0392B).copy(alpha = 0.5f))
                        ) {
                            Text("Decline", fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                currentChatSession?.let { req ->
                                    viewModel?.acceptChatRequest(req.id) { }
                                }
                            },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald)
                        ) {
                            Text("Accept", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Surface(
                    color = BgPeachTint,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(IslamicGold.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HourglassTop,
                                contentDescription = null,
                                tint = DeepEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Status: Waiting for Imam's Acceptance",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepEmerald
                            )
                            Text(
                                text = "Submitted to $sessionTitle. Direct messaging unlocks upon acceptance.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel?.fetchUserChatRequests()
                                currentChatSession?.id?.let { viewModel?.fetchChatMessages(it) }
                                Toast.makeText(context, "Checking acceptance status...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh",
                                tint = DeepEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        } else if (isRejected) {
            Surface(
                color = Color(0xFFFFECEB),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Block,
                        contentDescription = null,
                        tint = Color(0xFFC0392B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "The scholar was unable to accept this consultation request at this time.",
                        fontSize = 12.sp,
                        color = Color(0xFFC0392B)
                    )
                }
            }
        }

        // Upload progress indicator if active
        if (isUploadingAttachment) {
            Surface(
                color = BgGoldTint,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = IslamicGold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = uploadStatusMessage ?: "Uploading attachment...",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 3. MESSAGE LIST (Takes all available space)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (backendMessages.isEmpty() && pendingLocalMessages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = PureCream.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, IslamicGold.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth(0.92f)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (isPending) Icons.Rounded.HourglassTop else Icons.Rounded.VerifiedUser,
                                    contentDescription = null,
                                    tint = DeepEmerald,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = when {
                                        isPending -> "Consultation Request Submitted"
                                        isAccepted -> "Consultation Session Active"
                                        else -> "Direct Consultation with $sessionTitle"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = DeepEmerald
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = when {
                                        isPending -> "Your consultation request was submitted to $sessionTitle. Waiting for acceptance before real-time messaging starts."
                                        isAccepted -> "You are conversing privately with $sessionTitle. All communications are strictly confidential."
                                        else -> "Send your question or voice note below to request a private 1-on-1 consultation with $sessionTitle. Real-time chat unlocks once accepted."
                                    },
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }
            }

            // Backend messages if any
            items(backendMessages, key = { it.id ?: java.util.UUID.randomUUID().toString() }) { dto ->
                val myId = currentUser?.id?.trim()
                val myName = currentUser?.name?.trim() ?: currentUser?.username?.trim() ?: ""
                val senderId = dto.senderId?.trim()
                val senderName = dto.senderName?.trim() ?: ""

                val isMe = when {
                    // 1. Authoritative check: sender ID matches current user ID
                    !myId.isNullOrBlank() && !senderId.isNullOrBlank() -> {
                        myId.equals(senderId, ignoreCase = true)
                    }
                    // 2. Authoritative check: sender Name matches current user Name
                    myName.isNotBlank() && senderName.isNotBlank() -> {
                        myName.equals(senderName, ignoreCase = true)
                    }
                    // 3. Fallback based on session participants
                    currentChatSession != null -> {
                        val sessionUserId = currentChatSession.userId?.trim()
                        val sessionUserName = currentChatSession.userName?.trim()
                        val sessionImamName = currentChatSession.imamName?.trim()

                        if (isImam) {
                            // Viewing as the Scholar / Imam
                            if (senderName.isNotBlank() && !sessionImamName.isNullOrBlank() && senderName.equals(sessionImamName, ignoreCase = true)) {
                                true
                            } else if (!senderId.isNullOrBlank() && !sessionUserId.isNullOrBlank() && senderId.equals(sessionUserId, ignoreCase = true)) {
                                false
                            } else if (senderName.isNotBlank() && !sessionUserName.isNullOrBlank() && senderName.equals(sessionUserName, ignoreCase = true)) {
                                false
                            } else {
                                false
                            }
                        } else {
                            // Viewing as the Congregant / User
                            if (!senderId.isNullOrBlank() && !sessionUserId.isNullOrBlank() && senderId.equals(sessionUserId, ignoreCase = true)) {
                                true
                            } else if (senderName.isNotBlank() && !sessionUserName.isNullOrBlank() && senderName.equals(sessionUserName, ignoreCase = true)) {
                                true
                            } else if (senderName.isNotBlank() && !sessionImamName.isNullOrBlank() && senderName.equals(sessionImamName, ignoreCase = true)) {
                                false
                            } else {
                                false
                            }
                        }
                    }
                    else -> false
                }

                val rawUrl = dto.mediaUrl
                val lowerUrl = rawUrl?.lowercase() ?: ""
                val lowerText = dto.messageText.lowercase()

                val isAudio = lowerUrl.endsWith(".aac") || lowerUrl.endsWith(".mp3") || lowerUrl.endsWith(".m4a") || lowerText.contains("voice note")
                val isImage = lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") || lowerUrl.endsWith(".png") || lowerUrl.endsWith(".webp") || lowerText.startsWith("[image]")
                val isPdf = lowerUrl.endsWith(".pdf") || lowerText.startsWith("[pdf]")
                val isVideo = lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".mkv") || lowerUrl.endsWith(".mov") || lowerUrl.endsWith(".webm") || lowerText.startsWith("[video]")

                val detectedType = when {
                    isImage -> "IMAGE"
                    isPdf -> "PDF"
                    isVideo -> "VIDEO"
                    isAudio -> "AUDIO"
                    rawUrl != null -> "FILE"
                    else -> null
                }

                ChatBubble(
                    ChatMessageItem(
                        id = dto.id ?: java.util.UUID.randomUUID().toString(),
                        text = dto.messageText,
                        isFromMe = isMe,
                        senderName = dto.senderName,
                        audioUrl = if (isAudio) rawUrl else null,
                        mediaUrl = if (!isAudio) rawUrl else null,
                        fileType = detectedType,
                        fileName = rawUrl?.substringAfterLast("/")?.substringAfterLast("_"),
                        timestamp = formatChatTime(dto.sentAt)
                    )
                )
            }

            // Pending local messages (only optimistic messages waiting for backend confirmation)
            items(pendingLocalMessages, key = { it.id }) { msg ->
                ChatBubble(msg)
            }
        }

        // 4. CHAT INPUT OR PENDING / CLOSED STATE
        if (isPending) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                shape = RoundedCornerShape(22.dp),
                color = PureCream,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, IslamicGold.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.HourglassTop,
                        contentDescription = null,
                        tint = IslamicGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isImam) "Consultation request pending approval" else "Waiting for $sessionTitle to accept",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepEmerald
                        )
                        Text(
                            text = if (isImam) "Use Accept button at the top to begin" else "Chat input unlocks once accepted by the scholar",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else if (isRejected) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                shape = RoundedCornerShape(22.dp),
                color = PureCream,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, Color(0xFFE5DACB))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Block,
                        contentDescription = null,
                        tint = Color(0xFFC0392B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "This consultation session is closed.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
            // Floating pill input capsule with internal attachment clip + separate circular mic/send action button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 12.dp)
            ) {
                if (isRecordingAudio) {
                    Surface(
                        color = PureCream,
                        shape = RoundedCornerShape(26.dp),
                        shadowElevation = 4.dp,
                        border = BorderStroke(1.dp, Color(0xFFE5DACB)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                            VoiceRecorderWidget(
                                onVoiceRecorded = { filePath, duration ->
                                    isRecordingAudio = false
                                    val localItem = ChatMessageItem(
                                        text = "Voice Note (${duration}s)",
                                        isFromMe = true,
                                        senderName = currentUser?.name ?: if (isImam) "Imam" else "You",
                                        audioUrl = filePath,
                                        audioDurationSec = duration
                                    )
                                    localMessages.add(localItem)

                                    val file = java.io.File(filePath)
                                    if (file.exists()) {
                                        val uri = Uri.fromFile(file)
                                        viewModel?.uploadChatAttachment(uri) { success, resp, _ ->
                                            val finalUrl = if (success && resp != null) resp.url else filePath
                                            if (isAccepted && currentChatSession != null) {
                                                viewModel?.sendChatMessage(currentChatSession.id, "Voice Note (${duration}s)", finalUrl) { }
                                            } else {
                                                viewModel?.requestChatSession("Voice Note (${duration}s)", targetImamProfileId = selectedImam?.id, mediaUrl = finalUrl) { _, _ -> }
                                            }
                                        }
                                    } else {
                                        if (isAccepted && currentChatSession != null) {
                                            viewModel?.sendChatMessage(currentChatSession.id, "Voice Note (${duration}s)", filePath) { }
                                        } else {
                                            viewModel?.requestChatSession("Voice Note (${duration}s)", targetImamProfileId = selectedImam?.id) { _, _ -> }
                                        }
                                    }
                                    Toast.makeText(context, "Voice note sent successfully", Toast.LENGTH_SHORT).show()
                                },
                                onCancel = { isRecordingAudio = false }
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. WhatsApp Pill Capsule: Message input + Attachment Paperclip
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 50.dp),
                            shape = RoundedCornerShape(26.dp),
                            color = PureCream,
                            shadowElevation = 3.dp,
                            border = BorderStroke(1.dp, Color(0xFFE2D8C7))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 18.dp, end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Text Input with placeholder
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (messageText.isEmpty()) {
                                        Text(
                                            text = if (isNewConsultation) "Type initial question to request chat..." else "Message",
                                            fontSize = 14.sp,
                                            color = TextSecondary.copy(alpha = 0.8f)
                                        )
                                    }
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = messageText,
                                        onValueChange = { messageText = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = 15.sp,
                                            color = TextPrimary
                                        ),
                                        cursorBrush = androidx.compose.ui.graphics.SolidColor(DeepEmerald),
                                        maxLines = 4
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Attachment Paperclip Icon (inside the pill)
                                IconButton(
                                    onClick = {
                                        if (currentUser == null) {
                                            Toast.makeText(context, "Please sign in or register to send attachments", Toast.LENGTH_SHORT).show()
                                        } else {
                                            showAttachmentDialog = true
                                        }
                                    },
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AttachFile,
                                        contentDescription = "Attach File",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // 2. Separate WhatsApp Circular Action Button (Mic when empty, Send when typed)
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(DeepEmerald)
                                .clickable {
                                    if (currentUser == null) {
                                        Toast.makeText(context, "Please sign in or register to consult with the scholar", Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }
                                    if (messageText.isNotBlank()) {
                                        val text = messageText.trim()
                                        messageText = ""
                                        localMessages.add(ChatMessageItem(text = text, isFromMe = true, senderName = currentUser?.name ?: if (isImam) "Imam" else "You"))

                                        if (isAccepted && currentChatSession != null) {
                                            viewModel?.sendChatMessage(currentChatSession.id, text) { }
                                        } else {
                                            viewModel?.requestChatSession(text, targetImamProfileId = selectedImam?.id) { _, _ -> }
                                        }
                                    } else {
                                        isRecordingAudio = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (messageText.isNotBlank()) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Message",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Mic,
                                    contentDescription = "Record Voice Note",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 5. ATTACHMENT TYPE CHOOSER MODAL
    if (showAttachmentDialog) {
        AlertDialog(
            onDismissRequest = { showAttachmentDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AttachFile, contentDescription = null, tint = DeepEmerald)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Attachment Type", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enforced limit: Max 15 MB per file. Direct device save enabled.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    // Option 1: Image
                    AttachmentOptionRow(
                        icon = Icons.Rounded.Image,
                        iconTint = DeepEmerald,
                        title = "Image / Photo",
                        subtitle = "JPEG, PNG, WEBP (Max 15 MB)",
                        onClick = {
                            showAttachmentDialog = false
                            filePickerLauncher.launch("image/*")
                        }
                    )

                    // Option 2: PDF Document
                    AttachmentOptionRow(
                        icon = Icons.Rounded.PictureAsPdf,
                        iconTint = Color(0xFFD32F2F),
                        title = "PDF Document",
                        subtitle = "Masail, ruling books & guides (Max 15 MB)",
                        onClick = {
                            showAttachmentDialog = false
                            filePickerLauncher.launch("application/pdf")
                        }
                    )

                    // Option 3: Video
                    AttachmentOptionRow(
                        icon = Icons.Rounded.Videocam,
                        iconTint = Color(0xFF1976D2),
                        title = "Video Clip",
                        subtitle = "MP4, MKV, MOV recordings (Max 15 MB)",
                        onClick = {
                            showAttachmentDialog = false
                            filePickerLauncher.launch("video/*")
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAttachmentDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = PureCream,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun AttachmentOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFAF7F2),
        border = BorderStroke(1.dp, Color(0xFFE5DACB)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                Text(subtitle, fontSize = 11.sp, color = TextSecondary)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessageItem) {
    val context = LocalContext.current
    val isMe = msg.isFromMe
    val alignment = if (isMe) Alignment.End else Alignment.Start

    val rawMediaUrl = msg.mediaUrl
    val lowerUrl = rawMediaUrl?.lowercase() ?: ""
    val isImage = msg.fileType == "IMAGE" || lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") || lowerUrl.endsWith(".png") || lowerUrl.endsWith(".webp") || msg.text.startsWith("[Image]")
    val isPdf = msg.fileType == "PDF" || lowerUrl.endsWith(".pdf") || msg.text.startsWith("[PDF]")
    val isVideo = msg.fileType == "VIDEO" || lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".mkv") || lowerUrl.endsWith(".mov") || lowerUrl.endsWith(".webm") || msg.text.startsWith("[Video]")
    val isGenericFile = rawMediaUrl != null && !isImage && !isPdf && !isVideo && msg.audioUrl == null

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        // Show sender name for incoming message so recipient knows who sent it
        if (!isMe && !msg.senderName.isNullOrBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 6.dp, bottom = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = DeepEmerald,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = msg.senderName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepEmerald
                )
            }
        }

        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
                .background(if (isMe) DeepEmerald else PureCream)
                .border(
                    width = if (isMe) 0.dp else 1.dp,
                    color = Color(0xFFE5DACB),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                // 1. Audio Voice Note
                if (msg.audioUrl != null) {
                    val audioLabel = if (isMe) "Voice Note (You)" else (msg.senderName?.let { "Voice Note ($it)" } ?: "Audio Note")
                    VoicePlayerWidget(
                        title = audioLabel,
                        audioUrl = msg.audioUrl,
                        durationSec = if (msg.audioDurationSec > 0) msg.audioDurationSec else 25,
                        containerColor = if (isMe) ForestSlate else BgMintTint
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 2. Image Attachment
                if (isImage && rawMediaUrl != null) {
                    val fullUrl = ApiClient.resolveMediaUrl(rawMediaUrl)
                    val imgFileName = msg.fileName ?: "masail_image.jpg"
                    Column(modifier = Modifier.padding(bottom = 6.dp)) {
                        AsyncImage(
                            model = fullUrl,
                            contentDescription = "Attachment Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.05f)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        // Save Image to Device Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isMe) ForestSlate else BgMintTint)
                                .clickable {
                                    downloadAttachment(context, rawMediaUrl, imgFileName)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = "Save to device",
                                tint = if (isMe) Color.White else DeepEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Save Image to Device",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isMe) Color.White else DeepEmerald
                            )
                        }
                    }
                }

                // 3. PDF Attachment Card
                if (isPdf && rawMediaUrl != null) {
                    val pdfFileName = msg.fileName ?: msg.text.removePrefix("[PDF]").trim().ifEmpty { "Masail_Document.pdf" }
                    Surface(
                        color = if (isMe) ForestSlate else Color(0xFFFFF9F5),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isMe) Color.White.copy(alpha = 0.2f) else Color(0xFFFFCCBC)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFFEBEE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PictureAsPdf,
                                        contentDescription = "PDF",
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pdfFileName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMe) Color.White else TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "PDF Document • Saved locally on download",
                                        fontSize = 10.sp,
                                        color = if (isMe) Color.White.copy(alpha = 0.7f) else TextSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    downloadAttachment(context, rawMediaUrl, pdfFileName)
                                },
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isMe) DeepEmerald else Color(0xFFD32F2F)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download & Save PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                // 4. Video Attachment Card
                if (isVideo && rawMediaUrl != null) {
                    val vidFileName = msg.fileName ?: msg.text.removePrefix("[Video]").trim().ifEmpty { "Masail_Video.mp4" }
                    Surface(
                        color = if (isMe) ForestSlate else Color(0xFFF3F8FF),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isMe) Color.White.copy(alpha = 0.2f) else Color(0xFFBBDEFB)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE3F2FD)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Videocam,
                                        contentDescription = "Video",
                                        tint = Color(0xFF1976D2),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = vidFileName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMe) Color.White else TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Video File • Saved locally on download",
                                        fontSize = 10.sp,
                                        color = if (isMe) Color.White.copy(alpha = 0.7f) else TextSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    downloadAttachment(context, rawMediaUrl, vidFileName)
                                },
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isMe) DeepEmerald else Color(0xFF1976D2)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download & Save Video", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                // 5. Generic Document File Card
                if (isGenericFile && rawMediaUrl != null) {
                    val genericFileName = msg.fileName ?: "Attachment_File"
                    Surface(
                        color = if (isMe) ForestSlate else PureCream,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE5DACB)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.InsertDriveFile, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = genericFileName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMe) Color.White else TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    downloadAttachment(context, rawMediaUrl, genericFileName)
                                },
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald)
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download & Save File", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }

                // 6. Message Caption / Text (Hide redundant raw tag if identical to filename)
                val shouldShowText = msg.text.isNotBlank() &&
                        !(isPdf && msg.text.startsWith("[PDF]")) &&
                        !(isVideo && msg.text.startsWith("[Video]")) &&
                        !(isImage && msg.text.startsWith("[Image]"))

                if (shouldShowText) {
                    Text(
                        text = msg.text,
                        color = if (isMe) Color.White else TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = msg.timestamp,
                        color = if (isMe) Color.White.copy(alpha = 0.7f) else TextSecondary,
                        fontSize = 10.sp
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.DoneAll,
                            contentDescription = "Delivered",
                            tint = IslamicGold,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Downloads and saves the attachment directly into the device's public Downloads directory
 * using Android DownloadManager, allowing users and imams to retain their files locally.
 */
fun downloadAttachment(context: Context, rawUrl: String, suggestedName: String? = null) {
    try {
        val resolved = ApiClient.resolveMediaUrl(rawUrl)
        if (resolved.isNullOrBlank()) {
            Toast.makeText(context, "Invalid media URL", Toast.LENGTH_SHORT).show()
            return
        }

        val rawFileName = suggestedName?.takeIf { it.isNotBlank() }
            ?: rawUrl.substringAfterLast("/").substringAfterLast("_")
        val safeFileName = if (rawFileName.contains(".")) rawFileName else "$rawFileName.bin"

        val request = DownloadManager.Request(Uri.parse(resolved)).apply {
            setTitle(safeFileName)
            setDescription("Saving Angus Masail chat attachment...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeFileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        if (dm != null) {
            dm.enqueue(request)
            Toast.makeText(context, "Saving '$safeFileName' to Downloads folder...", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Download manager unavailable", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
