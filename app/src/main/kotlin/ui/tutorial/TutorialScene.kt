package org.keizar.android.ui.tutorial

import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import org.keizar.android.tutorial.Tutorial
import org.keizar.android.tutorial.Tutorials
import org.keizar.android.tutorial.respond
import org.keizar.android.ui.foundation.launchInBackground
import org.keizar.android.ui.game.GameBoard
import org.keizar.android.ui.game.GameBoardTopBar
import kotlin.time.Duration.Companion.seconds

@Composable
fun TutorialScene(
    tutorial: Tutorial,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val vm = remember(tutorial) {
        TutorialGameBoardViewModel(tutorial).also {
            it.launchInBackground {
                delay(2.seconds)
                tutorialSession.start()
            }
        }
    }
    TutorialSelectionPage(
        vm,
        onClickHome = { navController.popBackStack("home", false) },
        modifier
    )
}

@Composable
private fun TutorialSelectionPage(
    vm: TutorialGameBoardViewModel,
    onClickHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Tutorial") },
                navigationIcon = {
                    IconButton(onClick = onClickHome) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                }
            )
        },
    ) { contentPadding ->
        Column {
            BoxWithConstraints(
                Modifier
                    .padding(contentPadding)
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                GameBoard(
                    vm,
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .size(min(maxWidth, maxHeight)),
                    onClickHome = onClickHome,
                    onClickGameConfig = {},
                    topBar = { GameBoardTopBar(vm = vm, turnStatusIndicator = null) },
                    actions = {
                        Box(
                            Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            val message by vm.tutorialSession.presentation.message.collectAsState()
                            message?.let { it() }
                        }

                        Box {
                            TextButton(
                                onClick = { vm.launchInBackground { vm.tutorialSession.back() } },
                                enabled = remember(vm) {
                                    vm.tutorialSession.currentStep.map { it.index != 0 }
                                }.collectAsStateWithLifecycle(false).value,
                            ) {
                                Text(text = "Back")
                            }
                        }

                        Box {
                            val next by remember {
                                vm.tutorialSession.requests.requestingClickNext
                            }.collectAsStateWithLifecycle(null)
                            next.let { n ->
                                Button(
                                    onClick = { n?.respond() },
                                    enabled = n != null && !n.isResponded
                                ) {
                                    val buttonName by vm.tutorialSession.presentation.buttonName.collectAsState()
                                    Text(text = buttonName)
                                }
                            }
                        }
                    },
                    boardOverlay = {
                        val tooltip by vm.tutorialSession.presentation.tooltip.collectAsState()
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = tooltip != null,
                                enter = fadeIn(),
                                exit = fadeOut(snap()),
                            ) {
                                Tooltip(
                                    Modifier
                                        .align(Alignment.TopCenter)
                                ) {
                                    ProvideTextStyle(value = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) {
                                        tooltip?.invoke(this)
                                    }
                                }
                            }
                        }
                    }
                )
            }

            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .padding(vertical = 8.dp)
            ) {
            }

            vm.tutorialSession.requests.requestingCompose.collectAsStateWithLifecycle(initialValue = null).value?.let {
                it.content(it)
            }
        }
    }
}


@Composable
private fun Tooltip(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedCard(
        modifier
            .shadow(1.dp, shape = CardDefaults.outlinedShape)
            .alpha(0.98f),
    ) {
        Row(
            modifier
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
@Preview
private fun PreviewTooltip() {
    Tooltip {
        Text(text = "Test")
    }
}

@Preview
@Composable
private fun PreviewTutorialPage() {
    TutorialSelectionPage(
        vm = remember {
            TutorialGameBoardViewModel(Tutorials.Refresher1).also {
                it.launchInBackground {
                    tutorialSession.start()
                }
            }
        },
        onClickHome = { }
    )
}