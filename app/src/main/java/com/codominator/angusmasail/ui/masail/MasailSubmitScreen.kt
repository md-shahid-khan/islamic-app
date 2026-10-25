package com.codominator.angusmasail.ui.masail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.*
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
import com.codominator.angusmasail.ui.components.GlassmorphicSurface
import com.codominator.angusmasail.ui.components.StatusPill
import com.codominator.angusmasail.ui.components.VoicePlayerWidget
import com.codominator.angusmasail.ui.components.VoiceRecorderWidget
import com.codominator.angusmasail.ui.theme.*
import com.codominator.angusmasail.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasailSubmitScreen(
    viewModel: MainViewModel,
    onNavigateToChat: () -> Unit = {},
    onOpenAuth: () -> Unit = {}
) {
    val context = LocalContext.current
    var queryText by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("MASAIL") }
    val tags = listOf("MASAIL", "SUGGESTION", "CORRECTION")
    var isSubmitting by remember { mutableStateOf(false) }
    var isUrgent by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Submit, 1 = My History

    // Voice recording states
    var isRecordingVoice by remember { mutableStateOf(false) }
    var attachedAudioUrl by remember { mutableStateOf<String?>(null) }
    var attachedAudioDuration by remember { mutableIntStateOf(0) }

    val myQueries by viewModel.myQueries.collectAsState()
    val duplicateResults by viewModel.duplicateCheckResults.collectAsState()
    val selectedImam by viewModel.selectedImam.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchMyQueries()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = null, tint = IslamicGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Masail & Religious Inquiries", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                            Text("Confidential to ${selectedImam?.imamName ?: "Masjid Scholar"}", fontSize = 11.sp, color = TextSecondary)
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
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = WarmAlabaster,
                contentColor = DeepEmerald
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Ask Question", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        viewModel.fetchMyQueries()
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("My Questions (${myQueries.size})", fontWeight = FontWeight.SemiBold)
                            // If any query is accepted, show small highlight badge
                            val acceptedCount = myQueries.count { it.status.equals("ACCEPTED", ignoreCase = true) }
                            if (acceptedCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(LiveEmerald, CircleShape)
                                )
                            }
                        }
                    }
                )
            }

            if (selectedTab == 0) {
                // Submit Form
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (currentUser == null) {
                        item {
                            Surface(
                                color = BgGoldTint,
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, IslamicGold.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Account Required", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DeepEmerald)
                                        Text("Sign in or register to submit confidential questions to the scholar.", fontSize = 11.sp, color = TextSecondary)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = onOpenAuth,
                                        colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Sign In", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Text(
                            text = "Select Category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tags.forEach { tag ->
                                val isSelected = tag == selectedTag
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedTag = tag },
                                    label = {
                                        Text(
                                            text = tag,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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

                    item {
                        Text(
                            text = "Describe your Masail in detail",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ask via text or record a voice question for the scholar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = queryText,
                            onValueChange = {
                                if (it.length <= 1000) {
                                    queryText = it
                                    viewModel.checkDuplicates(it)
                                }
                            },
                            placeholder = { Text("Write your religious question, fiqh query or context here...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = PureCream,
                                unfocusedContainerColor = PureCream,
                                focusedBorderColor = IslamicGold,
                                unfocusedBorderColor = Color(0xFFDFD5C5)
                            ),
                            supportingText = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Addressed to: ${selectedImam?.imamName ?: "Imam"}", color = TextSecondary, fontSize = 11.sp)
                                    Text("${queryText.length}/1000", color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                        )
                    }

                    // Voice Note Option
                    item {
                        if (isRecordingVoice) {
                            VoiceRecorderWidget(
                                onVoiceRecorded = { url, duration ->
                                    attachedAudioUrl = url
                                    attachedAudioDuration = duration
                                    isRecordingVoice = false
                                    Toast.makeText(context, "Voice note attached ($duration sec)", Toast.LENGTH_SHORT).show()
                                },
                                onCancel = {
                                    isRecordingVoice = false
                                }
                            )
                        } else if (attachedAudioUrl != null) {
                            VoicePlayerWidget(
                                title = "Attached Voice Question",
                                audioUrl = attachedAudioUrl,
                                durationSec = attachedAudioDuration,
                                containerColor = BgMintTint,
                                onDelete = {
                                    attachedAudioUrl = null
                                    attachedAudioDuration = 0
                                }
                            )
                        } else {
                            OutlinedButton(
                                onClick = { isRecordingVoice = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepEmerald)
                            ) {
                                Icon(Icons.Rounded.Mic, contentDescription = "Record Voice", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Record Voice Question / Audio Note", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Duplicate question warning / FAQ match
                    if (duplicateResults.isNotEmpty()) {
                        item {
                            GlassmorphicSurface(
                                backgroundColor = BgGoldTint,
                                borderColor = IslamicGold.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Similar Masail Already Answered:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    duplicateResults.take(2).forEach { faq ->
                                        Text(
                                            text = "• \"${faq.questionText}\"",
                                            fontSize = 12.sp,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                        if (!faq.imamReplyText.isNullOrBlank()) {
                                            Text(
                                                text = "  Ans: ${faq.imamReplyText.take(90)}...",
                                                fontSize = 11.sp,
                                                color = DeepEmerald,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = PureCream),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Mark as Urgent", fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(
                                        "For funerals, emergency rulings or time-sensitive matters",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                                Switch(
                                    checked = isUrgent,
                                    onCheckedChange = { isUrgent = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = DeepEmerald, checkedTrackColor = BgMintTint)
                                )
                            }
                        }
                    }

                    item {
                        val canSubmit = queryText.isNotBlank() || attachedAudioUrl != null
                        Button(
                            onClick = {
                                if (currentUser == null) {
                                    Toast.makeText(context, "Please sign in or register to submit a Masail", Toast.LENGTH_SHORT).show()
                                    onOpenAuth()
                                    return@Button
                                }
                                if (canSubmit) {
                                    isSubmitting = true
                                    val finalQuestion = if (queryText.isNotBlank()) queryText else "Voice Question submitted via Angus Masail audio note."
                                    viewModel.submitMasail(
                                        tag = selectedTag,
                                        questionText = finalQuestion,
                                        audioUrl = attachedAudioUrl,
                                        targetImamProfileId = selectedImam?.id
                                    ) { success, errorMsg ->
                                        isSubmitting = false
                                        if (success) {
                                            Toast.makeText(context, "Query submitted successfully to the Imam", Toast.LENGTH_SHORT).show()
                                            queryText = ""
                                            attachedAudioUrl = null
                                            attachedAudioDuration = 0
                                            selectedTab = 1 // Switch to history tab
                                        } else {
                                            val feedback = if (!errorMsg.isNullOrBlank()) "Failed: $errorMsg" else "Submission failed. Please check network."
                                            Toast.makeText(context, feedback, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(26.dp),
                            enabled = !isSubmitting && canSubmit,
                            colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Submit Masail Securely", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            } else {
                // History List Tab
                if (myQueries.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No questions submitted yet", color = TextSecondary, fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(myQueries, key = { it.id }) { query ->
                            MasailHistoryCard(
                                query = query,
                                onContinueInChat = onNavigateToChat
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MasailHistoryCard(
    query: com.codominator.angusmasail.network.MasailQueryDto,
    onContinueInChat: () -> Unit
) {
    val isResolved = query.status.equals("RESOLVED", ignoreCase = true) || !query.imamReplyText.isNullOrBlank()
    val isAccepted = query.status.equals("ACCEPTED", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PureCream),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isAccepted) DeepEmerald.copy(alpha = 0.5f) else Color(0xFFE5DACB)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Category Tag + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(text = query.tag, bgColor = BgGoldTint, textColor = DeepEmerald)

                when {
                    isResolved -> {
                        StatusPill(text = "ANSWERED", bgColor = BgMintTint, textColor = DeepEmerald)
                    }
                    isAccepted -> {
                        StatusPill(text = "ACCEPTED BY IMAM", bgColor = DeepEmerald, textColor = Color.White)
                    }
                    else -> {
                        StatusPill(text = "PENDING REVIEW", bgColor = BgPeachTint, textColor = Color(0xFFC0392B))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Question Text
            Text(
                text = query.questionText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )

            // User's voice audio question if available
            if (!query.questionAudioUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                VoicePlayerWidget(
                    title = "Your Voice Question",
                    audioUrl = query.questionAudioUrl,
                    durationSec = 22,
                    containerColor = WarmAlabaster
                )
            }

            // Notification Banner when ACCEPTED by Imam
            if (isAccepted && !isResolved) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = BgMintTint,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Imam Accepted Your Question!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = DeepEmerald
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "The scholar is preparing your ruling. You can also consult directly in real-time chat.",
                            fontSize = 12.sp,
                            color = TextPrimary,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onContinueInChat,
                            colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            Icon(Icons.Rounded.Forum, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Continue Consultation in Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Imam's Response (Text or Voice)
            if (!query.imamReplyText.isNullOrBlank() || !query.imamReplyAudioUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                GlassmorphicSurface(
                    backgroundColor = BgMintTint,
                    borderColor = DeepEmerald.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Verified, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Imam's Scholarly Ruling:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DeepEmerald)
                        }

                        if (!query.imamReplyText.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = query.imamReplyText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                lineHeight = 20.sp
                            )
                        }

                        if (!query.imamReplyAudioUrl.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            VoicePlayerWidget(
                                title = "Imam's Audio Fatwa / Voice Note",
                                audioUrl = query.imamReplyAudioUrl,
                                durationSec = 35,
                                containerColor = PureCream,
                                contentColor = DeepEmerald
                            )
                        }
                    }
                }
            }
        }
    }
}
