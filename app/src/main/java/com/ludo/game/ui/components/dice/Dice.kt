package com.ludo.game.ui.components.dice

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Animated dice composable.
 */
@Composable
fun Dice(
    value: Int,
    isRolling: Boolean,
    enabled: Boolean,
    onRoll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var displayValue by remember { mutableIntStateOf(value) }
    val rotation by animateFloatAsState(
        targetValue = if (isRolling) 360f * 3 else 0f,
        animationSpec = tween(
            durationMillis = if (isRolling) 800 else 0,
            easing = FastOutSlowInEasing
        ),
        label = "dice_rotation"
    )
    
    // Animate through random values while rolling
    LaunchedEffect(isRolling) {
        if (isRolling) {
            repeat(10) {
                displayValue = (1..6).random()
                delay(80)
            }
            displayValue = value
        } else {
            displayValue = value
        }
    }
    
    Box(
        modifier = modifier
            .size(80.dp)
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .rotate(rotation)
            .clickable(enabled = enabled && !isRolling) { onRoll() },
        contentAlignment = Alignment.Center
    ) {
        DiceFace(
            value = displayValue,
            modifier = Modifier.fillMaxSize().padding(12.dp)
        )
    }
}

/**
 * Draws the dots on a dice face.
 */
@Composable
private fun DiceFace(
    value: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val dotRadius = size.minDimension / 8
        val centerX = size.width / 2
        val centerY = size.height / 2
        val offset = size.width / 4
        
        val dotPositions = when (value) {
            1 -> listOf(Offset(centerX, centerY))
            2 -> listOf(
                Offset(centerX - offset, centerY - offset),
                Offset(centerX + offset, centerY + offset)
            )
            3 -> listOf(
                Offset(centerX - offset, centerY - offset),
                Offset(centerX, centerY),
                Offset(centerX + offset, centerY + offset)
            )
            4 -> listOf(
                Offset(centerX - offset, centerY - offset),
                Offset(centerX + offset, centerY - offset),
                Offset(centerX - offset, centerY + offset),
                Offset(centerX + offset, centerY + offset)
            )
            5 -> listOf(
                Offset(centerX - offset, centerY - offset),
                Offset(centerX + offset, centerY - offset),
                Offset(centerX, centerY),
                Offset(centerX - offset, centerY + offset),
                Offset(centerX + offset, centerY + offset)
            )
            6 -> listOf(
                Offset(centerX - offset, centerY - offset),
                Offset(centerX + offset, centerY - offset),
                Offset(centerX - offset, centerY),
                Offset(centerX + offset, centerY),
                Offset(centerX - offset, centerY + offset),
                Offset(centerX + offset, centerY + offset)
            )
            else -> emptyList()
        }
        
        dotPositions.forEach { position ->
            drawCircle(
                color = Color(0xFF212121),
                radius = dotRadius,
                center = position
            )
        }
    }
}

/**
 * Dice with roll button and result display.
 */
@Composable
fun DiceWithControls(
    diceValue: Int?,
    isRolling: Boolean,
    canRoll: Boolean,
    isMyTurn: Boolean,
    onRoll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Dice(
            value = diceValue ?: 1,
            isRolling = isRolling,
            enabled = canRoll && isMyTurn,
            onRoll = onRoll
        )
        
        if (!isMyTurn) {
            androidx.compose.material3.Text(
                text = "Waiting for opponent...",
                color = Color.Gray
            )
        } else if (canRoll) {
            androidx.compose.material3.Text(
                text = "Tap dice to roll!",
                color = Color(0xFF4CAF50)
            )
        } else if (diceValue != null) {
            androidx.compose.material3.Text(
                text = "Select a token to move",
                color = Color(0xFF2196F3)
            )
        }
    }
}
