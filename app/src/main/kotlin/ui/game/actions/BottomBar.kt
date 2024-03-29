package org.keizar.android.ui.game.actions

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.keizar.android.ui.foundation.ProvideCompositionalLocalsForPreview
import org.keizar.android.ui.foundation.isSystemInLandscape
import org.keizar.android.ui.foundation.launchInBackground
import org.keizar.android.ui.game.GameBoard
import org.keizar.android.ui.game.GameBoardScaffold
import org.keizar.android.ui.game.GameBoardViewModel
import org.keizar.android.ui.game.MultiplayerGameBoardViewModel
import org.keizar.android.ui.game.PlayableGameBoardViewModel
import org.keizar.android.ui.game.rememberSinglePlayerGameBoardForPreview

@Composable
fun RoundOneBottomBar(
    vm: GameBoardViewModel,
    onClickHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top,
    ) {
        ActionButton(
            onClick = onClickHome,
            icon = { Icon(Icons.Default.Home, null) },
            text = { Text("Home", fontSize = 10.sp) })

        if (vm.singlePlayerMode) {
            ActionButton(
                onClick = { vm.replayCurrentRound() },
                icon = { Icon(Icons.Default.Replay, null) },
                text = { Text(text = "Replay", fontSize = 10.sp) })
        }

        var showDialog by remember { mutableStateOf(false) }
        val showResults = remember { mutableStateOf(true) }
        ActionButton(
            onClick = {
                showDialog = true
                showResults.value = true
            },
            icon = { Icon(Icons.Default.Assessment, null) },
            text = { Text(text = "Results", fontSize = 10.sp) })

        if (showDialog) {
            WinningRoundOneDialog(winner = vm.winner.collectAsState().value, vm, showResults)
        }

        val context = LocalContext.current
        ActionButton(
            onClick = { if (context is Activity) context.finish() },
            icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null) },
            text = { Text("Exit App", fontSize = 10.sp) })

        if (vm is MultiplayerGameBoardViewModel) {
            val isWaitingOpponentNext by vm.isWaitingOpponentNext.collectAsStateWithLifecycle()
            if (isWaitingOpponentNext) {
                ActionButton(
                    onClick = {
                        Toast.makeText(
                            context,
                            "Waiting for opponent to start next round",
                            Toast.LENGTH_LONG,
                        ).show()
                    },
                    icon = { Icon(Icons.Default.SkipNext, null) },
                    text = {
                        Text(
                            "Waiting...", fontSize = 10.sp,
                            softWrap = false,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    Modifier.width(IntrinsicSize.Max)
                )
            } else {
                ActionButton(
                    onClick = {
                        vm.startNextRound(vm.selfPlayer)
                    },
                    icon = { Icon(Icons.Default.SkipNext, null) },
                    text = { Text("Next Round", fontSize = 10.sp, softWrap = false) },
                    Modifier.then(if (isSystemInLandscape()) Modifier.widthIn(min = 120.dp) else Modifier)
                )
            }
        } else {
            ActionButton(
                onClick = {
                    vm.startNextRound(vm.selfPlayer)
                },
                icon = { Icon(Icons.Default.SkipNext, null) },
                text = { Text("Next Round", fontSize = 10.sp, softWrap = false) },
                Modifier.then(if (isSystemInLandscape()) Modifier.widthIn(min = 120.dp) else Modifier)
            )
        }
    }

}

@Composable
fun RoundTwoBottomBar(
    vm: GameBoardViewModel,
    onClickHome: () -> Unit,
    onClickNewGame: () -> Unit,
    onClickLogin: () -> Unit,
    modifier: Modifier = Modifier,
    hideReplay: Boolean = false,
) {
    Column {
        Row(
            modifier
                .padding(horizontal = 16.dp)
                .width(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            ActionButton(
                onClick = onClickHome,
                icon = { Icon(Icons.Default.Home, null) },
                text = { Text("Home", fontSize = 10.sp) })

            if (vm.singlePlayerMode && !hideReplay) {
                ActionButton(
                    onClick = {
                        vm.replayCurrentRound()
                        vm.setGameOverReadyToBeAnnouncement(false)
                    },

                    icon = { Icon(Icons.Default.Replay, null) },
                    text = { Text(text = "Replay Round", fontSize = 10.sp) })


                ActionButton(
                    onClick = {
                        vm.replayGame()
                        vm.setGameOverReadyToBeAnnouncement(false)
                    },

                    icon = { Icon(Icons.Default.Replay10, null) },
                    text = { Text(text = "Replay Game", fontSize = 10.sp) })
            }

            val context = LocalContext.current
            if (vm.currentGameDataId.collectAsState().value != "") {
                ActionButton(
                    onClick = {
                        vm.launchInBackground {
                            val isLoggedIn = vm.sessionManager.isLoggedIn.first()
                            if (isLoggedIn) {
                                if (vm is PlayableGameBoardViewModel) {
                                    try {
                                        vm.userSave()
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Game saved", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                "Game save failed, please check your network connection",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        throw e
                                    }
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    onClickLogin()
                                }
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.Save, null) },
                    text = { Text(text = "Save Game", fontSize = 10.sp, softWrap = false) },
                    modifier = Modifier.width(IntrinsicSize.Max)
                )
            }
            ActionButton(
                onClick = { vm.setShowGameOverResults(true) },
                icon = { Icon(Icons.Default.Assessment, null) },
                text = {
                    Text(
                        text = "Results",
                        fontSize = 10.sp
                    )
                })
        }

        Row(
            modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Top
        ) {
            val context = LocalContext.current
            TextButton(
                onClick = { if (context is Activity) context.finish() },
            ) {
                Text(
                    text = "Exit App", textAlign = TextAlign.Center, softWrap = false, maxLines = 1,
                    modifier = Modifier.width(IntrinsicSize.Max)
                )
            }

            Button(
                onClick = { onClickNewGame() },
            ) {
                Text(
                    text = "New Game",
                    textAlign = TextAlign.Center,
                    softWrap = false,
                    maxLines = 1,
                    modifier = Modifier.width(IntrinsicSize.Max)
                )
            }

        }
    }
}

/**
 * A button in the bottom bar
 */
@Composable
private fun ActionButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
//    backgroundColor: Color = MaterialTheme.colorScheme.background,
    isLoading: Boolean = false
) {
    Column(
        modifier
            .clip(RoundedCornerShape(8.dp))
//            .background(backgroundColor)
            .clickable(
                remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = false),
                onClick = onClick
            )
            .height(64.dp)
            .padding(horizontal = 4.dp)
            .padding(all = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        icon()
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProvideTextStyle(MaterialTheme.typography.labelMedium) {
                text()
            }

            AnimatedVisibility(isLoading) {
                Box(
                    Modifier
                        .padding(start = 8.dp)
                        .height(12.dp), contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Preview(showBackground = true, device = Devices.TABLET)
@Preview(showBackground = true, fontScale = 2f)
@Composable
private fun PreviewRoundOneBottomBar() {
    RoundOneBottomBar(
        rememberSinglePlayerGameBoardForPreview(),
        onClickHome = { /* Navigate to home page*/ }
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, device = Devices.TABLET)
@Preview(showBackground = true, fontScale = 2f)
@Composable
private fun PreviewRoundTwoBottomBar() {
    RoundTwoBottomBar(
        rememberSinglePlayerGameBoardForPreview(),
        onClickHome = { /* Navigate to home page*/ },
        onClickNewGame = { /* Navigate to game configuration page*/ },
        onClickLogin = { /* Navigate to login page*/ }
    )
}


/**
 * Container for [RoundOneBottomBar] and [RoundTwoBottomBar] in landscape mode
 */
@Composable
fun LandscapeBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier) {
        Box(
            Modifier
                .matchParentSize()
                .blur(10.dp)
        )
        val shape = MaterialTheme.shapes.medium
        Row(
            Modifier
                .shadow(1.dp, shape)
                .clip(shape)
                .alpha(0.97f)
                .background(MaterialTheme.colorScheme.surface)
                .width(IntrinsicSize.Min)
        ) {
            content()
        }
    }
}

@Preview(showBackground = true, device = Devices.TABLET)
@Composable
private fun PreviewBottomBar1() = ProvideCompositionalLocalsForPreview {
    val vm = rememberSinglePlayerGameBoardForPreview()
    GameBoardScaffold(
        vm,
        board = {
            GameBoard(vm, Modifier.size(400.dp))
        },
        bottomBar = {
            RoundOneBottomBar(vm = vm, onClickHome = {})
        }
    )
}

@Preview(showBackground = true, device = Devices.TABLET)
@Composable
private fun PreviewBottomBarSinglePlayerSaved() = ProvideCompositionalLocalsForPreview {
    val vm = rememberSinglePlayerGameBoardForPreview()
    GameBoardScaffold(
        vm,
        board = {
            GameBoard(vm, Modifier.size(400.dp))
        },
        bottomBar = {
            RoundTwoBottomBar(vm = vm, onClickHome = {}, onClickNewGame = {}, onClickLogin = {})
        }
    )
}


@Preview(showBackground = true, device = Devices.TABLET)
@Composable
private fun PreviewBottomBarMultiPlayer() = ProvideCompositionalLocalsForPreview {
    val vm = rememberSinglePlayerGameBoardForPreview()
    GameBoardScaffold(
        vm,
        board = {
            GameBoard(vm, Modifier.size(400.dp))
        },
        bottomBar = {
            RoundTwoBottomBar(
                vm = vm, onClickHome = {}, onClickNewGame = {}, onClickLogin = {},
                hideReplay = true,
                modifier = Modifier.widthIn(max = 400.dp),
            )
        },
    )
}
