package com.zakir.vestra.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zakir.vestra.shared.engine.EngineRouter
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.wardrobe.WardrobeRepository
import com.zakir.vestra.ui.screens.capture.GarmentScreen
import com.zakir.vestra.ui.screens.generate.GenerationScreen
import com.zakir.vestra.ui.screens.person.PersonSourceScreen
import com.zakir.vestra.ui.screens.result.ResultScreen
import com.zakir.vestra.ui.screens.settings.SettingsScreen
import com.zakir.vestra.ui.screens.studio.StudioScreen
import com.zakir.vestra.ui.screens.wardrobe.WardrobeScreen

/** Route names for the single-activity nav graph. */
object Routes {
    const val STUDIO = "studio"
    const val GARMENT = "garment"
    const val PERSON = "person"
    const val GENERATE = "generate"
    const val RESULT = "result"
    const val WARDROBE = "wardrobe"
    const val SETTINGS = "settings"
}

@Composable
fun VestraNavHost(
    appSettings: AppSettings,
    engineRouter: EngineRouter,
    wardrobe: WardrobeRepository,
    navController: NavHostController = rememberNavController(),
) {
    // One session ViewModel shared across the garment → person → generate → result flow.
    val tryOnViewModel: TryOnViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TryOnViewModel(engineRouter, appSettings, wardrobe) as T
        },
    )

    NavHost(navController = navController, startDestination = Routes.STUDIO) {
        composable(Routes.STUDIO) {
            StudioScreen(
                wardrobe = wardrobe,
                onNewLook = {
                    tryOnViewModel.resetSession()
                    navController.navigate(Routes.GARMENT)
                },
                onOpenWardrobe = { navController.navigate(Routes.WARDROBE) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.GARMENT) {
            GarmentScreen(
                viewModel = tryOnViewModel,
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Routes.PERSON) },
            )
        }
        composable(Routes.PERSON) {
            PersonSourceScreen(
                viewModel = tryOnViewModel,
                appSettings = appSettings,
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
                onBack = { navController.popBackStack() },
            )
        }
    }
}
