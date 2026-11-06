package com.ludo.game.ui.components.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ludo.game.data.model.PlayerColor
import com.ludo.game.ui.theme.*

/**
 * Main Ludo board composable.
 * The board is a 15x15 grid with 4 home bases in corners and paths connecting them.
 */
@Composable
fun LudoBoard(
    tokens: Map<PlayerColor, List<TokenPosition>>,
    currentPlayer: PlayerColor?,
    selectableTokens: List<TokenPosition>,
    onTokenClick: (PlayerColor, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .padding(8.dp)
    ) {
        val boardSize = minOf(maxWidth, maxHeight)
        val cellSize = boardSize / 15
        
        Canvas(
            modifier = Modifier.size(boardSize)
        ) {
            val cellSizePx = size.width / 15
            
            // Draw the board background
            drawRect(
                color = BoardBackground,
                size = size
            )
            
            // Draw home bases
            drawHomeBase(0f, 0f, cellSizePx * 6, LudoRed, LudoRedLight)
            drawHomeBase(cellSizePx * 9, 0f, cellSizePx * 6, LudoGreen, LudoGreenLight)
            drawHomeBase(0f, cellSizePx * 9, cellSizePx * 6, LudoBlue, LudoBlueLight)
            drawHomeBase(cellSizePx * 9, cellSizePx * 9, cellSizePx * 6, LudoYellow, LudoYellowLight)
            
            // Draw center winning area
            drawCenterTriangles(cellSizePx)
            
            // Draw paths
            drawPaths(cellSizePx)
            
            // Draw safe spots (stars)
            drawSafeSpots(cellSizePx)
        }
        
        // Draw tokens on top of the board
        tokens.forEach { (color, positions) ->
            positions.forEachIndexed { tokenId, position ->
                val isSelectable = selectableTokens.any { 
                    it.color == color && it.tokenId == tokenId 
                }
                
                TokenPiece(
                    color = color,
                    position = position,
                    cellSize = cellSize,
                    isSelectable = isSelectable,
                    isCurrentPlayer = color == currentPlayer,
                    onClick = { onTokenClick(color, tokenId) }
                )
            }
        }
    }
}

/**
 * Represents a token's position on the board.
 */
data class TokenPosition(
    val color: PlayerColor,
    val tokenId: Int,
    val boardPosition: Int,  // -1 = home, 0-56 = on track, 57 = finished
    val row: Int,
    val col: Int
)

/**
 * Draw a home base (6x6 corner area).
 */
private fun DrawScope.drawHomeBase(
    x: Float,
    y: Float,
    size: Float,
    primaryColor: Color,
    lightColor: Color
) {
    // Outer colored area
    drawRect(
        color = primaryColor,
        topLeft = Offset(x, y),
        size = Size(size, size)
    )
    
    // Inner white area with token starting positions
    val innerPadding = size * 0.15f
    val innerSize = size - (innerPadding * 2)
    
    drawRect(
        color = Color.White,
        topLeft = Offset(x + innerPadding, y + innerPadding),
        size = Size(innerSize, innerSize)
    )
    
    // Draw 4 token circles in home
    val circleRadius = size * 0.12f
    val spacing = innerSize / 3
    
    for (row in 0..1) {
        for (col in 0..1) {
            val cx = x + innerPadding + spacing * (col + 0.5f) + (spacing * col * 0.5f)
            val cy = y + innerPadding + spacing * (row + 0.5f) + (spacing * row * 0.5f)
            
            drawCircle(
                color = lightColor,
                radius = circleRadius,
                center = Offset(
                    x + innerPadding + innerSize * 0.25f + (col * innerSize * 0.5f),
                    y + innerPadding + innerSize * 0.25f + (row * innerSize * 0.5f)
                )
            )
            drawCircle(
                color = primaryColor,
                radius = circleRadius,
                center = Offset(
                    x + innerPadding + innerSize * 0.25f + (col * innerSize * 0.5f),
                    y + innerPadding + innerSize * 0.25f + (row * innerSize * 0.5f)
                ),
                style = Stroke(width = 3f)
            )
        }
    }
}

/**
 * Draw the center triangular winning area.
 */
private fun DrawScope.drawCenterTriangles(cellSize: Float) {
    val centerX = size.width / 2
    val centerY = size.height / 2
    val triangleSize = cellSize * 3
    
    // Red triangle (top)
    drawPath(
        path = Path().apply {
            moveTo(centerX, centerY)
            lineTo(centerX - triangleSize / 2, centerY - triangleSize / 2)
            lineTo(centerX + triangleSize / 2, centerY - triangleSize / 2)
            close()
        },
        color = LudoRed
    )
    
    // Green triangle (right)
    drawPath(
        path = Path().apply {
            moveTo(centerX, centerY)
            lineTo(centerX + triangleSize / 2, centerY - triangleSize / 2)
            lineTo(centerX + triangleSize / 2, centerY + triangleSize / 2)
            close()
        },
        color = LudoGreen
    )
    
    // Yellow triangle (bottom)
    drawPath(
        path = Path().apply {
            moveTo(centerX, centerY)
            lineTo(centerX + triangleSize / 2, centerY + triangleSize / 2)
            lineTo(centerX - triangleSize / 2, centerY + triangleSize / 2)
            close()
        },
        color = LudoYellow
    )
    
    // Blue triangle (left)
    drawPath(
        path = Path().apply {
            moveTo(centerX, centerY)
            lineTo(centerX - triangleSize / 2, centerY + triangleSize / 2)
            lineTo(centerX - triangleSize / 2, centerY - triangleSize / 2)
            close()
        },
        color = LudoBlue
    )
}

/**
 * Draw the path cells around the board.
 */
private fun DrawScope.drawPaths(cellSize: Float) {
    // Vertical paths (left side - columns 6,7,8)
    for (row in 0 until 6) {
        // Left column (6) - entry for Blue
        drawPathCell(6, row, cellSize, Color.White)
        // Middle column (7) - Blue's finish lane
        drawPathCell(7, row, cellSize, if (row < 5) LudoBlueLight else Color.White)
        // Right column (8) - exit path
        drawPathCell(8, row, cellSize, Color.White)
    }
    
    // Vertical paths (right side - columns 6,7,8)
    for (row in 9 until 15) {
        // Left column (6)
        drawPathCell(6, row, cellSize, Color.White)
        // Middle column (7) - Green's finish lane
        drawPathCell(7, row, cellSize, if (row > 9) LudoGreenLight else Color.White)
        // Right column (8)
        drawPathCell(8, row, cellSize, Color.White)
    }
    
    // Horizontal paths (top side - rows 6,7,8)
    for (col in 0 until 6) {
        // Top row (6)
        drawPathCell(col, 6, cellSize, Color.White)
        // Middle row (7) - Red's finish lane
        drawPathCell(col, 7, cellSize, if (col < 5) LudoRedLight else Color.White)
        // Bottom row (8)
        drawPathCell(col, 8, cellSize, Color.White)
    }
    
    // Horizontal paths (bottom side - rows 6,7,8)
    for (col in 9 until 15) {
        // Top row (6)
        drawPathCell(col, 6, cellSize, Color.White)
        // Middle row (7) - Yellow's finish lane
        drawPathCell(col, 7, cellSize, if (col > 9) LudoYellowLight else Color.White)
        // Bottom row (8)
        drawPathCell(col, 8, cellSize, Color.White)
    }
}

/**
 * Draw a single path cell.
 */
private fun DrawScope.drawPathCell(col: Int, row: Int, cellSize: Float, color: Color) {
    val x = col * cellSize
    val y = row * cellSize
    
    drawRect(
        color = color,
        topLeft = Offset(x, y),
        size = Size(cellSize, cellSize)
    )
    drawRect(
        color = Color.Gray,
        topLeft = Offset(x, y),
        size = Size(cellSize, cellSize),
        style = Stroke(width = 1f)
    )
}

/**
 * Draw safe spots (stars) on the board.
 */
private fun DrawScope.drawSafeSpots(cellSize: Float) {
    // Safe spot positions (col, row)
    val safeSpots = listOf(
        Pair(1, 6),   // Red start
        Pair(6, 2),   // Red path
        Pair(8, 1),   // Green start
        Pair(12, 6),  // Green path
        Pair(13, 8),  // Yellow start
        Pair(8, 12),  // Yellow path
        Pair(6, 13),  // Blue start
        Pair(2, 8)    // Blue path
    )
    
    safeSpots.forEach { (col, row) ->
        drawStar(
            centerX = col * cellSize + cellSize / 2,
            centerY = row * cellSize + cellSize / 2,
            radius = cellSize * 0.35f,
            color = SafeSpotStar
        )
    }
}

/**
 * Draw a star shape.
 */
private fun DrawScope.drawStar(
    centerX: Float,
    centerY: Float,
    radius: Float,
    color: Color
) {
    val path = Path()
    val points = 5
    val innerRadius = radius * 0.5f
    
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) radius else innerRadius
        val angle = Math.PI / 2 + i * Math.PI / points
        val x = centerX + (r * kotlin.math.cos(angle)).toFloat()
        val y = centerY - (r * kotlin.math.sin(angle)).toFloat()
        
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()
    
    drawPath(path = path, color = color)
}

/**
 * Individual token piece composable.
 */
@Composable
private fun BoxWithConstraintsScope.TokenPiece(
    color: PlayerColor,
    position: TokenPosition,
    cellSize: androidx.compose.ui.unit.Dp,
    isSelectable: Boolean,
    isCurrentPlayer: Boolean,
    onClick: () -> Unit
) {
    if (position.boardPosition == 57) return // Token finished, don't draw
    
    val tokenColor = when (color) {
        PlayerColor.RED -> LudoRed
        PlayerColor.GREEN -> LudoGreen
        PlayerColor.YELLOW -> LudoYellow
        PlayerColor.BLUE -> LudoBlue
    }
    
    val tokenSize = cellSize * 0.7f
    val offsetX = cellSize * position.col + (cellSize - tokenSize) / 2
    val offsetY = cellSize * position.row + (cellSize - tokenSize) / 2
    
    Box(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .size(tokenSize)
            .clip(CircleShape)
            .background(tokenColor)
            .then(
                if (isSelectable) {
                    Modifier
                        .clickable(onClick = onClick)
                        .background(tokenColor.copy(alpha = 0.8f))
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Inner circle for 3D effect
        Box(
            modifier = Modifier
                .size(tokenSize * 0.6f)
                .clip(CircleShape)
                .background(tokenColor.copy(alpha = 0.7f))
        )
        
        // Highlight ring for selectable tokens
        if (isSelectable) {
            Box(
                modifier = Modifier
                    .size(tokenSize)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
    }
}
