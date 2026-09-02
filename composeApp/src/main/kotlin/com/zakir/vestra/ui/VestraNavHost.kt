package com.zakir.vestra.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.zakir.vestra.data.NewsFeedConfig
import android.content.Intent
import com.zakir.vestra.diagnostics.CrashReporter
import com.zakir.vestra.shared.cloud.FreeCloudDiscovery
import com.zakir.vestra.shared.cloud.GenerativeCloudService
import com.zakir.vestra.shared.engine.EngineRouter
import com.zakir.vestra.shared.packs.ModelPackManager
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.usage.UsageLedger
import com.zakir.vestra.shared.wardrobe.WardrobeRepository
import com.zakir.vestra.ui.screens.capture.GarmentScreen
import com.zakir.vestra.ui.screens.casting.CastingStudioScreen
import com.zakir.vestra.ui.screens.onboarding.OnboardingScreen
import com.zakir.vestra.ui.screens.packs.PacksScreen
import com.zakir.vestra.ui.screens.generate.GenerationScreen
import com.zakir.vestra.ui.screens.person.PersonSourceScreen
import com.zakir.vestra.ui.screens.result.ResultScreen
import com.zakir.vestra.shared.chat.ChatRepository
import com.zakir.vestra.shared.diagnostics.RunDiagnostics
import com.zakir.vestra.shared.jobs.LocalJobStore
import com.zakir.vestra.ui.screens.settings.ApiMonitorScreen
import com.zakir.vestra.ui.screens.settings.DefaultModelsScreen
import com.zakir.vestra.ui.screens.settings.ModelsScreen
import com.zakir.vestra.ui.screens.settings.NotificationsScreen
import com.zakir.vestra.ui.screens.settings.ProviderModelsScreen
import com.zakir.vestra.shared.cloud.CloudPlatform
import com.zakir.vestra.ui.screens.settings.DiagnosticsScreen
import com.zakir.vestra.ui.screens.settings.SettingsScreen
import com.zakir.vestra.ui.screens.home.UnifiedMainScreen
import com.zakir.vestra.shared.news.NewsRepository
import com.zakir.vestra.shared.platformHttpClient
import com.zakir.vestra.ui.screens.changelog.ChangelogScreen
import com.zakir.vestra.ui.screens.help.HelpScreen
import com.zakir.vestra.ui.screens.privacy.PrivacyScreen
import com.zakir.vestra.ui.screens.wardrobe.WardrobeScreen

object Routes {
    const val ONBOARDING = "onboarding"
    // Home is now the single Gemini-style unified screen — Image/Video/Code/Audio/Chat all live
    // in one merged thread there (UnifiedMainScreen.kt), routed by a composer mode chip rather
    // than by separate isolated screens/routes.
    const val HOME = "home"
    const val GARMENT = "garment"
    const val CASTING = "casting"
    const val PERSON = "person"
    const val GENERATE = "generate"
    const val RESULT = "result"
    const val WARDROBE = "wardrobe"
    const val SETTINGS = "settings"
    const val SETTINGS_DIAGNOSTICS = "settings/diagnostics"
    const val SETTINGS_MODELS = "settings/models"
    const val SETTINGS_DEFAULT_MODELS = "settings/models/defaults"
    const val SETTINGS_NOTIFICATIONS = "settings/notifications"
    const val SETTINGS_API_MONITOR = "settings/api-monitor"

    /** [SETTINGS_MODELS_PROVIDER] takes the [CloudPlatform] enum name as its one argument. */
    const val SETTINGS_MODELS_PROVIDER = "settings/models/provider"
    const val ARG_PLATFORM = "platform"
    fun providerModels(platform: CloudPlatform) = "$SETTINGS_MODELS_PROVIDER/${platform.name}"
    const val PACKS = "packs"
    const val CREATE = "create"
    const val USAGE = "usage"
    const val HELP = "help"
    const val PRIVACY = "privacy"
    const val CHANGELOG = "changelog"

    fun deepLink(route: String) = "lookbook://screen/$route"
}

@Composable
fun VestraNavHost(
    appSettings: AppSettings,
    engineRouter: EngineRouter,
    wardrobe: WardrobeRepository,
    packManager: ModelPackManager,
    studioModels: com.zakir.vestra.data.StudioModelRepository,
    generative: GenerativeCloudService,
    usageLedger: UsageLedger,
    runDiagnostics: RunDiagnostics,
    localJobStore: LocalJobStore,
    chatRepository: ChatRepository,
    memoryRepository: com.zakir.vestra.shared.chat.MemoryRepository,
    deviceRamMb: Long,
    freeCloudDiscovery: FreeCloudDiscovery,
    humanParsing: com.zakir.vestra.shared.engine.lite.HumanParsing,
    liteEngineIo: com.zakir.vestra.shared.engine.lite.LiteEngineIo,
    navController: NavHostController = rememberNavController(),
    pendingDeepLinkIntent: Intent? = null,
    onDeepLinkHandled: () -> Unit = {},
) {
    val onboardingComplete by appSettings.onboardingComplete.collectAsState()
    val start = if (onboardingComplete) Routes.HOME else Routes.ONBOARDING

    LaunchedEffect(pendingDeepLinkIntent, onboardingComplete) {
        val intent = pendingDeepLinkIntent ?: return@LaunchedEffect
        if (!onboardingComplete) return@LaunchedEffect
        val route = intent.data
            ?.takeIf { it.scheme == "lookbook" && it.host == "screen" }
            ?.pathSegments
            ?.firstOrNull()
        if (route.isNullOrBlank()) return@LaunchedEffect
        // Prefer explicit navigate — handleDeepLink is flaky when the graph
        // is already composed on a warm singleTop Activity.
        runCatching {
            navController.navigate(route) {
                launchSingleTop = true
                restoreState = true
            }
        }
        onDeepLinkHandled()
    }

    val tryOnViewModel: TryOnViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TryOnViewModel(engineRouter, appSettings, wardrobe, runDiagnostics, deviceRamMb) as T
        },
    )

    // Null outside a real Android app process (tests, previews) — every notifier call is
    // null-safe, so a missing one never affects generation.
    val appContext = LocalContext.current.applicationContext
    val notifier = remember(appContext) {
        (appContext as? com.zakir.vestra.VestraApp)?.generationNotifier
    }

    val generativeViewModel: GenerativeViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                GenerativeViewModel(
                    generative,
                    appSettings,
                    usageLedger,
                    wardrobe,
                    runDiagnostics,
                    deviceRamMb,
                    localJobStore,
                    notifier,
                ) as T
        },
    )

    // Instant transitions — AnimatedContent measure of heavy screens (Settings)
    // was ANRing the main thread on mid/low devices and looking like a crash.
    val navEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navEntry?.destination?.route) {
        val route = navEntry?.destination?.route ?: "unknown"
        val tab = navEntry?.arguments?.getString("tab")
        CrashReporter.breadcrumb(if (tab != null) "$route#$tab" else route)
    }

    // No bottom dock any more — Home is a single Gemini-style unified screen, and Library/
    // Settings are reached from its own top-right icons instead of a shared tab bar.
    Box(Modifier.fillMaxSize()) {
    NavHost(
        navController = navController,
        startDestination = start,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                appSettings = appSettings,
                onDone = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = Routes.HOME,
            deepLinks = listOf(navDeepLink { uriPattern = Routes.deepLink(Routes.HOME) }),
        ) {
            val context = LocalContext.current
            val newsRepository = remember(context) {
                NewsRepository(platformHttpClient(), NewsFeedConfig.load(context))
            }
            val chatViewModel: ChatViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        ChatViewModel(
                            chatRepository,
                            newsRepository,
                            generative,
                            appSettings,
                            runDiagnostics,
                            deviceRamMb,
                            memoryRepository,
                        ) as T
                },
            )
            UnifiedMainScreen(
                generativeViewModel = generativeViewModel,
                chatViewModel = chatViewModel,
                appSettings = appSettings,
                localJobStore = localJobStore,
                freeCloudDiscovery = freeCloudDiscovery,
                packManager = packManager,
                onOpenLibrary = { navController.navigate(Routes.WARDROBE) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            route = Routes.GARMENT,
            deepLinks = listOf(navDeepLink { uriPattern = Routes.deepLink(Routes.GARMENT) }),
        ) {
            GarmentScreen(
                viewModel = tryOnViewModel,
                humanParsing = humanParsing,
                liteEngineIo = liteEngineIo,
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Routes.CASTING) },
            )
        }
        composable(Routes.CASTING) {
            CastingStudioScreen(
                viewModel = tryOnViewModel,
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Routes.PERSON) },
            )
        }
        composable(Routes.PERSON) {
            PersonSourceScreen(
                viewModel = tryOnViewModel,
                appSettings = appSettings,
                studioModels = studioModels,
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Routes.GENERATE) },
            )
        }
        composable(Routes.GENERATE) {
            GenerationScreen(
                viewModel = tryOnViewModel,
                onComplete = {
                    navController.navigate(Routes.RESULT) {
                        popUpTo(Routes.HOME)
                    }
                },
                onAbort = { navController.popBackStack(Routes.HOME, inclusive = false) },
                onOpenHelp = { navController.navigate(Routes.HELP) },
            )
        }
        composable(Routes.RESULT) {
            ResultScreen(
                viewModel = tryOnViewModel,
                wardrobe = wardrobe,
                onNewLook = {
                    tryOnViewModel.resetSession()
                    navController.navigate(Routes.GARMENT) {
                        popUpTo(Routes.HOME)
                    }
                },
                onBackToStudio = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
                onOpenWardrobe = { navController.navigate(Routes.WARDROBE) },
            )
        }
        composable(
            route = Routes.CREATE,
            deepLinks = listOf(navDeepLink { uriPattern = Routes.deepLink(Routes.CREATE) }),
        ) {
            LaunchedEffect(Unit) {
                generativeViewModel.bindStudio(com.zakir.vestra.shared.cloud.AiCapability.IMAGE_GEN)
                navController.navigate(Routes.HOME) {
                    launchSingleTop = true
                    popUpTo(Routes.CREATE) { inclusive = true }
                }
            }
        }
        composable(
            route = Routes.USAGE,
            deepLinks = listOf(navDeepLink { uriPattern = Routes.deepLink(Routes.USAGE) }),
        ) {
            // Was UsageScreen, which rendered the same ApiUsageDashboardCard as ApiMonitorScreen
            // plus a UsageLedger summary — two screens doing one job. The route is kept because
            // it is deep-linked and scripts/visual-verify.sh drives it; it resolves to the one
            // monitor screen now, which absorbed the ledger summary.
            ApiMonitorScreen(
                onBack = { navController.popBackStack() },
                onOpenKeys = { navController.navigate(Routes.SETTINGS_MODELS) },
            )
        }
        composable(
            route = Routes.HELP,
            deepLinks = listOf(navDeepLink { uriPattern = Routes.deepLink(Routes.HELP) }),
        ) {
            HelpScreen(
                onBack = { navController.popBackStack() },
                onOpenSettings = {
                    navController.navigate(Routes.SETTINGS) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(
            route = Routes.PRIVACY,
            deepLinks = listOf(navDeepLink { uriPattern = Routes.deepLink(Routes.PRIVACY) }),
        ) {
            PrivacyScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.CHANGELOG,
            deepLinks = listOf(navDeepLink { uriPattern = Routes.deepLink(Routes.CHANGELOG) }),
        ) {
            ChangelogScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.WARDROBE,
            deepLinks = listOf(navDeepLink { uriPattern = Routes.deepLink(Routes.WARDROBE) }),
        ) {
            WardrobeScreen(
                wardrobe = wardrobe,
                onBack = { navController.popBackStack() },
                // onStartTryOn omitted (defaults to null) while try-on is temporarily
                // disabled app-wide — restores the empty-state CTA when re-added.
                onReusePrompt = { prompt, isVideo ->
                    // bindStudio() must run before setPrompt() — the unified screen's own
                    // composer reads the bound studio's remembered prompt on first composition,
                    // which would otherwise overwrite the one just reused. Binding here first
                    // means that read is a same-key no-op, so the value set below survives it.
                    val capability = if (isVideo) {
                        com.zakir.vestra.shared.cloud.AiCapability.VIDEO
                    } else {
                        com.zakir.vestra.shared.cloud.AiCapability.IMAGE_GEN
                    }
                    generativeViewModel.bindStudio(capability)
                    generativeViewModel.setPrompt(prompt)
                    navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) }
                },
            )
        }
        composable(
            route = Routes.SETTINGS,
            deepLinks = listOf(navDeepLink { uriPattern = Routes.deepLink(Routes.SETTINGS) }),
        ) {
            SettingsScreen(
                appSettings = appSettings,
                packManager = packManager,
                freeCloudDiscovery = freeCloudDiscovery,
                usageLedger = usageLedger,
                memoryRepository = memoryRepository,
                onOpenPacks = { navController.navigate(Routes.PACKS) },
                onOpenHelp = { navController.navigate(Routes.HELP) },
                onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                onOpenChangelog = { navController.navigate(Routes.CHANGELOG) },
                onOpenDiagnostics = { navController.navigate(Routes.SETTINGS_DIAGNOSTICS) },
                onOpenModels = { navController.navigate(Routes.SETTINGS_MODELS) },
                onOpenDefaultModels = { navController.navigate(Routes.SETTINGS_DEFAULT_MODELS) },
                onOpenNotifications = { navController.navigate(Routes.SETTINGS_NOTIFICATIONS) },
                onOpenApiMonitor = { navController.navigate(Routes.SETTINGS_API_MONITOR) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS_MODELS) {
            ModelsScreen(
                appSettings = appSettings,
                packManager = packManager,
                engineRouter = engineRouter,
                onOpenProvider = { navController.navigate(Routes.providerModels(it)) },
                onOpenPacks = { navController.navigate(Routes.PACKS) },
                onOpenDefaults = { navController.navigate(Routes.SETTINGS_DEFAULT_MODELS) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "${Routes.SETTINGS_MODELS_PROVIDER}/{${Routes.ARG_PLATFORM}}",
            arguments = listOf(navArgument(Routes.ARG_PLATFORM) { type = NavType.StringType }),
        ) { entry ->
            // An unparseable argument can only come from a hand-typed deep link; fall back to
            // Hugging Face rather than crashing on valueOf.
            val platform = entry.arguments?.getString(Routes.ARG_PLATFORM)
                ?.let { name -> CloudPlatform.entries.firstOrNull { it.name == name } }
                ?: CloudPlatform.HF_INFERENCE
            ProviderModelsScreen(
                platform = platform,
                appSettings = appSettings,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS_DEFAULT_MODELS) {
            DefaultModelsScreen(
                appSettings = appSettings,
                freeCloudDiscovery = freeCloudDiscovery,
                packManager = packManager,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS_NOTIFICATIONS) {
            NotificationsScreen(
                appSettings = appSettings,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS_API_MONITOR) {
            ApiMonitorScreen(
                onBack = { navController.popBackStack() },
                onOpenKeys = { navController.navigate(Routes.SETTINGS_MODELS) },
            )
        }
        composable(Routes.SETTINGS_DIAGNOSTICS) {
            DiagnosticsScreen(
                diagnostics = runDiagnostics,
                usage = usageLedger,
                packManager = packManager,
                onBack = { navController.popBackStack() },
                onOpenHelp = { navController.navigate(Routes.HELP) },
            )
        }
        composable(
            route = Routes.PACKS,
            deepLinks = listOf(navDeepLink { uriPattern = Routes.deepLink(Routes.PACKS) }),
        ) {
            PacksScreen(
                packManager = packManager,
                onBack = { navController.popBackStack() },
            )
        }
    }

    com.zakir.vestra.ui.components.GlassSnackbarHost(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .safeDrawingPadding(),
    )
    }
}
