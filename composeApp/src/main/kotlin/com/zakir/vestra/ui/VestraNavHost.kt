package com.zakir.vestra.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zakir.vestra.shared.engine.EngineRouter
import com.zakir.vestra.shared.packs.ModelPackManager
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.wardrobe.WardrobeRepository
import com.zakir.vestra.ui.screens.capture.GarmentScreen
import com.zakir.vestra.ui.screens.casting.CastingStudioScreen
import com.zakir.vestra.ui.screens.onboarding.OnboardingScreen
import com.zakir.vestra.ui.screens.packs.PacksScreen
import com.zakir.vestra.ui.screens.generate.GenerationScreen
import com.zakir.vestra.ui.screens.person.PersonSourceScreen
import com.zakir.vestra.ui.screens.result.ResultScreen
import com.zakir.vestra.ui.screens.settings.SettingsScreen
import com.zakir.vestra.ui.screens.studio.StudioScreen
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
}

@Composable
fun VestraNavHost(
    appSettings: AppSettings,
    engineRouter: EngineRouter,
    wardrobe: WardrobeRepository,
    packManager: ModelPackManager,
    studioModels: com.zakir.vestra.data.StudioModelRepository,
    navController: NavHostController = rememberNavController(),
) {
    val onboardingComplete by appSettings.onboardingComplete.collectAsState()
    val start = if (onboardingComplete) Routes.STUDIO else Routes.ONBOARDING

    val tryOnViewModel: TryOnViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TryOnViewModel(engineRouter, appSettings, wardrobe) as T
        },
    )

    NavHost(navController = navController, startDestination = start) {
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
        composable(Routes.STUDIO) {
            StudioScreen(
                wardrobe = wardrobe,
                packManager = packManager,
                onNewLook = {
                    tryOnViewModel.resetSession()
                    navController.navigate(Routes.GARMENT)
                },
                onOpenWardrobe = { navController.navigate(Routes.WARDROBE) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenPacks = { navController.navigate(Routes.PACKS) },
            )
        }
        composable(Routes.GARMENT) {
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
            )
        }
        composable(Routes.WARDROBE) {
            WardrobeScreen(
                wardrobe = wardrobe,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                appSettings = appSettings,
                engineRouter = engineRouter,
                onOpenPacks = { navController.navigate(Routes.PACKS) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.PACKS) {
            PacksScreen(
                packManager = packManager,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
