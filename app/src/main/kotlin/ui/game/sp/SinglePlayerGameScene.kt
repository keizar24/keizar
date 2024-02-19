package org.keizar.android.ui.game.sp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import org.keizar.android.ui.game.BaseGamePage
import org.keizar.android.ui.game.UndoButton
import org.keizar.android.ui.game.configuration.GameStartConfiguration
import org.keizar.android.ui.game.configuration.createBoard
import org.keizar.android.ui.game.rememberSinglePlayerGameBoardViewModel
import org.keizar.game.Difficulty
import org.keizar.game.GameSession
import org.keizar.game.Role
import org.keizar.game.snapshot.buildGameSession
import org.keizar.utils.communication.game.Player


@Composable
fun SinglePlayerGameScene(
    startConfiguration: GameStartConfiguration,
    navController: NavController,
) {
    val vm = rememberSinglePlayerGameBoardViewModel(
        session = remember(startConfiguration) {
            GameSession.create(startConfiguration.createBoard())
        },
        selfPlayer = if (startConfiguration.playAs == Role.WHITE) {
            Player.FirstWhitePlayer
        } else {
            Player.FirstBlackPlayer
        },
        difficulty = startConfiguration.difficulty,
    )
    BaseGamePage(
        vm,
        onClickHome = { navController.popBackStack("home", false) },
        onClickGameConfig = { navController.popBackStack("game/configuration", false) },
        actions = {
            UndoButton(vm = vm)
        }
    )
}


@Preview
@Composable
private fun TestEndGameWhenNoPiecesCanMove() {
    BaseGamePage(
        rememberSinglePlayerGameBoardViewModel(
            session = remember {
                buildGameSession {
                    round {
                        resetPieces {
                            val c = 'a'
                            black("${c}8")
                            black("${c}7")
                            white("${c}6")
                            white("${c}4")
                        }
                    }
                    round { }
                }
            },
            selfPlayer = Player.FirstWhitePlayer,
            difficulty = Difficulty.EASY,
        ),
        onClickHome = { },
        onClickGameConfig = { }
    )
}
