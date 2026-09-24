package com.mport.tv.feature.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mport.tv.R
import kotlinx.coroutines.delay

private val Navy = Color(0xFF0A1628)
private val NavyDeep = Color(0xFF050B14)
private val Cyan = Color(0xFF00E5FF)
private val BlueGlow = Color(0xFF1E88E5)
private val WhiteSoft = Color(0xFFE3F2FD)

/**
 * Animated splash: gradient bg, pulsing rings, logo scale/fade, title + tagline.
 * Calls [onFinished] after the sequence (~2.4s).
 */
@Composable
fun AnimatedSplashScreen(onFinished: () -> Unit) {
    var phase by remember { mutableStateOf(0) }

    // Logo enter
    val logoScale = remember { Animatable(0.55f) }
    val logoAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffset = remember { Animatable(24f) }
    val tagAlpha = remember { Animatable(0f) }
    val exitAlpha = remember { Animatable(1f) }

    // Infinite pulse for rings
    val infinite = rememberInfiniteTransition(label = "splash")
    val ringPulse by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring"
    )
    val ringAlpha by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringA"
    )
    val dotAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit"
    )
    val shimmer by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    LaunchedEffect(Unit) {
        // 1) Logo appears
        logoAlpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        logoScale.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = 280f))
        phase = 1
        delay(200)
        // 2) Title
        titleAlpha.animateTo(1f, tween(400))
        titleOffset.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
        phase = 2
        delay(150)
        // 3) Tagline
        tagAlpha.animateTo(1f, tween(400))
        phase = 3
        delay(900)
        // 4) Exit
        exitAlpha.animateTo(0f, tween(350, easing = FastOutLinearInEasing))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(exitAlpha.value)
            .background(
                Brush.verticalGradient(
                    colors = listOf(NavyDeep, Navy, Color(0xFF0D2137))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Ambient glow behind logo
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2 - 40.dp.toPx()
            val baseR = size.minDimension * 0.22f

            // Soft radial glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        BlueGlow.copy(alpha = 0.35f * logoAlpha.value),
                        Cyan.copy(alpha = 0.08f * logoAlpha.value),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = baseR * 2.2f * ringPulse
                ),
                center = Offset(cx, cy),
                radius = baseR * 2.2f * ringPulse
            )

            // Outer arc ring
            drawCircle(
                color = Cyan.copy(alpha = ringAlpha * logoAlpha.value),
                radius = baseR * 1.55f * ringPulse,
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
            // Inner dashed-feel ring (partial arcs)
            drawArc(
                color = BlueGlow.copy(alpha = 0.6f * logoAlpha.value),
                startAngle = dotAngle,
                sweepAngle = 70f,
                useCenter = false,
                topLeft = Offset(cx - baseR * 1.25f, cy - baseR * 1.25f),
                size = androidx.compose.ui.geometry.Size(baseR * 2.5f, baseR * 2.5f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = Cyan.copy(alpha = 0.5f * logoAlpha.value),
                startAngle = dotAngle + 180f,
                sweepAngle = 50f,
                useCenter = false,
                topLeft = Offset(cx - baseR * 1.25f, cy - baseR * 1.25f),
                size = androidx.compose.ui.geometry.Size(baseR * 2.5f, baseR * 2.5f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Circuit dots on orbit
            val orbitR = baseR * 1.55f * ringPulse
            for (i in 0 until 6) {
                val a = Math.toRadians((dotAngle + i * 60.0))
                val dx = (kotlin.math.cos(a) * orbitR).toFloat()
                val dy = (kotlin.math.sin(a) * orbitR).toFloat()
                drawCircle(
                    color = Cyan.copy(alpha = (0.4f + 0.4f * ringAlpha) * logoAlpha.value),
                    radius = 4.dp.toPx(),
                    center = Offset(cx + dx, cy + dy)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.splash_logo),
                contentDescription = "MPorT TV",
                modifier = Modifier
                    .size(160.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "MPorT TV",
                color = WhiteSoft.copy(alpha = titleAlpha.value),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.offset(y = titleOffset.value.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Stream · Discover · Play",
                color = Cyan.copy(alpha = tagAlpha.value * (0.7f + 0.3f * shimmer)),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Minimal loading bar
            if (phase >= 1) {
                LoadingBar(
                    progress = when (phase) {
                        1 -> 0.35f
                        2 -> 0.65f
                        else -> 1f
                    },
                    alpha = tagAlpha.value.coerceAtLeast(0.3f) * logoAlpha.value
                )
            }
        }
    }
}

@Composable
private fun LoadingBar(progress: Float, alpha: Float) {
    val animated = animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "bar"
    )
    Box(
        modifier = Modifier
            .width(120.dp)
            .height(3.dp)
            .alpha(alpha)
            .background(Color.White.copy(alpha = 0.12f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated.value)
                .background(
                    Brush.horizontalGradient(listOf(BlueGlow, Cyan))
                )
        )
    }
}
