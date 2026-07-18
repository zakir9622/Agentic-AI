package com.zakir.vestra

import android.app.Application
import com.russhwolf.settings.SharedPreferencesSettings
import com.zakir.vestra.data.StudioModelRepository
import com.zakir.vestra.shared.cloud.AndroidCloudIo
import com.zakir.vestra.shared.cloud.CloudConfig
import com.zakir.vestra.shared.cloud.CloudEngine
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
import com.zakir.vestra.shared.safety.ReportQueue
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.wardrobe.AndroidTextFileStore
import com.zakir.vestra.shared.wardrobe.WardrobeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual composition root — the dependency graph is small enough that a DI
 * framework would be pure overhead. Pro/Cloud engines are mocks until M4/M5.
 */
class VestraApp : Application() {

    lateinit var appSettings: AppSettings
        private set

    lateinit var engineRouter: EngineRouter
        private set

    lateinit var wardrobe: WardrobeRepository
        private set

    lateinit var packManager: ModelPackManager
        private set

    lateinit var reportQueue: ReportQueue
        private set

    lateinit var studioModels: StudioModelRepository
        private set

    lateinit var garmentGuard: com.zakir.vestra.data.GarmentInputGuard
        private set

    /** Whether the Cloud tier has been configured with Supabase credentials. */
    val cloudConfigured: Boolean get() = SUPABASE_ANON_KEY.isNotBlank() && !SUPABASE_URL.contains("REPLACE_ME")

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        appSettings = AppSettings(
            SharedPreferencesSettings(getSharedPreferences("vestra_settings", MODE_PRIVATE)),
        )
        wardrobe = WardrobeRepository(AndroidTextFileStore(filesDir))

        packManager = ModelPackManager(
            fs = AndroidPackFileSystem(this),
            device = AndroidDeviceProbe(this),
            http = platformHttpClient(),
            manifestUrl = PACKS_MANIFEST_URL,
        )
        PackDownloadWorker.dependencies = { packManager }
        // Debug builds ship the Lite pack in assets so generation works before
        // the Hugging Face repo is set up. No-ops in release.
        DebugPackBootstrap.seedLitePack(this)
        studioModels = StudioModelRepository(this, packManager)

        val http = platformHttpClient()
        val cloudConfig = CloudConfig(
            tryOnUrl = "$SUPABASE_URL/functions/v1/tryon",
            reportUrl = "$SUPABASE_URL/functions/v1/report",
            anonKey = SUPABASE_ANON_KEY,
        )
        val liteIo = LiteEngineIo(this) { modelId -> studioModels.resolveBitmap(modelId) }
        val parsing = HumanParsing(packManager)
        garmentGuard = com.zakir.vestra.data.GarmentInputGuard(liteIo, parsing)
        engineRouter = EngineRouter(
            listOf(
                LiteEngine(packManager, liteIo, parsing),
                DiffusionEngine(
                    packs = packManager,
                    device = AndroidDeviceProbe(this),
                    io = liteIo,
                    masker = { person, category -> parsing.analyze(person, category)?.mask },
                ),
                CloudEngine(http, AndroidCloudIo(this, liteIo), cloudConfig),
            ),
        )

        reportQueue = ReportQueue(
            store = AndroidTextFileStore(filesDir),
            http = http,
            config = cloudConfig,
            appVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "dev",
        )
        // Deliver any reports queued while offline.
        appScope.launch { reportQueue.flush() }
    }

    companion object {
        // Placeholder until the production Hugging Face packs repo exists
        // (needs the owner's HF account). Override locally by pointing this at
        // any static server hosting exports/ from ml/manifest_gen.py.
        const val PACKS_MANIFEST_URL =
            "https://huggingface.co/datasets/Iamzakirzr/vestra-packs/resolve/main/manifest.json"

        // Supabase project backing the Cloud tier and report intake. The anon
        // key is publishable by design (RLS/service-role boundaries hold the
        // secrets); Replicate's token lives only in the project's Vault, read
        // by the tryon Edge Function via a service-role-only accessor.
        const val SUPABASE_URL = "https://todzunpexvvmbxpvdyap.supabase.co"
        const val SUPABASE_ANON_KEY =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRvZHp1bnBleHZ2bWJ4cHZkeWFwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQzNTQyNDcsImV4cCI6MjA5OTkzMDI0N30.yeu8BD1yO_7pk29H78kKxYnqy4mf7xdubHNHaONiHm4"
    }
}
