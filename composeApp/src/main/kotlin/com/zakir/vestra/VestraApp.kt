package com.zakir.vestra

import android.app.Application
import com.russhwolf.settings.SharedPreferencesSettings
import com.zakir.vestra.data.StudioModelRepository
import com.zakir.vestra.shared.cloud.AndroidCloudIo
import com.zakir.vestra.shared.cloud.CloudEngine
import com.zakir.vestra.shared.engine.EngineRouter
import com.zakir.vestra.shared.engine.lite.HumanParsing
import com.zakir.vestra.shared.engine.lite.LiteEngine
import com.zakir.vestra.shared.engine.lite.LiteEngineIo
import com.zakir.vestra.shared.engine.pro.DiffusionEngine
import com.zakir.vestra.shared.domain.effectiveCategory
import com.zakir.vestra.shared.packs.AndroidDeviceProbe
import com.zakir.vestra.shared.packs.AndroidPackFileSystem
import com.zakir.vestra.shared.packs.ModelPackManager
import com.zakir.vestra.shared.packs.PackDownloadWorker
import com.zakir.vestra.shared.platformHttpClient
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.wardrobe.AndroidTextFileStore
import com.zakir.vestra.shared.wardrobe.WardrobeRepository

class VestraApp : Application() {

    lateinit var appSettings: AppSettings
        private set

    lateinit var engineRouter: EngineRouter
        private set

    lateinit var wardrobe: WardrobeRepository
        private set

    lateinit var packManager: ModelPackManager
        private set

    lateinit var studioModels: StudioModelRepository
        private set

    override fun onCreate() {
        super.onCreate()
        appSettings = AppSettings(
            SharedPreferencesSettings(getSharedPreferences("vestra_settings", MODE_PRIVATE)),
        )
        wardrobe = WardrobeRepository(AndroidTextFileStore(filesDir))

        val http = platformHttpClient()
        packManager = ModelPackManager(
            fs = AndroidPackFileSystem(this),
            device = AndroidDeviceProbe(this),
            http = http,
            manifestUrl = PACKS_MANIFEST_URL,
        )
        PackDownloadWorker.dependencies = { packManager }
        DebugPackBootstrap.seedLitePack(this)
        studioModels = StudioModelRepository(this, packManager)

        val liteIo = LiteEngineIo(this) { modelId -> studioModels.resolveBitmap(modelId) }
        val parsing = HumanParsing(packManager)
        val cloudIo = AndroidCloudIo(this, liteIo, http)

        engineRouter = EngineRouter(
            listOf(
                LiteEngine(packManager, liteIo, parsing),
                DiffusionEngine(
                    packs = packManager,
                    device = AndroidDeviceProbe(this),
                    io = liteIo,
                    masker = { person, category -> parsing.analyze(person, category.effectiveCategory())?.mask },
                    applyWatermark = BuildConfig.APPLY_WATERMARK,
                ),
                CloudEngine(http, cloudIo, appSettings),
            ),
        )
    }

    companion object {
        const val PACKS_MANIFEST_URL =
            "https://huggingface.co/datasets/Iamzakirzr/vestra-packs/resolve/main/manifest.json"
    }
}
