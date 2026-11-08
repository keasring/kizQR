package com.kizqr.app.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.kizqr.app.R
import com.kizqr.app.ui.create.CreateScreen
import com.kizqr.app.ui.history.HistoryScreen
import com.kizqr.app.ui.scan.ScanScreen
import com.kizqr.app.ui.settings.SettingsScreen

private enum class Destination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    CREATE("create", R.string.nav_create, Icons.Rounded.QrCode),
    SCAN("scan", R.string.nav_scan, Icons.Rounded.QrCodeScanner),
    HISTORY("history", R.string.nav_history, Icons.Rounded.History),
    SETTINGS("settings", R.string.nav_settings, Icons.Rounded.Settings),
}

@Composable
fun KizQrApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                for (dest in Destination.entries) {
                    NavigationBarItem(
                        selected = currentRoute == dest.route,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = null) },
                        label = { Text(stringResource(dest.labelRes)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.CREATE.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(
                route = "create?payload={payload}",
                arguments = listOf(
                    navArgument("payload") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { entry ->
                CreateScreen(
                    prefilledPayload = entry.arguments?.getString("payload"),
                )
            }
            composable(route = Destination.SCAN.route) {
                ScanScreen(
                    onCreateQr = { raw ->
                        navController.navigate("create?payload=${Uri.encode(raw)}")
                    },
                )
            }
            composable(route = Destination.HISTORY.route) {
                HistoryScreen()
            }
            composable(route = Destination.SETTINGS.route) {
                SettingsScreen()
            }
        }
    }
}
