package org.keizar.client

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import org.keizar.client.modules.GameSessionModule
import org.keizar.game.Role
import org.keizar.game.RoundSession
import org.keizar.utils.communication.game.BoardPos

interface RemoteRoundSession: RoundSession

class RemoteRoundSessionImpl internal constructor(
    private val round: RoundSession,
    private val gameSessionModule: GameSessionModule,
): RoundSession by round, RemoteRoundSession {
    init {
        gameSessionModule.bind(this, round)
    }

    private val selfRole: StateFlow<Role> = gameSessionModule.getCurrentSelfRole()

    override fun getAvailableTargets(from: BoardPos): Flow<List<BoardPos>> {
        if (round.pieceAt(from) != selfRole.value) return flowOf(listOf())
        return round.getAvailableTargets(from)
    }

    override suspend fun move(from: BoardPos, to: BoardPos): Boolean {
        if (round.pieceAt(from) != selfRole.value) return false
        return round.move(from, to).also {
            if (it) gameSessionModule.sendMove(from, to)
        }
    }
}

