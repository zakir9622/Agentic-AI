package com.zakir.vestra

import android.app.Application
import com.russhwolf.settings.SharedPreferencesSettings
import com.zakir.vestra.data.MockCompositeRenderer
import com.zakir.vestra.shared.domain.EngineTier
import com.zakir.vestra.shared.engine.EngineRouter
import com.zakir.vestra.shared.engine.MockTryOnEngine
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.wardrobe.AndroidTextFileStore
import com.zakir.vestra.shared.wardrobe.WardrobeRepository

/**
 * Manual composition root — the dependency graph is small enough that a DI
 * framework would be pure overhead. Real engines replace the mocks in M3/M4/M5.
 */
class VestraApp : Application() {

    lateinit var appSettings: AppSettings
        private set

    lateinit var engineRouter: EngineRouter
        private set

    lateinit var wardrobe: WardrobeRepository
        private set

    override fun onCreate() {
        super.onCreate()
        appSettings = AppSettings(
            SharedPreferencesSettings(getSharedPreferences("vestra_settings", MODE_PRIVATE)),
        )
        wardrobe = WardrobeRepository(AndroidTextFileStore(filesDir))

        val mockRenderer = MockCompositeRenderer(this)
        engineRouter = EngineRouter(
            listOf(
                MockTryOnEngine(EngineTier.LITE, producePlaceholder = mockRenderer::render),
                MockTryOnEngine(EngineTier.PRO, producePlaceholder = mockRenderer::render),
                MockTryOnEngine(EngineTier.CLOUD, producePlaceholder = mockRenderer::render),
            ),
        )
    }
}
