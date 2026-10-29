package com.geo.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt

// Map opens here when there is no saved fence and no location permission
private val DEFAULT_CENTER = LatLng(20.59, 78.96)

// Below ~100m, normal GPS error causes false exits
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

    val title = (groupState as? UiState.Success)?.data?.groupName.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Geofence: $title") },
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
                is UiState.Success -> FenceEditor(
                    savedFence = state.data.fence,
                    saveState = saveState,
                    onSave = viewModel::saveFence
                )

                is UiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
                else -> CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun FenceEditor(
    savedFence: Fence?,
    saveState: UiState<Unit>,
    onSave: (Fence) -> Unit
) {
    val context = LocalContext.current

    // Screen-local draft; it only goes to Firestore when Save is pressed
    var center by remember { mutableStateOf(savedFence?.let { LatLng(it.lat, it.lng) }) }
    var radius by remember { mutableFloatStateOf(savedFence?.radiusM?.toFloat() ?: 300f) }

    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center ?: DEFAULT_CENTER, if (center != null) 15f else 4f)
    }

    var hasLocation by remember { mutableStateOf(context.hasLocationPermission()) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> hasLocation = result.values.any { it } }

    LaunchedEffect(Unit) {
        if (!hasLocation) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // New fence: owner's current location becomes the default center (tap to move it).
    // Editing: stay on the saved fence.
    LaunchedEffect(hasLocation) {
        if (hasLocation && savedFence == null) {
            val here = currentLocation(context) ?: return@LaunchedEffect
            val point = LatLng(here.latitude, here.longitude)
            // Don't overwrite a point the owner already tapped while location was loading
            if (center == null) center = point
            camera.animate(CameraUpdateFactory.newLatLngZoom(point, 15f))
        }
    }

    Column(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            cameraPositionState = camera,
            properties = MapProperties(isMyLocationEnabled = hasLocation),
            onMapClick = { center = it }
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

// Only called after hasLocationPermission() is true
@SuppressLint("MissingPermission")
private suspend fun currentLocation(context: Context): Location? = runCatching {
    LocationServices.getFusedLocationProviderClient(context)
        .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
        .await()
}.getOrNull()
