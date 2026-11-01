package co.edu.udea.motoscaribe.autogestion

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home

import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import co.edu.udea.motoscaribe.autogestion.ui.screens.BalanceScreen
import co.edu.udea.motoscaribe.autogestion.ui.screens.ExtensionScreen
import co.edu.udea.motoscaribe.autogestion.ui.screens.FinesScreen
import co.edu.udea.motoscaribe.autogestion.ui.screens.HomeScreen
import co.edu.udea.motoscaribe.autogestion.ui.screens.LoginScreen
import co.edu.udea.motoscaribe.autogestion.ui.screens.MaintenanceScreen
import co.edu.udea.motoscaribe.autogestion.notifications.AlertScheduler
import co.edu.udea.motoscaribe.autogestion.ui.theme.MotosCaribeTheme

private enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Inicio", Icons.Filled.Home),
    Balance("balance", "Saldo", Icons.AutoMirrored.Filled.List),
    Fines("fines", "Multas", Icons.Filled.Warning),
    Extension("extension", "Plazo", Icons.Filled.DateRange),
    Maintenance("maintenance", "Mant.", Icons.Filled.Build),
}

class MainActivity : ComponentActivity() {
    private val viewModel: CreditViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MotosCaribeTheme {
                val nav = rememberNavController()
                val state by viewModel.state.collectAsStateWithLifecycle()
                val current = nav.currentBackStackEntryAsState().value?.destination?.route
                val context = LocalContext.current
                val askNotifications = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { AlertScheduler.start(context) }

                if (!state.loggedIn) {
                    LoginScreen(viewModel::login)
                    return@MotosCaribeTheme
                }
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= 33) {
                        askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        AlertScheduler.start(context)
                    }
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            Dest.entries.forEach { d ->
                                NavigationBarItem(
                                    selected = current == d.route,
                                    onClick = {
                                        nav.navigate(d.route) {
                                            popUpTo(Dest.Home.route)
                                            launchSingleTop = true
                                        }
                                    },
                                    icon = { Icon(d.icon, contentDescription = d.label) },
                                    label = { Text(d.label) },
                                )
                            }
                        }
                    },
                ) { padding ->
                    NavHost(nav, Dest.Home.route, Modifier.padding(padding)) {
                        composable(Dest.Home.route) { HomeScreen(state) }
                        composable(Dest.Balance.route) { BalanceScreen(state) }
                        composable(Dest.Fines.route) { FinesScreen(state) }
                        composable(Dest.Extension.route) {
                            ExtensionScreen(state, viewModel::submitExtension)
                        }
                        composable(Dest.Maintenance.route) { MaintenanceScreen(state) }
                    }
                }
            }
        }
    }
}
