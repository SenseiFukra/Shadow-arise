package com.example.ui.screens

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.lerp
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ChatMessage
import com.example.HunterViewModel
import com.example.Sender
import com.example.data.database.DailyQuest
import com.example.data.database.HunterProfile
import com.example.data.database.WorkoutLog
import com.example.data.models.Exercise
import com.example.data.repository.ExerciseData
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ShadowAriseApp(
    viewModel: HunterViewModel = viewModel(factory = HunterViewModel.Factory(LocalContext.current.applicationContext as Application))
) {
    val profileState by viewModel.profile.collectAsStateWithLifecycle()
    val workouts by viewModel.workouts.collectAsStateWithLifecycle()
    val quests by viewModel.quests.collectAsStateWithLifecycle()
    val bmiHistory by viewModel.bmiHistory.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val levelUpEvent by viewModel.levelUpEvent.collectAsStateWithLifecycle()
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val chatLoading by viewModel.chatLoading.collectAsStateWithLifecycle()

    var lastWorkoutCount by remember { mutableStateOf(-1) }
    var lastCompletedQuestsCount by remember { mutableStateOf(-1) }
    var showMissionCompletePopup by remember { mutableStateOf(false) }

    LaunchedEffect(workouts, quests) {
        val currentWorkoutCount = workouts.size
        val totalQuests = quests.size
        val currentCompletedQuestsCount = quests.count { it.isCompleted }

        // Initialize state to avoid popup showing on initial database load
        if (lastWorkoutCount == -1) {
            lastWorkoutCount = currentWorkoutCount
        }
        if (lastCompletedQuestsCount == -1) {
            lastCompletedQuestsCount = currentCompletedQuestsCount
        }

        // Check if a new workout was logged
        val workoutLoggedDiff = currentWorkoutCount > lastWorkoutCount
        
        // Check if all quests are checked off complete
        val allQuestsCompletedDiff = totalQuests > 0 && 
                                     currentCompletedQuestsCount == totalQuests && 
                                     currentCompletedQuestsCount > lastCompletedQuestsCount

        if (workoutLoggedDiff || allQuestsCompletedDiff) {
            showMissionCompletePopup = true
        }

        lastWorkoutCount = currentWorkoutCount
        lastCompletedQuestsCount = currentCompletedQuestsCount
    }

    var appScreenState by remember { mutableStateOf("SPLASH") }
    var activeTab by remember { mutableStateOf("HOME") } // HELP, HOME, ACCOUNT
    var showChatDrawer by remember { mutableStateOf(false) }

    // Monitor profile updates to route properly (when not in splash screen)
    val handleSplashComplete = {
        if (profileState == null) {
            appScreenState = "LOGIN"
        } else if (profileState?.height == 0.0 || profileState?.weight == 0.0) {
            appScreenState = "ONBOARDING"
        } else {
            appScreenState = "MAIN"
        }
    }

    LaunchedEffect(profileState) {
        val p = profileState
        if (appScreenState != "SPLASH" && appScreenState != "LOGIN") {
            if (p == null) {
                appScreenState = "LOGIN"
            } else if (p.height == 0.0 || p.weight == 0.0) {
                appScreenState = "ONBOARDING"
            } else if (appScreenState == "ONBOARDING") {
                appScreenState = "MAIN"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepPurpleBg)
    ) {
        // Particle background animation
        BreathingBackgroundParticles()

        // Screen routing with elegant crossfade animation
        AnimatedContent(
            targetState = appScreenState,
            transitionSpec = {
                fadeIn(animationSpec = tween(500)) with fadeOut(animationSpec = tween(500))
            },
            label = "ScreenTransition"
        ) { targetScreen ->
            when (targetScreen) {
                "SPLASH" -> SplashScreenView(onComplete = { handleSplashComplete() })
                "LOGIN" -> LoginScreenView(
                    viewModel = viewModel,
                    onLoginSuccess = { isFirstTime ->
                        if (isFirstTime) {
                            appScreenState = "ONBOARDING"
                        } else {
                            appScreenState = "MAIN"
                        }
                    }
                )
                "ONBOARDING" -> OnboardingScreenView(
                    profile = profileState,
                    onOnboardingComplete = { h, w, age, g, u, act ->
                        viewModel.completeOnboarding(h, w, age, g, u, act)
                        appScreenState = "MAIN"
                    }
                )
                "MAIN" -> {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent,
                        topBar = {
                            HunterAppBar(
                                profile = profileState,
                                isOnline = isOnline,
                                xpScale = { viewModel.repository.getRankName(it) }
                            )
                        },
                        bottomBar = {
                            GlassBottomNavigation(
                                activeTab = activeTab,
                                onTabSelected = { activeTab = it }
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (activeTab) {
                                "EXERCISE" -> ExerciseScreenView(viewModel = viewModel, profile = profileState)
                                "HOME" -> HomeScreenView(
                                    viewModel = viewModel,
                                    profile = profileState,
                                    quests = quests
                                )
                                "ACCOUNT" -> AccountScreenView(viewModel = viewModel, profile = profileState, bmiHistory = bmiHistory, workouts = workouts)
                            }

                            // Companion FAB (glowing orb)
                            if (profileState?.aiChatEnabled == true) {
                                FloatingCompanionFab(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(bottom = 16.dp, end = 16.dp),
                                    isOnline = isOnline,
                                    onClick = { showChatDrawer = true }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Full Screen Partner-Chat Drawer Overlay
        if (showChatDrawer) {
            ChatDrawerOverlay(
                chatHistory = chatHistory,
                chatLoading = chatLoading,
                isOnline = isOnline,
                profile = profileState,
                onSendMessage = { viewModel.sendChatMessage(it) },
                onClose = { showChatDrawer = false },
                onSaveVisual = { viewModel.saveToGallery(it) }
            )
        }

        // Level Up Fullscreen Popup Event
        levelUpEvent?.let { (oldRank, newRank) ->
            LevelUpOverlay(
                oldRank = oldRank,
                newRank = newRank,
                onDismiss = { viewModel.dismissLevelUp() }
            )
        }

        // Dynamic slide-down Mission Complete Popup Overlay
        AnimatedVisibility(
            visible = showMissionCompletePopup,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            LaunchedEffect(showMissionCompletePopup) {
                if (showMissionCompletePopup) {
                    delay(2500)
                    showMissionCompletePopup = false
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF12121E).copy(alpha = 0.95f), RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.5.dp, Color(0xFF6C63FF).copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                    .drawBehind {
                        // Green left-accent line
                        drawRect(
                            color = Color(0xFF00FF87),
                            topLeft = Offset(0f, 0f),
                            size = Size(4.dp.toPx(), size.height)
                        )
                    }
                    .padding(vertical = 14.dp, horizontal = 18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Complete",
                            tint = Color(0xFF00FF87),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "MISSION COMPLETE",
                                color = Color(0xFF00FF87),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = "You have cleared all active system tasks.",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// BACKGROUND PARTICLES COMPOSABLE
// ==========================================
@Composable
fun BreathingBackgroundParticles() {
    val transition = rememberInfiniteTransition(label = "BackgroundBreathing")
    val animScale by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ParticleBreathing"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val points = listOf(
            Offset(size.width * 0.15f, size.height * 0.2f),
            Offset(size.width * 0.85f, size.height * 0.15f),
            Offset(size.width * 0.5f, size.height * 0.5f),
            Offset(size.width * 0.25f, size.height * 0.75f),
            Offset(size.width * 0.75f, size.height * 0.8f)
        )

        points.forEach { point ->
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GlowPurple.copy(alpha = 0.2f * animScale), Color.Transparent),
                    center = point,
                    radius = 180.dp.toPx()
                ),
                center = point,
                radius = 180.dp.toPx()
            )
            drawCircle(
                color = BrightPurpleHighlight.copy(alpha = 0.4f * animScale),
                center = point,
                radius = 3.dp.toPx()
            )
        }
    }
}

// ==========================================
// SPLASH SCREEN VIEW
// ==========================================
enum class LoadingScreenStyle(
    val primaryColor: Color,
    val accentColor: Color,
    val headerTitle: String,
    val systemText1: String,
    val systemText2: String,
    val systemText3: String,
    val particleColor1: Color,
    val particleColor2: Color
) {
    CHIBI_SUNG(
        primaryColor = Color(0xFF6C63FF),
        accentColor = Color(0xFF00D4FF),
        headerTitle = "[ HUNTER GATES ACTIVE ]",
        systemText1 = "[ SYSTEM ] INITIALIZING HUNTER DATABASE...",
        systemText2 = "[ STATUS ] ACCESSING SHADOW DIMENSIONS...",
        systemText3 = "[ DECREE ] SHADOW ARISE INITIALIZATION COMPLETED!",
        particleColor1 = Color(0xFF6C63FF),
        particleColor2 = Color(0xFF00D4FF)
    ),
    WARRIOR_GIRL(
        primaryColor = Color(0xFF6C63FF),
        accentColor = Color(0xFFFFDD00),
        headerTitle = "[ SHADOW RESURRECTION GATEWAY ]",
        systemText1 = "[ SYSTEM ] LOADING HUNTER SOUL CORES...",
        systemText2 = "[ STATUS ] PREPARING SHADOW SOLDIERS SUMMON...",
        systemText3 = "[ SWORD  ] LIGHT SYSTEM SECURED: COMMAND ARISE!",
        particleColor1 = Color(0xFF6C63FF),
        particleColor2 = Color(0xFFFFCC00)
    ),
    DARK_SUNG(
        primaryColor = Color(0xFF6C63FF),
        accentColor = Color(0xFF00E5FF),
        headerTitle = "[ SHADOW MONARCH SYSTEM ]",
        systemText1 = "[ SYSTEM ] SYNCHRONIZING SYSTEM ENERGY SYSTEM...",
        systemText2 = "[ STATUS ] ESTABLISHING SHADOW CORRIDOR...",
        systemText3 = "[ ENERGY ] COMPLETED! COMMAND MONARCH: ARISE!!!",
        particleColor1 = Color(0xFF6C63FF),
        particleColor2 = Color(0xFF00E5FF)
    )
}

fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = start.alpha + (end.alpha - start.alpha) * f
    )
}

@Composable
fun SplashScreenView(onComplete: () -> Unit) {
    val context = LocalContext.current
    
    // Select one loading screen randomly on entry (GTA V style random selection)
    val selectedStyle = remember { LoadingScreenStyle.values().random() }

    var animTimeMs by remember { mutableStateOf(0L) }
    var soundPlayed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val startTime = android.os.SystemClock.uptimeMillis()
        while (true) {
            val now = android.os.SystemClock.uptimeMillis()
            animTimeMs = now - startTime
            
            val durationLine1 = selectedStyle.systemText1.length * 22L
            val startLine2 = 200L + durationLine1 + 100L
            val durationLine2 = selectedStyle.systemText2.length * 22L
            val startLine3 = startLine2 + durationLine2 + 100L
            val durationLine3 = selectedStyle.systemText3.length * 22L
            val startProgress = startLine3 + durationLine3 + 100L
            val durationProgress = 800L
            val endProgress = startProgress + durationProgress
            val startFlash = endProgress
            val totalDuration = startFlash + 1800L

            // Play the dynamic thrum/gate opening audio hook precisely during the character reveal flash phase
            if (animTimeMs >= startFlash && !soundPlayed) {
                soundPlayed = true
                playThrumSound(context)
            }

            if (animTimeMs >= totalDuration) {
                onComplete()
                break
            }
            delay(16) // smooth updates
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "SplashGate")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    val startLine1 = 200L
    val durationLine1 = selectedStyle.systemText1.length * 22L
    val startLine2 = startLine1 + durationLine1 + 100L
    val durationLine2 = selectedStyle.systemText2.length * 22L
    val startLine3 = startLine2 + durationLine2 + 100L
    val durationLine3 = selectedStyle.systemText3.length * 22L
    val startProgress = startLine3 + durationLine3 + 100L
    val durationProgress = 800L
    val endProgress = startProgress + durationProgress
    val startFlash = endProgress

    // Letter extraction
    val typedText1 = if (animTimeMs >= startLine1) {
        val count = ((animTimeMs - startLine1) / 22).coerceIn(0, selectedStyle.systemText1.length.toLong()).toInt()
        selectedStyle.systemText1.substring(0, count)
    } else ""

    val typedText2 = if (animTimeMs >= startLine2) {
        val count = ((animTimeMs - startLine2) / 22).coerceIn(0, selectedStyle.systemText2.length.toLong()).toInt()
        selectedStyle.systemText2.substring(0, count)
    } else ""

    val typedText3 = if (animTimeMs >= startLine3) {
        val count = ((animTimeMs - startLine3) / 22).coerceIn(0, selectedStyle.systemText3.length.toLong()).toInt()
        selectedStyle.systemText3.substring(0, count)
    } else ""

    val progressBarVal = if (animTimeMs >= startProgress) {
        ((animTimeMs - startProgress).toFloat() / durationProgress.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val flashValue = if (animTimeMs >= startFlash) {
        val flashProg = (animTimeMs - startFlash) / 250f
        if (flashProg < 0.4f) {
            (flashProg / 0.4f) * 0.8f
        } else {
            (1f - ((flashProg - 0.4f) / 0.6f)).coerceIn(0f, 1f) * 0.8f
        }
    } else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060609)), // Absolute sleek dark background
        contentAlignment = Alignment.Center
    ) {
        // Centered glow backplate of signature purple for neat typography focus
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val glowCenter = Offset(width * 0.5f, height * 0.5f)
            val glowRadius = width * 0.85f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        selectedStyle.primaryColor.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = glowCenter,
                    radius = glowRadius
                ),
                center = glowCenter,
                radius = glowRadius
            )
        }

        // Top branding system bar row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedStyle.headerTitle,
                    color = selectedStyle.primaryColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "v4.0 LOADING...",
                    color = selectedStyle.accentColor.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }
        }

        // Clean typography-focused Center HUD panel (replaces all characters & portrait images)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.Center)
                .graphicsLayer {
                    val pulseFactor = 0.98f + 0.02f * pulseScale
                    scaleX = pulseFactor
                    scaleY = pulseFactor
                    
                    val floatMs = animTimeMs.toFloat()
                    translationY = 6f * kotlin.math.sin(floatMs * 0.002f).toFloat()
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Signature #6C63FF Purple centered title with intense glowing aura
                Text(
                    text = "SHADOW ARISE",
                    color = Color(0xFF6C63FF),
                    fontSize = 44.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 6.sp,
                    style = TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color(0xFF6C63FF),
                            offset = Offset(0f, 0f),
                            blurRadius = 38f
                        )
                    )
                )
                
                Spacer(modifier = Modifier.height(28.dp))
                
                // System notifications typewriter layout centered
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .border(1.dp, selectedStyle.primaryColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (typedText1.isNotEmpty()) {
                        Text(
                            text = typedText1,
                            color = selectedStyle.primaryColor,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    if (typedText2.isNotEmpty()) {
                        Text(
                            text = typedText2,
                            color = selectedStyle.primaryColor,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    if (typedText3.isNotEmpty()) {
                        Text(
                            text = typedText3,
                            color = selectedStyle.accentColor,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        )
                    } else {
                        Spacer(modifier = Modifier.height(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Skeleton vertebrae progress bar design with skulls and crossed bones
                if (animTimeMs >= startProgress) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "PREPARING SHADOW RESURRECTION...",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "${(progressBarVal * 100).toInt()}%",
                                color = selectedStyle.accentColor,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            // Left Skull representation
                            Canvas(modifier = Modifier.size(16.dp)) {
                                val skullC = selectedStyle.primaryColor
                                drawCircle(color = skullC, radius = size.width * 0.42f, center = Offset(size.width * 0.5f, size.height * 0.4f))
                                drawRect(color = skullC, topLeft = Offset(size.width * 0.35f, size.height * 0.65f), size = Size(size.width * 0.3f, size.height * 0.22f))
                                drawCircle(color = Color(0xFF060609), radius = size.width * 0.08f, center = Offset(size.width * 0.38f, size.height * 0.4f))
                                drawCircle(color = Color(0xFF060609), radius = size.width * 0.08f, center = Offset(size.width * 0.62f, size.height * 0.4f))
                            }

                            // 14 vertebrae segments
                            val segmentsCount = 14
                            for (idx in 0 until segmentsCount) {
                                val lowerBound = idx.toFloat() / segmentsCount
                                val upperBound = (idx + 1).toFloat() / segmentsCount
                                val isCompleted = progressBarVal >= upperBound
                                val isCurrentlyFilling = progressBarVal in lowerBound..upperBound
                                
                                val fillingFactor = if (isCurrentlyFilling) {
                                    ((progressBarVal - lowerBound) * segmentsCount).coerceIn(0f, 1f)
                                } else 0f
                                
                                val vertebraScale = if (isCurrentlyFilling) {
                                    1f + 0.25f * kotlin.math.sin(fillingFactor * kotlin.math.PI).toFloat()
                                } else 1f

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(9.dp)
                                        .graphicsLayer {
                                            scaleX = vertebraScale
                                            scaleY = vertebraScale
                                        }
                                        .background(
                                            color = if (isCompleted || isCurrentlyFilling) {
                                                val segmentColor = lerpColor(selectedStyle.primaryColor, selectedStyle.accentColor, idx.toFloat() / segmentsCount)
                                                if (isCurrentlyFilling) {
                                                    lerpColor(segmentColor, Color.White, 0.4f)
                                                } else segmentColor
                                            } else {
                                                Color.White.copy(alpha = 0.08f)
                                            },
                                            shape = RoundedCornerShape(1.5.dp)
                                        )
                                )
                            }

                            // Right Crossed Bones representation
                            Canvas(modifier = Modifier.size(16.dp)) {
                                val boneC = selectedStyle.accentColor
                                drawLine(color = boneC, start = Offset(2.dp.toPx(), 2.dp.toPx()), end = Offset(14.dp.toPx(), 14.dp.toPx()), strokeWidth = 1.8.dp.toPx())
                                drawLine(color = boneC, start = Offset(14.dp.toPx(), 2.dp.toPx()), end = Offset(2.dp.toPx(), 14.dp.toPx()), strokeWidth = 1.8.dp.toPx())
                                drawCircle(color = boneC, radius = 1.2.dp.toPx(), center = Offset(2.dp.toPx(), 2.dp.toPx()))
                                drawCircle(color = boneC, radius = 1.2.dp.toPx(), center = Offset(14.dp.toPx(), 14.dp.toPx()))
                                drawCircle(color = boneC, radius = 1.2.dp.toPx(), center = Offset(14.dp.toPx(), 2.dp.toPx()))
                                drawCircle(color = boneC, radius = 1.2.dp.toPx(), center = Offset(2.dp.toPx(), 14.dp.toPx()))
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(49.dp)) // Maintain consistent baseline size class heights
                }
            }
        }

        // Camera lightning transition overlay (glowing gate pulse)
        if (flashValue > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flashValue))
            )
        }
    }
}

// Low-frequency dynamic 'thrum' or 'gate opening' audio hook
fun playThrumSound(context: android.content.Context) {
    kotlin.concurrent.thread {
        try {
            val sampleRate = 44100
            val numSamples = (sampleRate * 2.0).toInt() // 2.0 seconds duration
            val buffer = ShortArray(numSamples)
            
            // Frequency sweeps for a dramatic 'low thrum / gates of shadow opening' rumbling sequence
            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                
                // Deep primary thrum frequency modulation (starting low at 40Hz, rising to 55Hz, then declining)
                val baseFreq = if (t < 0.6f) {
                    40f + 25f * (t / 0.6f)
                } else {
                    65f - 20f * ((t - 0.6f) / 1.4f)
                }
                
                // Low modulation envelope (rhythmic heartbeat-like gates pulses)
                val pulseMod = kotlin.math.sin(2f * kotlin.math.PI * 4f * t).toFloat() * 0.15f + 0.85f
                
                val wave1 = kotlin.math.sin(2f * kotlin.math.PI * baseFreq * t).toFloat()
                val wave2 = kotlin.math.sin(2f * kotlin.math.PI * (baseFreq * 1.8f) * t).toFloat() * 0.35f
                val subBass = kotlin.math.sin(2f * kotlin.math.PI * (baseFreq * 0.5f) * t).toFloat() * 0.5f
                val rumble = (Math.random() * 2.0 - 1.0).toFloat() * 0.08f // cinematic low-end dust / crackle
                
                // ADSR-like volume envelope
                val envelope = when {
                    t < 0.2f -> t / 0.2f // swift deep attack
                    t > 0.8f -> ((2.0f - t) / 1.2f).coerceAtLeast(0f) // long dramatic sustain decline
                    else -> 1.0f
                }
                
                val combined = (wave1 + wave2 + subBass + rumble) * pulseMod * envelope
                val sampleValue = (combined * 25000).toInt().coerceIn(-32768, 32767)
                buffer[i] = sampleValue.toShort()
            }
            
            val audioTrack = android.media.AudioTrack(
                android.media.AudioManager.STREAM_MUSIC,
                sampleRate,
                android.media.AudioFormat.CHANNEL_OUT_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT,
                numSamples * 2,
                android.media.AudioTrack.MODE_STATIC
            )
            audioTrack.write(buffer, 0, numSamples)
            audioTrack.play()
            
            // Release resources correctly after stopping
            android.os.SystemClock.sleep(2200)
            audioTrack.stop()
            audioTrack.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// Density helper
private fun dens(context: android.content.Context): Float {
    return context.resources.displayMetrics.density
}

// ==========================================
// LOGIN SCREEN VIEW
// ==========================================
@Composable
fun LoginScreenView(
    viewModel: HunterViewModel,
    onLoginSuccess: (isFirstTime: Boolean) -> Unit
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Register, 1: Sign In
    
    // Form States (Register)
    var nicknameState by remember { mutableStateOf("") }
    var usernameState by remember { mutableStateOf("") }
    var passwordState by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    
    // Form States (Login)
    var loginUsernameState by remember { mutableStateOf("") }
    var loginPasswordState by remember { mutableStateOf("") }
    var showLoginPassword by remember { mutableStateOf(false) }
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "◆ SHADOW ARISE ◆",
                style = TextStyle(
                    color = BrightPurpleHighlight,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    fontFamily = FontFamily.Monospace
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "\"Your body is your ultimate dungeon. Arise.\"",
                color = TextSecondary,
                fontSize = 13.sp,
                style = TextStyle(fontFamily = FontFamily.Serif),
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Main authentic dark system panel
            SystemWindow(
                title = "MONARCH SYSTEM GATEWAY",
                modifier = Modifier.fillMaxWidth()
            ) {
                // Custom Tab Row (Exactly TWO options: REGISTER and SIGN IN)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val tabs = listOf("REGISTER", "SIGN IN")
                    tabs.forEachIndexed { index, title ->
                        val isSelected = activeTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) GlowPurple.copy(alpha = 0.15f) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isSelected) GlowPurple else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    activeTab = index
                                    errorMessage = null
                                    successMessage = null
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) BrightPurpleHighlight else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Error Indicator
                errorMessage?.let { error ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .background(Color(0xFF3B1E1E), RoundedCornerShape(8.dp))
                            .border(1.dp, DangerRed, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "[ERROR] $error",
                            color = DangerRed,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Success Indicator
                successMessage?.let { success ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .background(Color(0xFF1B3B2B), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF00FF87), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "[SYSTEM] $success",
                            color = Color(0xFF00FF87),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                when (activeTab) {
                    0 -> {
                        // Register Tab
                        Text(
                            text = "Register a secure Monarch account to back up and preserve your strength across training cycles.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // 1. Nickname
                        OutlinedTextField(
                            value = nicknameState,
                            onValueChange = { nicknameState = it },
                            label = { Text("Hunter Nickname", fontFamily = FontFamily.Monospace, color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = GlowPurple,
                                unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                                focusedContainerColor = DeepPurpleBg.copy(alpha = 0.5f),
                                unfocusedContainerColor = DeepPurpleBg.copy(alpha = 0.5f)
                            ),
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = TextPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("register_nickname_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // 2. Username
                        OutlinedTextField(
                            value = usernameState,
                            onValueChange = { usernameState = it },
                            label = { Text("Monarch Username", fontFamily = FontFamily.Monospace, color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = GlowPurple,
                                unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                                focusedContainerColor = DeepPurpleBg.copy(alpha = 0.5f),
                                unfocusedContainerColor = DeepPurpleBg.copy(alpha = 0.5f)
                            ),
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = TextPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("register_username_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // 3. Password with eye option toggled
                        OutlinedTextField(
                            value = passwordState,
                            onValueChange = { passwordState = it },
                            label = { Text("Monarch Password", fontFamily = FontFamily.Monospace, color = TextSecondary) },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                Text(
                                    text = if (showPassword) "HIDE" else "SHOW",
                                    color = GlowPurple,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { showPassword = !showPassword }
                                        .padding(end = 12.dp)
                                        .testTag("toggle_register_password")
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = GlowPurple,
                                unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                                focusedContainerColor = DeepPurpleBg.copy(alpha = 0.5f),
                                unfocusedContainerColor = DeepPurpleBg.copy(alpha = 0.5f)
                            ),
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = TextPrimary),
                            modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("register_password_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Dynamic Visual password requirements checklists (Min 8 letters, 1 letter, 1 special char)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val lenOk = passwordState.length > 8
                            val letOk = passwordState.any { it.isLetter() }
                            val specOk = passwordState.any { !it.isLetterOrDigit() }

                            RequirementRow(text = "Must be greater than 8 characters", isMet = lenOk)
                            RequirementRow(text = "Contains at least 1 alphabetic letter", isMet = letOk)
                            RequirementRow(text = "Contains at least 1 special character (@, #, $, etc.)", isMet = specOk)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (nicknameState.trim().isEmpty()) {
                                    errorMessage = "Please enter an awakening Nickname."
                                    return@Button
                                }
                                if (usernameState.trim().isEmpty() || passwordState.trim().isEmpty()) {
                                    errorMessage = "All registry fields must be defined."
                                    return@Button
                                }
                                val checkPasswordLength = passwordState.length > 8
                                val checkPasswordLetter = passwordState.any { it.isLetter() }
                                val checkPasswordSpecial = passwordState.any { !it.isLetterOrDigit() }

                                if (!checkPasswordLength) {
                                    errorMessage = "Password must be greater than 8 characters."
                                    return@Button
                                }
                                if (!checkPasswordLetter) {
                                    errorMessage = "Password must contain at least one letter."
                                    return@Button
                                }
                                if (!checkPasswordSpecial) {
                                    errorMessage = "Password must contain at least one special character (such as @, #, $, etc.)."
                                    return@Button
                                }

                                isLoading = true
                                errorMessage = null
                                viewModel.registerUser(
                                    username = usernameState.trim(),
                                    passwordHash = passwordState,
                                    nickname = nicknameState.trim(),
                                    mobileNumber = "",
                                    onSuccess = {
                                        isLoading = false
                                        successMessage = "Monarch Registry: Account initialized!"
                                        onLoginSuccess(true)
                                    },
                                    onError = {
                                        isLoading = false
                                        errorMessage = it
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("register_account_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GlowPurple,
                                contentColor = DeepPurpleBg
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = DeepPurpleBg, modifier = Modifier.size(22.dp))
                            } else {
                                Text(
                                    text = "REGISTER MONARCH",
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.5.sp
                                )
                            }
                        }
                    }
                    1 -> {
                        // Sign In Tab
                        Text(
                            text = "Enter Monarch credentials to reopen the gateway and sync your achievements with the active session.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = loginUsernameState,
                            onValueChange = { loginUsernameState = it },
                            label = { Text("Monarch Username", fontFamily = FontFamily.Monospace, color = TextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = GlowPurple,
                                unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                                focusedContainerColor = DeepPurpleBg.copy(alpha = 0.5f),
                                unfocusedContainerColor = DeepPurpleBg.copy(alpha = 0.5f)
                            ),
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = TextPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("login_username_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = loginPasswordState,
                            onValueChange = { loginPasswordState = it },
                            label = { Text("Monarch Password", fontFamily = FontFamily.Monospace, color = TextSecondary) },
                            singleLine = true,
                            visualTransformation = if (showLoginPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                Text(
                                    text = if (showLoginPassword) "HIDE" else "SHOW",
                                    color = GlowPurple,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { showLoginPassword = !showLoginPassword }
                                        .padding(end = 12.dp)
                                        .testTag("toggle_login_password")
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = GlowPurple,
                                unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                                focusedContainerColor = DeepPurpleBg.copy(alpha = 0.5f),
                                unfocusedContainerColor = DeepPurpleBg.copy(alpha = 0.5f)
                            ),
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = TextPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (loginUsernameState.trim().isEmpty() || loginPasswordState.trim().isEmpty()) {
                                    errorMessage = "Please enter both credentials."
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = null
                                viewModel.loginUser(
                                    username = loginUsernameState.trim(),
                                    passwordHash = loginPasswordState,
                                    onSuccess = { isFirstTime ->
                                        isLoading = false
                                        successMessage = "Gateway connection verified!"
                                        onLoginSuccess(isFirstTime)
                                    },
                                    onError = {
                                        isLoading = false
                                        errorMessage = it
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("sign_in_account_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GlowPurple,
                                contentColor = DeepPurpleBg
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = DeepPurpleBg, modifier = Modifier.size(22.dp))
                            } else {
                                Text(
                                    text = "SIGN IN GATEWAY",
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RequirementRow(text: String, isMet: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.Close,
            contentDescription = null,
            tint = if (isMet) Color(0xFF00FF87) else Color.Gray.copy(alpha = 0.6f),
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = text,
            color = if (isMet) Color(0xFF00FF87) else Color.Gray,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

// ==========================================
// ONBOARDING SCREEN VIEW
// ==========================================
@Composable
fun OnboardingScreenView(
    profile: HunterProfile?,
    onOnboardingComplete: (height: Double, weight: Double, age: Int, gender: String, unitType: String, activityLevel: String) -> Unit
) {
    var heightCmStr by remember { mutableStateOf("") }
    var weightStr by remember { mutableStateOf("") }
    var ageStr by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var unitType by remember { mutableStateOf("METRIC") } // defaulted to METRIC
    var activityLevel by remember { mutableStateOf("Moderate") }
    var errorMsg by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp, 32.dp, 16.dp, 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "◆ SYSTEM REGISTRATION ◆",
            style = TextStyle(
                color = BrightPurpleHighlight,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
        )
        Text(
            text = "HUNTER PROFILE DATA REQUIRED",
            color = TextSecondary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        SystemWindow(
            title = "REGISTRATION FORM"
        ) {
            // GENDER SELECTION
            Text(
                "Gender Assignment:",
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Male", "Female", "Other").forEach { g ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (gender == g) GlowPurple else DarkPurpleAccent)
                            .border(
                                1.dp,
                                if (gender == g) BrightPurpleHighlight else GlowPurple.copy(alpha = 0.3f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { gender = g }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = g,
                            color = if (gender == g) DeepPurpleBg else TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // HEIGHT INPUT
            Text(
                text = "Height (cm):",
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
            TextField(
                value = heightCmStr,
                onValueChange = { heightCmStr = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkPurpleAccent,
                    unfocusedContainerColor = DarkPurpleAccent,
                    focusedBorderColor = GlowPurple,
                    unfocusedBorderColor = GlowPurple.copy(alpha = 0.4f)
                ),
                singleLine = true
            )

            // WEIGHT INPUT
            Text(
                text = "Weight (kg):",
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
            TextField(
                value = weightStr,
                onValueChange = { weightStr = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkPurpleAccent,
                    unfocusedContainerColor = DarkPurpleAccent,
                    focusedBorderColor = GlowPurple,
                    unfocusedBorderColor = GlowPurple.copy(alpha = 0.4f)
                ),
                singleLine = true
            )

            // AGE INPUT
            Text(
                text = "Age (Years):",
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
            TextField(
                value = ageStr,
                onValueChange = { ageStr = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkPurpleAccent,
                    unfocusedContainerColor = DarkPurpleAccent,
                    focusedBorderColor = GlowPurple,
                    unfocusedBorderColor = GlowPurple.copy(alpha = 0.4f)
                ),
                singleLine = true
            )

            // ACTIVITY LEVEL INPUT
            Text(
                text = "Activity Level Selection:",
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Sedentary", "Light", "Moderate", "Very", "Extra").forEach { act ->
                    val isSel = activityLevel == act
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) GlowPurple else DarkPurpleAccent)
                            .border(
                                1.dp,
                                if (isSel) BrightPurpleHighlight else GlowPurple.copy(alpha = 0.3f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { activityLevel = act }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = act,
                            color = if (isSel) DeepPurpleBg else TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            if (errorMsg.isNotEmpty()) {
                Text(
                    text = errorMsg,
                    color = DangerRed,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val hCm = heightCmStr.toDoubleOrNull()
                    val w = weightStr.toDoubleOrNull()
                    val age = ageStr.toIntOrNull()

                    if (hCm == null || hCm <= 0 || w == null || w <= 0 || age == null || age <= 0) {
                        errorMsg = "INVALID STATUS: Please enter authentic numerical dimensions."
                    } else {
                        onOnboardingComplete(hCm, w, age, gender, unitType, activityLevel)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_onboarding_button"),
                colors = ButtonDefaults.buttonColors(containerColor = GlowPurple),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "◆ COMPLETE REGISTRATION ◆",
                    color = DeepPurpleBg,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ==========================================
// HOME SCREEN VIEW
// ==========================================
@Composable
fun HomeScreenView(
    viewModel: HunterViewModel,
    profile: HunterProfile?,
    quests: List<DailyQuest>
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val workouts by viewModel.workouts.collectAsStateWithLifecycle()

    // Status Values
    val firstName = profile?.displayName?.split(" ")?.firstOrNull() ?: "Prince"
    val xp = profile?.totalXp ?: 10
    val rank = viewModel.repository.getRankName(xp)
    val rankTag = viewModel.repository.getRankTagline(xp)
    val xpProg = viewModel.repository.getXpProgress(xp)

    val currentStreak = profile?.currentStreak ?: 0

    // Conversions for Stats
    val height = profile?.height ?: 170.0
    val weight = profile?.weight ?: 65.0
    val unitType = profile?.unitType ?: "METRIC"
    val age = profile?.age ?: 25
    val gender = profile?.gender ?: "Male"

    val metricPair = viewModel.repository.convertToMetric(weight, height, unitType)
    val bmiValue = viewModel.repository.calculateBmi(metricPair.first, metricPair.second)
    val bmiLabel = viewModel.repository.getBmiLabel(bmiValue)

    // Calories: Mifflin-St Jeor with dynamic activityLevel
    val weightKg = metricPair.first
    val heightCm = metricPair.second
    val calories = viewModel.repository.calculateDailyCalories(
        weightKg = weightKg,
        heightCm = heightCm,
        age = age,
        gender = gender,
        activityLevel = profile?.activityLevel ?: "Moderate"
    )

    // Water: 35ml x Weight (kg)
    val water = viewModel.repository.calculateWaterIntake(weightKg)

    // Today's EXP: derived dynamically from workout logs completed today
    val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayXp = workouts.filter { it.dateString == todayDateStr }.sumOf { it.xpEarned }

    // State for info modals
    var activeInfoModal by remember { mutableStateOf<String?>(null) } // "BMI", "ENERGY", "WATER", "EXP"
    var showExpBreakdown by remember { mutableStateOf(false) }

    // Animation count-up values (600ms ease-out)
    val countAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        countAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = EaseOutQuad)
        )
    }

    // Quotes
    val quotes = listOf(
        "I don't need recognition. I just need to get stronger.",
        "Arise, shadows. Reclaim the battlefield.",
        "I alone level up.",
        "The system never lies. Hard work manifests in actual rank.",
        "A system gate cannot contain a shadow monarch."
    )
    val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    val selectedQuote = quotes[dayOfYear % quotes.size]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // LIVE STATUS UPDATE STATES
        val isOnlineState by viewModel.isOnline.collectAsStateWithLifecycle()
        var timeStr by remember { mutableStateOf("") }
        var dateStr by remember { mutableStateOf("") }
        LaunchedEffect(Unit) {
            val tFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val dFormat = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
            while (true) {
                val now = Date()
                timeStr = tFormat.format(now).uppercase()
                dateStr = dFormat.format(now).uppercase()
                delay(1000)
            }
        }
        val localCtx = LocalContext.current
        var batteryPercent by remember { mutableStateOf<Int?>(null) }
        LaunchedEffect(Unit) {
            try {
                val bm = localCtx.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
                if (bm != null) {
                    batteryPercent = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
                }
            } catch (e: Exception) {
                batteryPercent = null
            }
        }

        // HUNTER STATUS BAR PANEL - REDESIGNED MODERN HUD
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color(0xFF12121A), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF6C63FF).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left section: [Clock icon] HH:MM AM/PM  [Calendar icon] DAY, DD MMM
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Clock Icon
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCircle(color = Color(0xFF8888AA), radius = size.width * 0.45f, style = Stroke(width = 1.dp.toPx()))
                        drawLine(color = Color(0xFF8888AA), start = Offset(size.width * 0.5f, size.height * 0.5f), end = Offset(size.width * 0.5f, size.height * 0.22f), strokeWidth = 1.dp.toPx())
                        drawLine(color = Color(0xFF8888AA), start = Offset(size.width * 0.5f, size.height * 0.5f), end = Offset(size.width * 0.72f, size.height * 0.5f), strokeWidth = 1.dp.toPx())
                    }
                    Text(
                        text = timeStr,
                        color = Color(0xFF00D4FF), // cyan primary
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Calendar Icon
                    Canvas(modifier = Modifier.size(11.dp)) {
                        drawRect(color = Color(0xFF8888AA), topLeft = Offset(1.dp.toPx(), 2.dp.toPx()), size = Size(9.dp.toPx(), 7.dp.toPx()), style = Stroke(width = 1.dp.toPx()))
                        drawLine(color = Color(0xFF8888AA), start = Offset(1.dp.toPx(), 4.dp.toPx()), end = Offset(10.dp.toPx(), 4.dp.toPx()), strokeWidth = 1.dp.toPx())
                        drawCircle(color = Color(0xFF8888AA), radius = 0.6.dp.toPx(), center = Offset(3.dp.toPx(), 1.5.dp.toPx()))
                        drawCircle(color = Color(0xFF8888AA), radius = 0.6.dp.toPx(), center = Offset(7.dp.toPx(), 1.5.dp.toPx()))
                    }
                    Text(
                        text = dateStr,
                        color = Color(0xFFEAEAEA),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }

                // Center section: [Signal icon] ONLINE / OFFLINE (updates every 5s reconnection)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        // 3 bars signal
                        val barColor = if (isOnlineState) Color(0xFF00FF88) else Color(0xFFFF4455)
                        drawRect(color = barColor.copy(alpha = if (isOnlineState) 1f else 0.4f), topLeft = Offset(1.dp.toPx(), 7.dp.toPx()), size = Size(1.8f.dp.toPx(), 2.2f.dp.toPx()))
                        drawRect(color = barColor.copy(alpha = if (isOnlineState) 1f else 0.4f), topLeft = Offset(3.8f.dp.toPx(), 4.5f.dp.toPx()), size = Size(1.8f.dp.toPx(), 4.7f.dp.toPx()))
                        drawRect(color = barColor.copy(alpha = if (isOnlineState) 1f else 0.4f), topLeft = Offset(6.6f.dp.toPx(), 2.dp.toPx()), size = Size(1.8f.dp.toPx(), 7.2f.dp.toPx()))
                    }
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isOnlineState) Color(0xFF00FF88) else Color(0xFFFF4455))
                    )
                    Text(
                        text = if (isOnlineState) "ONLINE" else "OFFLINE — SOLO MODE",
                        color = if (isOnlineState) Color(0xFF00FF88) else Color(0xFFFF4455),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }

                // Right section: [Battery icon] XX% | [Fire icon] X DAY STREAK
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Battery indicator if available
                    if (batteryPercent != null) {
                        Canvas(modifier = Modifier.size(13.dp)) {
                            drawRect(color = Color(0xFF8888AA), topLeft = Offset(0.dp.toPx(), 3.dp.toPx()), size = Size(10.dp.toPx(), 5.5f.dp.toPx()), style = Stroke(width = 0.8f.dp.toPx()))
                            drawRect(color = Color(0xFF8888AA), topLeft = Offset(10.dp.toPx(), 4.5f.dp.toPx()), size = Size(1.2f.dp.toPx(), 2.5f.dp.toPx()))
                            val lvl = (batteryPercent ?: 100) / 100f
                            drawRect(color = Color(0xFF00FF88), topLeft = Offset(0.8f.dp.toPx(), 3.8f.dp.toPx()), size = Size((8.4f * lvl).dp.toPx(), 3.9f.dp.toPx()))
                        }
                        Text(
                            text = "${batteryPercent}%",
                            color = Color(0xFFEAEAEA),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Fire Streak Icon
                    Canvas(modifier = Modifier.size(11.dp)) {
                        val path = Path().apply {
                            moveTo(size.width * 0.5f, 0.5f.dp.toPx())
                            quadraticTo(size.width * 0.8f, size.height * 0.35f, size.width * 0.8f, size.height * 0.65f)
                            quadraticTo(size.width * 0.8f, size.height * 0.95f, size.width * 0.5f, size.height * 0.95f)
                            quadraticTo(size.width * 0.2f, size.height * 0.95f, size.width * 0.2f, size.height * 0.65f)
                            quadraticTo(size.width * 0.2f, size.height * 0.35f, size.width * 0.5f, 0.5f.dp.toPx())
                            close()
                        }
                        drawPath(path = path, color = Color(0xFFFF9500))
                    }
                    Text(
                        text = "$currentStreak",
                        color = Color(0xFFFF9500),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Amber badge if offline
            if (!isOnlineState) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF261900), RoundedCornerShape(6.dp))
                        .border(0.5.dp, Color(0xFFFFB300).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFB300))
                        )
                        Text(
                            text = "AI UNAVAILABLE IN OFFLINE MODE",
                            color = Color(0xFFFFB300),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // HUNTER HEADER: WELCOME BACK, HUNTER [NAME] [RANK]
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardPurpleBg)
                .border(1.dp, GlowPurple.copy(alpha = 0.40f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "STATUS SCREEN",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "WELCOME BACK, HUNTER ${firstName.uppercase()}",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                            // Glowing Badge for Rank Letter
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                (if (rank == "S") BrightPurpleHighlight else GlowPurple).copy(alpha = 0.35f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .border(1.5.dp, if (rank == "S") BrightPurpleHighlight else GlowPurple, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = rank,
                                    color = if (rank == "S") BrightPurpleHighlight else GlowPurple,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // XP PROGRESS BAR (Current rank badge | Thin progress bar | Next rank badge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "RANK $rank",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Animated progress fill on load
                    val barProgress = remember { Animatable(0f) }
                    LaunchedEffect(xpProg) {
                        val fraction = (xpProg.first.toFloat() / xpProg.second.toFloat()).coerceIn(0f, 1f)
                        barProgress.animateTo(fraction, animationSpec = tween(1000, easing = EaseOutQuad))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(DarkPurpleAccent)
                            .border(0.5.dp, GlowPurple.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(barProgress.value)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(GlowPurple, BrightPurpleHighlight)
                                    )
                                )
                        )
                    }

                    val nextRankLetter = when (rank) {
                        "E" -> "D"
                        "D" -> "C"
                        "C" -> "B"
                        "B" -> "A"
                        "A" -> "S"
                        else -> "S"
                    }
                    Text(
                        text = "RANK $nextRankLetter",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$xp EXP",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${xpProg.second - xpProg.first} EXP TO LVL UP",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // STATS GRID 2x2
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // CARD 1: BMI
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            1.dp,
                            GlowPurple.copy(alpha = 0.3f),
                            RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = CardPurpleBg)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Column {
                            Text("BMI RATIO", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(10.dp))
                            val displayBmi = bmiValue * countAnim.value
                            Text(
                                text = String.format(Locale.US, "%.1f", displayBmi),
                                color = BrightPurpleHighlight,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = bmiLabel.split(" — ").firstOrNull() ?: "",
                                color = if (bmiValue < 18.5) DangerRed else if (bmiValue < 25) SuccessGreen else WarningAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        StatInfoButton(
                            onClick = { activeInfoModal = "BMI" },
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                    }
                }

                // CARD 2: Daily Calories Goal
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            1.dp,
                            GlowPurple.copy(alpha = 0.3f),
                            RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = CardPurpleBg)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Column {
                            Text("ENERGY LIMIT", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(10.dp))
                            val displayCalories = calories * countAnim.value
                            Text(
                                text = "${displayCalories.toInt()} kcal",
                                color = BrightPurpleHighlight,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Daily Calorie limit",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        StatInfoButton(
                            onClick = { activeInfoModal = "ENERGY" },
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // CARD 3: Daily Water Intake
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            1.dp,
                            GlowPurple.copy(alpha = 0.3f),
                            RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = CardPurpleBg)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Column {
                            Text("HYDRATION", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(10.dp))
                            val displayWater = water * countAnim.value
                            Text(
                                text = String.format(Locale.US, "%.2f Litres", displayWater),
                                color = BrightPurpleHighlight,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Base prot. fluid flow",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        StatInfoButton(
                            onClick = { activeInfoModal = "WATER" },
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                    }
                }

                // CARD 4: Today's EXP
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            1.dp,
                            GlowPurple.copy(alpha = 0.3f),
                            RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = CardPurpleBg)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Column {
                            Text("TODAY'S EXP", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            val workoutXp = workouts.filter { it.dateString == todayDateStr }.sumOf { it.xpEarned }
                            val questXp = quests.filter { it.isCompleted }.sumOf { it.xpEarned }
                            val totalCombinedXp = workoutXp + questXp
                            val displayTodayXp = totalCombinedXp * countAnim.value
                            
                            Text(
                                text = "+${displayTodayXp.toInt()} EXP",
                                color = SuccessGreen,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Dual Source",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            // Inline breakdown tooltip
                            if (showExpBreakdown) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .border(0.5.dp, GlowPurple.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "◆ BREAKDOWN ◆",
                                        color = BrightPurpleHighlight,
                                        fontSize = 8.6.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Workout: +$workoutXp EXP",
                                        color = TextSecondary,
                                        fontSize = 8.6.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Quests: +$questXp EXP",
                                        color = TextSecondary,
                                        fontSize = 8.6.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Divider(color = GlowPurple.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                                    Text(
                                        text = "Total: +$totalCombinedXp EXP",
                                        color = SuccessGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                        StatInfoButton(
                            onClick = { showExpBreakdown = !showExpBreakdown },
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                    }
                }
            }
        }

        // TODAY'S QUEST BOARD
        SystemWindow(title = "TODAY'S SYSTEM QUESTS") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
                Text(
                    text = "◆ MISSION GATES — $currentDay",
                    color = BrightPurpleHighlight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                if (quests.isNotEmpty() && quests.all { it.isCompleted }) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SuccessGreen.copy(alpha = 0.15f))
                            .border(BorderStroke(1.2.dp, SuccessGreen), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Cleared",
                                tint = SuccessGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "MISSION COMPLETE",
                                color = SuccessGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (quests.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GlowPurple)
                }
            } else {
                quests.forEach { quest ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(if (quest.isCompleted) GlowPurple.copy(alpha = 0.05f) else Color.Transparent)
                            .border(
                                0.5.dp,
                                if (quest.isCompleted) GlowPurple else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (quest.isCompleted) Icons.Default.Check else Icons.Default.Info,
                                contentDescription = "Task Status",
                                tint = if (quest.isCompleted) SuccessGreen else GlowPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = quest.exerciseName,
                                    color = if (quest.isCompleted) TextPrimary.copy(alpha = 0.6f) else TextPrimary,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = quest.setsReps,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Checkbox(
                            checked = quest.isCompleted,
                            onCheckedChange = {
                                if (it) {
                                    viewModel.completeQuestItem(quest)
                                }
                            },
                            enabled = !quest.isCompleted,
                            colors = CheckboxDefaults.colors(
                                checkedColor = SuccessGreen,
                                uncheckedColor = GlowPurple,
                                checkmarkColor = DeepPurpleBg
                            )
                        )
                    }
                }
            }
        }

        // SCROLLING SYSTEM QUEST TICKER
        val questTips = listOf(
            "[SYSTEM NOTICE] Ensure precise flat-back deadlift form to avoid mana deplete state.",
            "[SYSTEM ALERT] Standard daily hydration ratio is 35ml × Weight(kg). Click (i) for formula flow.",
            "[SYSTEM METRIC] Every complete mission grants customized EXP targets instantly.",
            "[SYSTEM BONUS] Clearing all three daily quest metrics triggers a +100 EXP S-Rank evolution!",
            "[SYSTEM LORE] Arise! Shadow Monarch. Your physical container limits your maximum awakened power.",
            "[SYSTEM REPORT] Dynamic Canvas demonstrators are active. Expand 'Animation' tab inside drills."
        )

        var currentTipIndex by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(4500)
                currentTipIndex = (currentTipIndex + 1) % questTips.size
            }
        }

        SystemWindow(title = "SYSTEM QUEST TICKER") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "System News",
                    tint = BrightPurpleHighlight,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Crossfade(
                    targetState = questTips[currentTipIndex],
                    modifier = Modifier.weight(1f),
                    label = "SystemTickerAnimation"
                ) { tipText ->
                    Text(
                        text = tipText,
                        color = BrightPurpleHighlight,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ==========================================
        // SYSTEM PENALTY ZONE
        // ==========================================
        val penaltyActive by viewModel.penaltyActive.collectAsStateWithLifecycle()
        val DangerRed = Color(0xFFFF3B30)
        val WarningAmber = Color(0xFFFF9500)

        SystemWindow(title = "SYSTEM PENALTY ZONE") {
            if (!penaltyActive) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PENALTY PROTOCOL: STANDBY",
                            color = SuccessGreen,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Complete all daily exercises to remain optimal. Failure to do so triggers the Monarch's Penalty dimensional rift.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = {
                            viewModel.triggerPenalty()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DangerRed.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text(
                            text = "⚠️ SIMULATE FAILURE & SUBTRACT XP",
                            color = DangerRed,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(DangerRed)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "◆ WARNING: PENALTY GATES OPENED ◆",
                            color = DangerRed,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Divider(color = DangerRed.copy(alpha = 0.3f), thickness = 1.dp)

                    Text(
                        text = "You failed your daily physical checks! The Solo Leveling algorithm initiated a level subtraction (-150 XP). Your score has been reduced.",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, DangerRed, RoundedCornerShape(10.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "⚔️ HIDDEN GATE PROTOCOL ⚔️",
                                color = WarningAmber,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "ESCAPE PORTAL MISSION: Run 20 km",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "(Survival Goal: 2X normal 10km Running Target)",
                                color = DangerRed,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.clearPenalty()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "SURVIVE GATES (+50 XP back & Lift Penalty)",
                            color = DeepPurpleBg,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // MOTIVATIONAL SYSTEM QUOTE
        Text(
            text = selectedQuote,
            color = TextMuted,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
    }

    // Modal dialog overlays
    when (activeInfoModal) {
        "BMI" -> {
            SystemInfoModal(
                title = "BMI METRIC DECREE",
                onDismiss = { activeInfoModal = null }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("FORMULA:", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("Weight (kg) divided by Height (m) squared.", color = TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("HUNTER SIGNIFICANCE:", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("Your Body Mass Index indicates physical mass structural ratios. Normal values prevent fatigue debuffs during dungeons.", color = TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("RECOMMENDED RANGES:", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("◆ Underweight: < 18.5\n◆ Normal (Peak): 18.5 - 24.9\n◆ Overweight: 25.0 - 29.9\n◆ Obese: 30.0+", color = BrightPurpleHighlight, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
        "ENERGY" -> {
            SystemInfoModal(
                title = "CALORIC ENERGY DECREE",
                onDismiss = { activeInfoModal = null }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("FORMULA:", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("Mifflin-St Jeor Equation with a Moderate Activity multiplier coefficient (1.55).", color = TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("HUNTER SIGNIFICANCE:", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("Represents the exact energy value required daily to sustain your muscle restoration rate and athletic speed.", color = TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
        "WATER" -> {
            SystemInfoModal(
                title = "METABOLIC FLUID DECREE",
                onDismiss = { activeInfoModal = null }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("FORMULA:", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("Optimal fluid flow: 35ml per Kilogram of raw structural weight.", color = TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("HUNTER SIGNIFICANCE:", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("Prevents dehydration debuffs. High mana/stamina outputs mandate S-rank hydration protocols.", color = TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
        "EXP" -> {
            SystemInfoModal(
                title = "EXPERIENCE RECOVERY DECREE",
                onDismiss = { activeInfoModal = null }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("AGGREGATE METHOD:", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("Experience points accumulated automatically from physical quest checklists and raw drill systems logged today.", color = TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("HUNTER SIGNIFICANCE:", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("The primal catalyst of your Hunter Class. Accumulate experience threshold points to trigger a majestic Class Rank Evolution overlay alert.", color = TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

// ==========================================
// EXERCISE SCREEN VIEW
// ==========================================
@Composable
fun ExerciseScreenView(viewModel: HunterViewModel, profile: HunterProfile?) {
    val height = profile?.height ?: 170.0
    val weight = profile?.weight ?: 65.0
    val age = profile?.age ?: 25
    val unitType = profile?.unitType ?: "METRIC"

    val metricPair = viewModel.repository.convertToMetric(weight, height, unitType)
    val bmi = viewModel.repository.calculateBmi(metricPair.first, metricPair.second)

    // Assign rank class and difficulty
    val info = when {
        bmi > 27.0 || age > 50 -> Triple("E-class protocol", "Beginner", "Strength: 2 sets × 8 reps, Cardio: 15 min. Rest: 90 sec")
        bmi >= 22.0 && age >= 30 -> Triple("C-class protocol", "Intermediate", "Strength: 3 Sets × 12 Reps, Cardio: 25 min. Rest: 60 sec")
        else -> Triple("A-class protocol", "Advanced", "Strength: 4 Sets × 15 Reps, Cardio: 40 min. Rest: 45 sec")
    }

    val selectedProtocol = info.first
    val diffLabel = info.second
    val setsRepsDetail = info.third

    val strengthExercises = ExerciseData.allExercises.filter { it.category == "STRENGTH" }
    val coreExercises = ExerciseData.allExercises.filter { it.category == "CORE" }
    val cardioExercises = ExerciseData.allExercises.filter { it.category == "CARDIO" }
    val flexExercises = ExerciseData.allExercises.filter { it.category == "FLEXIBILITY" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // PLAN HEADER
            SystemWindow(title = "HUNTER TRAINING PROTOCOL") {
                Text(
                    "Generated for your current stats:",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Gate Match: $selectedProtocol",
                            color = BrightPurpleHighlight,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "BMI LEVEL: ${String.format(Locale.US, "%.1f", bmi)} | Difficulty: $diffLabel",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(GlowPurple.copy(alpha = 0.2f))
                            .border(1.dp, GlowPurple, RoundedCornerShape(4.dp))
                            .padding(6.dp, 2.dp)
                    ) {
                        Text(
                            diffLabel.uppercase(Locale.ROOT),
                            color = GlowPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // WEEKLY PLAN TRACKER
        item {
            WeeklyPlanGrid(viewModel)
        }

        // CATEGORY LISTS
        item {
            Text(
                "◆ DRILL SYSTEMS DATABASE",
                color = BrightPurpleHighlight,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        item {
            ExpandableCategoryHeader(
                categoryName = "━━━ STRENGTH TRAINING ━━━",
                exercises = strengthExercises,
                diffLabel = diffLabel,
                setsRepsDetail = setsRepsDetail,
                onLogWorkout = { viewModel.logWorkout(it, "STRENGTH") }
            )
        }

        item {
            ExpandableCategoryHeader(
                categoryName = "━━━ CORE ━━━",
                exercises = coreExercises,
                diffLabel = diffLabel,
                setsRepsDetail = setsRepsDetail,
                onLogWorkout = { viewModel.logWorkout(it, "CORE") }
            )
        }

        item {
            ExpandableCategoryHeader(
                categoryName = "━━━ CARDIO ━━━",
                exercises = cardioExercises,
                diffLabel = diffLabel,
                setsRepsDetail = setsRepsDetail,
                onLogWorkout = { viewModel.logWorkout(it, "CARDIO") }
            )
        }

        item {
            ExpandableCategoryHeader(
                categoryName = "━━━ FLEXIBILITY & BALANCE ━━━",
                exercises = flexExercises,
                diffLabel = diffLabel,
                setsRepsDetail = setsRepsDetail,
                onLogWorkout = { viewModel.logWorkout(it, "FLEXIBILITY") }
            )
        }
    }
}

// ==========================================
// EXPANDABLE CATEGORY ROW
// ==========================================
@Composable
fun ExpandableCategoryHeader(
    categoryName: String,
    exercises: List<Exercise>,
    diffLabel: String,
    setsRepsDetail: String,
    onLogWorkout: (name: String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, GlowPurple.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .background(CardPurpleBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = categoryName,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = "Expand",
                tint = GlowPurple
            )
        }

        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                exercises.forEach { ex ->
                    ExerciseListItem(
                        exercise = ex,
                        diffLabel = diffLabel,
                        setsRepsDetail = setsRepsDetail,
                        onComplete = { onLogWorkout(ex.name) }
                    )
                }
            }
        }
    }
}

@Composable
fun ExerciseListItem(
    exercise: Exercise,
    diffLabel: String,
    setsRepsDetail: String,
    onComplete: () -> Unit
) {
    var expandedItem by remember { mutableStateOf(false) }
    var completeCompleted by remember { mutableStateOf(false) }

    // Floating XP completion animation states
    var showFloatingXp by remember { mutableStateOf(false) }
    var animOffset by remember { mutableStateOf(0f) }
    var animAlpha by remember { mutableStateOf(1f) }

    LaunchedEffect(showFloatingXp) {
        if (showFloatingXp) {
            animOffset = 0f
            animAlpha = 1f
            val duration = 400
            val startTime = android.os.SystemClock.uptimeMillis()
            while (true) {
                val elapsed = android.os.SystemClock.uptimeMillis() - startTime
                val fraction = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                animOffset = -45f * fraction
                animAlpha = 1f - fraction
                if (elapsed >= duration) break
                delay(12)
            }
            showFloatingXp = false
        }
    }

    val xpVal = com.example.data.repository.ExerciseData.getExerciseExpValue(exercise.name)
    val expText = com.example.data.repository.ExerciseData.getExerciseExpText(exercise.name)

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .border(0.5.dp, GlowPurple.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                .background(DarkPurpleAccent)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedItem = !expandedItem }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        color = GlowPurple,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "Muscles: ${exercise.musclesWorked.joinToString(", ")}",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = expText,
                            color = BrightPurpleHighlight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(DeepPurpleBg)
                        .padding(6.dp, 2.dp)
                ) {
                    Text(
                        exercise.category,
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (expandedItem) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Divider(color = GlowPurple.copy(alpha = 0.2f), thickness = 0.5.dp)

                    Text(
                        text = "◆ LOG PROTOCOL DETAILS",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )

                    Text(
                        text = setsRepsDetail,
                        color = BrightPurpleHighlight,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "EXECUTION PROTOCOLS:",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    exercise.steps.forEachIndexed { i, step ->
                        Text(
                            text = "${i + 1}. $step",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            completeCompleted = true
                            showFloatingXp = true
                            onComplete()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("complete_exercise_${exercise.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (completeCompleted) SuccessGreen.copy(alpha = 0.3f) else GlowPurple
                        ),
                        shape = RoundedCornerShape(4.dp),
                        enabled = !completeCompleted
                    ) {
                        Text(
                            text = if (completeCompleted) "S-RANK ACCOMPLISHED (+$xpVal XP)" else "MARK COMPLETE (+$xpVal XP)",
                            color = if (completeCompleted) SuccessGreen else DeepPurpleBg,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Floating Risen XP text overlay
        if (showFloatingXp) {
            Text(
                text = "+$xpVal EXP",
                color = Color(0xFF00FF88),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = animOffset.dp)
                    .graphicsLayer { alpha = animAlpha }
            )
        }
    }
}

// ==========================================
// WEEKLY PLAN MONITOR GRID
// ==========================================
@Composable
fun WeeklyPlanGrid(viewModel: HunterViewModel) {
    val weeklyCompletion by viewModel.weeklyCompletion.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val streak = profile?.currentStreak ?: 0

    val weekDays = listOf(
        Pair("Mon", Calendar.MONDAY),
        Pair("Tue", Calendar.TUESDAY),
        Pair("Wed", Calendar.WEDNESDAY),
        Pair("Thu", Calendar.THURSDAY),
        Pair("Fri", Calendar.FRIDAY),
        Pair("Sat", Calendar.SATURDAY),
        Pair("Sun", Calendar.SUNDAY)
    )

    // Highlight today
    val calendar = Calendar.getInstance()
    calendar.firstDayOfWeek = Calendar.MONDAY
    val todayIndex = when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> 0
    }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dates = remember {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val list = mutableListOf<String>()
        for (i in 0..6) {
            list.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        list
    }

    SystemWindow(title = "WEEKLY SYSTEM MONITOR") {
        val workouts by viewModel.workouts.collectAsStateWithLifecycle()
        val allQuests by viewModel.allQuests.collectAsStateWithLifecycle()
        Column(modifier = Modifier.fillMaxWidth()) {
            // Flame streak counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🔥 $streak DAY STREAK",
                        color = Color(0xFFFF9500),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "STATUS: ACTIVE",
                    color = Color(0xFF00FF87),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 7-day row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                weekDays.forEachIndexed { index, pair ->
                    val dayName = pair.first
                    val isToday = index == todayIndex
                    val dateString = dates.getOrElse(index) { "" }
                    val isCompleted = weeklyCompletion[dateString] ?: false
                    val isPast = index < todayIndex

                    // Circle frame spec size 44.dp
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isToday) Color(0xFF161622) else Color(0xFF0F0F16)
                            )
                            .border(
                                width = if (isToday) 2.dp else 1.dp,
                                color = when {
                                    isToday -> Color(0xFF00D4FF) // active glowing cyan
                                    isCompleted -> GlowPurple
                                    else -> Color.White.copy(alpha = 0.08f)
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Interior displays
                        when {
                            isCompleted -> {
                                // Completed day -> glowing purple checkmark
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = GlowPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            isPast && !isCompleted -> {
                                // Past missed day -> red X
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Missed",
                                    tint = DangerRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            isToday -> {
                                // Active current day label (e.g. Mon, Tue)
                                Text(
                                    text = dayName.uppercase(),
                                    color = Color(0xFF00D4FF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            else -> {
                                // Future day label
                                Text(
                                    text = dayName.take(3).lowercase(),
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // ADDED: Weekly EXP bar chart (Segmented Canvas bars)
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = GlowPurple.copy(alpha = 0.15f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "◆ WEEKLY SYSTEM EXP BAR CHART",
                color = Color(0xFF8888AA),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFF0C0C12), RoundedCornerShape(8.dp))
                    .border(0.5.dp, GlowPurple.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                val width = size.width
                val height = size.height
                val numDays = 7

                val dailyExp = dates.map { dateStr ->
                    val dayWorkouts = workouts.filter { it.dateString == dateStr }
                    val dayQuests = allQuests.filter { it.dateString == dateStr }
                    val completedQuests = dayQuests.filter { it.isCompleted }
                    var sum = dayWorkouts.sumOf { it.xpEarned } + completedQuests.sumOf { it.xpEarned }
                    if (dayQuests.isNotEmpty() && dayQuests.all { it.isCompleted }) {
                        sum += 100
                    }
                    sum.toFloat()
                }

                val maxExp = (dailyExp.maxOrNull() ?: 100f).coerceAtLeast(100f)
                val barWidth = 24.dp.toPx()
                val gap = (width - (barWidth * numDays)) / (numDays + 1)

                for (i in 0 until numDays) {
                    val exp = dailyExp[i]
                    val dayName = weekDays[i].first.uppercase()

                    val x = gap + i * (barWidth + gap)
                    val availableHeight = height - 34.dp.toPx()
                    val barHeight = (exp / maxExp) * availableHeight
                    val y = height - 18.dp.toPx() - barHeight

                    // Draw Bar background track
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.04f),
                        topLeft = Offset(x, 10.dp.toPx()),
                        size = Size(barWidth, availableHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )

                    // Draw Filled Bar
                    if (barHeight > 0f) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF00D4FF), Color(0xFF6C63FF))
                            ),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }

                    // Draw EXP value text
                    val textPaint = android.graphics.Paint().apply {
                        color = if (exp > 0f) 0xFFEAEAEA.toInt() else 0xFF8888AA.toInt()
                        textSize = 8.dp.toPx()
                        typeface = android.graphics.Typeface.MONOSPACE
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        "${exp.toInt()}",
                        x + barWidth / 2f,
                        y - 4.dp.toPx(),
                        textPaint
                    )

                    // Draw day text below bar
                    val dayPaint = android.graphics.Paint().apply {
                        color = if (i == todayIndex) 0xFF00D4FF.toInt() else 0xFF8888AA.toInt()
                        textSize = 9.dp.toPx()
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        dayName,
                        x + barWidth / 2f,
                        height - 4.dp.toPx(),
                        dayPaint
                    )
                }
            }
        }
    }
}

// ==========================================
// ACCOUNT / PROFILE SCREEN VIEW
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountScreenView(viewModel: HunterViewModel, profile: HunterProfile?, bmiHistory: List<com.example.data.database.BmiHistory>, workouts: List<WorkoutLog>) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showProfileEditDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(profile?.displayName ?: "Prince Good") }
    var selectedPreset by remember { mutableStateOf(profile?.profilePicUrl ?: "shadow") }

    var heightInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }
    var ageInput by remember { mutableStateOf("") }
    var activityLevelInput by remember(profile) { mutableStateOf(profile?.activityLevel ?: "Moderate") }

    val h = profile?.height ?: 170.0
    val w = profile?.weight ?: 65.0
    val uType = profile?.unitType ?: "METRIC"
    val age = profile?.age ?: 25
    val currentStreak = profile?.currentStreak ?: 0
    val workoutsCompleted = profile?.totalWorkoutsCompleted ?: 0
    val caloriesBurned = profile?.totalCaloriesBurned ?: 0.0
    val xp = profile?.totalXp ?: 10

    var notifyEnabled by remember(profile) { mutableStateOf(profile?.notificationEnabled ?: true) }
    var aiChatEnabled by remember(profile) { mutableStateOf(profile?.aiChatEnabled ?: true) }

    val metricPair = viewModel.repository.convertToMetric(w, h, uType)
    val bmi = viewModel.repository.calculateBmi(metricPair.first, metricPair.second)
    val bmiLabel = viewModel.repository.getBmiLabel(bmi)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // PROFILE HERO CARD (EDITABLE NAME & CHOOSE PIC PRESETS)
        SystemWindow(title = "PROFILE STATUS") {
            val preset = profile?.profilePicUrl ?: "shadow"
            val avatarStyle = when (preset) {
                "saiyan" -> Pair(Color(0xFFFF9500), Color(0xFFFFD700)) // Gold / Yellow
                "god" -> Pair(Color(0xFF007AFF), Color(0xFF5AC8FA)) // Bluish Spark
                "chaos" -> Pair(Color(0xFFFF3B30), Color(0xFFFF453A)) // Red Crimson
                else -> Pair(GlowPurple, BrightPurpleHighlight) // Standard Shadow Purple
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Profile Avatar with dynamic presets
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(avatarStyle.first)
                            .border(2.dp, avatarStyle.second, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile?.displayName?.firstOrNull()?.toString() ?: "P",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepPurpleBg,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            profile?.displayName ?: "Prince Good",
                            color = BrightPurpleHighlight,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        val accountType = if (profile?.isRegistered == true) {
                            "Registry ID: mnc-${profile.username?.lowercase() ?: "register"}"
                        } else {
                            "System Role: Guest Hunter"
                        }
                        Text(
                            accountType,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "Member Since: ${profile?.joinDateString}",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                IconButton(
                    onClick = {
                        nameInput = profile?.displayName ?: "Prince Good"
                        selectedPreset = profile?.profilePicUrl ?: "shadow"
                        showProfileEditDialog = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile Name and Avatar",
                        tint = GlowPurple
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // HUNTER RECORDS STATUS
        SystemWindow(title = "HUNTER RECORDS") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Physical Dimensions:",
                        color = BrightPurpleHighlight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Height: $h ${if (uType == "METRIC") "cm" else "in"} | Weight: $w ${if (uType == "METRIC") "kg" else "lbs"} | Age: $age yrs",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(
                    onClick = {
                        heightInput = h.toString()
                        weightInput = w.toString()
                        ageInput = age.toString()
                        activityLevelInput = profile?.activityLevel ?: "Moderate"
                        showEditDialog = true
                    },
                    modifier = Modifier.testTag("edit_records_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Dimensions",
                        tint = GlowPurple
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = GlowPurple.copy(alpha = 0.2f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // BMI Records
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("BMI Score:", color = TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text(
                    text = String.format(Locale.US, "%.1f", bmi),
                    color = BrightPurpleHighlight,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = bmiLabel,
                color = if (bmi < 18.5) DangerRed else if (bmi < 25) SuccessGreen else WarningAmber,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Total statistics
            RecordRow("Total workouts completed (All-time)", "$workoutsCompleted Done")
            RecordRow("Total estimated energy incinerated", "${caloriesBurned.toInt()} kcal")
            RecordRow("Total system XP", "$xp XP")
            RecordRow("Run Streak consecutive bonus days", "$currentStreak days")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // BMI 30-DAY CHART
        SystemWindow(title = "BMI VARIATION LOGS") {
            Text(
                text = "Last 30 Days Record Map:",
                color = TextSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Live Custom line chart
            CustomLineBmiGraph(bmiHistory = bmiHistory)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ARIA VISUALS GALLERY
        val savedVisuals by viewModel.savedVisuals.collectAsStateWithLifecycle()
        SystemWindow(title = "ARIA VISUALS") {
            Text(
                text = "Artifact database of exercise demonstrators saved by the Hunter:",
                color = TextSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (savedVisuals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, GlowPurple.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No Visual artifacts stored. Command ARIA: 'Show me how to do a deadlift' from the companion chat to summon them.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Responsive Grid/Row of saved images
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    savedVisuals.forEach { resId ->
                        val name = if (resId == R.drawable.deadlift_demo) "DEADLIFT VISUAL PROTOCOL" else "SQUAT VISUAL PROTOCOL"
                        val drillDesc = if (resId == R.drawable.deadlift_demo) {
                            "Keep bar close to shins, flat neutral back, drive vertically via hips."
                        } else {
                            "Hips back and down, keep weight balanced on midfoot, knees aligned."
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, GlowPurple, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = CardPurpleBg)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "◆ $name ◆",
                                    color = BrightPurpleHighlight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Image(
                                    painter = androidx.compose.ui.res.painterResource(id = resId),
                                    contentDescription = name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = drillDesc,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SYSTEM SETTINGS PANEL
        SystemWindow(title = "SYSTEM CONFIGS") {
            // UNIT TOGGLE
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Unit Standards:", color = TextPrimary, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                    Text("Metric vs Imperial indicators", color = TextSecondary, fontSize = 11.sp)
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkPurpleAccent)
                ) {
                    Box(
                        modifier = Modifier
                            .background(if (uType == "METRIC") GlowPurple else Color.Transparent)
                            .clickable { viewModel.changeUnits("METRIC") }
                            .padding(8.dp)
                    ) {
                        Text(
                            "Metric",
                            color = if (uType == "METRIC") DeepPurpleBg else TextSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(if (uType == "IMPERIAL") GlowPurple else Color.Transparent)
                            .clickable { viewModel.changeUnits("IMPERIAL") }
                            .padding(8.dp)
                    ) {
                        Text(
                            "Imperial",
                            color = if (uType == "IMPERIAL") DeepPurpleBg else TextSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = GlowPurple.copy(alpha = 0.1f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // REMINDERS TOGGLE
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Gate Notifications:", color = TextPrimary, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                    Text("Log and target reminders", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = notifyEnabled,
                    onCheckedChange = {
                        notifyEnabled = it
                        viewModel.setNotificationEnabled(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BrightPurpleHighlight,
                        checkedTrackColor = GlowPurple,
                        uncheckedBorderColor = TextMuted
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = GlowPurple.copy(alpha = 0.1f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // AI CHAT COMPANION TOGGLE
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Summon Companion:", color = TextPrimary, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                    Text("Toggle 'Shadow' floating partner", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = aiChatEnabled,
                    onCheckedChange = {
                        aiChatEnabled = it
                        viewModel.setAiChatEnabled(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BrightPurpleHighlight,
                        checkedTrackColor = GlowPurple,
                        uncheckedBorderColor = TextMuted
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SIGN OUT BUTTON
            Button(
                onClick = { viewModel.signOut() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sign_out_button"),
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "◆ TERMINATE SYSTEM CONNECTION ◆",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = CardPurpleBg,
            title = {
                Text(
                    "◆ UPDATE DIMENSIONS ◆",
                    color = BrightPurpleHighlight,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        "Input verified update metrics below:",
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Height
                    Text("Height (${if (uType == "METRIC") "cm" else "in"}):", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    TextField(
                        value = heightInput,
                        onValueChange = { heightInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GlowPurple,
                            unfocusedBorderColor = GlowPurple.copy(alpha = 0.4f)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )

                    // Weight
                    Text("Weight (${if (uType == "METRIC") "kg" else "lbs"}):", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    TextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GlowPurple,
                            unfocusedBorderColor = GlowPurple.copy(alpha = 0.4f)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )

                    // Age
                    Text("Age (years):", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    TextField(
                        value = ageInput,
                        onValueChange = { ageInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GlowPurple,
                            unfocusedBorderColor = GlowPurple.copy(alpha = 0.4f)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )

                    // Activity Level
                    Text("Activity Level:", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Sedentary", "Light", "Moderate", "Very", "Extra").forEach { act ->
                            val isSel = activityLevelInput == act
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) GlowPurple else DarkPurpleAccent)
                                    .border(
                                        1.dp,
                                        if (isSel) BrightPurpleHighlight else GlowPurple.copy(alpha = 0.2f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { activityLevelInput = act }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = act,
                                    color = if (isSel) DeepPurpleBg else TextPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newH = heightInput.toDoubleOrNull()
                        val newW = weightInput.toDoubleOrNull()
                        val newAge = ageInput.toIntOrNull()

                        if (newH != null && newH > 0 && newW != null && newW > 0 && newAge != null && newAge > 0) {
                            viewModel.updateProfileMeasurements(newH, newW, newAge, activityLevelInput)
                            showEditDialog = false
                        }
                    }
                ) {
                    Text("COMMIT DATA", color = SuccessGreen, fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("ABORT", color = DangerRed, fontFamily = FontFamily.Monospace)
                }
            },
            modifier = Modifier.border(1.dp, GlowPurple, RoundedCornerShape(12.dp))
        )
    }

    if (showProfileEditDialog) {
        AlertDialog(
            onDismissRequest = { showProfileEditDialog = false },
            containerColor = CardPurpleBg,
            title = {
                Text(
                    "◆ MODIFY CODES: IDENTITY ◆",
                    color = BrightPurpleHighlight,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Alter your monarch identity display parameters:",
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )

                    // Display Name input
                    Text("Display Name:", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    TextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GlowPurple,
                            unfocusedBorderColor = GlowPurple.copy(alpha = 0.4f)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Avatar Theme Preset Selection
                    Text("Identity Aura Theme:", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val presets = listOf(
                            Triple("shadow", Color(0xFF9F3BFF), "Shadow Purple"),
                            Triple("saiyan", Color(0xFFFF9500), "Saiyan Gold"),
                            Triple("god", Color(0xFF007AFF), "Godly Blue"),
                            Triple("chaos", Color(0xFFFF3B30), "Chaos Red")
                        )

                        presets.forEach { presetItem ->
                            val isSelected = selectedPreset == presetItem.first
                            Box(
                                modifier = Modifier
                                    .size(45.dp)
                                    .clip(CircleShape)
                                    .background(presetItem.second)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedPreset = presetItem.first },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected Theme",
                                        tint = DeepPurpleBg,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = "Theme label: " + when(selectedPreset) {
                            "saiyan" -> "Super Saiyan Gold Core"
                            "god" -> "God Aura Limit Breaker"
                            "chaos" -> "Chaos Blood Demon Core"
                            else -> "Monarch Shadow Abyss"
                        },
                        color = BrightPurpleHighlight,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            viewModel.updateProfileNameAndAvatar(nameInput, selectedPreset)
                            showProfileEditDialog = false
                        }
                    }
                ) {
                    Text("SAVE CHANGES", color = SuccessGreen, fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileEditDialog = false }) {
                    Text("ABORT", color = DangerRed, fontFamily = FontFamily.Monospace)
                }
            },
            modifier = Modifier.border(1.dp, GlowPurple, RoundedCornerShape(12.dp))
        )
    }
}

@Composable
fun RecordRow(label: String, valStr: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Text(valStr, color = BrightPurpleHighlight, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

// ==========================================
// CUSTOM VECTOR LINE BMI GRAPH COMPOSABLE
// ==========================================
@Composable
fun CustomLineBmiGraph(bmiHistory: List<com.example.data.database.BmiHistory>) {
    // Show placeholder if empty or lists are too small
    if (bmiHistory.size < 2) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(DarkPurpleAccent)
                .border(0.5.dp, GlowPurple.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = TextMuted, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Insufficient logs to display variation trends. Make updates to records to map dynamic paths.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        // Draw real custom Canvas line nodes
        val bmiVals = bmiHistory.map { it.bmi.toFloat() }
        val maxBmi = bmiVals.maxOrNull() ?: 25f
        val minBmi = bmiVals.minOrNull() ?: 18f
        val diffBmi = (maxBmi - minBmi).coerceAtLeast(0.5f)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(DarkPurpleAccent, RoundedCornerShape(8.dp))
                .border(0.5.dp, GlowPurple.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            val width = size.width
            val height = size.height

            val spacingX = width / (bmiVals.size - 1)
            val points = mutableListOf<Offset>()

            bmiVals.forEachIndexed { index, value ->
                val x = index * spacingX
                // Match normalized position
                val normalizedY = (value - minBmi) / diffBmi
                val y = height - (normalizedY * (height - 20.dp.toPx())) - 10.dp.toPx()
                points.add(Offset(x, y))
            }

            // Draw shadow fill gradient under the path
            val pathFill = Path().apply {
                moveTo(0f, height)
                points.forEach { pt ->
                    lineTo(pt.x, pt.y)
                }
                lineTo(width, height)
                close()
            }
            drawPath(
                path = pathFill,
                brush = Brush.verticalGradient(
                    colors = listOf(GlowPurple.copy(alpha = 0.3f), Color.Transparent)
                )
            )

            // Draw clean connect lines
            val path = Path().apply {
                points.forEachIndexed { i, pt ->
                    if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                }
            }
            drawPath(
                path = path,
                color = GlowPurple,
                style = Stroke(width = 2.5.dp.toPx(), join = StrokeJoin.Round)
            )

            // Draw tiny accent rings on point changes
            points.forEach { pt ->
                drawCircle(
                    color = DeepPurpleBg,
                    radius = 4.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = BrightPurpleHighlight,
                    radius = 2.dp.toPx(),
                    center = pt
                )
            }
        }
    }
}

// ==========================================
// FLOATING COMPANION ORB (FAB)
// ==========================================
@Composable
fun FloatingCompanionFab(
    modifier: Modifier = Modifier,
    isOnline: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "FabAnim")
    val sizeGlow by infiniteTransition.animateFloat(
        initialValue = 54f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowPulse"
    )

    Box(
        modifier = modifier
            .size(sizeGlow.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Glowing Outer Ambient Rings
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (isOnline) GlowPurple.copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Core Circle
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    if (isOnline) {
                        Brush.linearGradient(colors = listOf(GlowPurple, BrightPurpleHighlight))
                    } else {
                        Brush.linearGradient(colors = listOf(Color.Gray, Color.DarkGray))
                    }
                )
                .border(2.dp, DeepPurpleBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Summon companion",
                tint = DeepPurpleBg,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ==========================================
// CHAT DRAWER OVERLAY (COMPANION VIEW)
// ==========================================
@Composable
fun ChatDrawerOverlay(
    chatHistory: List<ChatMessage>,
    chatLoading: Boolean,
    isOnline: Boolean,
    profile: HunterProfile?,
    onSendMessage: (String) -> Unit,
    onClose: () -> Unit,
    onSaveVisual: (Int) -> Unit
) {
    var chatText by remember { mutableStateOf("") }
    val firstName = profile?.displayName?.split(" ")?.firstOrNull() ?: "Prince"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(onClick = onClose) // Click outside to close
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(DeepPurpleBg)
                .border(
                    width = 1.dp,
                    color = GlowPurple.copy(alpha = 0.60f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Capture clicks inside
                )
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
        ) {
            // DRAWER HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Profile image for Shadow
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(DarkPurpleAccent)
                            .border(1.dp, GlowPurple, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "S",
                            color = BrightPurpleHighlight,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "SHADOW • COMPANION",
                            color = BrightPurpleHighlight,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnline) SuccessGreen else WarningAmber)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isOnline) "LOYAL AND MANIFEST" else "RESTING",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Divider(color = GlowPurple.copy(alpha = 0.2f), thickness = 0.5.dp)

            if (!isOnline) {
                // OFFLINE MODE DISPLAY
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(
                            "◆ CONNECTION ERROR ◆",
                            color = DangerRed,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Shadow cannot be summoned without an active system connection, Hunter.",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Establish a cloud network to awaken your loyal companion.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // ONLINE ACTIVE CHAT PANEL
                val chatScroll = rememberScrollState()

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (chatHistory.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Greeting, Hunter $firstName.",
                                color = BrightPurpleHighlight,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "\"Your systems are operational. Speak, and I shall provide strategies for today's battlefields.\"",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                style = TextStyle(fontFamily = FontFamily.Serif),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(chatScroll)
                        ) {
                            chatHistory.forEach { msg ->
                                val isShadow = msg.sender == Sender.SHADOW
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = if (isShadow) Alignment.CenterStart else Alignment.CenterEnd
                                ) {
                                    Column(
                                        horizontalAlignment = if (isShadow) Alignment.Start else Alignment.End
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .widthIn(max = 280.dp)
                                                .clip(
                                                    RoundedCornerShape(
                                                        topStart = 12.dp,
                                                        topEnd = 12.dp,
                                                        bottomStart = if (isShadow) 0.dp else 12.dp,
                                                        bottomEnd = if (isShadow) 12.dp else 0.dp
                                                    )
                                                )
                                                .background(if (isShadow) CardPurpleBg else GlowPurple)
                                                .border(
                                                    width = 0.5.dp,
                                                    color = if (isShadow) GlowPurple else BrightPurpleHighlight,
                                                    shape = RoundedCornerShape(
                                                        topStart = 12.dp,
                                                        topEnd = 12.dp,
                                                        bottomStart = if (isShadow) 0.dp else 12.dp,
                                                        bottomEnd = if (isShadow) 12.dp else 0.dp
                                                    )
                                                )
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                text = msg.text,
                                                color = if (isShadow) TextPrimary else DeepPurpleBg,
                                                fontSize = 13.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        // Render illustration if present
                                        msg.illustResId?.let { resId ->
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Card(
                                                modifier = Modifier
                                                    .width(220.dp)
                                                    .border(1.dp, GlowPurple.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                                                colors = CardDefaults.cardColors(containerColor = CardPurpleBg)
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Image(
                                                        painter = androidx.compose.ui.res.painterResource(id = resId),
                                                        contentDescription = "Demo visual",
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .aspectRatio(1f)
                                                            .clip(RoundedCornerShape(6.dp)),
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))

                                                    var savedState by remember { mutableStateOf(false) }

                                                    Button(
                                                        onClick = {
                                                            onSaveVisual(resId)
                                                            savedState = true
                                                        },
                                                        enabled = !savedState,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = GlowPurple,
                                                            disabledContainerColor = DarkPurpleAccent
                                                        ),
                                                        contentPadding = PaddingValues(vertical = 4.dp),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = if (savedState) "SAVED IN ARIA VISUALS" else "STORE VISUAL PROTOCOL",
                                                            color = if (savedState) TextMuted else DeepPurpleBg,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (chatLoading) {
                                Box(
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CardPurpleBg)
                                        .padding(12.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(0, 1, 2).forEach { dot ->
                                            val animatedAlpha by rememberInfiniteTransition().animateFloat(
                                                initialValue = 0.2f,
                                                targetValue = 1f,
                                                animationSpec = infiniteRepeatable(
                                                    animation = tween(600, delayMillis = dot * 150),
                                                    repeatMode = RepeatMode.Reverse
                                                )
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(BrightPurpleHighlight.copy(alpha = animatedAlpha))
                                            )
                                        }
                                    }
                                }
                            }

                            // Auto Scroll to Bottom effect
                            LaunchedEffect(chatHistory.size, chatLoading) {
                                chatScroll.animateScrollTo(chatScroll.maxValue)
                            }
                        }
                    }
                }

                Divider(color = GlowPurple.copy(alpha = 0.15f), thickness = 0.5.dp)

                // TEXT FIELD ENTRY INPUT
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = chatText,
                        onValueChange = { chatText = it },
                        placeholder = { Text("Command your shadow army...", color = TextMuted, fontSize = 13.sp, fontFamily = FontFamily.Monospace) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GlowPurple,
                            unfocusedBorderColor = GlowPurple.copy(alpha = 0.4f),
                            focusedContainerColor = DarkPurpleAccent,
                            unfocusedContainerColor = DarkPurpleAccent
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (chatText.trim().isNotEmpty()) {
                                onSendMessage(chatText)
                                chatText = ""
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(GlowPurple)
                            .testTag("send_chat_button")
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = DeepPurpleBg)
                    }
                }
            }
        }
    }
}

// ==========================================
// LEVEL UP POPUP OVERLAY VIEW
// ==========================================
@Composable
fun LevelUpOverlay(
    oldRank: String,
    newRank: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(onClick = onDismiss)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "PortalPulseGlow")
        val portalScale by infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "PortalScaling"
        )

        Column(
            modifier = Modifier
                .border(2.dp, GlowPurple, RoundedCornerShape(16.dp))
                .background(DeepPurpleBg)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Portal Ring Graphic
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    GlowPurple.copy(alpha = 0.8f),
                                    Color.Transparent
                                )
                            ),
                            radius = 80.dp.toPx() * portalScale
                        )
                        drawCircle(
                            color = BrightPurpleHighlight,
                            radius = 50.dp.toPx() * portalScale,
                            style = Stroke(width = 5.dp.toPx())
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = newRank,
                    color = BrightPurpleHighlight,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "◆ YOU HAVE LEVELED UP, HUNTER ◆",
                color = BrightPurpleHighlight,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Class Evolution Achieved!",
                color = TextPrimary,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = oldRank,
                    color = TextSecondary,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "  ➜  ",
                    color = GlowPurple,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = newRank,
                    color = SuccessGreen,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Tap screen to dismiss protocol",
                color = TextMuted,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ==========================================
// CUSTOM VISUAL SYSTEM WINDOW PANEL
// ==========================================
@Composable
fun SystemWindow(
    modifier: Modifier = Modifier,
    title: String? = null,
    borderWidth: Dp = 1.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(
                borderWidth,
                GlowPurple.copy(alpha = 0.50f),
                RoundedCornerShape(16.dp)
            )
            .background(CardPurpleBg, RoundedCornerShape(16.dp))
    ) {
        if (title != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(GlowPurple.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "◆ $title ◆",
                    color = BrightPurpleHighlight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp
                )
            }
            Divider(color = GlowPurple.copy(alpha = 0.3f), thickness = 0.5.dp)
        }

        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            content()
        }
    }
}

// ==========================================
// APP BAR HEADER COMPOSABLE
// ==========================================
@Composable
fun HunterAppBar(
    profile: HunterProfile?,
    isOnline: Boolean,
    xpScale: (Int) -> String
) {
    val xp = profile?.totalXp ?: 0
    val rankCode = xpScale(xp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(DeepPurpleBg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Little portal indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(DarkPurpleAccent)
                    .border(1.dp, GlowPurple, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(BrightPurpleHighlight)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SHADOW ARISE",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        // Connection State Dot
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isOnline) SuccessGreen.copy(alpha = 0.1f) else WarningAmber.copy(alpha = 0.1f))
                .border(
                    width = 0.5.dp,
                    color = if (isOnline) SuccessGreen.copy(alpha = 0.3f) else WarningAmber.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) SuccessGreen else WarningAmber)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isOnline) "Connected" else "Offline Mode",
                color = if (isOnline) SuccessGreen else WarningAmber,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ==========================================
// GLASS NAVIGATION BAR COMPOSABLE
// ==========================================
@Composable
fun GlassBottomNavigation(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding() // Soft cutout for gesture system nav lines
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(DeepPurpleBg.copy(alpha = 0.82f))
            .border(
                width = 1.dp,
                color = GlowPurple.copy(alpha = 0.40f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            Triple("EXERCISE", Icons.Default.Star, "exercise_tab"),
            Triple("HOME", Icons.Default.Home, "home_tab"),
            Triple("ACCOUNT", Icons.Default.Person, "account_tab")
        ).forEach { tab ->
            val isCurrent = activeTab == tab.first
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .testTag(tab.third)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onTabSelected(tab.first) }
                    )
                    .padding(8.dp)
            ) {
                // Active Dot Indicator ABOVE
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isCurrent) BrightPurpleHighlight else Color.Transparent)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Icon(
                    imageVector = tab.second,
                    contentDescription = tab.first,
                    tint = if (isCurrent) BrightPurpleHighlight else TextMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ==========================================
// STAT INFO BUTTON COMPOSABLE
// ==========================================
@Composable
fun StatInfoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .border(1.dp, GlowPurple.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "i",
                color = GlowPurple,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ==========================================
// SYSTEM INFO MODAL DIALOG
// ==========================================
@Composable
fun SystemInfoModal(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DeepPurpleBg)
                .border(2.dp, GlowPurple, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "◆ $title",
                        color = BrightPurpleHighlight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = DangerRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = GlowPurple.copy(alpha = 0.2f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                content()

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(GlowPurple)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "DISMISS DECREE",
                        color = DeepPurpleBg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
