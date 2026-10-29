package com.geo.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geo.commontes.UiState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds


private val DEFAULT_CENTER = LatLng(20.59, 78.96)


private const val MIN_RADIUS_M = 100f
private const val MAX_RADIUS_M = 2000f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FenceScreen(
    onBack: () -> Unit,
    viewModel: FenceViewModel = hiltViewModel()
) {
    val groupState by viewModel.group.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()

    LaunchedEffect(saveState) {
        if (saveState is UiState.Success) onBack()
    }

    val group = (groupState as? UiState.Success)?.data

    val readOnly = group != null && group.ownerId != viewModel.myUid

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text((if (readOnly) "Geofence: " else "Set Geofence: ") + group?.groupName.orEmpty())
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (val state = groupState) {
                is UiState.Success -> if (readOnly && state.data.fence == null) {
                    Text("The owner hasn't set a geofence yet")
                } else {
                    FenceEditor(
                        savedFence = state.data.fence,
                        readOnly = readOnly,
                        saveState = saveState,
                        onSave = viewModel::saveFence
                    )
                }

                is UiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
                else -> CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun FenceEditor(
    savedFence: Fence?,
    readOnly: Boolean,
    saveState: UiState<Unit>,
    onSave: (Fence) -> Unit
) {
    val context = LocalContext.current
    var center by remember { mutableStateOf(savedFence?.let { LatLng(it.lat, it.lng) }) }
    var radius by remember { mutableFloatStateOf(savedFence?.radiusM?.toFloat() ?: 300f) }

    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center ?: DEFAULT_CENTER, if (center != null) 15f else 4f)
    }

    var locating by remember { mutableStateOf(savedFence == null && !readOnly) }
    var locationError by remember { mutableStateOf<String?>(null) }

    var hasLocation by remember { mutableStateOf(context.hasLocationPermission()) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasLocation = result.values.any { it }
        if (!hasLocation) locating = false
    }


    LaunchedEffect(Unit) {
        if (!hasLocation && !readOnly) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }


    LaunchedEffect(hasLocation) {
        if (!hasLocation || savedFence != null || readOnly) return@LaunchedEffect
        val here = currentLocation(context)
        if (here != null) {
            val point = LatLng(here.latitude, here.longitude)
            center = point
            camera.position = CameraPosition.fromLatLngZoom(point, 15f)
        } else {
            locationError = "Couldn't find your location. Turn on location, or tap the map to choose."
        }
        locating = false
    }

    Column(Modifier.fillMaxSize()) {
        if (locating) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Text("Finding your location…", Modifier.padding(top = 12.dp))
            }
        } else GoogleMap(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            cameraPositionState = camera,
            properties = MapProperties(isMyLocationEnabled = hasLocation),
            onMapClick = if (readOnly) null else { point -> center = point }
        ) {
            center?.let { point ->
                Marker(state = rememberUpdatedMarkerState(position = point))
                Circle(
                    center = point,
                    radius = radius.toDouble(),
                    strokeColor = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4f,
                    fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            }
        }

        if (readOnly) {
            Text(
                "Radius: ${radius.roundToInt()} m · set by the group owner",
                Modifier.padding(16.dp)
            )
            return@Column
        }

        Column(Modifier.padding(16.dp)) {
            Text(
                if (center == null) "Tap on the map to choose the center"
                else "Radius: ${radius.roundToInt()} m"
            )
            Slider(
                value = radius,
                onValueChange = { radius = it },
                valueRange = MIN_RADIUS_M..MAX_RADIUS_M
            )
            locationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (saveState is UiState.Error) {
                Text(saveState.message, color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = {
                    center?.let { onSave(Fence(it.latitude, it.longitude, radius.toDouble())) }
                },
                enabled = center != null && saveState !is UiState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (saveState is UiState.Loading) "Saving..." else "Save Fence")
            }
        }
    }
}

private fun Context.hasLocationPermission() =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED


@SuppressLint("MissingPermission")

private suspend fun currentLocation(context: Context): Location? = runCatching {
    val client = LocationServices.getFusedLocationProviderClient(context)
    withTimeoutOrNull(10_000.milliseconds) {
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
    } ?: client.lastLocation.await()
}.getOrNull()
