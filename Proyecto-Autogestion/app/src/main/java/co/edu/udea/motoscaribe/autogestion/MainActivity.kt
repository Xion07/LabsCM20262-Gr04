package co.edu.udea.motoscaribe.autogestion

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import co.edu.udea.motoscaribe.autogestion.notifications.AlertScheduler
import co.edu.udea.motoscaribe.autogestion.notifications.Notifier
import co.edu.udea.motoscaribe.autogestion.ui.screens.BalanceScreen
import co.edu.udea.motoscaribe.autogestion.ui.screens.ExtensionScreen
import co.edu.udea.motoscaribe.autogestion.ui.screens.FinesScreen
import co.edu.udea.motoscaribe.autogestion.ui.screens.HomeScreen
import co.edu.udea.motoscaribe.autogestion.ui.screens.LoginScreen
import co.edu.udea.motoscaribe.autogestion.ui.screens.MaintenanceScreen
import co.edu.udea.motoscaribe.autogestion.ui.theme.MotosCaribeTheme

private enum class Dest(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    Home(Routes.HOME, R.string.nav_home, Icons.Filled.Home),
    Balance(Routes.BALANCE, R.string.nav_balance, Icons.AutoMirrored.Filled.List),
    Fines(Routes.FINES, R.string.nav_fines, Icons.Filled.Warning),
    Extension(Routes.EXTENSION, R.string.nav_extension, Icons.Filled.DateRange),
    Km(Routes.KM, R.string.nav_km, Icons.Filled.Place),
}

/** Ancho (dp) desde el que se usa un riel lateral en lugar de la barra inferior. */
private const val WIDE_DP = 600

class MainActivity : ComponentActivity() {
    private val viewModel: CreditViewModel by viewModels()

    /** Pantalla pedida por una notificacion; se consume al navegar. */
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        pendingRoute = intent.getStringExtra(Notifier.EXTRA_ROUTE)
        setContent {
            MotosCaribeTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                if (state.loggedIn) MainContent(state) else LoginScreen { d, p -> viewModel.login(d, p) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingRoute = intent.getStringExtra(Notifier.EXTRA_ROUTE)
    }

    @Composable
    private fun MainContent(state: UiState) {
        val context = LocalContext.current
        val nav = rememberNavController()
        val current = nav.currentBackStackEntryAsState().value?.destination?.route
        val wide = LocalConfiguration.current.screenWidthDp >= WIDE_DP

        // Permiso de notificaciones: primero se explica para que sirve.
        var asked by rememberSaveable { mutableStateOf(false) }
        var showRationale by rememberSaveable { mutableStateOf(false) }
        val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            AlertScheduler.start(context)
        }
        LaunchedEffect(Unit) {
            if (asked) return@LaunchedEffect
            asked = true
            val granted = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) AlertScheduler.start(context) else showRationale = true
        }

        // Abrir la pantalla indicada por la notificacion tocada.
        LaunchedEffect(pendingRoute) {
            pendingRoute?.let {
                nav.goTab(it)
                pendingRoute = null
            }
        }

        Row(Modifier.fillMaxSize()) {
            if (wide) {
                NavigationRail {
                    Dest.entries.forEach { d ->
                        NavigationRailItem(
                            selected = current == d.route,
                            onClick = { nav.goTab(d.route) },
                            icon = { Icon(d.icon, contentDescription = null) },
                            label = { Text(stringResource(d.label)) },
                        )
                    }
                }
            }
            Scaffold(
                modifier = Modifier.weight(1f),
                bottomBar = {
                    if (!wide) {
                        NavigationBar {
                            Dest.entries.forEach { d ->
                                NavigationBarItem(
                                    selected = current == d.route,
                                    onClick = { nav.goTab(d.route) },
                                    icon = { Icon(d.icon, contentDescription = null) },
                                    label = { Text(stringResource(d.label)) },
                                    alwaysShowLabel = true,
                                )
                            }
                        }
                    }
                },
            ) { padding ->
                NavHost(nav, Routes.HOME, Modifier.padding(padding)) {
                    composable(Routes.HOME) {
                        HomeScreen(
                            state,
                            onRequestExtension = { nav.goTab(Routes.EXTENSION) },
                            onLogout = {
                                AlertScheduler.cancel(context)
                                viewModel.logout()
                            },
                        )
                    }
                    composable(Routes.BALANCE) { BalanceScreen(state) }
                    composable(Routes.FINES) { FinesScreen(state) }
                    composable(Routes.EXTENSION) { ExtensionScreen(state, viewModel::submitExtension) }
                    composable(Routes.KM) { MaintenanceScreen(state) }
                }
            }
        }

        if (showRationale) {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.perm_title)) },
                text = { Text(stringResource(R.string.perm_text)) },
                confirmButton = {
                    TextButton({
                        showRationale = false
                        askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }) { Text(stringResource(R.string.perm_yes)) }
                },
                dismissButton = {
                    TextButton({
                        showRationale = false
                        AlertScheduler.start(context)
                    }) { Text(stringResource(R.string.perm_no)) }
                },
            )
        }
    }
}

/** Cambia de pestana conservando lo escrito en cada una. */
private fun NavHostController.goTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
