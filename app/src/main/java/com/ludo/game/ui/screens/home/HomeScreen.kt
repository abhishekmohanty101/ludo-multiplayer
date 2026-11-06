package com.ludo.game.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Home screen with options to create or join a game.
 */
@Composable
fun HomeScreen(
    onCreateGame: (String) -> Unit,
    onJoinGame: (String) -> Unit,
    onOpenHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var playerName by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo/Title
        Text(
            text = "🎲",
            fontSize = 80.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "LUDO",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Multiplayer",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Create Game Button
        Button(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Create Game",
                fontSize = 18.sp
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Join Game Button
        OutlinedButton(
            onClick = { showJoinDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Join Game",
                fontSize = 18.sp
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // History Button
        TextButton(
            onClick = onOpenHistory,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "📊 Game History & Stats",
                fontSize = 16.sp
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "How to Play",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Create a game and share the code with friends\n" +
                           "• Or join using a code from your friend\n" +
                           "• 2-4 players can play together\n" +
                           "• Roll 6 to move tokens out of home",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    // Create Game Dialog
    if (showCreateDialog) {
        PlayerNameDialog(
            title = "Create Game",
            buttonText = "Create",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                showCreateDialog = false
                onCreateGame(name)
            }
        )
    }
    
    // Join Game Dialog
    if (showJoinDialog) {
        JoinGameDialog(
            onDismiss = { showJoinDialog = false },
            onConfirm = { name ->
                showJoinDialog = false
                onJoinGame(name)
            }
        )
    }
}

/**
 * Dialog to enter player name.
 */
@Composable
fun PlayerNameDialog(
    title: String,
    buttonText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Your Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text(buttonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog to join a game with name and room code.
 */
@Composable
fun JoinGameDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var roomCode by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join Game") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = roomCode,
                    onValueChange = { roomCode = it.uppercase().take(6) },
                    label = { Text("Room Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm("$name|$roomCode") }, // Pass both values
                enabled = name.isNotBlank() && roomCode.length == 6
            ) {
                Text("Join")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
