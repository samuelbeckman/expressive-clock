package app.expressiveclock.ui.screens

import android.app.Activity
import android.app.NotificationManager
import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.expressiveclock.overlay.OverlayAccessibilityService
import kotlin.math.cos
import kotlin.math.sin

/**
 * Material 3 Expressive scalloped shape for onboarding permission icons.
 */
private val OnboardingScallopedShape: Shape = GenericShape { size, _ ->
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val outerRadius = minOf(size.width, size.height) / 2f
    val innerRadius = outerRadius * 0.88f
    val lobes = 10
    val angleStep = (2 * Math.PI / lobes).toFloat()

    for (i in 0 until lobes) {
        val angle = i * angleStep
        val nextAngle = (i + 1) * angleStep
        val midAngle = angle + angleStep / 2f

        val xOuter = centerX + outerRadius * cos(midAngle)
        val yOuter = centerY + outerRadius * sin(midAngle)
        val xInner = centerX + innerRadius * cos(nextAngle)
        val yInner = centerY + innerRadius * sin(nextAngle)

        if (i == 0) {
            val xStart = centerX + innerRadius * cos(0f)
            val yStart = centerY + innerRadius * sin(0f)
            moveTo(xStart, yStart)
        }

        quadraticTo(xOuter, yOuter, xInner, yInner)
    }
    close()
}

/**
 * Dedicated first-launch setup screen for Expressive Clock permissions.
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isNotificationsGranted by remember { mutableStateOf(checkNotificationsGranted(context)) }
    var isFullScreenIntentGranted by remember { mutableStateOf(checkFullScreenIntentGranted(context)) }
    var isBackgroundGranted by remember { mutableStateOf(checkBackgroundGranted(context)) }
    var isAccessibilityGranted by remember { mutableStateOf(checkAccessibilityGranted(context)) }

    fun refreshStates() {
        isNotificationsGranted = checkNotificationsGranted(context)
        isFullScreenIntentGranted = checkFullScreenIntentGranted(context)
        isBackgroundGranted = checkBackgroundGranted(context)
        isAccessibilityGranted = checkAccessibilityGranted(context)
    }

    // Auto-refresh permission statuses whenever returning to app from Android Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // POST_NOTIFICATIONS runtime permission launcher (API 33+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        refreshStates()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
    ) {
        Spacer(Modifier.height(28.dp))

        // Title
        Text(
            text = "Welcome to\nExpressive Clock",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 38.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(12.dp))

        // Subtitle
        Text(
            text = "A few quick permissions will make sure alarms, timers and the Dynamic Island work reliably, even when the app is closed.",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                lineHeight = 22.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(28.dp))

        // Card 1: Notifications
        OnboardingSetupCard(
            title = "Notifications",
            description = "Allows Expressive Clock to show Alarm, Timer and Stopwatch notifications, including when the app is closed.",
            icon = Icons.Outlined.NotificationsNone,
            iconBgColor = Color(0xFFFFDADA),
            iconTintColor = Color(0xFFBA1A1A),
            buttonBgColor = Color(0xFFFFDAD6),
            buttonTextColor = Color(0xFF410002),
            isGranted = isNotificationsGranted,
            onSetup = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    launchIntent(
                        context,
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                }
            },
        )

        Spacer(Modifier.height(14.dp))

        // Card 2: Full-screen alarms
        OnboardingSetupCard(
            title = "Full-screen alarms",
            description = "Allows ringing alarms to display full-screen over your lock screen. If unavailable, alarms will display as high-priority heads-up notifications with dismiss and snooze controls.",
            icon = Icons.Outlined.Alarm,
            iconBgColor = Color(0xFFFFDCCA),
            iconTintColor = Color(0xFF8B4A00),
            buttonBgColor = Color(0xFFFFDCCA),
            buttonTextColor = Color(0xFF3B1A00),
            isGranted = isFullScreenIntentGranted,
            onSetup = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    launchIntent(context, intent) {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    }
                } else {
                    launchIntent(
                        context,
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                }
            },
        )

        Spacer(Modifier.height(14.dp))

        // Card 3: Background operation
        OnboardingSetupCard(
            title = "Background operation",
            description = "Prevent Android from restricting Expressive Clock so your alarms, timers and stopwatch keep running reliably.",
            icon = Icons.Outlined.BatteryChargingFull,
            iconBgColor = Color(0xFFD8EED8),
            iconTintColor = Color(0xFF1E631E),
            buttonBgColor = Color(0xFFD8EED8),
            buttonTextColor = Color(0xFF072107),
            isGranted = isBackgroundGranted,
            onSetup = {
                Log.d("DynamicIslandOverlay", "SETUP BATTERY DIRECT REQUEST")
                @Suppress("BATTERY_CHARGING_STATE")
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                launchIntent(context, intent) {
                    Log.d("DynamicIslandOverlay", "SETUP BATTERY FALLBACK")
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                }
            },
        )

        Spacer(Modifier.height(14.dp))

        // Card 4: Accessibility (optional)
        OnboardingSetupCard(
            title = "Accessibility (optional)",
            description = "Enable the Dynamic Island to show active timers and stopwatch controls over other apps, with interactive controls (e.g. pause, lap, dismiss). No personal data or screen content is read or collected.",
            icon = Icons.Outlined.AccessibilityNew,
            iconBgColor = Color(0xFFD7E3FF),
            iconTintColor = Color(0xFF13428B),
            buttonBgColor = Color(0xFFD7E3FF),
            buttonTextColor = Color(0xFF001B3F),
            isGranted = isAccessibilityGranted,
            onSetup = {
                Log.d("DynamicIslandOverlay", "SETUP ACCESSIBILITY REQUEST")
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                val launched = launchIntent(context, intent)
                if (launched) {
                    Toast.makeText(context, "Tap Expressive Clock and turn on Accessibility.", Toast.LENGTH_LONG).show()
                }
            },
        )

        Spacer(Modifier.height(28.dp))

        // Main Continue Primary Button
        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Text(
                text = "Continue",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                ),
            )
        }

        Spacer(Modifier.height(8.dp))

        // Subdued Set up later Text Button
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            TextButton(
                onClick = onComplete,
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                Text(
                    text = "Set up later",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Privacy Policy Link
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            TextButton(
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://samuelbeckman.github.io/expressive-clock/privacy-policy.html"),
                    )
                    runCatching { context.startActivity(intent) }
                },
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                Text(
                    text = "Privacy policy",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 14.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun OnboardingSetupCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTintColor: Color,
    buttonBgColor: Color,
    buttonTextColor: Color,
    isGranted: Boolean,
    onSetup: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(enabled = !isGranted, onClick = onSetup)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Scalloped Icon Container
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(OnboardingScallopedShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTintColor,
                    modifier = Modifier.size(24.dp),
                )
            }

            // Title + Description + Setup Button
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false),
                    )

                    // Set up / Already granted Button
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isGranted) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
                                else buttonBgColor
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = if (isGranted) "Already granted" else "Set up",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                ),
                                color = if (isGranted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                else buttonTextColor,
                            )
                            if (!isGranted) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = buttonTextColor,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun checkNotificationsGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}

private fun checkFullScreenIntentGranted(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.canUseFullScreenIntent() ?: true
    } else {
        true
    }
}

private fun checkBackgroundGranted(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    return pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
}

fun checkAccessibilityGranted(context: Context): Boolean {
    val enabledServicesSetting = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false

    val expectedComponent = ComponentName(context, OverlayAccessibilityService::class.java)
    val colonSplitter = TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServicesSetting)

    while (colonSplitter.hasNext()) {
        val componentNameString = colonSplitter.next()
        val enabledComponent = ComponentName.unflattenFromString(componentNameString)
        if (enabledComponent != null && enabledComponent == expectedComponent) {
            return true
        }
    }
    return false
}

private fun launchIntent(context: Context, intent: Intent, fallbackProvider: (() -> Intent)? = null): Boolean {
    val pm = context.packageManager
    val isIgnoringBefore = checkBackgroundGranted(context)
    Log.d("ExpressiveClock.BatteryOptimization", "launchIntent: isIgnoringBefore=$isIgnoringBefore, action=${intent.action}, data=${intent.data}")

    val activity = context as? Activity
    val result = runCatching {
        val resolved = intent.resolveActivity(pm)
        Log.d("ExpressiveClock.BatteryOptimization", "launchIntent: intent.resolveActivity = $resolved")

        // Crucial: Do NOT add FLAG_ACTIVITY_NEW_TASK when launched from an Activity context!
        // Adding FLAG_ACTIVITY_NEW_TASK causes Android to create a new Settings Task window behind the dialog.
        // Launching directly from an Activity context renders the battery optimization confirmation as a true dialog overlay!
        if (activity != null) {
            activity.startActivity(intent)
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
        Log.d("ExpressiveClock.BatteryOptimization", "launchIntent: startActivity SUCCESS for action=${intent.action}")
        true
    }.getOrElse { ex ->
        Log.e("ExpressiveClock.BatteryOptimization", "launchIntent: startActivity FAILED for action=${intent.action}", ex)
        val fallback = fallbackProvider?.invoke()
        if (fallback != null) {
            runCatching {
                val fallbackResolved = fallback.resolveActivity(pm)
                Log.d("ExpressiveClock.BatteryOptimization", "launchIntent: FALLBACK intent.resolveActivity = $fallbackResolved")
                if (activity != null) {
                    activity.startActivity(fallback)
                } else {
                    fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(fallback)
                }
                Log.d("ExpressiveClock.BatteryOptimization", "launchIntent: FALLBACK startActivity SUCCESS for action=${fallback.action}")
                true
            }.getOrElse { fallbackEx ->
                Log.e("ExpressiveClock.BatteryOptimization", "launchIntent: FALLBACK startActivity FAILED", fallbackEx)
                false
            }
        } else false
    }
    return result
}
