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
 * Тонкий модуль: MainActivity знает про существование экранов feature-модулей
 * (SelfTestScreen/ConnectScreen), но не про то, как они устроены внутри —
 * только маршруты. Deep link'и (vpnclient://selftest, vpnclient://connect)
 * позволяют попасть на конкретный экран, минуя навигацию внутри приложения —
 * так же, как реальные фичи открывают друг друга по ссылке, не зная внутренностей.
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
