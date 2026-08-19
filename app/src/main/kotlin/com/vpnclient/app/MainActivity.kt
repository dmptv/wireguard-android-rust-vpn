package com.vpnclient.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.vpnclient.feature.connect.ConnectScreen
import com.vpnclient.feature.selftest.SelfTestScreen

/**
 * Thin module: MainActivity knows the feature-module screens exist
 * (SelfTestScreen/ConnectScreen), but nothing about how they're built
 * internally — only their routes. Deep links (vpnclient://selftest,
 * vpnclient://connect) reach a specific screen directly, bypassing in-app
 * navigation — the same way real features open one another by link without
 * knowing each other's internals.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val navController = rememberNavController()

                Row(Modifier.padding(16.dp)) {
                    Button(onClick = { navController.navigate("selftest") }) {
                        Text("Self-test")
                    }
                    Button(
                        onClick = { navController.navigate("connect") },
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Text("Подключение")
                    }
                }

                NavHost(navController = navController, startDestination = "selftest") {
                    composable(
                        route = "selftest",
                        deepLinks = listOf(navDeepLink { uriPattern = "vpnclient://selftest" }),
                    ) { SelfTestScreen() }

                    composable(
                        route = "connect",
                        deepLinks = listOf(navDeepLink { uriPattern = "vpnclient://connect" }),
                    ) { ConnectScreen() }
                }
            }
        }
    }
}
