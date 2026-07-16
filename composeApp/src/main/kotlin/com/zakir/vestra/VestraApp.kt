package com.zakir.vestra

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.russhwolf.settings.SharedPreferencesSettings
import com.zakir.vestra.data.AiModelCatalog
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

        val http = platformHttpClient()
        val cloudConfig = CloudConfig(
            tryOnUrl = "$SUPABASE_URL/functions/v1/tryon",
            reportUrl = "$SUPABASE_URL/functions/v1/report",
            anonKey = SUPABASE_ANON_KEY,
        )
        val liteIo = LiteEngineIo(this, ::renderAiModel)
        val parsing = HumanParsing(packManager)
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

    /** Rasterizes a base-model drawable for the Lite pipeline. */
    private fun renderAiModel(modelId: String): Bitmap? {
        val model = AiModelCatalog.byId(modelId) ?: return null
        val drawable = ContextCompat.getDrawable(this, model.image) ?: return null
        val bitmap = createBitmap(900, 1200)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    companion object {
        // Placeholder until the production Hugging Face packs repo exists
        // (needs the owner's HF account). Override locally by pointing this at
        // any static server hosting exports/ from ml/manifest_gen.py.
        const val PACKS_MANIFEST_URL =
            "https://huggingface.co/datasets/REPLACE_ME/vestra-packs/resolve/main/manifest.json"

        // Supabase project backing the Cloud tier and report intake. The anon
        // key is publishable by design (RLS/service-role boundaries hold the
        // secrets); Replicate's token lives only in Edge Function secrets.
        const val SUPABASE_URL = "https://REPLACE_ME.supabase.co"
        const val SUPABASE_ANON_KEY = ""
    }
}
