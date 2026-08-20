package com.zakir.vestra

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.russhwolf.settings.SharedPreferencesSettings
import com.zakir.vestra.data.StudioModelRepository
import com.zakir.vestra.shared.cloud.AndroidCloudIo
import com.zakir.vestra.shared.cloud.CloudEngine
import com.zakir.vestra.shared.cloud.GenerativeCloudService
import com.zakir.vestra.shared.domain.effectiveCategory
import com.zakir.vestra.shared.engine.EngineRouter
import com.zakir.vestra.shared.engine.lite.HumanParsing
import com.zakir.vestra.shared.engine.lite.LiteEngine
import com.zakir.vestra.shared.engine.lite.LiteEngineIo
import com.zakir.vestra.shared.engine.pro.DiffusionEngine
import com.zakir.vestra.shared.packs.AndroidDeviceProbe
import com.zakir.vestra.shared.packs.AndroidPackFileSystem
import com.zakir.vestra.shared.packs.ModelPackManager
import com.zakir.vestra.shared.packs.PackDownloadWorker
import com.zakir.vestra.shared.platformHttpClient
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.usage.UsageLedger
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

    lateinit var usageLedger: UsageLedger
        private set

    lateinit var generative: GenerativeCloudService
        private set

    override fun onCreate() {
        super.onCreate()
        val prefs = SharedPreferencesSettings(getSharedPreferences("vestra_settings", MODE_PRIVATE))
        appSettings = AppSettings(prefs)
        appSettings.networkProbe = { isNetworkAvailable(this) }
        usageLedger = UsageLedger(prefs)
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
        val cloudIo = AndroidCloudIo(
            this,
            liteIo,
            http,
            applyVisibleWatermark = true, // always stamp AI provenance on cloud outputs
        )
        generative = GenerativeCloudService(http, cloudIo, appSettings, usageLedger)

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
                CloudEngine(http, cloudIo, appSettings, usageLedger),
            ),
        )
    }

    companion object {
        const val PACKS_MANIFEST_URL =
            "https://huggingface.co/datasets/Iamzakirzr/vestra-packs/resolve/main/manifest.json"

        fun isNetworkAvailable(context: Context): Boolean {
            val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }
}
