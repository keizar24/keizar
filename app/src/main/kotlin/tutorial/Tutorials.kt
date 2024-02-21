@file:Suppress("MemberVisibilityCanBePrivate")

package org.keizar.android.tutorial

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.dp
import org.keizar.android.ui.rules.RuleReferencesPage
import org.keizar.game.Role
import org.keizar.game.TileType
import kotlin.time.Duration.Companion.seconds

private object PawnColors {
    val Pawn = Color.Unspecified
    val Rook = Color(255, 96, 0)
    val Knight = Color(0, 168, 0)
    val Bishop = Color(61, 54, 255)
    val Queen = Color(255, 0, 0)
    val King = Color(255, 255, 0)
    val Keizar = Color(179, 0, 255)
}

object Tutorials {
    val Refresher1 = buildTutorial("fresher-1") {
        board {
            tiles {
                change("f3", TileType.ROOK)
                change("a3", TileType.KNIGHT)
                change("b5", TileType.BISHOP)
                change("e8", TileType.QUEEN)
                change("e6", TileType.KING)
            }
            round {
                allowFreeMove()
                disableWinner()
                curRole { Role.BLACK }
                resetPieces {
                    white("f3")
                    black("g7")
                }
            }
        }

        playerStartAsBlack()

        steps {
            step("start")

            step("move black") {
                tooltip { Text("Pawn") }
                showPossibleMovesThenMove { "g7" to "g5" }
                delay(1.seconds)
                showPossibleMovesThenMove { "g5" to "g4" }
                delay(1.seconds)
                removeTooltip()
            }

            step("capture white") {
                tooltip { Text("Pawn", color = PawnColors.Pawn) }
                showPossibleMovesThenMove { "g4" to "f3" }
                tooltip(3.seconds) { Text("Captured") }
            }

            step("move as rook") {
                tooltip { Text("Rook", color = PawnColors.Rook) }
                showPossibleMovesThenMove { "f3" to "a3" }
                removeTooltip()
            }

            step("move as knight") {
                tooltip { Text("Knight", color = PawnColors.Knight) }
                showPossibleMovesThenMove { "a3" to "b5" }
                removeTooltip()
            }

            step("move as bishop") {
                tooltip { Text("Bishop", color = PawnColors.Bishop) }
                showPossibleMovesThenMove { "b5" to "e8" }
                removeTooltip()
            }

            step("move as queen") {
                tooltip { Text("Queen", color = PawnColors.Queen) }
                showPossibleMovesThenMove { "e8" to "e6" }
                removeTooltip()
            }

            step("move as king to keizar") {
                tooltip {
                    Text(
                        "King", color = PawnColors.King,
                        style = LocalTextStyle.current
                            .copy(
                                shadow = Shadow(
                                    color = Color.Black,
                                    blurRadius = 1.dp.value,
                                ),
                            )
                    )
                }
                showPossibleMovesThenMove { "e6" to "d5" }
                tooltip { Text("KEIZÁR", color = PawnColors.Keizar) }
                removeTooltip()
            }

            step("show rules") {
                showBottomSheet {
                    RuleReferencesPage(
                        Modifier
                            .padding(all = 16.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
            }.then(awaitNext = false) {
                awaitNext("Finish")
            }
        }
    }

    /**
     * Builds a move using the [moveBuilder], show the possible moves for the `from` position, then move the piece.
     *
     * Suspends until the piece has been moved.
     *
     * See [buildMove] for how to describe the move.
     */
    private suspend inline fun StepActionContext.showPossibleMovesThenMove(moveBuilder: MoveBuilder.() -> Unit) {
        val builder = MoveBuilder().apply(moveBuilder)
        val (from, to) = builder.build()
        showPossibleMoves(from)
        movePlayer(from, to)
    }

    private val list = buildList {
        add(Refresher1)
    }

    fun getById(id: String): Tutorial {
        return list.first { it.id == id }
    }
}

//@Composable
//@Preview(showBackground = true)
//private fun PreviewKingText() {
//    Text(
//        "King", color = PawnColors.King,
//        style = LocalTextStyle.current
//            .copy(
////                                drawStyle = Stroke(width = 1f),
//                shadow = Shadow(
//                    color = Color.Black,
//                    blurRadius = 1.dp.value,
////                                    offset = Offset(2f, 2f)
//                ),
//            )
//    )
//}