package com.ml.shubham0204.facenet_android.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ml.shubham0204.facenet_android.presentation.theme.FaceNetAndroidTheme
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartDetection: () -> Unit,
    onAddUser: () -> Unit,
    onOpenUsers: () -> Unit,
    ) {
        val viewModel: HomeScreenViewModel = koinViewModel()
        val users by viewModel.personFlow.collectAsState(emptyList())
        val startupRoute by viewModel.startupRouteState

        LaunchedEffect(users.size, startupRoute) {
            if (users.isNotEmpty() && startupRoute == StartupDestination.DETECT) {
                onStartDetection()
            }
        }

        FaceNetAndroidTheme {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(title = { Text(text = "Face recognition") })
                },
            ) { innerPadding ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                            .background(MaterialTheme.colorScheme.background),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(32.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = if (users.isEmpty()) "Welcome" else "Ready to recognize",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (users.isEmpty()) {
                                        "Add the first user to build your local face gallery."
                                    } else {
                                        "${users.size} saved user(s) are ready for live recognition."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Button(onClick = onAddUser, modifier = Modifier.weight(1f)) {
                                        Text(text = if (users.isEmpty()) "Add first user" else "Add another user")
                                    }
                                    Button(
                                        onClick = onStartDetection,
                                        modifier = Modifier.weight(1f),
                                        enabled = users.isNotEmpty(),
                                    ) {
                                        Text(text = "Recognize existing users")
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = onOpenUsers,
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = users.isNotEmpty(),
                                ) {
                                    Text(text = "Open user list")
                                }
                            }
                        }

                    }
                }
            }
        }
    }