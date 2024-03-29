package org.keizar.client.internal

import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.SelectClause0
import kotlinx.coroutines.selects.select
import me.him188.ani.utils.logging.info
import me.him188.ani.utils.logging.logger
import org.keizar.game.GameSession
import org.keizar.game.Role
import org.keizar.game.RoundSession
import org.keizar.utils.communication.game.BoardPos
import org.keizar.utils.communication.game.Player
import org.keizar.utils.communication.message.ConfirmNextRound
import org.keizar.utils.communication.message.Move
import org.keizar.utils.communication.message.PlayerStateChange
import org.keizar.utils.communication.message.Respond
import org.keizar.utils.communication.message.RoomStateChange
import kotlin.coroutines.CoroutineContext

internal interface GameSessionWsHandler : AutoCloseable {
    /**
     * Returns `true` if this session is still connected and running.
     * `false` indicates either [close] is called or network connection lost.
     */
    val isActive: Boolean

    /**
     * A clause completes when the websocket session is closed,
     * either by calling [close] or by network connection lost.
     */
    val onComplete: SelectClause0

    fun getCurrentSelfRole(): StateFlow<Role>
    fun getSelfPlayer(): Player
    fun bind(session: GameSession)
    fun bind(remote: RemoteRoundSession, round: RoundSession)
    suspend fun sendConfirmNextRound()
    suspend fun sendMove(from: BoardPos, to: BoardPos)
    suspend fun start()
}

/**
 * The websocket handler for an ongoing game.
 * Used by a [RemoteGameSession] to communication with server.
 * Created by a [GameRoomClient] whose state changes to [GameRoomState.PLAYING].
 *
 * Accepts two callback functions [onPlayerStateChange] and [onRoomStateChange] to handle state changes.
 */
internal class GameSessionWsHandlerImpl(
    parentCoroutineContext: CoroutineContext,
    private val session: DefaultClientWebSocketSession,
    private val selfPlayer: Player,
    private val onPlayerStateChange: suspend (respond: PlayerStateChange) -> Unit,
    private val onRoomStateChange: suspend (respond: RoomStateChange) -> Unit,
) : GameSessionWsHandler {
    private val logger = logger(GameSessionWsHandlerImpl::class)

    private val myCoroutineScope: CoroutineScope =
        CoroutineScope(parentCoroutineContext + Job(parent = parentCoroutineContext[Job]))

    /**
     * The underlying [GameSession] and [RoundSession]s of the [RemoteGameSession] bound to this handler.
     * Used by the handler to reproduce the opponent's operations (moves, confirm next rounds, etc.)
     */
    private lateinit var gameSession: GameSession
    private val underlyingRoundSessionMap: MutableMap<RoundSession, RoundSession> = mutableMapOf()
    private val currentSelfRole: MutableStateFlow<Role> = MutableStateFlow(Role.WHITE)

    private val websocketSessionHandler =
        object : AbstractWebsocketSessionHandler(
            session = session,
            parentCoroutineContext = parentCoroutineContext,
            cancelWebsocketOnExit = true
        ) {
            override suspend fun processResponse(respond: Respond) {
                println("Game websocket handler processing: $respond")
                when (respond) {
                    ConfirmNextRound -> {
                        gameSession.confirmNextRound(selfPlayer.opponent())
                    }

                    is Move -> {
                        val round = gameSession.currentRound.first()
                        getUnderlyingRound(round).move(respond.from, respond.to)
                    }

                    is PlayerStateChange -> onPlayerStateChange(respond)
                    is RoomStateChange -> onRoomStateChange(respond)
                    else -> {
                        // ignore
                    }
                }
            }
        }

    init {
        myCoroutineScope.launch {
            select { websocketSessionHandler.onComplete {} }
            logger.info { "Game websocket handler completed, GameSessionWsHandlerImpl is closing" }
            close()
        }
    }

    override val isActive: Boolean
        get() = myCoroutineScope.isActive

    override val onComplete: SelectClause0
        get() = myCoroutineScope.coroutineContext[Job]!!.onJoin

    override suspend fun start() {
        websocketSessionHandler.start()
        println("Game websocket handler started")
    }

    override fun close() {
        websocketSessionHandler.close()
        println("Game websocket handler closed")
        myCoroutineScope.cancel()
    }

    private fun startUpdateCurRole() {
        myCoroutineScope.launch {
            val player = getSelfPlayer()
            gameSession.currentRoundNo.collect { curRoundNo ->
                currentSelfRole.value = gameSession.getRole(player, curRoundNo)
            }
        }
    }

    override fun getCurrentSelfRole(): StateFlow<Role> {
        return currentSelfRole
    }

    override fun getSelfPlayer(): Player {
        return selfPlayer
    }

    override fun bind(session: GameSession) {
        gameSession = session
        startUpdateCurRole()
    }

    override fun bind(remote: RemoteRoundSession, round: RoundSession) {
        underlyingRoundSessionMap[remote] = round
    }

    private fun getUnderlyingRound(remote: RoundSession): RoundSession {
        return underlyingRoundSessionMap[remote]!!
    }

    override suspend fun sendConfirmNextRound() {
        websocketSessionHandler.sendRequest(ConfirmNextRound)
    }

    override suspend fun sendMove(from: BoardPos, to: BoardPos) {
        websocketSessionHandler.sendRequest(Move(from, to))
    }
}