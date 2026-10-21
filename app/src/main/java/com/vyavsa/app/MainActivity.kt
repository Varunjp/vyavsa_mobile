package com.vyavsa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.vyavsa.app.core.designsystem.VyavsaTheme
import com.vyavsa.app.core.security.TokenManager
import com.vyavsa.app.navigation.VyavsaNavGraph
import androidx.compose.foundation.isSystemInDarkTheme
import com.vyavsa.app.core.settings.SettingsPreferences
import com.vyavsa.app.core.settings.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var settingsPreferences: SettingsPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val sessionState by tokenManager.sessionState.collectAsState()
            val themeMode by settingsPreferences.themeMode.collectAsState()
            val fontSize by settingsPreferences.fontSize.collectAsState()
            val navController = rememberNavController()

            val isDark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            VyavsaTheme(
                darkTheme = isDark,
                fontSizeScale = fontSize.scaleFactor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VyavsaNavGraph(
                        navController = navController,
                        sessionState = sessionState,
                        tokenManager = tokenManager,
                        settingsPreferences = settingsPreferences
                    )
                }
            }
        }
    }
}
