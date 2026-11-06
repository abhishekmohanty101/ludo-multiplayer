package com.ludo.game.ui.components.board

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ludo.game.data.model.PlayerColor
import com.ludo.game.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Animated token piece with movement and capture animations.
 */
@Composable
fun AnimatedToken(
    color: PlayerColor,
    fromPosition: TokenPosition?,
    toPosition: TokenPosition,
    cellSize: Dp,
    isSelectable: Boolean,
    isBeingCaptured: Boolean,
    onAnimationComplete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokenColor = when (color) {
        PlayerColor.RED -> LudoRed
        PlayerColor.GREEN -> LudoGreen
        PlayerColor.YELLOW -> LudoYellow
        PlayerColor.BLUE -> LudoBlue
    }
    
    // Movement animation
    var animatedRow by remember { mutableFloatStateOf(fromPosition?.row?.toFloat() ?: toPosition.row.toFloat()) }
    var animatedCol by remember { mutableFloatStateOf(fromPosition?.col?.toFloat() ?: toPosition.col.toFloat()) }
    
    // Animate position change
    LaunchedEffect(toPosition) {
        if (fromPosition != null && (fromPosition.row != toPosition.row || fromPosition.col != toPosition.col)) {
            // Animate movement
            val steps = 20
            val rowStep = (toPosition.row - fromPosition.row) / steps.toFloat()
            val colStep = (toPosition.col - fromPosition.col) / steps.toFloat()
            
            repeat(steps) { step ->
                animatedRow = fromPosition.row + (rowStep * (step + 1))
                animatedCol = fromPosition.col + (colStep * (step + 1))
                delay(25)
            }
            
            animatedRow = toPosition.row.toFloat()
            animatedCol = toPosition.col.toFloat()
            onAnimationComplete()
        } else {
            animatedRow = toPosition.row.toFloat()
            animatedCol = toPosition.col.toFloat()
        }
    }
    
    // Capture animation (explode effect)
    val captureScale by animateFloatAsState(
        targetValue = if (isBeingCaptured) 0f else 1f,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "capture_scale"
    )
    
    // Pulse animation for selectable tokens
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    // Glow effect for selectable tokens
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    
    if (toPosition.boardPosition == 57 && !isBeingCaptured) return // Token finished
    
    val tokenSize = cellSize * 0.7f
    val offsetX = cellSize * animatedCol + (cellSize - tokenSize) / 2
    val offsetY = cellSize * animatedRow + (cellSize - tokenSize) / 2
    
    Box(
        modifier = modifier
            .offset(x = offsetX, y = offsetY)
            .size(tokenSize)
            .scale(if (isBeingCaptured) captureScale else if (isSelectable) pulseScale else 1f)
    ) {
        // Glow effect for selectable tokens
        if (isSelectable) {
            Box(
                modifier = Modifier
                    .size(tokenSize * 1.3f)
                    .offset(x = -tokenSize * 0.15f, y = -tokenSize * 0.15f)
                    .clip(CircleShape)
                    .background(tokenColor.copy(alpha = glowAlpha))
            )
        }
        
        // Main token
        Box(
            modifier = Modifier
                .size(tokenSize)
                .clip(CircleShape)
                .background(tokenColor)
                .clickable(enabled = isSelectable) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            // Inner highlight
            Box(
                modifier = Modifier
                    .size(tokenSize * 0.7f)
                    .offset(x = -tokenSize * 0.05f, y = -tokenSize * 0.05f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
            )
            
            // Center dot
            Box(
                modifier = Modifier
                    .size(tokenSize * 0.25f)
                    .clip(CircleShape)
                    .background(tokenColor.copy(alpha = 0.7f))
            )
        }
    }
}

/**
 * Capture explosion effect.
 */
@Composable
fun CaptureExplosion(
    position: TokenPosition,
    cellSize: Dp,
    color: PlayerColor,
    onComplete: () -> Unit
) {
    val tokenColor = when (color) {
        PlayerColor.RED -> LudoRed
        PlayerColor.GREEN -> LudoGreen
        PlayerColor.YELLOW -> LudoYellow
        PlayerColor.BLUE -> LudoBlue
    }
    
    var isVisible by remember { mutableStateOf(true) }
    
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 2f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        finishedListener = { onComplete() },
        label = "explosion_scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 0f else 1f,
        animationSpec = tween(400),
        label = "explosion_alpha"
    )
    
    LaunchedEffect(Unit) {
        delay(50)
        isVisible = false
    }
    
    val offsetX = cellSize * position.col + cellSize / 2
    val offsetY = cellSize * position.row + cellSize / 2
    
    Canvas(
        modifier = Modifier
            .offset(x = offsetX - cellSize / 2, y = offsetY - cellSize / 2)
            .size(cellSize)
    ) {
        // Draw expanding circles
        drawCircle(
            color = tokenColor.copy(alpha = (1f - alpha) * 0.5f),
            radius = size.minDimension * scale / 2,
            center = Offset(size.width / 2, size.height / 2)
        )
        
        // Draw particle rays
        val rays = 8
        for (i in 0 until rays) {
            val angle = (i * 360f / rays) * (Math.PI / 180f)
            val length = size.minDimension * scale * 0.3f
            
            drawLine(
                color = tokenColor.copy(alpha = 1f - alpha),
                start = Offset(size.width / 2, size.height / 2),
                end = Offset(
                    (size.width / 2 + kotlin.math.cos(angle) * length).toFloat(),
                    (size.height / 2 + kotlin.math.sin(angle) * length).toFloat()
                ),
                strokeWidth = 4f
            )
        }
    }
}

/**
 * Token entering home celebration.
 */
@Composable
fun TokenHomeCelebration(
    cellSize: Dp,
    color: PlayerColor,
    onComplete: () -> Unit
) {
    val tokenColor = when (color) {
        PlayerColor.RED -> LudoRed
        PlayerColor.GREEN -> LudoGreen
        PlayerColor.YELLOW -> LudoYellow
        PlayerColor.BLUE -> LudoBlue
    }
    
    var stage by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        repeat(3) {
            stage = it
            delay(200)
        }
        onComplete()
    }
    
    val centerX = cellSize * 7.5f
    val centerY = cellSize * 7.5f
    
    // Stars bursting from center
    val starOffsets = remember {
        (0 until 8).map {
            val angle = it * 45.0 * Math.PI / 180
            Pair(
                kotlin.math.cos(angle).toFloat(),
                kotlin.math.sin(angle).toFloat()
            )
        }
    }
    
    starOffsets.forEachIndexed { index, (dx, dy) ->
        val distance = cellSize * (stage + 1) * 0.5f
        
        Box(
            modifier = Modifier
                .offset(
                    x = centerX - 8.dp + distance * dx,
                    y = centerY - 8.dp + distance * dy
                )
                .size(16.dp)
                .background(tokenColor.copy(alpha = 1f - stage * 0.3f), CircleShape)
        )
    }
}

/**
 * Movement trail effect.
 */
@Composable
fun MovementTrail(
    positions: List<TokenPosition>,
    cellSize: Dp,
    color: PlayerColor
) {
    val tokenColor = when (color) {
        PlayerColor.RED -> LudoRed
        PlayerColor.GREEN -> LudoGreen
        PlayerColor.YELLOW -> LudoYellow
        PlayerColor.BLUE -> LudoBlue
    }
    
    positions.forEachIndexed { index, position ->
        val alpha = (positions.size - index).toFloat() / positions.size * 0.5f
        val size = cellSize * 0.3f * (positions.size - index).toFloat() / positions.size
        
        Box(
            modifier = Modifier
                .offset(
                    x = cellSize * position.col + (cellSize - size) / 2,
                    y = cellSize * position.row + (cellSize - size) / 2
                )
                .size(size)
                .background(tokenColor.copy(alpha = alpha), CircleShape)
        )
    }
}
