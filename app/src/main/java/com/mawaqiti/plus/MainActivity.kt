package com.mawaqiti.plus

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mawaqiti.plus.data.AppLanguage
import com.mawaqiti.plus.data.AppSettings
import com.mawaqiti.plus.ui.components.isArabicLocale
import com.mawaqiti.plus.ui.screens.CitySelectionDialog
import com.mawaqiti.plus.ui.screens.PrayerScreen
import com.mawaqiti.plus.ui.screens.QiblaScreen
import com.mawaqiti.plus.ui.screens.SettingsScreen
import com.mawaqiti.plus.ui.theme.MawaqitiTheme

class MainActivity : AppCompatActivity() {
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
    val storedSettings by viewModel.settingsState.collectAsState()
    val settings = storedSettings ?: AppSettings()
    val city by viewModel.city.collectAsState()
    val day by viewModel.day.collectAsState()
    val today by viewModel.today.collectAsState()
    val isArabic = isArabicLocale()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showCityPicker by rememberSaveable { mutableStateOf(false) }

    // First launch: pin the app to Arabic explicitly, then persist through
    // AppCompat's autoStoreLocales + our DataStore flag. Using the exact
    // emitted object avoids overwriting a stored English choice.
    LaunchedEffect(storedSettings) {
        val value = storedSettings
        if (value != null && !value.languageInitialized) {
            viewModel.setLanguage(AppLanguage.ARABIC)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
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
                    city = city,
                    day = day,
                    date = today,
                    isArabic = isArabic,
                    onChooseCity = { showCityPicker = true }
                )

                1 -> QiblaScreen(city = city, isArabic = isArabic)

                else -> SettingsScreen(
                    settings = settings,
                    city = city,
                    isArabic = isArabic,
                    onOpenCityPicker = { showCityPicker = true },
                    onSelectLanguage = viewModel::setLanguage,
                    onSummerTimeChange = viewModel::setSummerTime
                )
            }
        }
    }

    if (showCityPicker) {
        CitySelectionDialog(
            cities = viewModel.cities,
            selectedCity = city,
            isArabic = isArabic,
            onDismiss = { showCityPicker = false },
            onConfirm = viewModel::selectCity
        )
    }
}
