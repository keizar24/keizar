package org.keizar.android.ui.tutorial

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import org.keizar.android.tutorial.Tutorial
import org.keizar.android.tutorial.Tutorials
import org.keizar.android.ui.foundation.launchInBackground
import org.keizar.android.ui.game.GameBoard
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
    TutorialPage(
        vm,
        onClickHome = { navController.popBackStack("home", false) },
        modifier
    )
}

@Composable
private fun TutorialPage(
    vm: TutorialGameBoardViewModel,
    onClickHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Game") },
                navigationIcon = {
                    IconButton(onClick = onClickHome) {
                        Icon(Icons.Rounded.Home, contentDescription = "Back")
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
                )
            }

            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .padding(vertical = 8.dp)
            ) {
                val message by vm.tutorialSession.presentation.message.collectAsState()
                message?.let { it() }

                val next by vm.tutorialSession.requests.requestingClickNext.collectAsStateWithLifecycle(null)
                next?.let { n ->
                    Button(onClick = {
                        n.respond()
                    }) {
                        Text(text = "Next")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewTutorialPage() {
    TutorialPage(
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