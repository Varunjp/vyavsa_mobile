package com.vyavsa.app.core.common

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import com.vyavsa.app.core.settings.AppLanguage
import java.util.Locale

/**
 * A ContextWrapper that preserves the Activity base chain for Hilt and Compose while providing
 * localized Resources and Assets according to the user's selected configuration.
 */
class LocalizedActivityContext(
    base: Context,
    private val config: Configuration
) : ContextWrapper(base) {
    private val baseCtx: Context = base

    override fun getBaseContext(): Context = baseCtx

    private val localizedContext: Context by lazy {
        runCatching {
            baseCtx.createConfigurationContext(config)
        }.getOrDefault(baseCtx)
    }

    override fun getResources(): Resources = localizedContext.resources
    override fun getAssets(): AssetManager = localizedContext.assets
}

/**
 * Traverses the ContextWrapper chain to locate the hosting Activity, ensuring compatibility
 * when Context is wrapped by LocalizedActivityContext or other ContextWrappers.
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext?.findActivity()
    else -> null
}

object LocaleHelper {

    fun getLocale(languageCode: String): Locale {
        return when (languageCode.lowercase()) {
            "hi" -> Locale("hi", "IN")
            "ml" -> Locale("ml", "IN")
            "ta" -> Locale("ta", "IN")
            "kn" -> Locale("kn", "IN")
            "te" -> Locale("te", "IN")
            "bn" -> Locale("bn", "IN")
            "mr" -> Locale("mr", "IN")
            "gu" -> Locale("gu", "IN")
            "pa" -> Locale("pa", "IN")
            "or" -> Locale("or", "IN")
            "as" -> Locale("as", "IN")
            else -> Locale("en", "US")
        }
    }

    fun applyLanguage(context: Context, language: AppLanguage) {
        val locale = getLocale(language.code)
        Locale.setDefault(locale)

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                localeManager?.applicationLocales = LocaleList(locale)
            } else {
                val config = Configuration(context.resources.configuration)
                config.setLocale(locale)
                config.setLayoutDirection(locale)
                @Suppress("DEPRECATION")
                context.resources.updateConfiguration(config, context.resources.displayMetrics)
            }
        }
    }

    fun wrapContext(context: Context, languageCode: String): Context {
        val locale = getLocale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return LocalizedActivityContext(context, config)
    }
}

