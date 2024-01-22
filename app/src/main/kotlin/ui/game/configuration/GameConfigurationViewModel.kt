package org.keizar.android.ui.game.configuration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import org.keizar.android.ui.foundation.AbstractViewModel
import org.keizar.game.BoardProperties
import org.keizar.game.Difficulty
import org.keizar.game.Player

interface GameConfigurationViewModel {
    @Stable
    val boardSeedText: StateFlow<String>

    @Stable
    val boardSeed: Flow<Int?>

    @Stable
    val boardProperties: SharedFlow<BoardProperties>
    fun setBoardSeedText(value: String)
    fun updateRandomBoardSeed()


    @Stable
    val playAs: StateFlow<Player?>
    fun setPlayAs(player: Player?)

    @Stable
    val difficulty: StateFlow<Difficulty>
    fun setDifficulty(difficulty: Difficulty)


    fun createGameStartConfiguration(): GameStartConfiguration {
        return GameStartConfiguration(
            seed = boardSeedText.value.toIntOrNull() ?: BoardProperties.generateRandomSeed(),
            playAs = playAs.value,
            difficulty = difficulty.value,
        )
    }
}

@Composable
fun rememberGameConfigurationViewModel(): GameConfigurationViewModel = GameConfigurationViewModelImpl()

@Serializable
class GameStartConfiguration(
    @ProtoNumber(1) val seed: Int,
    @ProtoNumber(2) val playAs: Player?,
    @ProtoNumber(3) val difficulty: Difficulty,
)

private class GameConfigurationViewModelImpl(
    initialBoardSeed: Int = BoardProperties.generateRandomSeed(),
) : GameConfigurationViewModel, AbstractViewModel() {
    override val boardSeedText = MutableStateFlow(initialBoardSeed.toString())
    override val boardSeed: Flow<Int?> = boardSeedText.map { it.toIntOrNull() }
    override val boardProperties: SharedFlow<BoardProperties> =
        boardSeed.map { BoardProperties.getStandardProperties(it ?: 0) }
            .shareInBackground()

    override val playAs: MutableStateFlow<Player?> = MutableStateFlow(null)
    override fun setPlayAs(player: Player?) {
        playAs.value = player
    }

    override val difficulty: MutableStateFlow<Difficulty> = MutableStateFlow(Difficulty.EASY)

    override fun setDifficulty(difficulty: Difficulty) {
        this.difficulty.value = difficulty
    }

    override fun setBoardSeedText(value: String) {
        boardSeedText.value = value
    }

    override fun updateRandomBoardSeed() {
        boardSeedText.value = BoardProperties.generateRandomSeed().toString()
    }

}