package com.vyavsa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.vyavsa.app.core.designsystem.VyavsaTheme
import com.vyavsa.app.core.security.TokenManager
import com.vyavsa.app.navigation.VyavsaNavGraph
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val sessionState by tokenManager.sessionState.collectAsState()
            val navController = rememberNavController()

            VyavsaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VyavsaNavGraph(
                        navController = navController,
                        sessionState = sessionState
                    )
                }
            }
        }
    }
}
