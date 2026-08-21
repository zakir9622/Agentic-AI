package com.zakir.vestra.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import android.content.Intent
import com.zakir.vestra.shared.cloud.FreeCloudDiscovery
import com.zakir.vestra.shared.cloud.GenerativeCloudService
import com.zakir.vestra.shared.engine.EngineRouter
import com.zakir.vestra.shared.packs.ModelPackManager
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.usage.UsageLedger
import com.zakir.vestra.shared.wardrobe.WardrobeRepository
import com.zakir.vestra.ui.screens.capture.GarmentScreen
import com.zakir.vestra.ui.screens.casting.CastingStudioScreen
import com.zakir.vestra.ui.screens.code.CodeStudioScreen
import com.zakir.vestra.ui.screens.create.CreateStudioScreen
import com.zakir.vestra.ui.screens.onboarding.OnboardingScreen
import com.zakir.vestra.ui.screens.packs.PacksScreen
import com.zakir.vestra.ui.screens.generate.GenerationScreen
import com.zakir.vestra.ui.screens.person.PersonSourceScreen
import com.zakir.vestra.ui.screens.result.ResultScreen
import com.zakir.vestra.ui.screens.settings.SettingsScreen
import com.zakir.vestra.ui.screens.studio.StudioScreen
import com.zakir.vestra.ui.screens.usage.UsageScreen
import com.zakir.vestra.ui.screens.video.VideoStudioScreen
import com.zakir.vestra.ui.screens.help.HelpScreen
import com.zakir.vestra.ui.screens.wardrobe.WardrobeScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val STUDIO = "studio"
    const val GARMENT = "garment"
    const val CASTING = "casting"
    const val PERSON = "person"
    const val GENERATE = "generate"
    const val RESULT = "result"
    const val WARDROBE = "wardrobe"
    const val SETTINGS = "settings"
    const val PACKS = "packs"
    const val CREATE = "create"
    const val CODE = "code"
    const val VIDEO = "video"
    const val USAGE = "usage"
    const val HELP = "help"

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
    freeCloudDiscovery: FreeCloudDiscovery,
    navController: NavHostController = rememberNavController(),
    pendingDeepLinkIntent: Intent? = null,
    onDeepLinkHandled: () -> Unit = {},
) {
    val onboardingComplete by appSettings.onboardingComplete.collectAsState()
    val start = if (onboardingComplete) Routes.STUDIO else Routes.ONBOARDING

    LaunchedEffect(pendingDeepLinkIntent, onboardingComplete) {
        val intent = pendingDeepLinkIntent ?: return@LaunchedEffect
        if (!onboardingComplete) return@LaunchedEffect
        val handled = navController.handleDeepLink(intent)
        if (handled) onDeepLinkHandled()
    }

    val tryOnViewModel: TryOnViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TryOnViewModel(engineRouter, appSettings, wardrobe) as T
        },
    )

    val generativeViewModel: GenerativeViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                GenerativeViewModel(generative, appSettings, usageLedger, wardrobe) as T
        },
    )

    // Instant transitions — AnimatedContent measure of heavy screens (Settings)
    // was ANRing the main thread on mid/low devices and looking like a crash.
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
                    navController.navigate(Routes.STUDIO) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = Routes.STUDIO,
            deepLinks = listOf(navDeepLink { uriPattern = Routes.deepLink(Routes.STUDIO) }),
        ) {
            StudioScreen(
                appSettings = appSettings,
                wardrobe = wardrobe,
                packManager = packManager,
                onNewLook = {
                    tryOnViewModel.resetSession()
                    navController.navigate(Routes.GARMENT)
                },
                onOpenCreate = {
                    generativeViewModel.prepareStudio(resetIfIdle = true)
                    navController.navigate(Routes.CREATE)
                },
                onOpenCode = {
                    generativeViewModel.prepareStudio(resetIfIdle = true)
                    navController.navigate(Routes.CODE)
                },
                onOpenVideo = {
                    generativeViewModel.prepareStudio(resetIfIdle = true)
                    navController.navigate(Routes.VIDEO)
                },
                onOpenWardrobe = { navController.navigate(Routes.WARDROBE) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenPacks = { navController.navigate(Routes.PACKS) },
                onOpenUsage = { navController.navigate(Routes.USAGE) },
                onOpenHelp = { navController.navigate(Routes.HELP) },
            )
        }
        composable(
            route = Routes.GARMENT,
            deepLinks = listOf(navDeepLink { uriPattern = Routes.deepLink(Routes.GARMENT) }),
        ) {
            GarmentScreen(
                viewModel = tryOnViewModel,
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
                        popUpTo(Routes.STUDIO)
                    }
                },
                onAbort = { navController.popBackStack(Routes.STUDIO, inclusive = false) },
            )
        }
        composable(Routes.RESULT) {
            ResultScreen(
                viewModel = tryOnViewModel,
                onNewLook = {
                    tryOnViewModel.resetSession()
                    navController.navigate(Routes.GARMENT) {
                        popUpTo(Routes.STUDIO)
                    }
                },
                onBackToStudio = {
                    navController.popBackStack(Routes.STUDIO, inclusive = false)
                },
            )
        }
        composable(
            route = Routes.CREATE,
            deepLinks = listOf(navDeepLink { uriPattern = Routes.deepLink(Routes.CREATE) }),
        ) {
            CreateStudioScreen(
                viewModel = generativeViewModel,
                onBack = { navController.popBackStack() },
                onOpenSettings = {
                    navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
                },
            )
        }
        composable(
            route = Routes.CODE,
            deepLinks = listOf(navDeepLink { uriPattern = Routes.deepLink(Routes.CODE) }),
        ) {
            CodeStudioScreen(
                viewModel = generativeViewModel,
                onBack = { navController.popBackStack() },
                onOpenSettings = {
                    navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
                },
            )
        }
        composable(
            route = Routes.VIDEO,
            deepLinks = listOf(navDeepLink { uriPattern = Routes.deepLink(Routes.VIDEO) }),
        ) {
            VideoStudioScreen(
                viewModel = generativeViewModel,
                onBack = { navController.popBackStack() },
                onOpenSettings = {
                    navController.navigate(Routes.SETTINGS) { launchSingleTop = true }
                },
            )
        }
        composable(
            route = Routes.USAGE,
            deepLinks = listOf(navDeepLink { uriPattern = Routes.deepLink(Routes.USAGE) }),
        ) {
            UsageScreen(
                usage = usageLedger,
                onBack = { navController.popBackStack() },
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
            route = Routes.WARDROBE,
            deepLinks = listOf(navDeepLink { uriPattern = Routes.deepLink(Routes.WARDROBE) }),
        ) {
            WardrobeScreen(
                wardrobe = wardrobe,
                onBack = { navController.popBackStack() },
                onStartTryOn = {
                    navController.navigate(Routes.GARMENT) {
                        popUpTo(Routes.STUDIO)
                    }
                },
            )
        }
        composable(
            route = Routes.SETTINGS,
            deepLinks = listOf(navDeepLink { uriPattern = Routes.deepLink(Routes.SETTINGS) }),
        ) {
            SettingsScreen(
                appSettings = appSettings,
                engineRouter = engineRouter,
                packManager = packManager,
                freeCloudDiscovery = freeCloudDiscovery,
                usageLedger = usageLedger,
                onOpenPacks = { navController.navigate(Routes.PACKS) },
                onOpenUsage = { navController.navigate(Routes.USAGE) },
                onOpenHelp = { navController.navigate(Routes.HELP) },
                onBack = { navController.popBackStack() },
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
}
