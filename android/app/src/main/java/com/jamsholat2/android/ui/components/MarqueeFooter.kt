package com.jamsholat2.android.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun MarqueeFooter(
    text: String,
    speed: Int, // 1-9 like js scroll speed, 5 default
    modifier: Modifier = Modifier
) {
    // Background colorRotate animation: #006BA6 -> #0496FF -> #2D3047 -> #D81159 -> #B80C09 10s alternate
    val infiniteTransition = rememberInfiniteTransition(label = "colorRotate")
    val bgColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF006BA6),
        targetValue = Color(0xFFB80C09),
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 10000
                Color(0xFF006BA6) at 0
                Color(0xFF0496FF) at 2500
                Color(0xFF2D3047) at 5000
                Color(0xFFD81159) at 7500
                Color(0xFFB80C09) at 10000
            },
            repeatMode = RepeatMode.Reverse
        ), label = "bgColor"
    )

    // Marquee speed formula: animationSeconds = ((10 - speed)/10)*text.length/2 ; min 1s
    val animSeconds = remember(text, speed) {
        val s = speed.coerceIn(1, 9)
        var sec = ((10 - s) / 10.0) * text.length / 2.0
        if (sec <= 0) sec = 1.0
        // Clamp to reasonable 5..60
        sec.coerceIn(5.0, 60.0)
    }

    var containerWidthPx by remember { mutableStateOf(0) }
    var textWidthPx by remember { mutableStateOf(0) }

    // Two copies for seamless marquee with delay
    // Use key(speed,text) so animation restarts when speed or text changes
    val offset by key(speed, text) {
        val transition = rememberInfiniteTransition(label = "marquee-$speed")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = -1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = (animSeconds * 1000).toInt(), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ), label = "offset"
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(39.dp)
            .background(bgColor)
            .onGloballyPositioned { containerWidthPx = it.size.width },
        contentAlignment = Alignment.CenterStart
    ) {
        // We use simple offset animation; measure not needed for correctness, just animate translation
        // Use Row with offset
        val translatePx = remember(offset, containerWidthPx, textWidthPx) {
            // Map offset 0..-1 to containerWidth .. -textWidth
            val total = containerWidthPx + textWidthPx
            if (total == 0) 0f else offset * total + containerWidthPx
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(translatePx.roundToInt(), 0) }
                .onGloballyPositioned { textWidthPx = it.size.width }
        ) {
            Text(
                text = text,
                color = Color.Yellow,
                fontSize = 19.sp,
                maxLines = 1,
                modifier = Modifier
            )
        }
    }
}
