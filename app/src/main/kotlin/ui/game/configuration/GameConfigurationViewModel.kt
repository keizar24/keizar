package org.keizar.android.ui.game.configuration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import org.keizar.android.GameStartConfigurationEncoder
import org.keizar.android.ui.foundation.AbstractViewModel
import org.keizar.android.ui.foundation.Disposable
import org.keizar.game.BoardProperties
import org.keizar.game.Difficulty
import org.keizar.game.Role
import java.util.Timer
import java.util.TimerTask

interface GameConfigurationViewModel : Disposable {
    val configuration: StateFlow<GameStartConfiguration>
    val configurationSeed: Flow<String>
    val isSingle: Boolean
    fun updateRandomSeed()

    @Stable
    val boardProperties: SharedFlow<BoardProperties>

    @Stable
    val configurationSeedText: StateFlow<String>
    fun setConfigurationSeedText(value: String)

    @Stable
    val isConfigurationSeedTextError: Flow<Boolean>

    @Stable
    val playAs: Flow<Role>
    fun setPlayAs(role: Role)

    @Stable
    val difficulty: Flow<Difficulty>
    fun setDifficulty(difficulty: Difficulty)

    @Stable
    val freshButtonEnable: Flow<Boolean>

    fun setFreshButtonEnable(value: Boolean)
}

@Composable
fun rememberGameConfigurationViewModel(): GameConfigurationViewModel = remember {
    GameConfigurationViewModelImpl(isSingle = true)
}

fun GameConfigurationViewModel(isSingle: Boolean): GameConfigurationViewModel = GameConfigurationViewModelImpl(isSingle = isSingle)

@Serializable
data class GameStartConfiguration(
    @ProtoNumber(1) val layoutSeed: Int,
    @ProtoNumber(2) val playAs: Role,
    @ProtoNumber(3) val difficulty: Difficulty,
) {
    companion object {
        fun random(): GameStartConfiguration {
            return GameStartConfiguration(
                layoutSeed = BoardProperties.generateRandomSeed(),
                playAs = Role.entries.random(),
                difficulty = Difficulty.MEDIUM,
            )
        }
    }
}

fun GameStartConfiguration.createBoard(): BoardProperties =
    BoardProperties.getStandardProperties(layoutSeed)

private class GameConfigurationViewModelImpl(
    initialConfiguration: GameStartConfiguration = GameStartConfiguration.random(),
    override val isSingle: Boolean
) : GameConfigurationViewModel, AbstractViewModel() {
    override val configuration: MutableStateFlow<GameStartConfiguration> =
        MutableStateFlow(initialConfiguration)
    override val configurationSeed: Flow<String> = configuration.map { GameStartConfigurationEncoder.encode(it) }

    private val _configurationSeedText = MutableStateFlow(GameStartConfigurationEncoder.encode(initialConfiguration))
    override val configurationSeedText = merge(_configurationSeedText, configurationSeed).stateInBackground(
        GameStartConfigurationEncoder.encode(initialConfiguration)
    )

    private val layoutSeed: Flow<Int> = configuration.map { it.layoutSeed }
    override val playAs: Flow<Role> = configuration.map { it.playAs }
    override fun setPlayAs(role: Role) {
        updateConfiguration {
            copy(playAs = role)
        }
    }

    override val boardProperties: SharedFlow<BoardProperties> =
        layoutSeed.filterNotNull().map { layoutSeed ->
            BoardProperties.getStandardProperties(layoutSeed)
        }.shareInBackground()


    override val difficulty: Flow<Difficulty> = configuration.map { it.difficulty }

    override fun setDifficulty(difficulty: Difficulty) {
        updateConfiguration {
            copy(difficulty = difficulty)
        }
    }

    override val freshButtonEnable: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override fun setFreshButtonEnable(value: Boolean) {
        freshButtonEnable.value = value
    }

    override fun setConfigurationSeedText(value: String) {
        _configurationSeedText.value = value
        GameStartConfigurationEncoder.decode(value)?.let {
            updateConfiguration { it }
        }
    }

    override val isConfigurationSeedTextError: Flow<Boolean> = configurationSeedText.map {
        GameStartConfigurationEncoder.decode(it) == null
    }

    override fun updateRandomSeed() {
        if (!isSingle) {
            setFreshButtonEnable(false)
            updateConfiguration {
                GameStartConfiguration.random()
            }
            val timer = Timer()
            timer.schedule(object : TimerTask() {
                override fun run() {
                    setFreshButtonEnable(true)
                }
            }, 3000)
        }
        else {
            updateConfiguration {
                GameStartConfiguration.random()
            }
        }
    }

    private inline fun updateConfiguration(block: GameStartConfiguration.() -> GameStartConfiguration) {
        this.configuration.value = this.configuration.value.block()
    }

}
