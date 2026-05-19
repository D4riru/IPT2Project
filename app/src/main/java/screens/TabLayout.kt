// TabLayout.kt
package screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController

sealed class TabScreen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : TabScreen("dashboard", "DASHBOARD", Icons.Default.Dashboard)
    object Stats : TabScreen("stats", "STATISTICS", Icons.Default.BarChart)
    object Profile : TabScreen("profile", "PROFILE", Icons.Default.Person)
}

@Composable
fun TabLayout(navController: NavController) {
    var selectedTab by remember { mutableStateOf<TabScreen>(TabScreen.Dashboard) }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                val tabs = listOf(TabScreen.Dashboard, TabScreen.Stats, TabScreen.Profile)
                tabs.forEach { tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF006156),
                            selectedTextColor = Color(0xFF006156),
                            indicatorColor = Color(0xFFE6F0EE),
                            unselectedIconColor = Color(0xFF9CA3AF),
                            unselectedTextColor = Color(0xFF9CA3AF)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                is TabScreen.Dashboard -> DashboardScreen(navController, onNavigateToProfile = { selectedTab = TabScreen.Profile })
                is TabScreen.Stats -> StatsScreen(navController)
                is TabScreen.Profile -> ProfileScreen(navController)
            }
        }
    }
}