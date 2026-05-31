package io.github.mobdev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.mobdev.ui.ChatsScreen
import io.github.mobdev.ui.ImageScreen
import io.github.mobdev.ui.LoginScreen
import io.github.mobdev.ui.MessagesScreen
import io.github.mobdev.ui.LandscapeScreen
import io.github.mobdev.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ChatApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun ChatApp(viewModel: ChatViewModel) {
    val navController = rememberNavController()
    val token by viewModel.token.collectAsState()
    val error by viewModel.error.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(error) {
        if (error == "401") {
            navController.navigate("login") {
                popUpTo(0)
            }
            viewModel.clearError()
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (token != null) "chats" else "login"
    ) {
        composable("login") {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate("chats") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("chats") {
            if (isLandscape) {
                LandscapeScreen(
                    viewModel = viewModel,
                    onImageClick = { link ->
                        val encoded = java.net.URLEncoder.encode(link, "UTF-8")
                        navController.navigate("image/$encoded")
                    },
                    onLogout = {
                        viewModel.logout {
                            navController.navigate("login") {
                                popUpTo(0)
                            }
                        }
                    }
                )
            } else {
                ChatsScreen(
                    viewModel = viewModel,
                    onChatClick = { channel ->
                        viewModel.selectChannel(channel)
                        navController.navigate("messages/$channel")
                    },
                    onLogout = {
                        viewModel.logout {
                            navController.navigate("login") {
                                popUpTo(0)
                            }
                        }
                    }
                )
            }
        }
        composable("messages/{channel}") { backStackEntry ->
            val channel = backStackEntry.arguments?.getString("channel") ?: ""
            if (isLandscape) {
                LandscapeScreen(
                    viewModel = viewModel,
                    onImageClick = { link ->
                        val encoded = java.net.URLEncoder.encode(link, "UTF-8")
                        navController.navigate("image/$encoded")
                    },
                    onLogout = {
                        viewModel.logout {
                            navController.navigate("login") {
                                popUpTo(0)
                            }
                        }
                    }
                )
            } else {
                MessagesScreen(
                    viewModel = viewModel,
                    channelName = channel,
                    onBack = { navController.popBackStack() },
                    onImageClick = { link ->
                        val encoded = java.net.URLEncoder.encode(link, "UTF-8")
                        navController.navigate("image/$encoded")
                    }
                )
            }
        }
        composable("image/{link}") { backStackEntry ->
            val link = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("link") ?: "", "UTF-8"
            )
            ImageScreen(
                imageLink = link,
                onBack = { navController.popBackStack() }
            )
        }
    }
}