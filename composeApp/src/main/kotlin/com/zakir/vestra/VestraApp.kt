package com.zakir.vestra

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.russhwolf.settings.SharedPreferencesSettings
import com.zakir.vestra.data.StudioModelRepository
import com.zakir.vestra.shared.cloud.AndroidCloudIo
import com.zakir.vestra.shared.cloud.CloudEngine
import com.zakir.vestra.shared.cloud.FreeCloudDiscovery
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
import com.zakir.vestra.shared.quality.createQualityPostProcessor
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.usage.UsageLedger
import com.zakir.vestra.shared.wardrobe.AndroidTextFileStore
import com.zakir.vestra.shared.wardrobe.WardrobeRepository
import com.zakir.vestra.storage.DurableStorage
import com.zakir.vestra.storage.TokenSidecar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VestraApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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

    lateinit var freeCloudDiscovery: FreeCloudDiscovery
        private set

    lateinit var humanParsing: HumanParsing
        private set

    lateinit var liteEngineIo: LiteEngineIo
        private set

    override fun onCreate() {
        super.onCreate()
        val prefs = SharedPreferencesSettings(getSharedPreferences("vestra_settings", MODE_PRIVATE))
        appSettings = AppSettings(prefs)
        appSettings.networkProbe = { isNetworkAvailable(this) }
        usageLedger = UsageLedger(prefs)
        wardrobe = WardrobeRepository(AndroidTextFileStore(filesDir))

        val http = platformHttpClient()
        freeCloudDiscovery = FreeCloudDiscovery(http)
        // Restore tokens from Documents/TheLookbook/tokens.json after reinstall.
        TokenSidecar.restoreIntoPrefsIfEmpty(this, appSettings)
        // Optional sideload seed from local.properties / LOOKBOOK_HF_TOKEN (gitignored).
        TokenSidecar.applyDefaultHfIfBlank(appSettings, BuildConfig.DEFAULT_HF_TOKEN)
        TokenSidecar.applyDefaultOpenRouterIfBlank(appSettings, BuildConfig.DEFAULT_OPENROUTER_TOKEN)
        TokenSidecar.applyDefaultGroqIfBlank(appSettings, BuildConfig.DEFAULT_GROQ_TOKEN)
        if (DurableStorage.hasAllFilesAccess()) {
            TokenSidecar.autoFetchFromDocuments(appSettings, overwriteExisting = false)
        }
        packManager = ModelPackManager(
            fs = AndroidPackFileSystem(this) { DurableStorage.resolvePacksRoot(this) },
            device = AndroidDeviceProbe(this),
            http = http,
            manifestUrl = PACKS_MANIFEST_URL,
        )
        PackDownloadWorker.dependencies = { packManager }
        // Load bundled lite manifest immediately so Settings/Studio show Lite as available.
        appScope.launch { packManager.refresh(networkAllowed = isNetworkAvailable(this@VestraApp)) }
        DebugPackBootstrap.seedLitePackAsync(this, DurableStorage.resolvePacksRoot(this)) {
            appScope.launch { packManager.refresh(networkAllowed = isNetworkAvailable(this@VestraApp)) }
        }
        appScope.launch {
            if (!appSettings.hfToken.value.isNullOrBlank()) {
                runCatching { freeCloudDiscovery.refreshRouterDiscovery(appSettings) }
            }
        }
        studioModels = StudioModelRepository(this, packManager)

        liteEngineIo = LiteEngineIo(this) { modelId -> studioModels.resolveBitmap(modelId) }
        humanParsing = HumanParsing(packManager)
        val quality = createQualityPostProcessor(packManager)
        val cloudIo = AndroidCloudIo(
            this,
            liteEngineIo,
            http,
            applyVisibleWatermark = true, // always stamp AI provenance on cloud outputs
        )
        generative = GenerativeCloudService(http, cloudIo, appSettings, usageLedger)

        engineRouter = EngineRouter(
            listOf(
                LiteEngine(packManager, liteEngineIo, humanParsing, quality),
                DiffusionEngine(
                    packs = packManager,
                    device = AndroidDeviceProbe(this),
                    io = liteEngineIo,
                    masker = { person, category -> humanParsing.analyze(person, category.effectiveCategory())?.mask },
                    applyWatermark = BuildConfig.APPLY_WATERMARK,
                    quality = quality,
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
