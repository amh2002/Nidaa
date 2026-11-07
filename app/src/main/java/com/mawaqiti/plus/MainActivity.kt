package com.mawaqiti.plus

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mawaqiti.plus.data.LocationHelper
import com.mawaqiti.plus.ui.screens.PrayerScreen
import com.mawaqiti.plus.ui.screens.QiblaScreen
import com.mawaqiti.plus.ui.screens.SettingsScreen
import com.mawaqiti.plus.ui.theme.MawaqitiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MawaqitiTheme {
                MawaqitiApp()
            }
        }
    }
}

@Composable
fun MawaqitiApp(viewModel: MawaqitiViewModel = viewModel()) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val loaded by viewModel.loaded.collectAsState()
    val day by viewModel.day.collectAsState()
    val hasError by viewModel.hasError.collectAsState()
    val locating by viewModel.locating.collectAsState()
    val messageRes by viewModel.messageRes.collectAsState()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showRationale by rememberSaveable { mutableStateOf(false) }
    var askedThisRun by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.completeOnboarding()
        if (granted) viewModel.refreshLocation() else viewModel.notifyPermissionDenied()
    }

    LaunchedEffect(loaded, settings.onboardingDone) {
        if (loaded && !settings.onboardingDone && !askedThisRun) {
            askedThisRun = true
            showRationale = true
        }
    }

    val requestLocation: () -> Unit = {
        if (LocationHelper.hasPermission(context)) {
            viewModel.refreshLocation()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val messageResValue = messageRes
    val message = if (messageResValue != null) stringResource(messageResValue) else null
    LaunchedEffect(messageResValue) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = {
                showRationale = false
                viewModel.completeOnboarding()
            },
            title = { Text(stringResource(R.string.permission_title)) },
            text = { Text(stringResource(R.string.permission_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }) { Text(stringResource(R.string.allow)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRationale = false
                    viewModel.completeOnboarding()
                }) { Text(stringResource(R.string.later)) }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.AccessTime, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_prayer)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Explore, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_qibla)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_settings)) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                0 -> PrayerScreen(
                    settings = settings,
                    day = day,
                    hasError = hasError,
                    onRefreshLocation = requestLocation
                )

                1 -> QiblaScreen(
                    latitude = settings.latitude,
                    longitude = settings.longitude
                )

                else -> SettingsScreen(
                    settings = settings,
                    cities = viewModel.cities,
                    locating = locating,
                    onSelectMethod = { viewModel.setMethod(it) },
                    onSelectMadhab = { viewModel.setMadhab(it) },
                    onSelectHighLatitude = { viewModel.setHighLatitudeRule(it) },
                    onSaveCustomAngles = { fajr, isha -> viewModel.setCustomAngles(fajr, isha) },
                    onRefreshLocation = requestLocation,
                    onSelectCity = { viewModel.selectCity(it) },
                    onSaveManualLocation = { lat, lon -> viewModel.setManualLocation(lat, lon) }
                )
            }
        }
    }
}
