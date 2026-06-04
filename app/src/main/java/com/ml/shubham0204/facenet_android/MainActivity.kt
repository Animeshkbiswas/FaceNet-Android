package com.ml.shubham0204.facenet_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ml.shubham0204.facenet_android.data.SettingsStore
import com.ml.shubham0204.facenet_android.domain.PersonUseCase
import com.ml.shubham0204.facenet_android.presentation.screens.home.HomeScreen
import com.ml.shubham0204.facenet_android.presentation.screens.home.StartupDestination
import com.ml.shubham0204.facenet_android.presentation.screens.add_face.AddFaceScreen
import com.ml.shubham0204.facenet_android.presentation.screens.detect_screen.DetectScreen
import com.ml.shubham0204.facenet_android.presentation.screens.face_list.FaceListScreen
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val settingsStore: SettingsStore by inject()
    private val personUseCase: PersonUseCase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val startDestination = resolveStartDestination()
        setContent {
            val navHostController = rememberNavController()
            NavHost(
                navController = navHostController,
                startDestination = startDestination,
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() },
            ) {
                composable(StartupDestination.HOME) {
                    HomeScreen(
                        onStartDetection = { navHostController.navigate(StartupDestination.DETECT) },
                        onAddUser = { navHostController.navigate(StartupDestination.ADD_FACE) },
                        onOpenUsers = { navHostController.navigate("face-list") },
                    )
                }
                composable(StartupDestination.ADD_FACE) { AddFaceScreen { navHostController.navigateUp() } }
                composable(StartupDestination.DETECT) {
                    DetectScreen(
                        onOpenFaceListClick = { navHostController.navigate("face-list") },
                        onNavigateHome = { navHostController.navigate(StartupDestination.HOME) },
                    )
                }
                composable("face-list") {
                    FaceListScreen(
                        onNavigateBack = { navHostController.navigateUp() },
                        onAddFaceClick = { navHostController.navigate(StartupDestination.ADD_FACE) },
                        onNavigateHome = { navHostController.navigate(StartupDestination.HOME) },
                    )
                }
            }
        }
    }

    private fun resolveStartDestination(): String {
        if (personUseCase.getCount() <= 0L) {
            return StartupDestination.HOME
        }
        val startupRoute = settingsStore.get(StartupDestination.PREF_STARTUP_ROUTE)
        return when (startupRoute) {
            StartupDestination.DETECT -> StartupDestination.DETECT
            StartupDestination.ADD_FACE -> StartupDestination.ADD_FACE
            else -> StartupDestination.HOME
        }
    }
}
