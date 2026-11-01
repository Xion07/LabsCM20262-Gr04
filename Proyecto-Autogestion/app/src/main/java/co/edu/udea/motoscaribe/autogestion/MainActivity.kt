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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
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
import co.edu.udea.motoscaribe.autogestion.ui.components.ErrorState
import co.edu.udea.motoscaribe.autogestion.ui.motion.DURATION_ENTER
import co.edu.udea.motoscaribe.autogestion.ui.motion.DURATION_EXIT
import co.edu.udea.motoscaribe.autogestion.ui.motion.EmphasizedDecelerate
import co.edu.udea.motoscaribe.autogestion.ui.motion.IntroOverlay
import co.edu.udea.motoscaribe.autogestion.ui.screens.BalanceScreen
import co.edu.udea.motoscaribe.autogestion.ui.screens.ExtensionScreen
import co.edu.udea.motoscaribe.autogestion.ui.screens.FinesScreen
import co.edu.udea.motoscaribe.autogestion.ui.screens.HomeScreen
import co.edu.udea.motoscaribe.autogestion.ui.screens.LoginScreen
import co.edu.udea.motoscaribe.autogestion.ui.screens.MaintenanceScreen
import co.edu.udea.motoscaribe.autogestion.ui.theme.MotosCaribeTheme

private enum class Dest(val route: String, @StringRes val label: Int, val icon: ImageVector, val iconOutlined: ImageVector) {
    Home(Routes.HOME, R.string.nav_home, Icons.Rounded.Home, Icons.Outlined.Home),
    Balance(Routes.BALANCE, R.string.nav_balance, Icons.Rounded.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    Fines(Routes.FINES, R.string.nav_fines, Icons.Rounded.Gavel, Icons.Outlined.Gavel),
    Extension(Routes.EXTENSION, R.string.nav_extension, Icons.Rounded.EventRepeat, Icons.Outlined.EventRepeat),
    Km(Routes.KM, R.string.nav_km, Icons.Rounded.Speed, Icons.Outlined.Speed),
}

/** Ancho (dp) desde el que se usa un riel lateral en lugar de la barra inferior. */
private const val WIDE_DP = 600

class MainActivity : ComponentActivity() {
    private val viewModel: CreditViewModel by viewModels { CreditViewModel.factory(applicationContext) }

    /** Pantalla pedida por una notificacion; se consume al navegar. */
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        pendingRoute = intent.getStringExtra(Notifier.EXTRA_ROUTE)
        setContent {
            MotosCaribeTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                // La intro se muestra una sola vez por sesion (sobrevive a la rotacion).
                var introDone by rememberSaveable { mutableStateOf(false) }
                Box(Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = state.loggedIn,
                        transitionSpec = {
                            (fadeIn(tween(DURATION_ENTER, easing = EmphasizedDecelerate)) + scaleIn(tween(DURATION_ENTER), initialScale = 0.94f))
                                .togetherWith(fadeOut(tween(DURATION_EXIT)) + scaleOut(tween(DURATION_EXIT), targetScale = 1.04f))
                        },
                        label = "session",
                    ) { loggedIn ->
                        if (loggedIn) {
                            MainContent(state)
                        } else {
                            LoginScreen(state.loggingIn, state.loginError) { d, p -> viewModel.login(d, p) }
                        }
                    }
                    if (!introDone) IntroOverlay { introDone = true }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingRoute = intent.getStringExtra(Notifier.EXTRA_ROUTE)
    }

    @OptIn(ExperimentalMaterial3Api::class)
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
                NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Dest.entries.forEach { d ->
                        val selected = current == d.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { nav.goTab(d.route) },
                            icon = { TabIcon(d, selected) },
                            label = { Text(stringResource(d.label)) },
                        )
                    }
                }
            }
            Scaffold(
                modifier = Modifier.weight(1f),
                bottomBar = {
                    if (!wide) {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 0.dp) {
                            Dest.entries.forEach { d ->
                                val selected = current == d.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { nav.goTab(d.route) },
                                    icon = { TabIcon(d, selected) },
                                    label = { Text(stringResource(d.label)) },
                                    alwaysShowLabel = true,
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    ),
                                )
                            }
                        }
                    }
                },
            ) { padding ->
                if (state.loadError && state.contract == null) {
                    ErrorState(
                        stringResource(R.string.load_error), stringResource(R.string.retry), viewModel::load,
                    )
                    return@Scaffold
                }
                // Deslizar hacia abajo recarga los datos en cualquier pestana.
                PullToRefreshBox(
                    isRefreshing = state.refreshing && state.contract != null,
                    onRefresh = viewModel::load,
                    modifier = Modifier.padding(padding),
                ) {
                    NavHost(
                        nav, Routes.HOME, Modifier.fillMaxSize(),
                        enterTransition = {
                            fadeIn(tween(DURATION_ENTER, easing = EmphasizedDecelerate)) +
                                slideInVertically(tween(DURATION_ENTER, easing = EmphasizedDecelerate)) { it / 18 } +
                                scaleIn(tween(DURATION_ENTER, easing = EmphasizedDecelerate), initialScale = 0.985f)
                        },
                        exitTransition = { fadeOut(tween(DURATION_EXIT)) },
                        popEnterTransition = { fadeIn(tween(DURATION_ENTER, easing = EmphasizedDecelerate)) },
                        popExitTransition = { fadeOut(tween(DURATION_EXIT)) },
                    ) {
                        composable(Routes.HOME) {
                            HomeScreen(
                                state,
                                onRequestExtension = { nav.goTab(Routes.EXTENSION) },
                                onLogout = viewModel::logout,
                            )
                        }
                        composable(Routes.BALANCE) { BalanceScreen(state) }
                        composable(Routes.FINES) { FinesScreen(state) }
                        composable(Routes.EXTENSION) {
                            ExtensionScreen(state, viewModel::submitExtension, viewModel::submitConsumed)
                        }
                        composable(Routes.KM) { MaintenanceScreen(state) }
                    }
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

/** Icono de pestana: relleno y con un pequeno rebote al seleccionarse, contorno si no. */
@Composable
private fun TabIcon(d: Dest, selected: Boolean) {
    val scale by animateFloatAsState(
        if (selected) 1.12f else 1f,
        spring(dampingRatio = 0.55f, stiffness = 600f),
        label = "tabScale",
    )
    Icon(
        if (selected) d.icon else d.iconOutlined,
        contentDescription = null,
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
    )
}

/** Cambia de pestana conservando lo escrito en cada una. */
private fun NavHostController.goTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
