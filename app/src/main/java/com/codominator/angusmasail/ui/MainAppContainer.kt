package com.codominator.angusmasail.ui

import androidx.compose.ui.text.style.TextOverflow

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ContactSupport
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codominator.angusmasail.network.ImamProfileDto
import com.codominator.angusmasail.network.TokenManager
import com.codominator.angusmasail.ui.admin.AdminDashboardScreen
import com.codominator.angusmasail.ui.chat.ChatScreen
import com.codominator.angusmasail.ui.components.GlassmorphicSurface
import com.codominator.angusmasail.ui.components.StatusPill
import com.codominator.angusmasail.ui.components.bouncingClickable
import com.codominator.angusmasail.ui.home.HomeScreen
import com.codominator.angusmasail.ui.imam.AvailableImamsScreen
import com.codominator.angusmasail.ui.imam.ImamDashboardScreen
import com.codominator.angusmasail.ui.imam.ImamRegistrationDialog
import com.codominator.angusmasail.ui.mosque.RegisterMosqueDialog
import com.codominator.angusmasail.network.MosqueDto
import com.codominator.angusmasail.ui.library.LibraryScreen
import com.codominator.angusmasail.ui.masail.MasailSubmitScreen
import com.codominator.angusmasail.ui.qibla.QiblaScreen
import com.codominator.angusmasail.ui.quran.QuranHubScreen
import com.codominator.angusmasail.ui.quran.QuranReaderScreen
import com.codominator.angusmasail.ui.quran.SurahListScreen
import com.codominator.angusmasail.ui.theme.*
import com.codominator.angusmasail.ui.auth.AuthDialog
import com.codominator.angusmasail.ui.settings.SettingsDialog
import com.codominator.angusmasail.viewmodel.MainViewModel
import kotlinx.coroutines.launch

enum class NavDestination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Home),
    QIBLA("Qibla", Icons.Rounded.Explore),
    MASAIL("Masail", Icons.AutoMirrored.Rounded.HelpOutline),
    CHAT("Chat", Icons.Rounded.Forum),
    AVAILABLE_IMAMS("Imam", Icons.Rounded.People),
    IMAM_DASHBOARD("Dashboard", Icons.Rounded.AdminPanelSettings),
    ADMIN_DASHBOARD("Admin", Icons.Rounded.Security),
    QURAN_HUB("Quran", Icons.Rounded.MenuBook),
    QURAN_SURAHS("Surahs", Icons.Rounded.FormatListNumbered),
    QURAN_READER("Read Quran", Icons.Rounded.AutoStories),
    LIBRARY("Library", Icons.Rounded.LocalLibrary)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var currentDestination by remember { mutableStateOf(NavDestination.HOME) }
    var showImamSelectSheet by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showBooksSheet by remember { mutableStateOf(false) }

    // Dialog States for Hamburger Menu
    var showLoginDialog by remember { mutableStateOf(false) }
    var showRegisterDialog by remember { mutableStateOf(false) }
    var showImamRegistrationDialog by remember { mutableStateOf(false) }
    var showPendingApprovalDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showRegisterMosqueDialog by remember { mutableStateOf(false) }

    val imams by viewModel.imams.collectAsState()
    val selectedImam by viewModel.selectedImam.collectAsState()
    val resources by viewModel.resources.collectAsState()
    val statuses by viewModel.statuses.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val imamQueries by viewModel.imamQueries.collectAsState()
    val surahs by viewModel.surahs.collectAsState()
    val mosques by viewModel.mosques.collectAsState()

    var readerSurahNumber by remember { mutableStateOf(1) }
    var readerInitialAyah by remember { mutableStateOf<Int?>(null) }

    val isLoggedIn = currentUser != null

    val pendingQueriesCount = remember(imamQueries) {
        imamQueries.count { it.status.equals("PENDING", ignoreCase = true) || it.status.equals("SUBMITTED", ignoreCase = true) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = WarmAlabaster,
                drawerContentColor = TextPrimary,
                modifier = Modifier.width(310.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    // Drawer Header Profile Card
                    GlassmorphicSurface(
                        backgroundColor = BgGoldTint,
                        borderColor = IslamicGold.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(DeepEmerald),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (userRole) {
                                            "ROLE_IMAM" -> Icons.Rounded.AdminPanelSettings
                                            "ROLE_ADMIN" -> Icons.Rounded.Security
                                            else -> Icons.Rounded.Person
                                        },
                                        contentDescription = null,
                                        tint = IslamicGold,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = currentUser?.name ?: "Guest Congregant",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (isLoggedIn) (currentUser?.phone ?: "") else "Tap below to log in or register",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusPill(
                                    text = when (userRole) {
                                        "ROLE_IMAM" -> "IMAM SCHOLAR"
                                        "ROLE_ADMIN" -> "MASJID ADMIN"
                                        else -> if (isLoggedIn) "CONGREGANT" else "GUEST"
                                    },
                                    bgColor = DeepEmerald,
                                    textColor = Color.White
                                )

                                Text(
                                    text = selectedImam?.masjidName ?: "Select Mosque",
                                    fontSize = 11.sp,
                                    color = DeepEmerald,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "MENU & SERVICES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Hamburger Menu Items Gated By Authentication & Role
                    if (!isLoggedIn) {
                        // Guest mode: show Login, Register, Imam Onboarding, Contact, Select Masjid
                        DrawerMenuItem(
                            icon = Icons.AutoMirrored.Rounded.Login,
                            label = "Login",
                            subtitle = "Access account or switch persona"
                        ) {
                            coroutineScope.launch { drawerState.close() }
                            showLoginDialog = true
                        }

                        DrawerMenuItem(
                            icon = Icons.Rounded.PersonAdd,
                            label = "Register Congregant",
                            subtitle = "Create user account"
                        ) {
                            coroutineScope.launch { drawerState.close() }
                            showRegisterDialog = true
                        }

                        DrawerMenuItem(
                            icon = Icons.Rounded.AccountBalance,
                            label = "Register as an Imam",
                            subtitle = "Onboard your masjid, fiqh & prayer timetable",
                            iconTint = IslamicGold
                        ) {
                            coroutineScope.launch { drawerState.close() }
                            showImamRegistrationDialog = true
                        }

                        DrawerMenuItem(
                            icon = Icons.Rounded.LocationCity,
                            label = "Select Mosque",
                            subtitle = "Choose your congregation or local mosque"
                        ) {
                            coroutineScope.launch { drawerState.close() }
                            showImamSelectSheet = true
                        }

                        DrawerMenuItem(
                            icon = Icons.Rounded.MenuBook,
                            label = "Al Quran (Kanzul Iman)",
                            subtitle = "Online Quran with Ala Hazrat translation & audio",
                            iconTint = DeepEmerald
                        ) {
                            coroutineScope.launch { drawerState.close() }
                            currentDestination = NavDestination.QURAN_HUB
                        }

                        DrawerMenuItem(
                            icon = Icons.Rounded.LocalLibrary,
                            label = "Islamic Library",
                            subtitle = "Public books & PDF resources (Quran, Hadith, Aqaid, Namaz, Masail)",
                            iconTint = DeepEmerald
                        ) {
                            coroutineScope.launch { drawerState.close() }
                            currentDestination = NavDestination.LIBRARY
                        }

                        DrawerMenuItem(
                            icon = Icons.AutoMirrored.Rounded.ContactSupport,
                            label = "Contact Support",
                            subtitle = "Help, helpline & community support"
                        ) {
                            coroutineScope.launch { drawerState.close() }
                            showContactDialog = true
                        }

                        DrawerMenuItem(
                            icon = Icons.Rounded.Settings,
                            label = "Settings & Preferences",
                            subtitle = "Preferences & specific community details",
                            iconTint = DeepEmerald
                        ) {
                            coroutineScope.launch { drawerState.close() }
                            showSettingsDialog = true
                        }
                    } else {
                        // Logged in user: HIDE Login and Register!
                        when (userRole) {
                            "ROLE_IMAM" -> {
                                // Imam View: Imam Portal with pending query alert
                                DrawerMenuItem(
                                    icon = Icons.Rounded.AdminPanelSettings,
                                    label = if (pendingQueriesCount > 0) "Imam Portal ($pendingQueriesCount Pending)" else "Imam Portal",
                                    subtitle = "Review pending Masail & edit prayer timetable",
                                    iconTint = if (pendingQueriesCount > 0) Color(0xFFC0392B) else DeepEmerald
                                ) {
                                    coroutineScope.launch { drawerState.close() }
                                    currentDestination = NavDestination.IMAM_DASHBOARD
                                }

                                DrawerMenuItem(
                                    icon = Icons.Rounded.Verified,
                                    label = "My Mosque: ${selectedImam?.masjidName ?: "Assigned Mosque"}",
                                    subtitle = "Assigned congregation (Imam cannot change mosque)",
                                    iconTint = IslamicGold
                                ) {
                                    coroutineScope.launch { drawerState.close() }
                                    Toast.makeText(context, "You are registered as the Imam of ${selectedImam?.masjidName ?: "your mosque"}", Toast.LENGTH_SHORT).show()
                                }

                                DrawerMenuItem(
                                    icon = Icons.Rounded.LocalLibrary,
                                    label = "Islamic Library",
                                    subtitle = "Public books & PDF resources (Upload & Browse)",
                                    iconTint = DeepEmerald
                                ) {
                                    coroutineScope.launch { drawerState.close() }
                                    currentDestination = NavDestination.LIBRARY
                                }

                                DrawerMenuItem(
                                    icon = Icons.Rounded.MenuBook,
                                    label = "Al Quran (Kanzul Iman)",
                                    subtitle = "Online Quran with Ala Hazrat translation & audio",
                                    iconTint = DeepEmerald
                                ) {
                                    coroutineScope.launch { drawerState.close() }
                                    currentDestination = NavDestination.QURAN_HUB
                                }

                                DrawerMenuItem(
                                    icon = Icons.AutoMirrored.Rounded.ContactSupport,
                                    label = "Contact Administration",
                                    subtitle = "Community helpline & admin feedback"
                                ) {
                                    coroutineScope.launch { drawerState.close() }
                                    showContactDialog = true
                                }
                            }
                            "ROLE_ADMIN" -> {
                                // Admin View: Admin Portal and Imam Portal
                                DrawerMenuItem(
                                    icon = Icons.Rounded.Security,
                                    label = "Admin Portal",
                                    subtitle = "Central masjid management & statistics"
                                ) {
                                    coroutineScope.launch { drawerState.close() }
                                    currentDestination = NavDestination.ADMIN_DASHBOARD
                                }

                                DrawerMenuItem(
                                    icon = Icons.Rounded.AdminPanelSettings,
                                    label = "Imam Portal",
                                    subtitle = "Review Masail queries & timetables"
                                ) {
                                    coroutineScope.launch { drawerState.close() }
                                    currentDestination = NavDestination.IMAM_DASHBOARD
                                }

                                DrawerMenuItem(
                                    icon = Icons.Rounded.LocationCity,
                                    label = "Manage Mosques",
                                    subtitle = "View registered mosques & scholars"
                                ) {
                                    coroutineScope.launch { drawerState.close() }
                                    showImamSelectSheet = true
                                }

                                DrawerMenuItem(
                                    icon = Icons.Rounded.LocalLibrary,
                                    label = "Islamic Library",
                                    subtitle = "Public books & PDF resources",
                                    iconTint = DeepEmerald
                                ) {
                                    coroutineScope.launch { drawerState.close() }
                                    currentDestination = NavDestination.LIBRARY
                                }

                                DrawerMenuItem(
                                    icon = Icons.AutoMirrored.Rounded.ContactSupport,
                                    label = "Contact & Help",
                                    subtitle = "Support and system alerts"
                                ) {
                                    coroutineScope.launch { drawerState.close() }
                                    showContactDialog = true
                                }
                            }
                            else -> {
                                // Regular Congregant: CANNOT see Login, Register, Imam Portal, or Admin Portal!
                                DrawerMenuItem(
                                    icon = Icons.Rounded.LocationCity,
                                    label = "My Mosque / Congregation",
                                    subtitle = "View or switch preferred local mosque"
                                ) {
                                    coroutineScope.launch { drawerState.close() }
                                    showImamSelectSheet = true
                                }

                                DrawerMenuItem(
                                    icon = Icons.Rounded.LocalLibrary,
                                    label = "Islamic Library",
                                    subtitle = "Public books & PDF resources (Quran, Hadith, Masail)",
                                    iconTint = DeepEmerald
                                ) {
                                    coroutineScope.launch { drawerState.close() }
                                    currentDestination = NavDestination.LIBRARY
                                }

                                DrawerMenuItem(
                                    icon = Icons.Rounded.MenuBook,
                                    label = "Al Quran (Kanzul Iman)",
                                    subtitle = "Online Quran with Ala Hazrat translation & audio",
                                    iconTint = DeepEmerald
                                ) {
                                    coroutineScope.launch { drawerState.close() }
                                    currentDestination = NavDestination.QURAN_HUB
                                }

                                DrawerMenuItem(
                                    icon = Icons.AutoMirrored.Rounded.ContactSupport,
                                    label = "Contact Mosque & Support",
                                    subtitle = "Helpline, directions & community support"
                                ) {
                                    coroutineScope.launch { drawerState.close() }
                                    showContactDialog = true
                                }

                                if (currentUser?.isImamPending == true) {
                                    DrawerMenuItem(
                                        icon = Icons.Rounded.PendingActions,
                                        label = "Imam Status: Pending Review",
                                        subtitle = "Tap to check if Admin has approved application",
                                        iconTint = Color(0xFFC0392B)
                                    ) {
                                        viewModel.checkMyImamStatus { hasApp, isApproved ->
                                            if (isApproved) {
                                                coroutineScope.launch { drawerState.close() }
                                                Toast.makeText(context, "Approved! Welcome to the Imam Portal.", Toast.LENGTH_LONG).show()
                                                currentDestination = NavDestination.IMAM_DASHBOARD
                                            } else {
                                                showPendingApprovalDialog = true
                                            }
                                        }
                                    }
                                } else {
                                    DrawerMenuItem(
                                        icon = Icons.Rounded.AccountBalance,
                                        label = "Register as an Imam",
                                        subtitle = "Scholar onboarding & prayer timetable setup",
                                        iconTint = IslamicGold
                                    ) {
                                        coroutineScope.launch { drawerState.close() }
                                        showImamRegistrationDialog = true
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFFE2D9C8))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Settings & Profile Option
                        DrawerMenuItem(
                            icon = Icons.Rounded.Settings,
                            label = "Settings & Profile",
                            subtitle = "Specific details, locality, manhaj & preferences",
                            iconTint = DeepEmerald
                        ) {
                            coroutineScope.launch { drawerState.close() }
                            showSettingsDialog = true
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Logout Option
                        DrawerMenuItem(
                            icon = Icons.AutoMirrored.Rounded.Logout,
                            label = "Logout Account",
                            subtitle = "Sign out from session",
                            iconTint = Color(0xFFC0392B)
                        ) {
                            coroutineScope.launch { drawerState.close() }
                            viewModel.logout()
                            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFE2D9C8))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Switch Role Shortcut (for developer testing)
                    Text(
                        text = "QUICK TEST PERSONA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = userRole == "ROLE_USER",
                            onClick = {
                                viewModel.switchRole("ROLE_USER")
                                Toast.makeText(context, "Active Persona: Congregant (User)", Toast.LENGTH_SHORT).show()
                            },
                            label = { Text("User", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = userRole == "ROLE_IMAM",
                            onClick = {
                                viewModel.switchRole("ROLE_IMAM")
                                Toast.makeText(context, "Active Persona: Imam Scholar", Toast.LENGTH_SHORT).show()
                            },
                            label = { Text("Imam", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = userRole == "ROLE_ADMIN",
                            onClick = {
                                viewModel.switchRole("ROLE_ADMIN")
                                Toast.makeText(context, "Active Persona: Admin Trustee", Toast.LENGTH_SHORT).show()
                            },
                            label = { Text("Admin", fontSize = 11.sp) }
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                if (currentDestination != NavDestination.QURAN_READER) {
                    FloatingBottomNavigation(
                        currentDestination = currentDestination,
                        userRole = userRole,
                        pendingQueriesCount = pendingQueriesCount,
                        onNavigate = { currentDestination = it }
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Crossfade(targetState = currentDestination, label = "screen_transition") { destination ->
                    when (destination) {
                        NavDestination.HOME -> HomeScreen(
                            viewModel = viewModel,
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                            onNavigateToMasail = {
                                if (userRole == "ROLE_IMAM") {
                                    currentDestination = NavDestination.IMAM_DASHBOARD
                                } else {
                                    currentDestination = NavDestination.MASAIL
                                }
                            },
                            onNavigateToChat = { currentDestination = NavDestination.CHAT },
                            onOpenImamSelector = {
                                if (userRole != "ROLE_IMAM") {
                                    showImamSelectSheet = true
                                } else {
                                    Toast.makeText(context, "You are registered as the Imam of ${selectedImam?.masjidName ?: "your mosque"}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onOpenImamStatus = { showStatusDialog = true },
                            onQuickAction = { label ->
                                when {
                                    label.startsWith("Quran:") -> {
                                        val parts = label.split(":")
                                        val sNum = parts.getOrNull(1)?.toIntOrNull() ?: 1
                                        val aNum = parts.getOrNull(2)?.toIntOrNull()
                                        readerSurahNumber = sNum
                                        readerInitialAyah = aNum
                                        currentDestination = NavDestination.QURAN_READER
                                    }
                                    label == "Masail" -> {
                                        if (userRole == "ROLE_IMAM") {
                                            currentDestination = NavDestination.IMAM_DASHBOARD
                                        } else {
                                            currentDestination = NavDestination.MASAIL
                                        }
                                    }
                                    label == "Books" || label == "Library" -> currentDestination = NavDestination.LIBRARY
                                    label == "News" -> showStatusDialog = true
                                    label == "Quran" || label == "Al Quran" -> currentDestination = NavDestination.QURAN_HUB
                                }
                            }
                        )
                        NavDestination.QIBLA -> QiblaScreen()
                        NavDestination.MASAIL -> {
                            if (userRole == "ROLE_IMAM") {
                                ImamDashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToChat = { currentDestination = NavDestination.CHAT },
                                    onDisassociated = { currentDestination = NavDestination.HOME }
                                )
                            } else {
                                MasailSubmitScreen(
                                    viewModel = viewModel,
                                    onNavigateToChat = { currentDestination = NavDestination.CHAT },
                                    onOpenAuth = { showLoginDialog = true }
                                )
                            }
                        }
                        NavDestination.CHAT -> ChatScreen(
                            imamName = selectedImam?.imamName ?: "Imam Syed Ahmed",
                            viewModel = viewModel
                        )
                        NavDestination.AVAILABLE_IMAMS -> AvailableImamsScreen(
                            viewModel = viewModel,
                            onNavigateToMasail = { currentDestination = NavDestination.MASAIL },
                            onNavigateToChat = { currentDestination = NavDestination.CHAT },
                            onRegisterAsImam = { showImamRegistrationDialog = true },
                            onNavigateToImamDashboard = { currentDestination = NavDestination.IMAM_DASHBOARD }
                        )
                        NavDestination.IMAM_DASHBOARD -> ImamDashboardScreen(
                            viewModel = viewModel,
                            onNavigateToChat = { currentDestination = NavDestination.CHAT },
                            onDisassociated = { currentDestination = NavDestination.HOME }
                        )
                        NavDestination.ADMIN_DASHBOARD -> AdminDashboardScreen(
                            viewModel = viewModel,
                            onNavigateToImamPortal = { currentDestination = NavDestination.IMAM_DASHBOARD }
                        )
                        NavDestination.QURAN_HUB -> QuranHubScreen(
                            surahs = surahs,
                            onBack = { currentDestination = NavDestination.HOME },
                            onNavigateToSurahList = { currentDestination = NavDestination.QURAN_SURAHS },
                            onOpenSurahReader = { sNum, aNum ->
                                readerSurahNumber = sNum
                                readerInitialAyah = aNum
                                currentDestination = NavDestination.QURAN_READER
                            },
                            onOpenBookmarks = {
                                readerSurahNumber = 1
                                readerInitialAyah = null
                                currentDestination = NavDestination.QURAN_READER
                            }
                        )
                        NavDestination.QURAN_SURAHS -> SurahListScreen(
                            surahs = surahs,
                            onBack = { currentDestination = NavDestination.QURAN_HUB },
                            onSelectSurah = { sNum ->
                                readerSurahNumber = sNum
                                readerInitialAyah = null
                                currentDestination = NavDestination.QURAN_READER
                            }
                        )
                        NavDestination.QURAN_READER -> QuranReaderScreen(
                            surahNumber = readerSurahNumber,
                            initialAyahNumber = readerInitialAyah,
                            viewModel = viewModel,
                            surahs = surahs,
                            onSurahChanged = { newSurah -> readerSurahNumber = newSurah },
                            onBack = { currentDestination = NavDestination.QURAN_SURAHS }
                        )
                        NavDestination.LIBRARY -> LibraryScreen(
                            viewModel = viewModel,
                            onBack = { currentDestination = NavDestination.HOME }
                        )
                    }
                }
            }
        }
    }

    // 1. AUTH DIALOG (Sign In / Register with Email OTP)
    if (showLoginDialog) {
        AuthDialog(
            viewModel = viewModel,
            initialTab = 0,
            onDismiss = { showLoginDialog = false },
            onOpenImamRegistration = {
                showLoginDialog = false
                showImamRegistrationDialog = true
            }
        )
    }

    if (showRegisterDialog) {
        AuthDialog(
            viewModel = viewModel,
            initialTab = 1,
            onDismiss = { showRegisterDialog = false },
            onOpenImamRegistration = {
                showRegisterDialog = false
                showImamRegistrationDialog = true
            }
        )
    }

    // 1.5 SETTINGS & SPECIFIC DETAILS DIALOG
    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false }
        )
    }

    // 2.5. IMAM & MOSQUE REGISTRATION DIALOG
    if (showImamRegistrationDialog) {
        ImamRegistrationDialog(
            viewModel = viewModel,
            onDismiss = { showImamRegistrationDialog = false },
            onRegistrationSuccess = {
                showImamRegistrationDialog = false
                showPendingApprovalDialog = true
            }
        )
    }

    // 2.6. PENDING APPROVAL NOTICE DIALOG
    if (showPendingApprovalDialog) {
        AlertDialog(
            onDismissRequest = { showPendingApprovalDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(BgGoldTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.HourglassTop,
                        contentDescription = null,
                        tint = DeepEmerald,
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Application Submitted",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Your Imam & Mosque registration has been sent to the Angus Masail administration for verification and approval.",
                        fontSize = 13.sp,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = BgMintTint,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Info, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Once approved, your Imam Portal will unlock automatically.",
                                fontSize = 11.sp,
                                color = DeepEmerald
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.checkMyImamStatus { hasApp, isApproved ->
                            if (isApproved) {
                                showPendingApprovalDialog = false
                                Toast.makeText(context, "Approved! Welcome to the Imam Portal.", Toast.LENGTH_SHORT).show()
                                currentDestination = NavDestination.IMAM_DASHBOARD
                            } else {
                                Toast.makeText(context, "Application status: Pending review by Admin.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald)
                ) {
                    Text("Check Status")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPendingApprovalDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // 3. CONTACT DIALOG
    if (showContactDialog) {
        ContactDialog(
            selectedImam = selectedImam,
            onDismiss = { showContactDialog = false }
        )
    }

    // 4. Imam / Mosque Selector Bottom Sheet
    if (showImamSelectSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImamSelectSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Select Mosque / Congregation", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Choose a mosque to sync prayers & announcements", fontSize = 12.sp, color = TextSecondary)
                    }
                    Button(
                        onClick = {
                            if (currentUser == null) {
                                Toast.makeText(context, "Please log in to register a mosque", Toast.LENGTH_SHORT).show()
                                showLoginDialog = true
                            } else {
                                showRegisterMosqueDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IslamicGold,
                            contentColor = DeepEmerald
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.AddLocationAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Register", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (mosques.isNotEmpty()) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(mosques, key = { it.id }) { mosque ->
                            val isSelected = mosque.name.equals(selectedImam?.masjidName, ignoreCase = true) || mosque.id == selectedImam?.id
                            MosqueSelectionItem(
                                mosque = mosque,
                                isSelected = isSelected,
                                onSelect = {
                                    viewModel.selectMosque(mosque)
                                    showImamSelectSheet = false
                                    Toast.makeText(context, "Synced to ${mosque.name}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                } else if (imams.isNotEmpty()) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(imams, key = { it.id }) { imam ->
                            ImamSelectionItem(
                                imam = imam,
                                isSelected = imam.id == selectedImam?.id,
                                onSelect = {
                                    viewModel.selectImam(imam)
                                    showImamSelectSheet = false
                                    Toast.makeText(context, "Synced to ${imam.masjidName}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                } else {
                    Text("No Mosques found", color = TextSecondary)
                }
            }
        }
    }

    if (showRegisterMosqueDialog) {
        RegisterMosqueDialog(
            viewModel = viewModel,
            onDismiss = { showRegisterMosqueDialog = false },
            onSuccess = {
                showRegisterMosqueDialog = false
                viewModel.fetchMosques()
                viewModel.fetchAvailableMosques()
            }
        )
    }

    // 5. Books & Resources Sheet
    if (showBooksSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBooksSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Book, contentDescription = null, tint = IslamicGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Islamic Books & Resources", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Verified resources published by ${selectedImam?.imamName ?: "Imam"}", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(resources, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = PureCream),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(BgGoldTint, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Book, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                    if (item.description != null) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(item.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, fontSize = 12.sp)
                                    }
                                }
                                IconButton(onClick = { Toast.makeText(context, "Opening resource: ${item.title}", Toast.LENGTH_SHORT).show() }) {
                                    Icon(Icons.Rounded.Download, contentDescription = "Download", tint = DeepEmerald)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 6. 24h Status Dialog
    if (showStatusDialog) {
        val activeText = if (statuses.isNotEmpty()) statuses.first().contentText ?: "Active Notice" else "Jumu'ah Mubarak! Today's Khutbah will be on the virtues of Tahajjud and steadfastness in prayer."

        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Campaign, contentDescription = null, tint = IslamicGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Notice from ${selectedImam?.imamName ?: "Imam"}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(activeText, fontSize = 14.sp, color = TextPrimary, lineHeight = 22.sp)
            },
            confirmButton = {
                Button(
                    onClick = { showStatusDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald)
                ) {
                    Text("Understood")
                }
            }
        )
    }
}

@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    subtitle: String,
    iconTint: Color = DeepEmerald,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PureCream),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                Text(subtitle, fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}



@Composable
fun ContactDialog(selectedImam: ImamProfileDto?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.ContactSupport, contentDescription = null, tint = IslamicGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Contact & Community Help", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "For emergency fatwas, funeral arrangements or general inquiries:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                ContactInfoRow(icon = Icons.Rounded.Phone, label = "Toll-Free Helpline", value = "+91 98765 43210")
                ContactInfoRow(icon = Icons.Rounded.Email, label = "Email Support", value = "support@angusmasail.org")
                ContactInfoRow(icon = Icons.Rounded.LocationOn, label = "Active Masjid", value = selectedImam?.masjidName ?: "Central Angus Mosque")
                ContactInfoRow(icon = Icons.Rounded.AccountCircle, label = "Resident Scholar", value = selectedImam?.imamName ?: "Imam Syed Ahmed")

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        Toast.makeText(context, "Emergency hotline initiated", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.Call, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Call Central Helpline", color = DeepEmerald)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald)
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun ContactInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(label, fontSize = 10.sp, color = TextSecondary)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
    }
}

@Composable
fun ImamSelectionItem(imam: ImamProfileDto, isSelected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bouncingClickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BgGoldTint else PureCream
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, IslamicGold) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(imam.masjidName, style = MaterialTheme.typography.titleMedium, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text("${imam.imamName ?: "Imam"} • ${imam.fiqhSchool ?: "Sunni"}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, fontSize = 12.sp)
            }
            if (isSelected) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = IslamicGold)
            }
        }
    }
}

@Composable
fun FloatingBottomNavigation(
    currentDestination: NavDestination,
    userRole: String,
    pendingQueriesCount: Int,
    onNavigate: (NavDestination) -> Unit
) {
    // When logged in as IMAM: show Home, Qibla, Inquiries (Dashboard Queue), Consult Chats, Library (NO Masail form!)
    // When logged in as USER: show Home, Qibla, Masail (Ask Question), Chat, Available Imams
    val bottomDestinations = when (userRole) {
        "ROLE_IMAM" -> listOf(
            NavDestination.HOME,
            NavDestination.QIBLA,
            NavDestination.IMAM_DASHBOARD,
            NavDestination.CHAT,
            NavDestination.LIBRARY
        )
        "ROLE_ADMIN" -> listOf(
            NavDestination.HOME,
            NavDestination.QIBLA,
            NavDestination.ADMIN_DASHBOARD,
            NavDestination.CHAT,
            NavDestination.LIBRARY
        )
        else -> listOf(
            NavDestination.HOME,
            NavDestination.QIBLA,
            NavDestination.MASAIL,
            NavDestination.CHAT,
            NavDestination.AVAILABLE_IMAMS
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp, start = 18.dp, end = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassmorphicSurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp),
            shape = RoundedCornerShape(32.dp),
            backgroundColor = Color(0xFFFFFDF9).copy(alpha = 0.94f),
            borderColor = Color.White.copy(alpha = 0.95f)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomDestinations.forEach { destination ->
                    val isSelected = currentDestination == destination

                    val destLabel = when {
                        userRole == "ROLE_IMAM" && destination == NavDestination.IMAM_DASHBOARD -> "Inquiries"
                        userRole == "ROLE_IMAM" && destination == NavDestination.CHAT -> "Consults"
                        userRole == "ROLE_ADMIN" && destination == NavDestination.ADMIN_DASHBOARD -> "Admin"
                        else -> destination.label
                    }

                    val destIcon = when {
                        userRole == "ROLE_IMAM" && destination == NavDestination.IMAM_DASHBOARD && pendingQueriesCount > 0 -> Icons.Rounded.NotificationsActive
                        userRole == "ROLE_IMAM" && destination == NavDestination.IMAM_DASHBOARD -> Icons.Rounded.QuestionAnswer
                        else -> destination.icon
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .bouncingClickable { onNavigate(destination) }
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                if (destination == NavDestination.IMAM_DASHBOARD && pendingQueriesCount > 0) {
                                    Badge(
                                        containerColor = Color(0xFFC0392B),
                                        contentColor = Color.White
                                    ) {
                                        Text("$pendingQueriesCount", fontSize = 9.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = destIcon,
                                contentDescription = destLabel,
                                tint = if (isSelected) DeepEmerald else TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = destLabel,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) DeepEmerald else TextSecondary
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun MosqueSelectionItem(
    mosque: MosqueDto,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bouncingClickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) BgGoldTint else PureCream
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, IslamicGold) else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0D7C6))
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        mosque.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (mosque.verified) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Rounded.Verified, contentDescription = "Verified", tint = IslamicGold, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${mosque.city}${if (!mosque.address.isNullOrBlank()) " • ${mosque.address}" else ""} • ${mosque.fiqhSchool ?: "Sunni"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val imamText = if (mosque.hasImam && !mosque.currentImamName.isNullOrBlank()) "Imam: ${mosque.currentImamName}" else "Open (No Imam Assigned)"
                val imamColor = if (mosque.hasImam) DeepEmerald else TextSecondary
                Text(
                    text = imamText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = imamColor
                )
            }
            if (isSelected) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = IslamicGold)
            }
        }
    }
}
