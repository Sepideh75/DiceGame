package com.example.dice2026
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dice2026.ui.theme.Dice2026Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
const val WINNING_SCORE = 30
const val TILE_COUNT = 12
val NAMES = listOf("Player 1", "Player 2")
val COLORS = listOf(Color(0xFF1565C0), Color(0xFF8660CD))
val DICE = listOf(R.drawable.dice1, R.drawable.dice2, R.drawable.dice3, R.drawable.dice4, R.drawable.dice5, R.drawable.dice6)
val TILE_CELLS = listOf(
    0 to 0, 0 to 1, 0 to 2, 0 to 3, 1 to 3, 2 to 3, 3 to 3, 3 to 2, 3 to 1, 3 to 0, 2 to 0, 1 to 0)
fun generateBoard() =
    (List(9) { (1..10).random() } + List(3) { (-10..-1).random() }).shuffled()
fun rollDie() = (0..5).random()
fun List<Int>.update(i: Int, change: (Int) -> Int) =
    toMutableList().also { it[i] = change(it[i]) }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { Dice2026Theme { DiceGameScreen() } }
    }
}

@Composable
fun DiceGameScreen() {
    var board by rememberSaveable { mutableStateOf(generateBoard()) }
    var scores by rememberSaveable { mutableStateOf(listOf(0, 0)) }
    var positions by rememberSaveable { mutableStateOf(listOf(-1, -1)) }
    var wins by rememberSaveable { mutableStateOf(listOf(0, 0)) }
    var current by rememberSaveable { mutableIntStateOf(0) }
    var winner by rememberSaveable { mutableIntStateOf(-1) }
    var dieIndex by rememberSaveable { mutableIntStateOf(5) }
    var rolling by remember { mutableStateOf(false) }
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun newGame() {
        board = generateBoard()
        scores = listOf(0, 0)
        positions = listOf(-1, -1)
        current = 0
        winner = -1
        dieIndex = 5
    }

    fun roll() {
        if (rolling || winner != -1) return
        rolling = true
        val p = current
        scope.launch {
            val spin = launch { rotation.animateTo(720f, tween(600)) }
            repeat(8) { dieIndex = rollDie(); delay(70) }
            spin.join()
            rotation.snapTo(0f)

            dieIndex = rollDie()
            repeat(dieIndex + 1) {
                positions = positions.update(p) { (it + 1) % TILE_COUNT }
                delay(250)
            }

            scores = scores.update(p) { it + board[positions[p]] }
            if (scores[p] >= WINNING_SCORE) {
                winner = p
                wins = wins.update(p) { it + 1 }
            } else {
                current = 1 - p
            }
            rolling = false
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surface)
                    )
                )
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Dice Board: Race to $WINNING_SCORE",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                "Games won  •  P1: ${wins[0]}   P2: ${wins[1]}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (p in 0..1) {
                    PlayerCard(
                        player = p,
                        score = scores[p],
                        position = positions[p],
                        isActive = current == p && winner == -1,
                        isWinner = winner == p,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            GameBoard(board, positions, dieIndex, rotation.value)

            Button(
                onClick = { roll() },
                enabled = !rolling && winner == -1,
                colors = ButtonDefaults.buttonColors(containerColor = COLORS[current]),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text("${NAMES[current]}: Roll", fontSize = 18.sp) }

            TextButton(onClick = { if (!rolling) newGame() }) { Text("New Game (new board)") }
        }
    }

    if (winner != -1) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("🏆 ${NAMES[winner]} Wins!") },
            text = { Text("Reached ${scores[winner]} points.") },
            confirmButton = { Button(onClick = { newGame() }) { Text("Play Again") } }
        )
    }
}

@Composable
fun GameBoard(board: List<Int>, positions: List<Int>, dieIndex: Int, rotation: Float) {
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1f)) {
        val cell = maxWidth / 4
        TILE_CELLS.forEachIndexed { i, (row, col) ->
            Tile(
                index = i,
                value = board[i],
                players = (0..1).filter { positions[it] == i },
                modifier = Modifier.offset(cell * col, cell * row).size(cell).padding(3.dp)
            )
        }
        Dice(dieIndex, rotation, Modifier.offset(cell, cell).size(cell * 2).padding(12.dp))
    }
}

@Composable
fun Tile(index: Int, value: Int, players: List<Int>, modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (value > 0) Color(0xFF2E7D32) else Color(0xFFC62828))
    ) {
        Text(
            if (index == 0) "START" else "${index + 1}",
            modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            if (value > 0) "+$value" else "$value",
            modifier = Modifier.align(Alignment.Center),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) { players.forEach { Token(it) } }
    }
}

@Composable
fun Token(player: Int) {
    Box(
        Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(COLORS[player])
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("${player + 1}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
@Composable
fun PlayerCard(
    player: Int,
    score: Int,
    position: Int,
    isActive: Boolean,
    isWinner: Boolean,
    modifier: Modifier
) {
    val highlight = isActive || isWinner
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        border = if (highlight) BorderStroke(3.dp, COLORS[player]) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (highlight) 6.dp else 1.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Token(player)
                Text(NAMES[player], fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Text(
                when { isWinner -> "WINNER"; isActive -> "Your turn"; else -> "Waiting" },
                style = MaterialTheme.typography.labelMedium,
                color = COLORS[player]
            )
            Text("$score", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
            LinearProgressIndicator(
                progress = { (score / WINNING_SCORE.toFloat()).coerceIn(0f, 1f) },
                color = COLORS[player],
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
            Text(
                if (position == -1) "Not on board yet" else "On tile ${position + 1}",
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
@Composable
fun Dice(dieIndex: Int, rotation: Float, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Image(
            painter = painterResource(DICE[dieIndex]),
            contentDescription = "Die showing ${dieIndex + 1}",
            modifier = Modifier.fillMaxSize().padding(16.dp).rotate(rotation)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DiceGamePreview() {
    Dice2026Theme { DiceGameScreen() }
}