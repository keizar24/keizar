package org.keizar.game

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.keizar.game.internal.RuleEngine
import org.keizar.game.internal.RuleEngineCoreImpl
import org.keizar.game.internal.RuleEngineImpl
import org.keizar.game.snapshot.GameSnapshot
import org.keizar.utils.communication.game.GameResult
import org.keizar.utils.communication.game.Player
import org.keizar.utils.communication.game.RoundStats
import kotlin.time.Duration.Companion.seconds

/***
 * API for the backend. Representation of a complete game that may contain multiple rounds
 * on the same random-generated board.
 */
interface GameSession {
    // Game rules configuration
    val properties: BoardProperties

    // Sessions for different rounds
    val rounds: List<RoundSession>

    // Current round in progress.
    // Emits a new round after both player call confirmNextRound()
    val currentRound: Flow<RoundSession>

    // Round number of current round. starting from 0.
    // The currentRoundNo starts from 0 and will never exceed properties.rounds.
    // At the last round, after both player calls confirmNextRound() and finalIWinner is
    // updated, the value remains to be properties.rounds - 1.
    val currentRoundNo: StateFlow<Int>

    // The final winner. Emits a non-null value when the whole game ends.
    // Values could be GameResult.Winner(Player1/Player2) or GameResult.Draw.
    val finalWinner: Flow<GameResult?>

    // The players who have confirmed the next round.
    // This flow will only emit non-empty list when the game is in the state of
    // finishing one round and waiting for both players to confirm the next round.
    val playersConfirmedNextRound: StateFlow<Set<Player>>

    // Returns the role (black/white) of the specified player in current round of game.
    fun currentRole(player: Player): StateFlow<Role>


    // Accumulated number of rounds this player has won.
    fun wonRounds(player: Player): Flow<Int>

    // Accumulated number of pieces this player has lost (been captured by the opponent).
    fun lostPieces(player: Player): Flow<Int>

    // The game will proceed to the next round only after both players call confirmNextRound().
    suspend fun confirmNextRound(player: Player): Boolean

    // Return a serializable snapshot of the GameSession that can be restored to a GameSession
    // by GameSession.restore().
    fun getSnapshot(): GameSnapshot = GameSnapshot(
        properties = properties,
        rounds = rounds.map { it.getSnapshot() },
        currentRoundNo = currentRoundNo.value,
        playersConfirmedNextRound = playersConfirmedNextRound.value,
    )

    // Replay current round of the game. Reset the round state.
    // Should only be called in single player mode.
    fun replayCurrentRound(): Boolean

    // Replay the whole game. Change the currentRoundNo to 0, and reset the game state.
    // Should only be called in single player mode.
    fun replayGame(): Boolean

    // Calculate the corresponding player of the role in the specified round.
    fun getPlayer(role: Role, roundNo: Int): Player

    // Calculate the role of the player in the specified round.
    fun getRole(player: Player, roundNo: Int): Role

    // Get the winner of the specified round.
    fun getRoundWinner(roundNo: Int): Flow<Player?>

    // Get the statistics of the specified round.
    fun getRoundStats(roundNo: Int, selfPlayer: Player): Flow<RoundStats>

    companion object {
        // Create a standard GameSession using the seed provided.
        fun create(seed: Int? = null): GameSession {
            val properties = BoardProperties.getStandardProperties(seed)
            return create(properties)
        }

        // Create a standard GameSession using the BoardProperties provided.
        fun create(properties: BoardProperties): GameSession {
            return GameSessionImpl(properties) {
                val ruleEngine = RuleEngineImpl(
                    boardProperties = properties,
                    ruleEngineCore = RuleEngineCoreImpl(properties),
                )
                RoundSessionImpl(ruleEngine)
            }
        }

        // Restore a GameSession by a snapshot of the game.
        fun restore(snapshot: GameSnapshot): GameSession {
            return GameSessionImpl(
                properties = snapshot.properties,
                startFromRoundNo = snapshot.currentRoundNo,
                initialPlayersConfirmedNextRound = snapshot.playersConfirmedNextRound,
            ) { index ->
                val ruleEngine = RuleEngineImpl.restore(
                    properties = snapshot.properties,
                    roundSnapshot = snapshot.rounds[index],
                    ruleEngineCore = RuleEngineCoreImpl(snapshot.properties),
                )
                RoundSessionImpl(ruleEngine)
            }
        }

        // Restore a GameSession by a snapshot of the game and a RoundSessionConstructor provided.
        fun restore(
            snapshot: GameSnapshot,
            roundSessionConstructor: (ruleEngine: RuleEngine) -> RoundSession,
        ): GameSession {
            return GameSessionImpl(
                properties = snapshot.properties,
                startFromRoundNo = snapshot.currentRoundNo,
                initialPlayersConfirmedNextRound = snapshot.playersConfirmedNextRound,
            ) { index ->
                val ruleEngine = RuleEngineImpl.restore(
                    properties = snapshot.properties,
                    roundSnapshot = snapshot.rounds[index],
                    ruleEngineCore = RuleEngineCoreImpl(snapshot.properties),
                )
                roundSessionConstructor(ruleEngine)
            }
        }
    }
}

class GameSessionImpl(
    override val properties: BoardProperties,
    startFromRoundNo: Int = 0,
    initialPlayersConfirmedNextRound: Set<Player> = emptySet(),
    roundSessionConstructor: (index: Int) -> RoundSession,
) : GameSession {
    override val rounds: List<RoundSession>

    override val currentRound: Flow<RoundSession>
    private val _currentRoundNo: MutableStateFlow<Int> = MutableStateFlow(startFromRoundNo)
    override val currentRoundNo: StateFlow<Int> = _currentRoundNo.asStateFlow()
    override val finalWinner: Flow<GameResult?>

    private val curRoles: List<MutableStateFlow<Role>>
    private val wonRounds: List<Flow<List<Int>>>

    override val playersConfirmedNextRound: MutableStateFlow<Set<Player>>
    private val confirmNextRoundLock: Mutex = Mutex()

    private var round1Stats: Flow<RoundStats>? = null
    private var round2Stats: Flow<RoundStats>? = null

    init {
        rounds = (0..<properties.rounds).map {
            roundSessionConstructor(it)
        }
        currentRound = currentRoundNo.map { rounds[it] }

        curRoles = listOf(
            MutableStateFlow(getRole(Player.FirstWhitePlayer, currentRoundNo.value)),
            MutableStateFlow(getRole(Player.FirstBlackPlayer, currentRoundNo.value)),
        )

        wonRounds = Player.entries.map { player ->
            combine(rounds.map { it.winner }) { winners ->
                winners.mapIndexed { roundNo, role -> Pair(roundNo, role) }
                    .filter { (roundNo, role) -> role == getRole(player, roundNo) }
                    .map { (roundNo, _) -> roundNo }
            }
        }

        playersConfirmedNextRound = MutableStateFlow(initialPlayersConfirmedNextRound)

        finalWinner = combine(
            rounds[properties.rounds - 1].winner,
            wonRounds(Player.FirstWhitePlayer),
            wonRounds(Player.FirstBlackPlayer),
            lostPieces(Player.FirstWhitePlayer),
            lostPieces(Player.FirstBlackPlayer),
        ) { finalRoundWinner, player1Wins, player2Wins, player1LostPieces, player2LostPieces ->
            if (finalRoundWinner == null) {
                null
            } else if (player1Wins > player2Wins) {
                GameResult.Winner(Player.FirstWhitePlayer)
            } else if (player1Wins < player2Wins) {
                GameResult.Winner(Player.FirstBlackPlayer)
            } else if (player1LostPieces < player2LostPieces) {
                GameResult.Winner(Player.FirstWhitePlayer)
            } else if (player1LostPieces > player2LostPieces) {
                GameResult.Winner(Player.FirstBlackPlayer)
            } else {
                GameResult.Draw
            }
        }.debounce(1.seconds)
    }

    override fun currentRole(player: Player): StateFlow<Role> {
        return curRoles[player.ordinal]
    }

    override fun wonRounds(player: Player): Flow<Int> {
        return wonRounds[player.ordinal].map { it.count() }
    }

    override fun lostPieces(player: Player): Flow<Int> {
        return combine(rounds.mapIndexed { index, round ->
            round.getLostPiecesCount(getRole(player, index))
        }) {
            it.sum()
        }
    }

    override suspend fun confirmNextRound(player: Player): Boolean {
        confirmNextRoundLock.withLock {
            if (currentRoundNo.value >= properties.rounds) return false
            val value = playersConfirmedNextRound.value
            if (value.contains(player)) return false
            playersConfirmedNextRound.value = value.plus(player)
            if (playersConfirmedNextRound.value.size == Player.entries.size) {
                playersConfirmedNextRound.value = emptySet()
                proceedToNextRound()
            }
            return true
        }
    }

    private fun proceedToNextRound() {
        if (currentRoundNo.value != properties.rounds - 1) {
            ++_currentRoundNo.value
            curRoles.forEach { role -> role.value = role.value.other() }
        }
    }

    override fun replayCurrentRound(): Boolean {
        rounds[_currentRoundNo.value].reset()
        playersConfirmedNextRound.value = emptySet()
        return true
    }

    override fun replayGame(): Boolean {
        rounds.forEach { it.reset() }
        playersConfirmedNextRound.value = emptySet()
        resetGameStatus()
        return true
    }

    private fun resetGameStatus() {
        curRoles[0].value = Role.WHITE
        curRoles[1].value = Role.BLACK
        _currentRoundNo.value = 0
    }

    override fun getRoundWinner(roundNo: Int): Flow<Player?> {
        return rounds[roundNo].winner.map { it?.let { role -> getPlayer(role, roundNo) } }
    }

    override fun getRoundStats(roundNo: Int, selfPlayer: Player): Flow<RoundStats> {

        val stats = getRoundWinner(roundNo).map { winner ->
            RoundStats(
                neutralStats = rounds[roundNo].getNeutralStatistics(),
                player = selfPlayer,
                winner = winner,
            )
        }
        if (roundNo == 1) {
            round1Stats = stats
        } else {
            round2Stats = stats
        }

        return stats
    }

    override fun getRole(player: Player, roundNo: Int): Role {
        return if ((player.ordinal + roundNo) % 2 == 0) Role.WHITE else Role.BLACK
    }

    override fun getPlayer(role: Role, roundNo: Int): Player {
        return Player.fromOrdinal((role.ordinal + roundNo) % 2)
    }
}
