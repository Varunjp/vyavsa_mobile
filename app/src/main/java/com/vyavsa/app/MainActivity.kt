package com.vyavsa.app

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.vyavsa.app.core.common.LocaleHelper
import com.vyavsa.app.core.common.LocalizedActivityContext
import com.vyavsa.app.core.designsystem.VyavsaTheme
import com.vyavsa.app.core.security.TokenManager
import com.vyavsa.app.core.settings.SettingsPreferences
import com.vyavsa.app.core.settings.ThemeMode
import com.vyavsa.app.navigation.VyavsaNavGraph
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
            val currentLanguage by settingsPreferences.language.collectAsState()
            val navController = rememberNavController()

            val localizedConfig = remember(currentLanguage) {
                val locale = LocaleHelper.getLocale(currentLanguage.code)
                Configuration(resources.configuration).apply {
                    setLocale(locale)
                    setLayoutDirection(locale)
                }
            }
            val localizedActivityContext = remember(currentLanguage, localizedConfig) {
                LocalizedActivityContext(this@MainActivity, localizedConfig)
            }

            val isDark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            CompositionLocalProvider(
                LocalContext provides localizedActivityContext,
                LocalConfiguration provides localizedConfig
            ) {
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
}
