package com.jamsholat2.android.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TimeBox(
    label: String,
    time: String,
    isCurrent: Boolean,
    isNext: Boolean,
    isBlinkingRed: Boolean,
    isBlinkingGreen: Boolean,
    modifier: Modifier = Modifier
) {
    // Colors matching index.html
    val greenBorder = Color(0xFF006300)
    val yellowBorder = Color.Yellow
    val redBorder = Color.Red

    val infiniteTransition = rememberInfiniteTransition(label = "blink-$label")
    val blinkProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "blinkProgress"
    )

    // Background colors: default rgba(0,0,0,0.5) ; blinking toggles to transparent vs red/green
    val dataBg = when {
        isBlinkingRed -> {
            // blinkingred 0% rgba(255,0,0,1) -> 100% rgba(0,0,0,0.5)
            if (blinkProgress < 0.5f) Color.Red else Color(0x80000000)
        }
        isBlinkingGreen -> {
            // blinkinggreen 0% rgb(0,255,0) -> rgba(0,0,0,0.5)
            if (blinkProgress < 0.5f) Color(0xFF00FF00) else Color(0x80000000)
        }
        else -> Color(0x80000000)
    }

    val borderColor = when {
        isCurrent -> redBorder
        isNext -> yellowBorder
        else -> greenBorder
    }

    // Special handling for current also has pray-active, but border should be red overriding
    val finalBorder = if (isCurrent) redBorder else if (isNext) yellowBorder else greenBorder

    Column(
        modifier = modifier
            .border(width = 3.dp, color = finalBorder, shape = RoundedCornerShape(7.dp))
            .clip(RoundedCornerShape(7.dp))
            .background(Color.Transparent)
            .fillMaxHeight()
    ) {
        // label container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    when {
                        isNext -> Color.Yellow.copy(alpha = 0.3f)
                        isCurrent -> Color.Red.copy(alpha = 0.3f)
                        else -> Color(0xD9006300) // rgba(0,99,0,0.85)
                    }
                )
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
        // time data
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(dataBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = time,
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
