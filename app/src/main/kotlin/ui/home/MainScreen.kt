package org.keizar.android.ui.home

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.protobuf.ProtoBuf
import org.keizar.android.R
import org.keizar.android.ui.game.GameScene
import org.keizar.android.ui.game.configuration.GameConfigurationScene
import org.keizar.android.ui.game.configuration.GameStartConfiguration
import org.keizar.android.ui.game.mp.MatchPage

@Composable
@Preview(showBackground = true)
fun MainScreen() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "home") {
        composable("home") { HomePage(navController) }
        composable(
            "single player game",
        ) {
            GameConfigurationScene(navController)
        }
        composable(
            "game",
            listOf(navArgument("configuration") {
                nullable = false
                type = NavType.StringType
            })
        ) { entry ->
            entry.arguments?.getString("configuration")?.let {
                val configuration = ProtoBuf.decodeFromHexString(GameStartConfiguration.serializer(), it)
                GameScene(
                    startConfiguration = configuration,
                    navController = navController,
                )
            }
        }
        composable("multiplayer game") {
            MatchPage({ navController.navigate("home") }, Modifier.fillMaxSize())
        }
        composable("saved games") { /* TODO: saved games page*/ }
        composable("tutorial") { /* TODO: tutorial page*/ }
    }
}

@Composable
fun HomePage(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        // Title or logo can be added here
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = R.drawable.keizar_icon),
                contentDescription = "Keizar Icon",
                tint = Color.Unspecified
            )
        }

        Column(
            Modifier
                .padding(bottom = 80.dp)
                .weight(2f),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            // Single Player Button
            Button(onClick = { navController.navigate("single player game") }, modifier = Modifier.width(170.dp)) {
                Text("Play vs Computer")
            }

            // Multiplayer Button
            Button(onClick = { navController.navigate("multiplayer game") }, modifier = Modifier.width(170.dp)) {
                Text("Multiplayer")
            }

            // Saved game Button
            Button(onClick = { /* TODO: Handle saved game click */ }, modifier = Modifier.width(170.dp)) {
                Text("Saved games")
            }

            // Tutorial Button
            Button(onClick = { /* TODO: Handle tutorial click */ }, modifier = Modifier.width(170.dp)) {
                Text("Tutorial")
            }

            val context = LocalContext.current
            Button(onClick = { if (context is Activity) context.finish() }, modifier = Modifier.width(170.dp)) {
                Text("Exit")
            }
        }
    }
}