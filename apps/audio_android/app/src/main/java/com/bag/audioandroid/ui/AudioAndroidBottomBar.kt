package com.bag.audioandroid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.ui.model.AppTab
import com.bag.audioandroid.ui.state.AudioAppUiState

private val AudioAndroidNavigationTabs = listOf(AppTab.Config, AppTab.Audio, AppTab.Library)

@Composable
internal fun AudioAndroidBottomBar(
    uiState: AudioAppUiState,
    navigationBarColors: NavigationBarItemColors,
    onTabSelected: (AppTab) -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(26.dp),
        color = playerDockContainerColor(uiState),
        tonalElevation = 0.dp,
        shadowElevation = 6.dp,
    ) {
        NavigationBar(
            modifier = Modifier.height(60.dp),
            windowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = playerDockContainerColor(uiState),
            // Force 0.dp tonal elevation to match the MiniPlayer and ensure pure color.
            tonalElevation = 0.dp,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            AudioAndroidNavigationTabs.forEach { tab ->
                val selected = tab == uiState.selectedTab
                AudioAndroidBottomTabItem(
                    selected = selected,
                    onClick = { onTabSelected(tab) },
                    tab = tab,
                    colors = navigationBarColors,
                )
            }
        }
    }
}

@Composable
private fun RowScope.AudioAndroidBottomTabItem(
    selected: Boolean,
    tab: AppTab,
    colors: NavigationBarItemColors,
    onClick: () -> Unit,
) {
    val label = stringResource(tab.labelResId)
    val selectedIconColor = colors.selectedIconColor
    val selectedTextColor = colors.selectedTextColor
    val selectedIndicatorColor = colors.selectedIndicatorColor
    val unselectedIconColor = colors.unselectedIconColor
    val unselectedTextColor = colors.unselectedTextColor
    val iconTint = if (selected) selectedIconColor else unselectedIconColor
    val textTint = if (selected) selectedTextColor else unselectedTextColor
    Row(
        modifier =
            Modifier
                .weight(1f)
                .height(60.dp)
                .clickable(
                    onClick = onClick,
                    role = Role.Tab,
                ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(width = 32.dp, height = 24.dp)
                        .background(
                            color = if (selected) selectedIndicatorColor else Color.Transparent,
                            shape = RoundedCornerShape(13.dp),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = label,
                    tint = iconTint,
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = label,
                color = textTint,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
