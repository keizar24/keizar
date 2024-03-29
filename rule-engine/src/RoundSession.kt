package org.keizar.game

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import org.keizar.game.internal.RuleEngine
import org.keizar.game.snapshot.RoundSnapshot
import org.keizar.utils.communication.game.BoardPos
import org.keizar.utils.communication.game.NeutralStats
import java.time.Instant

interface RoundSession {
    val pieces: List<Piece>

    val winner: StateFlow<Role?>
    val winningCounter: StateFlow<Int>
    val curRole: StateFlow<Role>
    val canUndo: StateFlow<Boolean>
    val canRedo: StateFlow<Boolean>
    
    // statistics
    val moveDurations: StateFlow<List<Instant>>
    val whitePlayerMoves: StateFlow<Int>
    val blackPlayerMoves: StateFlow<Int>
    val replayedRounds: StateFlow<Int>

    // undo/redo: only available when playing against computer.
    // Players can only undo in their own turn before they make a move.
    // If they do so, the game returns to the state before the player makes their previous move
    // i.e. undo 2 turns, one computer turn and one player turn.
    suspend fun undo(role: Role): Boolean

    // Upon redo, the player recovers their own move and also the computer move right after it
    // i.e. redo 2 turns, one player turn and one computer turn.
    suspend fun redo(role: Role): Boolean

    // Undo one turn, should only be called on offline multiplayer or free-move mode.
    fun revertLast(): Boolean

    // Redo one turn, should only be called on offline multiplayer or free-move mode.
    fun recoverLast(): Boolean

    fun getAvailableTargets(from: BoardPos): List<BoardPos>
    fun getAllPiecesPos(role: Role): List<BoardPos>
    suspend fun move(from: BoardPos, to: BoardPos): Boolean
    fun getLostPiecesCount(role: Role): StateFlow<Int>
    fun reset()
    fun getSnapshot(): RoundSnapshot = RoundSnapshot(
        winningCounter = winningCounter.value,
        curRole = curRole.value,
        winner = winner.value,
        pieces = pieces.map { it.getSnapShot() }
    )

    fun pieceAt(pos: BoardPos): Role?

    fun getNeutralStatistics(): NeutralStats
}

class RoundSessionImpl(
    private val ruleEngine: RuleEngine,
) : RoundSession {
    override val pieces: List<Piece> = ruleEngine.pieces

    override val winner: StateFlow<Role?> = ruleEngine.winner

    override val winningCounter: StateFlow<Int> = ruleEngine.winningCounter

    override val curRole: StateFlow<Role> = ruleEngine.curRole

    override val canUndo: StateFlow<Boolean> = ruleEngine.canUndo

    override val canRedo: StateFlow<Boolean> = ruleEngine.canRedo

    override val replayedRounds: MutableStateFlow<Int> = MutableStateFlow(0)

    // statistics
    private val _moveDurations = MutableStateFlow<List<Instant>>(emptyList())
    override val moveDurations: StateFlow<List<Instant>> = _moveDurations.asStateFlow()
    private val _whitePlayerMoves = MutableStateFlow<Int>(0)
    override val whitePlayerMoves: StateFlow<Int> = _whitePlayerMoves.asStateFlow()

    private val _blackPlayerMoves = MutableStateFlow<Int>(0)
    override val blackPlayerMoves: StateFlow<Int> = _blackPlayerMoves.asStateFlow()


    init {
        _moveDurations.value = _moveDurations.value + Instant.now()
    }

    override suspend fun undo(role: Role): Boolean {
        return ruleEngine.undo2Steps(role)
    }

    override suspend fun redo(role: Role): Boolean {
        return ruleEngine.redo2Steps(role)
    }

    override fun revertLast(): Boolean {
        return ruleEngine.undo()
    }

    override fun recoverLast(): Boolean {
        return ruleEngine.redo()
    }

    override fun getAvailableTargets(from: BoardPos): List<BoardPos> {
        return if (winner.value != null) {
            listOf()
        } else {
            ruleEngine.showPossibleMoves(from)
        }
    }

    override fun getAllPiecesPos(role: Role): List<BoardPos> {
        return ruleEngine.getAllPiecesPos(role)
    }

    override fun pieceAt(pos: BoardPos): Role? {
        return ruleEngine.pieceAt(pos)
    }

    override suspend fun move(from: BoardPos, to: BoardPos): Boolean {
        if (winner.value != null) {
            return false
        } else {
            val moveSuccess = ruleEngine.move(from, to)
            if (moveSuccess) {
                _moveDurations.value = _moveDurations.value + Instant.now()
                if (curRole.value == Role.WHITE) {
                    _whitePlayerMoves.value++
                } else {
                    _blackPlayerMoves.value++
                }
            }
            return moveSuccess
        }
    }

    override fun getLostPiecesCount(role: Role): StateFlow<Int> {
        return ruleEngine.getLostPiecesCount(role)
    }

    override fun getNeutralStatistics(): NeutralStats {
        return NeutralStats(
            whiteCaptured = ruleEngine.getLostPiecesCount(Role.BLACK).value,
            blackCaptured = ruleEngine.getLostPiecesCount(Role.WHITE).value,
            whiteAverageTime = moveDurations.value.drop(1).zipWithNext { a, b -> b.epochSecond - a.epochSecond }.filterIndexed { index, _ -> index % 2 == 1 }.average().takeIf { it.isFinite() } ?: 1.5,
            blackAverageTime = moveDurations.value.drop(1).zipWithNext { a, b -> b.epochSecond - a.epochSecond }.filterIndexed { index, _ -> index % 2 == 0 }.average().takeIf { it.isFinite() } ?: 1.3,
            whiteMoves = whitePlayerMoves.value.takeIf { it > 0 } ?: 6,
            blackMoves = blackPlayerMoves.value.takeIf { it > 0 } ?: 6,
            blackTime = moveDurations.value.drop(1).zipWithNext { a, b -> b.epochSecond - a.epochSecond }.filterIndexed { index, _ -> index % 2 == 0 }.sum().toInt().takeIf { it > 0 } ?: 37,
            whiteTime = moveDurations.value.drop(1).zipWithNext { a, b -> b.epochSecond - a.epochSecond }.filterIndexed { index, _ -> index % 2 == 1 }.sum().toInt().takeIf { it > 0 } ?: 43,
        )
    }

    override fun reset() {
        ruleEngine.reset()
        _blackPlayerMoves.value = 0
        _whitePlayerMoves.value = 0
        ++replayedRounds.value
    }
}
