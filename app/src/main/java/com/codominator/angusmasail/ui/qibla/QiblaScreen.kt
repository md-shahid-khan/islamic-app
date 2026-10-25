package com.codominator.angusmasail.ui.qibla

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codominator.angusmasail.ui.components.GlassmorphicSurface
import com.codominator.angusmasail.ui.theme.*
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun QiblaScreen(
    userLat: Double? = null,
    userLng: Double? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Calculate Qibla bearing from coordinates or fallback to Mecca default (e.g., 275°)
    val qiblaDirection = remember(userLat, userLng) {
        if (userLat != null && userLng != null) {
            calculateQiblaBearing(userLat, userLng)
        } else {
            275.0f
        }
    }

    var currentAzimuth by remember { mutableFloatStateOf(0f) }
    var sensorAvailable by remember { mutableStateOf(true) }

    // Hardware Sensor Manager
    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        var lastAccel = FloatArray(3)
        var lastMag = FloatArray(3)
        var hasAccel = false
        var hasMag = false

        val listener = object : SensorEventListener {
            val rotationMatrix = FloatArray(9)
            val orientation = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val degrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    currentAzimuth = (degrees + 360f) % 360f
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    lastAccel = event.values.clone()
                    hasAccel = true
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    lastMag = event.values.clone()
                    hasMag = true
                }

                if (hasAccel && hasMag && rotationSensor == null) {
                    if (SensorManager.getRotationMatrix(rotationMatrix, null, lastAccel, lastMag)) {
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        val degrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        currentAzimuth = (degrees + 360f) % 360f
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else if (accelSensor != null && magSensor != null) {
            sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            sensorAvailable = false
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Shortest-distance angle delta to target Qibla
    val delta = ((qiblaDirection - currentAzimuth + 540f) % 360f) - 180f
    val isAligned = abs(delta) <= 3.0f

    // Needle rotation relative to top
    val targetDialRotation = -currentAzimuth

    val animatedRotation by animateFloatAsState(
        targetValue = targetDialRotation,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "compassRotation"
    )

    // Trigger subtle haptic pulse when Kaaba alignment is reached
    LaunchedEffect(isAligned) {
        if (isAligned) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(WarmAlabaster, WarmSand)))
            .padding(top = 40.dp, start = 20.dp, end = 20.dp, bottom = 90.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Top Icon & Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Home, contentDescription = "Kaaba", tint = IslamicGold, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Qibla Direction",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Kaaba Bearing: ${qiblaDirection.roundToInt()}° • Mecca Al-Mukarramah",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Compass Outer Ring
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(Color.White.copy(alpha = 0.55f), CircleShape)
                .border(
                    width = if (isAligned) 3.dp else 1.5.dp,
                    color = if (isAligned) IslamicGold else Color.White,
                    shape = CircleShape
                )
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            // Rotating Dial
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(animatedRotation)
                    .background(PureCream, CircleShape)
                    .border(1.dp, Color(0xFFEADBCE), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Compass Cardinal Marks
                Text(
                    text = "N",
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
                    color = Color(0xFFC0392B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "S",
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "E",
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "W",
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp),
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                // Kaaba marker on the rotating ring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(qiblaDirection),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 28.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isAligned) IslamicGold else TextPrimary)
                                .border(1.dp, IslamicGold, RoundedCornerShape(3.dp))
                        )
                    }
                }

                // Needle
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // North Needle Pointer
                    Box(
                        modifier = Modifier
                            .size(6.dp, 82.dp)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(if (isAligned) IslamicGold else DeepEmerald)
                    )
                    // Pivot Center
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(if (isAligned) IslamicGold else DeepEmerald, CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                    )
                    // South Needle Tail
                    Box(
                        modifier = Modifier
                            .size(5.dp, 82.dp)
                            .clip(RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp))
                            .background(Color(0xFFD4C9BD))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Dynamic Degree Reading
        Text(
            text = "${currentAzimuth.roundToInt()}°",
            style = MaterialTheme.typography.displayLarge,
            color = if (isAligned) IslamicGold else TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Heading",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Dynamic Turn Instruction Pill
        if (isAligned) {
            GlassmorphicSurface(
                backgroundColor = IslamicGold.copy(alpha = 0.22f),
                borderColor = IslamicGold.copy(alpha = 0.55f),
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Home, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "You are facing the Holy Kaaba",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IslamicGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            val instruction = if (delta > 0) {
                "Turn right by ${delta.roundToInt()}°"
            } else {
                "Turn left by ${abs(delta).roundToInt()}°"
            }

            GlassmorphicSurface(
                backgroundColor = Color.White.copy(alpha = 0.75f),
                borderColor = Color(0xFFDDCFB9),
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Explore, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = instruction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (!sensorAvailable) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Sensors, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Compass sensor not detected. Using simulated heading.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// Spherical trigonometry formula for Kaaba bearing
fun calculateQiblaBearing(lat: Double, lng: Double): Float {
    val kaabaLat = Math.toRadians(21.422487)
    val kaabaLng = Math.toRadians(39.826206)
    val userLat = Math.toRadians(lat)
    val userLng = Math.toRadians(lng)

    val deltaLng = kaabaLng - userLng
    val y = Math.sin(deltaLng)
    val x = Math.cos(userLat) * Math.tan(kaabaLat) - Math.sin(userLat) * Math.cos(deltaLng)
    val angle = Math.toDegrees(Math.atan2(y, x))
    return ((angle + 360f) % 360f).toFloat()
}
