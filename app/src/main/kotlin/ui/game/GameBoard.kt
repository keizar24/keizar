package org.keizar.android.ui.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.min
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.keizar.game.BoardProperties
import org.keizar.game.Player
import kotlin.random.Random


@Composable
fun GameBoard(
    properties: BoardProperties,
    modifier: Modifier = Modifier,
) {
    val vm = rememberGameBoardViewModel(boardProperties = properties)
    Column(modifier = Modifier) {
        WinningCounter(vm)

        CapturedPieces(vm, Player.BLACK)

        Box(modifier = modifier) {
            BoardBackground(properties, vm)
            BoardPieces(vm)
            PossibleMovesOverlay(vm)
        }
        CapturedPieces(vm, Player.WHITE)
    }
}

@Composable
fun CapturedPieces(vm: GameBoardViewModel, player: Player) {
    val capturedPieces by if (player == Player.WHITE) {
        vm.whiteCapturedPieces.collectAsState()
    } else {
        vm.blackCapturedPieces.collectAsState()
    }
    if (player == Player.WHITE) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "White Captured Pieces:")
            for (i in 0 until capturedPieces) {
                PlayerIcon(color = Color.White,
                    modifier = Modifier.padding(start = 8.dp).align(Alignment.CenterVertically))
            }
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Black Captured Pieces:")
            for (i in 0 until capturedPieces) {
                PlayerIcon(color = Color.Black,
                           modifier = Modifier.padding(start = 8.dp).align(Alignment.CenterVertically))
            }
        }
    }

}

@Composable
fun WinningCounter(vm: GameBoardViewModel) {
    val winningCounter by vm.winningCounter.collectAsState()
    Text(text = "Winning Keizar Counter: $winningCounter")

}


@Preview(showBackground = true)
@Composable
private fun PreviewGameBoard() {
    BoxWithConstraints {
        GameBoard(
            remember {
                BoardProperties.getStandardProperties(Random(0))
            },
            Modifier.size(min(maxWidth, maxHeight))
        )
    }
}