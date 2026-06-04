package com.ml.shubham0204.facenet_android.presentation.screens.detect_screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.ml.shubham0204.facenet_android.R
import com.ml.shubham0204.facenet_android.presentation.components.AppAlertDialog
import com.ml.shubham0204.facenet_android.presentation.components.DelayedVisibility
import com.ml.shubham0204.facenet_android.presentation.components.FaceDetectionOverlay
import com.ml.shubham0204.facenet_android.presentation.components.createAlertDialog
import com.ml.shubham0204.facenet_android.presentation.theme.FaceNetAndroidTheme
import org.koin.androidx.compose.koinViewModel
import kotlin.OptIn

private val cameraPermissionStatus = mutableStateOf(false)
private val cameraFacing = mutableIntStateOf(CameraSelector.LENS_FACING_BACK)
private lateinit var cameraPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectScreen(
    onOpenFaceListClick: (() -> Unit),
    onNavigateHome: (() -> Unit),
) {
    val viewModel: DetectScreenViewModel = koinViewModel()
    FaceNetAndroidTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(),
                    title = {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    },
                    actions = {
                        IconButton(onClick = onNavigateHome) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Go Home",
                            )
                        }
                        IconButton(onClick = onOpenFaceListClick) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "Open Face List",
                            )
                        }
                        IconButton(onClick = { viewModel.changeCameraFacing() }) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera",
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                ScreenUI(viewModel)
            }
        }
    }
}

@Composable
private fun ScreenUI(viewModel: DetectScreenViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        Camera(viewModel)

        val showDetails = remember { mutableStateOf(false) }
        if (viewModel.getNumPeople() > 0L) {
            DetailToggle(
                isExpanded = showDetails.value,
                onClick = { showDetails.value = !showDetails.value },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 112.dp),
            )

            DelayedVisibility(showDetails.value) {
                val metrics by remember { viewModel.faceDetectionMetricsState }
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 6.dp,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text(
                            text = "Live recognition active",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${viewModel.getNumPeople()} enrolled face(s)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        metrics?.let {
                            Spacer(modifier = Modifier.height(12.dp))
                            MetricChip("Detect", "${it.timeFaceDetection} ms")
                            Spacer(modifier = Modifier.height(8.dp))
                            MetricChip("Embed", "${it.timeFaceEmbedding} ms")
                            Spacer(modifier = Modifier.height(8.dp))
                            MetricChip("Search", "${it.timeVectorSearch} ms")
                            Spacer(modifier = Modifier.height(8.dp))
                            MetricChip("Spoof", "${it.timeFaceSpoofDetection} ms")
                        }
                    }
                }
            }

            DelayedVisibility(viewModel.notLiveWarningTextState.value.isNotBlank()) {
                NotLiveBanner(
                    message = viewModel.notLiveWarningTextState.value,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                )
            }
        }

        DelayedVisibility(viewModel.getNumPeople() == 0L) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f),
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "No faces enrolled yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Add a user first, then return here for live recognition.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "When a face is detected, the recognition label and performance stats will appear above the camera view.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        AppAlertDialog()
    }
}

@Composable
private fun MetricChip(
    title: String,
    value: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun DetailToggle(
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isExpanded) "Hide details" else "More details",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun NotLiveBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.96f),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 8.dp,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Not live detected",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (message.isBlank()) {
                    "The current face looks spoofed or not live. Try using a real person in front of the camera."
                } else {
                    "$message. Try using a real person in front of the camera."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun Camera(viewModel: DetectScreenViewModel) {
    val context = LocalContext.current
    cameraPermissionStatus.value =
        ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    val cameraFacing by remember { viewModel.cameraFacing }
    val lifecycleOwner = LocalLifecycleOwner.current

    cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                cameraPermissionStatus.value = true
            } else {
                camaraPermissionDialog()
            }
        }

    DelayedVisibility(cameraPermissionStatus.value) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { FaceDetectionOverlay(lifecycleOwner, context, viewModel) },
            update = { it.initializeCamera(cameraFacing) },
        )
    }

    DelayedVisibility(!cameraPermissionStatus.value) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Allow Camera Permissions\nThe app cannot work without the camera permission.",
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(text = "Allow")
            }
        }
    }
}

private fun camaraPermissionDialog() {
    createAlertDialog(
        "Camera Permission",
        "The app couldn't function without the camera permission.",
        "ALLOW",
        "CLOSE",
        onPositiveButtonClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
        onNegativeButtonClick = {
            // TODO: Handle deny camera permission action
            //       close the app
        },
    )
}
