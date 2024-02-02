package org.keizar.android.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.him188.ani.utils.logging.info
import org.keizar.aiengine.RandomGameAIImpl
import org.keizar.android.ui.foundation.AbstractViewModel
import org.keizar.android.ui.foundation.HasBackgroundScope
import org.keizar.android.ui.foundation.launchInBackground
import org.keizar.android.ui.game.transition.BoardTransitionController
import org.keizar.game.BoardPos
import org.keizar.game.BoardProperties
import org.keizar.game.GameResult
import org.keizar.game.GameSession
import org.keizar.game.Piece
import org.keizar.game.Player
import org.keizar.game.Role
import org.keizar.game.RoundSession
import kotlin.time.Duration.Companion.seconds

interface GameBoardViewModel {
    @Stable
    val boardProperties: BoardProperties

    @Stable
    val pieceArranger: PieceArranger

    @Stable
    val boardTransitionController: BoardTransitionController

    @Stable
    val selfRole: StateFlow<Role>

    @Stable
    val selfPlayer: Player

    /**
     * List of the pieces on the board.
     */
    @Stable
    val pieces: StateFlow<List<UiPiece>>

    /**
     * Currently picked piece. `null` if no piece is picked.
     */
    @Stable
    val currentPick: StateFlow<Pick?>

    /**
     * Currently available **logical** positions where the picked piece can move to. `null` if no piece is picked.
     */
    @Stable
    val availablePositions: SharedFlow<List<BoardPos>?>

    /**
     * `true` if the last move is done by [onRelease].
     */
    @Stable
    val lastMoveIsDrag: MutableStateFlow<Boolean>

    @Stable
    val winningCounter: StateFlow<Int>

    @Stable
    val blackCapturedPieces: StateFlow<Int>

    @Stable
    val whiteCapturedPieces: StateFlow<Int>

    @Stable
    val winner: StateFlow<Role?>

    @Stable
    val finalWinner: StateFlow<GameResult?>

    @Stable
    val currentRound: SharedFlow<RoundSession>

    @Stable
    val currentRoundCount: StateFlow<Int>

    @Stable
    val round1Winner: StateFlow<Role?>

    @Stable
    val round2Winner: StateFlow<Role?>

    // clicking

    /**
     * Called when the player single-clicks the piece.
     */
    fun onClickPiece(piece: UiPiece)

    /**
     * Called when the player single-clicks the empty tile.
     */
    fun onClickTile(logicalPos: BoardPos)


    // dragging

    /**
     * Called when the player long-presses the piece.
     */
    fun onHold(piece: UiPiece)

    /**
     * The offset of the piece from the point where the player stared holding the piece.
     *
     * If the player is not holding any piece, the flow emits [DpOffset.Zero].
     */
    val draggingOffset: StateFlow<DpOffset>

    /**
     * Add the given [offset] to the current [draggingOffset].
     *
     * This is called when the player drags the piece.
     */
    fun addDraggingOffset(offset: DpOffset)

    /**
     * Called when the player releases the piece after long-pressing it.
     */
    fun onRelease(piece: UiPiece)

    /**
     * Called when the the first round is finished by the players to start the next one.
     */
    fun startNextRound(selfPlayer: Player)

    /**
     * Get the count of pieces of the given [role] in [roundNo] round.
     */
    fun getRoundPieceCount(roundNo: Int, role: Role): StateFlow<Int>

}

@Composable
fun rememberGameBoardViewModel(
    game: GameSession,
    selfPlayer: Player,
): GameBoardViewModel {
    return remember {
        SinglePlayerGameBoardViewModel(game, selfPlayer)
    }
}

private class SinglePlayerGameBoardViewModel(
    game: GameSession,
    selfPlayer: Player,
) : BaseGameBoardViewModel(
    game,
    selfPlayer,
) {
    private val gameAi =
        RandomGameAIImpl(game, Player.entries.first { it != selfPlayer }, backgroundScope.coroutineContext)

    init {
        launchInBackground {
            delay(5.seconds) // Wait a few seconds before computer starts as white
            gameAi.start()
        }
    }
}

@Suppress("LeakingThis")
private sealed class BaseGameBoardViewModel(
    private val game: GameSession,
    @Stable override val selfPlayer: Player,
) : AbstractViewModel(), GameBoardViewModel {
    override val boardProperties = game.properties

    @Stable
    override val selfRole: StateFlow<Role> = game.currentRole(selfPlayer)

    @Stable
    override val pieceArranger = PieceArranger(
        boardProperties = boardProperties,
        viewedAs = selfRole
    )

    @Stable
    override val boardTransitionController = BoardTransitionController(
        initialPlayAs = selfRole.value,
//        playAs,
//        playAs = selfRole,
        backgroundScope,
    )

    @Stable
    override val pieces: StateFlow<List<UiPiece>> = game.currentRound.map { it.pieces }.map { list ->
        list.map {
            UiPiece(
                enginePiece = it,
                offsetInBoard = boardTransitionController.pieceOffset(pieceArranger.offsetFor(it.pos)),
                backgroundScope,
            )
        }
    }.stateInBackground(emptyList())

    @Stable
    override val currentPick: MutableStateFlow<Pick?> = MutableStateFlow(null)

    @Stable
    private val currentRole: StateFlow<Role> = selfRole
//        game.currentRound.flatMapLatest { it.curRole }.stateInBackground(Role.WHITE)

    @Stable
    override val winningCounter: StateFlow<Int> = game.currentRound
        .flatMapLatest { it.winningCounter }
        .stateInBackground(0)

    @Stable
    override val blackCapturedPieces: StateFlow<Int> =
        game.currentRound.flatMapLatest { it.getLostPiecesCount(Role.BLACK) }.stateInBackground(0)

    @Stable
    override val whiteCapturedPieces: StateFlow<Int> =
        game.currentRound.flatMapLatest { it.getLostPiecesCount(Role.WHITE) }.stateInBackground(0)


    @Stable
    override val winner: StateFlow<Role?> = game.currentRound.flatMapLatest { it.winner }
        .stateInBackground(null)


    @Stable
    override val finalWinner: StateFlow<GameResult?> = game.finalWinner.stateInBackground(null)

    @Stable
    override val currentRound: SharedFlow<RoundSession> = game.currentRound.shareInBackground()

    @Stable
    override val currentRoundCount: StateFlow<Int> = game.currentRoundNo

    @Stable
    override val round1Winner: StateFlow<Role?> = game.rounds[0].winner

    @Stable
    override val round2Winner: StateFlow<Role?> = game.rounds[1].winner

    @Stable
    override val availablePositions: SharedFlow<List<BoardPos>?> = game.currentRound.flatMapLatest { turn ->
        currentPick.flatMapLatest { pick ->
            if (pick == null) {
                flowOf(emptyList())
            } else {
                turn.getAvailableTargets(pieceArranger.viewToLogical(pick.viewPos).first())
            }
        }.map { list ->
            list
        }
    }.shareInBackground()

    override val lastMoveIsDrag: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override fun onClickPiece(piece: UiPiece) {
        val currentPick = currentPick.value
        if (currentPick == null) {
            if (piece.role != selfRole.value) return
            launchInBackground(start = CoroutineStart.UNDISPATCHED) {
                startPick(piece)
            }
        } else {
            // Pick another piece
            completePick(isDrag = false)
            launchInBackground(start = CoroutineStart.UNDISPATCHED) {
                movePiece(currentPick.logicalPos, piece.pos.value)
            }
            return
        }
    }

    override fun onClickTile(logicalPos: BoardPos) {
        val pick = currentPick.value ?: return
        completePick(isDrag = false)
        launchInBackground(start = CoroutineStart.UNDISPATCHED) {
            movePiece(pick.logicalPos, logicalPos)
        }
    }

    override val draggingOffset = MutableStateFlow(DpOffset.Zero)

    override fun onHold(piece: UiPiece) {
        if (piece.role != currentRole.value) return
        launchInBackground(start = CoroutineStart.UNDISPATCHED) {
            startPick(piece)
        }
    }


    override fun addDraggingOffset(offset: DpOffset) {
        this.draggingOffset.value += offset
    }

    override fun onRelease(piece: UiPiece) {
        val currentPick = currentPick.value ?: return
        if (currentPick.piece != piece) return

        val dragOffset = draggingOffset.value

        piece.hide() // hide it now to avoid flickering
        launchInBackground(start = CoroutineStart.UNDISPATCHED) {
            try {
                movePiece(
                    currentPick.viewPos,
                    pieceArranger.getNearestPos(dragOffset, from = piece.pos.value).first()
                )
                completePick(isDrag = true)
            } finally {
                piece.cancelHide()
            }
        }
        return
    }


    private suspend fun startPick(piece: UiPiece) {
        this.currentPick.value = Pick(piece, pieceArranger.viewToLogical(piece.pos.value).first())
    }

    private fun completePick(isDrag: Boolean) {
        this.currentPick.value = null
        lastMoveIsDrag.value = isDrag
        draggingOffset.value = DpOffset.Zero
    }

    private suspend fun movePiece(from: BoardPos, to: BoardPos) {
        game.currentRound.first().move(from, to).also {
            logger.info { "[board] move $from to $to: $it" }
        }
    }

    override fun startNextRound(selfPlayer: Player) {
        launchInBackground(Dispatchers.Main.immediate, start = CoroutineStart.UNDISPATCHED) {
            boardTransitionController.turnBoard()
        }
        launchInBackground(Dispatchers.IO) {
            game.confirmNextRound(selfPlayer)
        }
    }

    override fun getRoundPieceCount(roundNo: Int, role: Role): StateFlow<Int> {
        return game.rounds[roundNo].getLostPiecesCount(role)
    }

}

@Immutable
class Pick(
    val piece: UiPiece,
    val viewPos: BoardPos
)

val Pick.logicalPos: BoardPos
    get() = piece.pos.value


/**
 * A wrapper of [Piece] that is aware of the UI states.
 */
@Stable
class UiPiece internal constructor(
    private val enginePiece: Piece,
    /**
     * The offset of the piece on the board, starting from the top-left corner.
     */
    val offsetInBoard: Flow<DpOffset>,
    override val backgroundScope: CoroutineScope,
) : Piece by enginePiece, HasBackgroundScope {

    private val _overrideVisible = MutableStateFlow<Boolean?>(null)
    val isVisible = combine(_overrideVisible, isCaptured) { override, isCaptured ->
        override ?: !isCaptured
    }.shareInBackground()

    fun hide() {
        _overrideVisible.value = false
    }

    fun cancelHide() {
        _overrideVisible.value = null
    }
}