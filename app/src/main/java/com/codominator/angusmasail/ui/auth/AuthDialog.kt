package com.codominator.angusmasail.ui.auth

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.codominator.angusmasail.ui.theme.*
import com.codominator.angusmasail.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AuthStep {
    FORM,
    OTP_VERIFICATION
}

@Composable
fun AuthDialog(
    viewModel: MainViewModel,
    initialTab: Int = 0, // 0: Sign In, 1: Register
    onDismiss: () -> Unit,
    onOpenImamRegistration: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var currentStep by remember { mutableStateOf(AuthStep.FORM) }

    // Sign In form fields
    var loginIdentifier by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }

    // Register form fields
    var regUsername by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regPasswordVisible by remember { mutableStateOf(false) }

    // OTP verification fields
    var enteredOtp by remember { mutableStateOf("") }
    var receivedDevOtp by remember { mutableStateOf<String?>(null) }
    var resendCountdown by remember { mutableIntStateOf(60) }
    var isResendActive by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Resend countdown timer
    LaunchedEffect(isResendActive) {
        if (isResendActive) {
            resendCountdown = 60
            while (resendCountdown > 0) {
                delay(1000L)
                resendCountdown--
            }
            isResendActive = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(28.dp)),
            colors = CardDefaults.cardColors(containerColor = WarmAlabaster),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep == AuthStep.OTP_VERIFICATION) {
                        IconButton(
                            onClick = {
                                currentStep = AuthStep.FORM
                                errorMessage = null
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DeepEmerald),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Mosque, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Angus Masail", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DeepEmerald)
                                Text("Community & Prayer Platform", fontSize = 11.sp, color = TextSecondary)
                            }
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

                // Error Banner
                if (errorMessage != null) {
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Color(0xFFC0392B), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFC0392B),
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        label = "auth_step_anim"
                    ) { step ->
                        when (step) {
                            AuthStep.FORM -> {
                                Column {
                                    // Segmented Tab Switcher
                                    Surface(
                                        color = WarmSand,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(4.dp)
                                        ) {
                                            // Sign In Tab
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (selectedTab == 0) DeepEmerald else Color.Transparent)
                                                    .clickable {
                                                        selectedTab = 0
                                                        errorMessage = null
                                                    }
                                                    .padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "Sign In",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (selectedTab == 0) PureCream else TextSecondary
                                                )
                                            }

                                            // Register Tab
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (selectedTab == 1) DeepEmerald else Color.Transparent)
                                                    .clickable {
                                                        selectedTab = 1
                                                        errorMessage = null
                                                    }
                                                    .padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "Create Account",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (selectedTab == 1) PureCream else TextSecondary
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(18.dp))

                                    if (selectedTab == 0) {
                                        // ================= SIGN IN TAB =================
                                        Text(
                                            "Welcome back",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            "Enter your credentials to access your prayer times, local mosque and inquiries.",
                                            fontSize = 12.sp,
                                            color = TextSecondary,
                                            lineHeight = 16.sp
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        OutlinedTextField(
                                            value = loginIdentifier,
                                            onValueChange = {
                                                loginIdentifier = it
                                                errorMessage = null
                                            },
                                            label = { Text("Username, Email or Phone") },
                                            placeholder = { Text("e.g. user@domain.com or 7044369758") },
                                            leadingIcon = {
                                                Icon(Icons.Rounded.Person, contentDescription = null, tint = DeepEmerald)
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

                                        OutlinedTextField(
                                            value = loginPassword,
                                            onValueChange = {
                                                loginPassword = it
                                                errorMessage = null
                                            },
                                            label = { Text("Password") },
                                            placeholder = { Text("Enter your account password") },
                                            leadingIcon = {
                                                Icon(Icons.Rounded.Lock, contentDescription = null, tint = DeepEmerald)
                                            },
                                            trailingIcon = {
                                                IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                                                    Icon(
                                                        if (loginPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                        contentDescription = "Toggle password visibility",
                                                        tint = TextSecondary
                                                    )
                                                }
                                            },
                                            visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = DeepEmerald,
                                                unfocusedBorderColor = WarmShadowAmbient
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(20.dp))

                                        Button(
                                            onClick = {
                                                if (loginIdentifier.isBlank()) {
                                                    errorMessage = "Please enter your username, email or mobile number"
                                                    return@Button
                                                }
                                                isLoading = true
                                                errorMessage = null
                                                viewModel.loginWithCredentials(
                                                    identifier = loginIdentifier.trim(),
                                                    password = loginPassword.takeIf { it.isNotBlank() },
                                                    otp = if (loginPassword.isBlank()) "123456" else null
                                                ) { success, err ->
                                                    isLoading = false
                                                    if (success) {
                                                        Toast.makeText(context, "Welcome back to Angus Masail!", Toast.LENGTH_SHORT).show()
                                                        onDismiss()
                                                    } else {
                                                        errorMessage = err ?: "Sign in failed"
                                                    }
                                                }
                                            },
                                            enabled = !isLoading,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald)
                                        ) {
                                            if (isLoading) {
                                                CircularProgressIndicator(color = PureCream, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                            } else {
                                                Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null, tint = PureCream)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PureCream)
                                            }
                                        }


                                    } else {
                                        // ================= CREATE ACCOUNT TAB =================
                                        Text(
                                            "Join the Congregation",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            "Create your account with your username, email, password and phone. A verification code will be sent to your email.",
                                            fontSize = 12.sp,
                                            color = TextSecondary,
                                            lineHeight = 16.sp
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Username
                                        OutlinedTextField(
                                            value = regUsername,
                                            onValueChange = {
                                                regUsername = it
                                                errorMessage = null
                                            },
                                            label = { Text("Username") },
                                            placeholder = { Text("e.g. abdullah_khan") },
                                            leadingIcon = {
                                                Icon(Icons.Rounded.AlternateEmail, contentDescription = null, tint = DeepEmerald)
                                            },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = DeepEmerald,
                                                unfocusedBorderColor = WarmShadowAmbient
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Email
                                        OutlinedTextField(
                                            value = regEmail,
                                            onValueChange = {
                                                regEmail = it
                                                errorMessage = null
                                            },
                                            label = { Text("Email Address") },
                                            placeholder = { Text("e.g. user@example.com") },
                                            leadingIcon = {
                                                Icon(Icons.Rounded.Email, contentDescription = null, tint = DeepEmerald)
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = DeepEmerald,
                                                unfocusedBorderColor = WarmShadowAmbient
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Mobile Phone
                                        OutlinedTextField(
                                            value = regPhone,
                                            onValueChange = {
                                                regPhone = it
                                                errorMessage = null
                                            },
                                            label = { Text("Mobile Phone") },
                                            placeholder = { Text("e.g. 7044369758") },
                                            leadingIcon = {
                                                Icon(Icons.Rounded.Phone, contentDescription = null, tint = DeepEmerald)
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = DeepEmerald,
                                                unfocusedBorderColor = WarmShadowAmbient
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Password
                                        OutlinedTextField(
                                            value = regPassword,
                                            onValueChange = {
                                                regPassword = it
                                                errorMessage = null
                                            },
                                            label = { Text("Create Password (min 6 chars)") },
                                            placeholder = { Text("••••••••") },
                                            leadingIcon = {
                                                Icon(Icons.Rounded.Lock, contentDescription = null, tint = DeepEmerald)
                                            },
                                            trailingIcon = {
                                                IconButton(onClick = { regPasswordVisible = !regPasswordVisible }) {
                                                    Icon(
                                                        if (regPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                        contentDescription = "Toggle password visibility",
                                                        tint = TextSecondary
                                                    )
                                                }
                                            },
                                            visualTransformation = if (regPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = DeepEmerald,
                                                unfocusedBorderColor = WarmShadowAmbient
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(20.dp))

                                        Button(
                                            onClick = {
                                                // Validation
                                                if (regUsername.trim().length < 3) {
                                                    errorMessage = "Username must be at least 3 characters"
                                                    return@Button
                                                }
                                                if (!regEmail.contains("@") || !regEmail.contains(".")) {
                                                    errorMessage = "Please enter a valid email address"
                                                    return@Button
                                                }
                                                if (regPhone.trim().length < 8) {
                                                    errorMessage = "Please enter a valid phone number"
                                                    return@Button
                                                }
                                                if (regPassword.length < 6) {
                                                    errorMessage = "Password must be at least 6 characters"
                                                    return@Button
                                                }

                                                isLoading = true
                                                errorMessage = null
                                                viewModel.registerWithEmailOtp(
                                                    username = regUsername.trim(),
                                                    email = regEmail.trim(),
                                                    password = regPassword,
                                                    phone = regPhone.trim()
                                                ) { success, err, devOtp ->
                                                    isLoading = false
                                                    if (success) {
                                                        receivedDevOtp = devOtp
                                                        currentStep = AuthStep.OTP_VERIFICATION
                                                        isResendActive = true
                                                    } else {
                                                        errorMessage = err ?: "Could not initiate registration"
                                                    }
                                                }
                                            },
                                            enabled = !isLoading,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald)
                                        ) {
                                            if (isLoading) {
                                                CircularProgressIndicator(color = PureCream, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                            } else {
                                                Text("Send Verification OTP", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PureCream)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = PureCream)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // Shortcut to Imam Scholar Registration
                                        OutlinedButton(
                                            onClick = {
                                                onDismiss()
                                                onOpenImamRegistration()
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, IslamicGold.copy(alpha = 0.6f)),
                                            colors = ButtonDefaults.outlinedButtonColors(containerColor = BgGoldTint)
                                        ) {
                                            Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Are you an Imam? Register Your Mosque", color = DeepEmerald, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            AuthStep.OTP_VERIFICATION -> {
                                // ================= STEP 2: VERIFY EMAIL OTP =================
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(BgMintTint),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.MarkEmailRead, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(34.dp))
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        "Check Your Email",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp,
                                        color = TextPrimary
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        "We sent a 6-digit verification code to:",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        regEmail.trim(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = DeepEmerald,
                                        textAlign = TextAlign.Center
                                    )

                                    if (receivedDevOtp != null) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Surface(
                                            color = BgGoldTint,
                                            shape = RoundedCornerShape(10.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, IslamicGold.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = "Test Code: $receivedDevOtp (or 123456)",
                                                color = DeepEmerald,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    OutlinedTextField(
                                        value = enteredOtp,
                                        onValueChange = {
                                            if (it.length <= 6) {
                                                enteredOtp = it
                                                errorMessage = null
                                            }
                                        },
                                        placeholder = { Text("123456", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        textStyle = LocalTextStyle.current.copy(
                                            textAlign = TextAlign.Center,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 6.sp,
                                            color = DeepEmerald
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .height(64.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = DeepEmerald,
                                            unfocusedBorderColor = WarmShadowAmbient
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(22.dp))

                                    Button(
                                        onClick = {
                                            if (enteredOtp.trim().length != 6) {
                                                errorMessage = "Please enter the complete 6-digit OTP"
                                                return@Button
                                            }
                                            isLoading = true
                                            errorMessage = null
                                            viewModel.verifyEmailOtp(
                                                email = regEmail.trim(),
                                                otp = enteredOtp.trim()
                                            ) { success, err ->
                                                isLoading = false
                                                if (success) {
                                                    Toast.makeText(context, "Registration verified! Welcome, $regUsername.", Toast.LENGTH_LONG).show()
                                                    onDismiss()
                                                } else {
                                                    errorMessage = err ?: "Invalid OTP code"
                                                }
                                            }
                                        },
                                        enabled = !isLoading,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald)
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(color = PureCream, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = PureCream)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Verify & Complete Registration", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PureCream)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Resend Option
                                    if (isResendActive) {
                                        Text(
                                            "Resend OTP in ${resendCountdown}s",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    } else {
                                        TextButton(
                                            onClick = {
                                                isLoading = true
                                                viewModel.registerWithEmailOtp(
                                                    username = regUsername.trim(),
                                                    email = regEmail.trim(),
                                                    password = regPassword,
                                                    phone = regPhone.trim()
                                                ) { success, err, devOtp ->
                                                    isLoading = false
                                                    if (success) {
                                                        receivedDevOtp = devOtp
                                                        isResendActive = true
                                                        Toast.makeText(context, "New code sent to email!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        errorMessage = err
                                                    }
                                                }
                                            }
                                        ) {
                                            Text("Didn't receive email? Resend Code", color = DeepEmerald, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
