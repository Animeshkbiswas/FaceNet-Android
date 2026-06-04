package com.ml.shubham0204.facenet_android.presentation.screens.face_list

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ml.shubham0204.facenet_android.data.PersonRecord
import com.ml.shubham0204.facenet_android.presentation.components.AppAlertDialog
import com.ml.shubham0204.facenet_android.presentation.components.createAlertDialog
import com.ml.shubham0204.facenet_android.presentation.theme.FaceNetAndroidTheme
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceListScreen(
    onNavigateBack: (() -> Unit),
    onAddFaceClick: (() -> Unit),
    onNavigateHome: (() -> Unit),
) {
    FaceNetAndroidTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = "Face List", style = MaterialTheme.typography.headlineSmall)
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "Navigate Back",
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateHome) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Go Home",
                            )
                        }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onAddFaceClick) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add a new face")
                }
            },
        ) { innerPadding ->
            val viewModel: FaceListScreenViewModel = koinViewModel()
            Column(modifier = Modifier.padding(innerPadding)) {
                ScreenUI(viewModel)
                AppAlertDialog()
            }
        }
    }
}

@Composable
private fun ScreenUI(viewModel: FaceListScreenViewModel) {
    val faces by viewModel.personFlow.collectAsState(emptyList())
    if (faces.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "No users added yet",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Use the add button to enroll the first person in the database.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            items(faces) { FaceListItem(it) { viewModel.removeFace(it.personID) } }
        }
    }
}

@Composable
private fun FaceListItem(
    personRecord: PersonRecord,
    onRemoveFaceClick: (() -> Unit),
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = personRecord.personName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Text(
                    text = personRecord.personName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = DateUtils.getRelativeTimeSpanString(personRecord.addTime).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = {
                    createAlertDialog(
                        dialogTitle = "Remove person",
                        dialogText =
                            "Are you sure you want to remove this person from the database? Their face will no longer be detected in realtime.",
                        dialogPositiveButtonText = "Remove",
                        onPositiveButtonClick = onRemoveFaceClick,
                        dialogNegativeButtonText = "Cancel",
                        onNegativeButtonClick = {},
                    )
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Remove face",
                )
            }
        }
    }
}
