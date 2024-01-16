package org.keizar.game

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.keizar.game.internal.RuleEngineCoreImpl
import org.keizar.game.local.RuleEngine
import org.keizar.game.local.RuleEngineImpl
import kotlin.random.Random

interface GameSession {
    val properties: BoardProperties

    fun pieceAt(pos: BoardPos): Flow<Player?>
    val winner: Flow<Player?>
    val winningCounter: Flow<Int>
    val curPlayer: Flow<Player>

    suspend fun undo(): Boolean
    suspend fun redo(): Boolean

    fun getAvailableTargets(from: BoardPos): Flow<List<BoardPos>>
    suspend fun move(from: BoardPos, to: BoardPos): Boolean

    companion object {
        fun create(random: Random): GameSession {
            val properties = BoardProperties.getStandardProperties(random)
            val ruleEngine = RuleEngineImpl(
                boardProperties = properties,
                ruleEngineCore = RuleEngineCoreImpl(),
            )
            return GameSessionImpl(properties, ruleEngine)
        }
    }
}

class GameSessionImpl(
    override val properties: BoardProperties,
    private val ruleEngine: RuleEngine,
) : GameSession {
    override fun pieceAt(pos: BoardPos): Flow<Player?> {
        return flowOf(ruleEngine.pieceAt(pos))
    }

    override val winner: Flow<Player?>
        get() = flowOf(ruleEngine.winner)

    override val winningCounter: Flow<Int>
        get() = flowOf(ruleEngine.winningCounter)

    override val curPlayer: Flow<Player>
        get() = flowOf(ruleEngine.curPlayer)

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

    override suspend fun move(from: BoardPos, to: BoardPos): Boolean {
        return ruleEngine.move(from, to)
    }
}