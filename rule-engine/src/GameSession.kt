package org.keizar.game

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import org.keizar.game.internal.RuleEngine
import org.keizar.game.internal.RuleEngineCoreImpl
import org.keizar.game.internal.RuleEngineImpl
import kotlin.random.Random

interface GameSession {
    val properties: BoardProperties

    val pieces: List<Piece>

    val winner: StateFlow<Player?>
    val winningCounter: StateFlow<Int>
    val curPlayer: StateFlow<Player>

    suspend fun undo(): Boolean
    suspend fun redo(): Boolean

    fun getAvailableTargets(from: BoardPos): Flow<List<BoardPos>>
    fun getAllPiecesPos(player: Player): Flow<List<BoardPos>>
    suspend fun move(from: BoardPos, to: BoardPos): Boolean
    fun getLostPiecesCount(player: Player): StateFlow<Int>

    companion object {
        fun create(random: Random = Random): GameSession {
            val properties = BoardProperties.getStandardProperties(random)
            return create(properties)
        }

        fun create(properties: BoardProperties): GameSession {
            val ruleEngine = RuleEngineImpl(
                boardProperties = properties,
                ruleEngineCore = RuleEngineCoreImpl(properties),
            )
            return GameSessionImpl(properties, ruleEngine)
        }
    }
}

class GameSessionImpl(
    override val properties: BoardProperties,
    private val ruleEngine: RuleEngine,
) : GameSession {
    override val pieces: List<Piece> = ruleEngine.pieces

    override val winner: StateFlow<Player?> = ruleEngine.winner

    override val winningCounter: StateFlow<Int> = ruleEngine.winningCounter

    override val curPlayer: StateFlow<Player> = ruleEngine.curPlayer

    override suspend fun undo(): Boolean {
        // TODO("Not yet implemented")
        return true
    }

    override suspend fun redo(): Boolean {
        // TODO("Not yet implemented")
        return true
    }

    override fun getAvailableTargets(from: BoardPos): Flow<List<BoardPos>> {
        return flowOf(ruleEngine.showPossibleMoves(from))
    }

    override fun getAllPiecesPos(player: Player): Flow<List<BoardPos>> {
        return flowOf(ruleEngine.getAllPiecesPos(player))
    }

    override suspend fun move(from: BoardPos, to: BoardPos): Boolean {
        return ruleEngine.move(from, to)
    }

    override fun getLostPiecesCount(player: Player): StateFlow<Int> {
        return ruleEngine.getLostPiecesCount(player)
    }
}
