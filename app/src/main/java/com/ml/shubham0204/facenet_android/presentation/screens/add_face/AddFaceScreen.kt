package com.ml.shubham0204.facenet_android.presentation.screens.add_face

import android.Manifest
import android.net.Uri
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.ml.shubham0204.facenet_android.presentation.components.AppProgressDialog
import com.ml.shubham0204.facenet_android.presentation.components.DelayedVisibility
import com.ml.shubham0204.facenet_android.presentation.components.createAlertDialog
import com.ml.shubham0204.facenet_android.presentation.components.hideProgressDialog
import com.ml.shubham0204.facenet_android.presentation.components.showProgressDialog
import com.ml.shubham0204.facenet_android.presentation.theme.FaceNetAndroidTheme
import org.koin.androidx.compose.koinViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFaceScreen(onNavigateBack: (() -> Unit)) {
    FaceNetAndroidTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = "Add Faces", style = MaterialTheme.typography.headlineSmall)
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "Navigate Back",
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                val viewModel: AddFaceScreenViewModel = koinViewModel()
                ScreenUI(viewModel)
                ImageReadProgressDialog(viewModel, onNavigateBack)
            }
        }
    }
}

@Composable
private fun ScreenUI(viewModel: AddFaceScreenViewModel) {
    val context = LocalContext.current
    val pickVisualMediaLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia(),
        ) {
            viewModel.selectedImageURIs.value = it
        }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraCapture by remember { mutableStateOf(false) }
    var takePictureLauncher by remember {
        mutableStateOf<ManagedActivityResultLauncher<Uri, Boolean>?>(null)
    }
    fun launchCameraCapture() {
        val imageFile = createCameraImageFile(context)
        val imageUri = createCameraImageUri(context, imageFile)
        pendingCameraFile = imageFile
        pendingCameraUri = imageUri
        takePictureLauncher?.launch(imageUri)
    }
    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted && pendingCameraCapture) {
                launchCameraCapture()
            }
            pendingCameraCapture = false
        }
    takePictureLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val capturedFile = pendingCameraFile
            val capturedUri = pendingCameraUri
            pendingCameraFile = null
            pendingCameraUri = null

            if (success && capturedUri != null) {
                viewModel.selectedImageURIs.value = viewModel.selectedImageURIs.value + capturedUri
            } else {
                capturedFile?.delete()
            }
        }
    var personName by remember { viewModel.personNameState }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
    ) {
        Text(
            text = "Add a new user",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Pick a person name, choose clear photos from gallery or camera, then save them to the local database.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = personName,
            onValueChange = { personName = it },
            label = { Text(text = "Enter the person's name") },
            singleLine = true,
            supportingText = { Text(text = "Use the same name you want to see during recognition.") },
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                enabled = viewModel.personNameState.value.isNotEmpty(),
                modifier = Modifier.weight(1f),
                onClick = {
                    pickVisualMediaLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            ) {
                Icon(imageVector = Icons.Default.Photo, contentDescription = "Choose photos")
                Text(text = "Choose photos")
            }
            Button(
                enabled = viewModel.personNameState.value.isNotEmpty(),
                modifier = Modifier.weight(1f),
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        launchCameraCapture()
                    } else {
                        pendingCameraCapture = true
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
            ) {
                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Capture photo")
                Text(text = "Take photo")
            }
        }
        if (viewModel.selectedImageURIs.value.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { viewModel.addImages() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Add to database")
            }
        }
        if (viewModel.selectedImageURIs.value.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${viewModel.selectedImageURIs.value.size} photo(s) selected",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        ImagesGrid(viewModel)
    }
}

@Composable
private fun ImagesGrid(viewModel: AddFaceScreenViewModel) {
    val uris by remember { viewModel.selectedImageURIs }
    if (uris.isEmpty()) {
        Text(
            text = "Selected photos will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 24.dp),
        )
    } else {
        LazyVerticalGrid(columns = GridCells.Fixed(2)) {
            items(uris) { AsyncImage(model = it, contentDescription = null) }
        }
    }
}

private fun createCameraImageFile(context: android.content.Context): File {
    return File.createTempFile("face_capture_", ".jpg", context.cacheDir)
}

private fun createCameraImageUri(
    context: android.content.Context,
    imageFile: File,
): Uri {
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile,
    )
}

@Composable
private fun ImageReadProgressDialog(
    viewModel: AddFaceScreenViewModel,
    onNavigateBack: () -> Unit,
) {
    val isProcessing by remember { viewModel.isProcessingImages }
    val numImagesProcessed by remember { viewModel.numImagesProcessed }
    val context = LocalContext.current
    AppProgressDialog()
    if (isProcessing) {
        showProgressDialog()
    } else {
        if (numImagesProcessed > 0) {
            onNavigateBack()
            Toast.makeText(context, "Added to database", Toast.LENGTH_SHORT).show()
        }
        hideProgressDialog()
    }
}
