package com.jamsholat2.android.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val Aqua = Color(0xFF00FFFF)
private val AquaDark = Color(0xFF00BCD4)

fun Modifier.aquaFocusBorder(
    shape: RoundedCornerShape = RoundedCornerShape(6.dp),
    width: Dp = 4.dp,
    color: Color = Aqua
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    this
        .onFocusChanged { isFocused = it.isFocused }
        .border(
            width = if (isFocused) width else 0.dp,
            color = if (isFocused) color else Color.Transparent,
            shape = shape
        )
}

@Composable
fun Modifier.aquaFocusBorder(
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource,
    shape: RoundedCornerShape = RoundedCornerShape(6.dp),
    width: Dp = 4.dp,
    color: Color = Aqua,
    enabled: Boolean = true
): Modifier {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val show = isFocused && enabled
    return this.border(
        width = if (show) width else 0.dp,
        color = if (show) color else Color.Transparent,
        shape = shape
    )
}

@Composable
fun aquaButtonColors(interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource): androidx.compose.material3.ButtonColors {
    val isFocused by interactionSource.collectIsFocusedAsState()
    return androidx.compose.material3.ButtonDefaults.buttonColors(
        containerColor = if (isFocused) AquaDark else Color(0xFF2D2D2D),
        contentColor = if (isFocused) Color.Black else Color.White
    )
}

@Composable
fun aquaOutlinedButtonColors(interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource): androidx.compose.material3.ButtonColors {
    val isFocused by interactionSource.collectIsFocusedAsState()
    return androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
        containerColor = if (isFocused) AquaDark else Color.Transparent,
        contentColor = if (isFocused) Color.Black else Color.White
    )
}

@Composable
fun aquaButtonBorder(interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource): androidx.compose.foundation.BorderStroke? {
    val isFocused by interactionSource.collectIsFocusedAsState()
    return if (isFocused) androidx.compose.foundation.BorderStroke(2.dp, AquaDark) else null
}
